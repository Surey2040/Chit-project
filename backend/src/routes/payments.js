const express = require('express');
const router = express.Router();
const { Payment, Installment, ChitGroup, User } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');
const crypto = require('crypto');

router.use(authenticateToken);

// GET /payments
router.get('/', async (req, res) => {
    try {
        const { memberId, groupId, status } = req.query;
        const whereClause = {};
        
        if (memberId) whereClause.memberId = memberId;
        if (status) whereClause.status = status;
        
        // For groupId filtering we would ideally join with Installment.
        // Doing basic for now
        
        const payments = await Payment.findAll({ where: whereClause });
        res.json(payments);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /payments (agents can record payments)
router.post('/', requireRole(['ADMIN', 'AGENT']), async (req, res) => {
    try {
        const { installmentId, memberId, amountPaid, mode, referenceNo } = req.body;

        const installment = await Installment.findByPk(installmentId, { include: [ChitGroup] });
        if (!installment) {
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        }

        // Calculate amountDue based on baseAmount and kasaruPerMember
        let discount = 0;
        if (installment.kasaruAmount) {
             discount = installment.kasaruAmount;
        }
        const amountDue = installment.baseAmount - discount;

        if (amountPaid > amountDue) {
            return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'Overpayment not allowed'
            });
        }

        // Duplicate payment check
        const existingPayment = await Payment.findOne({
            where: { installmentId, memberId }
        });
        
        if (existingPayment) {
            // Need confirmation logic in UI, but API rejects outright by default or updates existing
            return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'A payment already exists for this member and installment.'
            });
        }

        let status = 'PAID';
        if (amountPaid < amountDue) {
            status = 'PARTIAL';
        }

        const receiptNo = 'RCT-' + Date.now() + '-' + crypto.randomBytes(2).toString('hex').toUpperCase();

        const payment = await Payment.create({
            installmentId,
            memberId,
            amountPaid,
            amountDue,
            status,
            mode,
            referenceNo,
            collectedBy: req.user.id,
            receiptNo,
            paidAt: new Date()
        });

        // Generate PDF
        const member = await User.findByPk(memberId);
        const { generateReceiptPdf } = require('../utils/pdfGenerator');
        const pdfUrl = await generateReceiptPdf(payment, member);
        
        // Trigger WhatsApp Notification
        const { sendWhatsAppMessage } = require('../services/notificationService');
        const messageText = `Dear ${member.username},\nWe have received your payment of Rs. ${amountPaid / 100} towards Chit Installment #${installment.installmentNo}.\nReceipt No: ${receiptNo}\n\nThank you,\nJothi Vel Chits`;
        
        // Generate a mock full URL for the attachment
        const fullPdfUrl = `https://api.jothivelchits.com${pdfUrl}`;
        await sendWhatsAppMessage(member.phone, messageText, fullPdfUrl);

        res.status(201).json({ ...payment.toJSON(), pdfUrl });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /payments/:id/receipt
router.get('/:id/receipt', async (req, res) => {
    try {
        const payment = await Payment.findByPk(req.params.id);
        if (!payment) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Payment not found' });
        
        const member = await User.findByPk(payment.memberId);
        const { generateReceiptPdf } = require('../utils/pdfGenerator');
        const pdfUrl = await generateReceiptPdf(payment, member);

        res.json({ receiptNo: payment.receiptNo, pdfUrl });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
