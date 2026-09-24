const express = require('express');
const router = express.Router();
const { ChitGroup, Payment, Installment, User, Payout, sequelize } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { Op } = require('sequelize');

router.use(authenticateToken);

// GET /reports/dashboard
router.get('/dashboard', requireRole(['ADMIN', 'AGENT']), async (req, res) => {
    try {
        const activeChitsCount = await ChitGroup.count({ where: { status: 'ACTIVE' } });
        const activeChits = await ChitGroup.findAll({ where: { status: 'ACTIVE' }, attributes: ['chitValue'] });
        const totalChitValue = activeChits.reduce((sum, c) => sum + c.chitValue, 0);

        const totalMembers = await User.count({ where: { role: 'MEMBER' } });
        
        // Month collected
        const startOfMonth = new Date();
        startOfMonth.setDate(1);
        startOfMonth.setHours(0, 0, 0, 0);
        
        const monthCollected = await Payment.sum('amountPaid', {
            where: {
                paidAt: { [Op.gte]: startOfMonth },
                status: { [Op.in]: ['PAID', 'PARTIAL'] }
            }
        });

        const pendingPaymentsCount = await Payment.count({
            where: { status: { [Op.in]: ['DUE', 'OVERDUE'] } }
        });

        res.json({
            activeChitsCount,
            totalChitValue,
            totalMembers,
            monthCollected: monthCollected || 0,
            pendingPaymentsCount
        });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /reports/dues
router.get('/dues', requireRole(['ADMIN', 'AGENT']), async (req, res) => {
    try {
        const { groupId, memberId } = req.query;
        
        let whereClause = {
            status: { [Op.in]: ['DUE', 'OVERDUE'] }
        };
        
        if (memberId) whereClause.memberId = memberId;
        
        const includeClause = [
            {
                model: Installment,
                where: groupId ? { groupId } : {},
                include: [{ model: ChitGroup, attributes: ['registerNo', 'chitValue'] }]
            },
            { model: User, attributes: ['name', 'phone'] }
        ];

        const dues = await Payment.findAll({
            where: whereClause,
            include: includeClause
        });

        res.json({ message: 'Dues Report', data: dues });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /reports/collections
router.get('/collections', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { date, agentId } = req.query;
        
        let whereClause = {
            status: { [Op.in]: ['PAID', 'PARTIAL'] }
        };
        
        if (date) {
            whereClause.paidAt = {
                [Op.gte]: new Date(date),
                [Op.lt]: new Date(new Date(date).setDate(new Date(date).getDate() + 1))
            };
        }
        
        if (agentId) whereClause.collectedBy = agentId;

        const collections = await Payment.findAll({
            where: whereClause,
            include: [
                { model: User, attributes: ['name'] },
                { model: User, as: 'Agent', attributes: ['name'] }
            ]
        });

        const totalCollected = collections.reduce((sum, p) => sum + p.amountPaid, 0);

        res.json({ message: 'Collections Report', totalCollected, data: collections });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /reports/daily-tally
router.get('/daily-tally', requireRole(['ADMIN']), async (req, res) => {
    try {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        const paymentsToday = await Payment.findAll({
            where: {
                paidAt: { [Op.gte]: today },
                status: { [Op.in]: ['PAID', 'PARTIAL'] }
            },
            attributes: ['mode', 'amountPaid']
        });
        
        let cashTotal = 0;
        let upiTotal = 0;
        let bankTotal = 0;
        
        paymentsToday.forEach(p => {
            if (p.mode === 'CASH') cashTotal += p.amountPaid;
            else if (p.mode === 'UPI') upiTotal += p.amountPaid;
            else if (p.mode === 'BANK_TRANSFER') bankTotal += p.amountPaid;
        });

        res.json({
            message: 'Daily Cash Tally',
            date: today,
            cashInHand: cashTotal,
            upiTotal,
            bankTotal,
            totalCollected: cashTotal + upiTotal + bankTotal
        });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /reports/pl
router.get('/pl', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { groupId } = req.query;
        
        if (!groupId) return res.status(400).json({ message: 'groupId is required' });
        
        const group = await ChitGroup.findByPk(groupId);
        if (!group) return res.status(404).json({ message: 'Group not found' });
        
        // Organizer commission is usually 5% of Chit Value per month
        const commissionPerMonth = (group.chitValue * 0.05);
        const totalProjectedCommission = commissionPerMonth * group.durationMonths;

        res.json({ 
            message: 'Profit & Loss Report', 
            group: group.registerNo,
            totalProjectedCommission
        });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /reports/settlement
router.get('/settlement', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { groupId } = req.query;
        
        if (!groupId) return res.status(400).json({ message: 'groupId is required' });

        const installments = await Installment.findAll({
            where: { groupId },
            attributes: ['id']
        });
        
        const installmentIds = installments.map(i => i.id);

        const totalCollected = await Payment.sum('amountPaid', {
            where: { installmentId: { [Op.in]: installmentIds } }
        });

        const totalPayouts = await Payout.sum('amount', {
            where: { installmentId: { [Op.in]: installmentIds } }
        });

        res.json({ 
            message: 'Settlement Report', 
            groupId,
            totalCollected: totalCollected || 0,
            totalPayouts: totalPayouts || 0,
            balance: (totalCollected || 0) - (totalPayouts || 0)
        });
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
