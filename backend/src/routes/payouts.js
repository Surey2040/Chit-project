const express = require('express');
const router = express.Router();
const { Payout, Installment, ChitGroup, sequelize } = require('../models');
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
    const { installmentId, memberId, amount, proofUrl } = req.body;

    const t = await sequelize.transaction();
    try {
        // Row-lock the installment so two near-simultaneous requests for the same
        // installment serialize instead of both passing the existence check below.
        const installment = await Installment.findByPk(installmentId, { transaction: t, lock: t.LOCK.UPDATE });
        if (!installment) {
            await t.rollback();
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        }

        if (installment.winningMemberId !== memberId) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Member is not the winner for this installment' });
        }

        if (installment.payoutAmount !== amount) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Amount does not match installment payout amount' });
        }

        const existingPayout = await Payout.findOne({ where: { installmentId }, transaction: t, lock: t.LOCK.UPDATE });
        if (existingPayout) {
             await t.rollback();
             return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Payout already recorded for this installment' });
        }

        const payout = await Payout.create({
            installmentId,
            memberId,
            amount,
            proofUrl,
            disbursedBy: req.user.id,
            disbursedAt: new Date()
        }, { transaction: t });

        await t.commit();
        res.status(201).json(payout);
    } catch (error) {
        await t.rollback();
        // Belt-and-suspenders backstop: the DB-level unique index on Payout.installmentId
        // (see models/index.js) guards against a double payout even if the transaction/lock
        // above somehow still races (e.g. concurrent connections outside this process).
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(409).json({ errorCode: 'CONFLICT', message: 'Payout already recorded for this installment' });
        }
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
