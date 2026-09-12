const express = require('express');
const router = express.Router();
const { Payout, Installment, ChitGroup } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');

router.use(authenticateToken);

// GET /payouts
router.get('/', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { groupId, installmentNo } = req.query;
        const payouts = await Payout.findAll();
        res.json(payouts);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /payouts (admin only)
router.post('/', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { installmentId, memberId, amount, proofUrl } = req.body;

        const installment = await Installment.findByPk(installmentId);
        if (!installment) {
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        }

        if (installment.winningMemberId !== memberId) {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Member is not the winner for this installment' });
        }

        if (installment.payoutAmount !== amount) {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Amount does not match installment payout amount' });
        }

        const existingPayout = await Payout.findOne({ where: { installmentId } });
        if (existingPayout) {
             return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Payout already recorded for this installment' });
        }

        const payout = await Payout.create({
            installmentId,
            memberId,
            amount,
            proofUrl,
            disbursedBy: req.user.id,
            disbursedAt: new Date()
        });

        res.status(201).json(payout);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
