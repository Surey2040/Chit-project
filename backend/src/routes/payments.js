const express = require('express');
const router = express.Router();
const { Payment, Installment, ChitGroup, User, sequelize } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');
const crypto = require('crypto');
const { generateReceiptPdf } = require('../utils/pdfGenerator');
const { sendWhatsAppMessage } = require('../services/notificationService');

router.use(authenticateToken);

const MAX_PAGE_SIZE = 200;

// GET /payments
router.get('/', async (req, res) => {
    try {
        const { memberId, groupId, status } = req.query;
        const whereClause = {};

        if (memberId) whereClause.memberId = memberId;
        if (status) whereClause.status = status;

        const limit = Math.min(parseInt(req.query.limit, 10) || 50, MAX_PAGE_SIZE);
        const offset = Math.max(parseInt(req.query.offset, 10) || 0, 0);

        const includeClause = [
            {
                model: Installment,
                attributes: ['id', 'installmentNo', 'groupId'],
                where: groupId ? { groupId } : undefined,
                required: !!groupId
            }
        ];

        const payments = await Payment.findAll({
            where: whereClause,
            include: includeClause,
            limit,
            offset,
            order: [['createdAt', 'DESC']]
        });
        res.json(payments);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /payments (agents can record payments) - creates the due row on first collection,
// or tops up an existing DUE/PARTIAL row for later partial/final installments.
router.post('/', requireRole(['ADMIN', 'AGENT']), async (req, res) => {
    const { installmentId, memberId, amountPaid, mode, referenceNo } = req.body;

    if (!Number.isInteger(amountPaid) || amountPaid <= 0) {
        return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'amountPaid must be a positive integer (paise)', field: 'amountPaid' });
    }
    if (!['CASH', 'UPI', 'BANK_TRANSFER'].includes(mode)) {
        return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Invalid payment mode', field: 'mode' });
    }
    if ((mode === 'UPI' || mode === 'BANK_TRANSFER') && !referenceNo) {
        return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Reference/UTR number is required for UPI and bank transfers', field: 'referenceNo' });
    }

    const t = await sequelize.transaction();
    try {
        const installment = await Installment.findByPk(installmentId, { include: [ChitGroup], transaction: t });
        if (!installment) {
            await t.rollback();
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        }

        const existingPayment = await Payment.findOne({
            where: { installmentId, memberId },
            transaction: t,
            lock: t.LOCK.UPDATE
        });

        let payment;
        let receiptNo;

        if (existingPayment) {
            if (existingPayment.status === 'PAID') {
                await t.rollback();
                return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'This installment is already fully paid for this member' });
            }

            const remainingDue = existingPayment.amountDue - existingPayment.amountPaid;
            if (amountPaid > remainingDue) {
                await t.rollback();
                return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: `Overpayment not allowed. Remaining due is ${remainingDue} paise`, field: 'amountPaid' });
            }

            receiptNo = 'RCT-' + Date.now() + '-' + crypto.randomBytes(2).toString('hex').toUpperCase();
            existingPayment.amountPaid += amountPaid;
            existingPayment.status = existingPayment.amountPaid >= existingPayment.amountDue ? 'PAID' : 'PARTIAL';
            existingPayment.mode = mode;
            existingPayment.referenceNo = referenceNo;
            existingPayment.receiptNo = receiptNo;
            existingPayment.collectedBy = req.user.id;
            existingPayment.paidAt = new Date();
            payment = await existingPayment.save({ transaction: t });
        } else {
            // No pre-generated due row (e.g. first installment before any auction has run yet).
            const discount = installment.kasaruAmount || 0;
            const amountDue = installment.baseAmount - discount;

            if (amountPaid > amountDue) {
                await t.rollback();
                return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Overpayment not allowed', field: 'amountPaid' });
            }

            receiptNo = 'RCT-' + Date.now() + '-' + crypto.randomBytes(2).toString('hex').toUpperCase();
            payment = await Payment.create({
                installmentId,
                memberId,
                amountPaid,
                amountDue,
                status: amountPaid >= amountDue ? 'PAID' : 'PARTIAL',
                mode,
                referenceNo,
                collectedBy: req.user.id,
                receiptNo,
                paidAt: new Date()
            }, { transaction: t });
        }

        await t.commit();

        // Payment is already durably committed at this point. Receipt PDF generation and
        // WhatsApp delivery are best-effort side effects - their failure must never be
        // reported back to the client as a failed payment.
        let pdfUrl = null;
        let notificationWarning = null;
        try {
            const member = await User.findByPk(memberId);
            pdfUrl = await generateReceiptPdf(payment, member);
            const messageText = `Dear ${member.name},\nWe have received your payment of Rs. ${amountPaid / 100} towards Chit Installment #${installment.installmentNo}.\nReceipt No: ${receiptNo}\n\nThank you,\nJothi Vel Chits`;
            const fullPdfUrl = pdfUrl ? `${process.env.PUBLIC_BASE_URL || ''}${pdfUrl}` : null;
            await sendWhatsAppMessage(member.phone, messageText, fullPdfUrl);
        } catch (sideEffectError) {
            notificationWarning = 'Payment was saved, but the receipt/notification could not be generated: ' + sideEffectError.message;
        }

        res.status(201).json({ ...payment.toJSON(), pdfUrl, ...(notificationWarning ? { warning: notificationWarning } : {}) });
    } catch (error) {
        await t.rollback();
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(409).json({ errorCode: 'CONFLICT', message: 'A payment for this member and installment was just recorded by someone else. Please retry.' });
        }
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /payments/:id/receipt
router.get('/:id/receipt', async (req, res) => {
    try {
        const payment = await Payment.findByPk(req.params.id);
        if (!payment) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Payment not found' });
        
        const member = await User.findByPk(payment.memberId);
        const pdfUrl = await generateReceiptPdf(payment, member);

        res.json({ receiptNo: payment.receiptNo, pdfUrl });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
