const express = require('express');
const router = express.Router();
const { ChitGroup, Installment, User, MemberSubscription, Payment, sequelize } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');
const KasaruService = require('../services/kasaruService');
const { sendWhatsAppMessage } = require('../services/notificationService');
const { Op } = require('sequelize');

// Apply authentication to all routes in this file
router.use(authenticateToken);

// GET /groups - list all groups
router.get('/', async (req, res) => {
    try {
        const { role, id } = req.user;
        let groups;

        if (role === 'ADMIN') {
            groups = await ChitGroup.findAll();
        } else {
            // For AGENT and MEMBER, they might only see groups they are part of or assigned to.
            // For now, return all ACTIVE and COMPLETED groups for agents/members.
            groups = await ChitGroup.findAll({
                where: { status: ['ACTIVE', 'COMPLETED'] }
            });
        }

        res.json(groups);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /groups - create a new group (ADMIN only)
router.post('/', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { registerNo, chitValue, durationMonths, subscriberCount, branch, startDate } = req.body;

        if (durationMonths !== subscriberCount) {
            return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'durationMonths MUST equal subscriberCount',
                field: 'durationMonths'
            });
        }

        const newGroup = await ChitGroup.create({
            registerNo,
            chitValue,
            durationMonths,
            subscriberCount,
            branch,
            startDate,
            createdBy: req.user.id
        });

        // Auto-generate the installment table on creation
        const baseAmount = chitValue / durationMonths;
        const installments = [];
        for (let i = 1; i <= durationMonths; i++) {
            installments.push({
                groupId: newGroup.id,
                installmentNo: i,
                baseAmount: baseAmount,
                status: 'UPCOMING'
            });
        }
        await Installment.bulkCreate(installments);

        res.status(201).json(newGroup);
    } catch (error) {
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Register No already exists', field: 'registerNo' });
        }
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /groups/:id
router.get('/:id', async (req, res) => {
    try {
        const group = await ChitGroup.findByPk(req.params.id);
        if (!group) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Group not found' });
        res.json(group);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// PUT /groups/:id (admin only)
router.put('/:id', requireRole(['ADMIN']), async (req, res) => {
    try {
        const group = await ChitGroup.findByPk(req.params.id);
        if (!group) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Group not found' });

        if (group.status === 'ACTIVE') {
            const { chitValue, durationMonths, subscriberCount } = req.body;
            if (chitValue || durationMonths || subscriberCount) {
                return res.status(400).json({
                    errorCode: 'VALIDATION_ERROR',
                    message: 'Cannot modify value or duration of an ACTIVE group'
                });
            }
        }

        await group.update(req.body);
        res.json(group);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /groups/:id/activate (admin only)
router.post('/:id/activate', requireRole(['ADMIN']), async (req, res) => {
    try {
        const group = await ChitGroup.findByPk(req.params.id);
        if (!group) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Group not found' });

        if (group.durationMonths !== group.subscriberCount) {
             return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'durationMonths MUST equal subscriberCount before activation'
            });
        }

        // Verify that all slots are filled (optional based on business rule, but a good check)
        const subsCount = await MemberSubscription.count({ where: { groupId: group.id } });
        if (subsCount !== group.subscriberCount) {
             return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: `Cannot activate. Expected ${group.subscriberCount} subscribers, but found ${subsCount}`
            });
        }

        group.status = 'ACTIVE';
        await group.save();
        res.json(group);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /groups/:id/installments
router.get('/:id/installments', async (req, res) => {
    try {
        const installments = await Installment.findAll({
            where: { groupId: req.params.id },
            order: [['installmentNo', 'ASC']]
        });
        res.json(installments);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /groups/:id/installments/:no
router.get('/:id/installments/:no', async (req, res) => {
    try {
        const inst = await Installment.findOne({
            where: { groupId: req.params.id, installmentNo: req.params.no }
        });
        if (!inst) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        res.json(inst);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /groups/:id/installments/:no/auction (admin only)
router.post('/:id/installments/:no/auction', requireRole(['ADMIN']), async (req, res) => {
    const { winningMemberId, winningBid, auctionDate, commissionPercentage = 5.0 } = req.body;

    if (typeof winningBid !== 'number' || !Number.isFinite(winningBid) || winningBid < 0) {
        return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Winning bid must be a non-negative number', field: 'winningBid' });
    }
    if (typeof commissionPercentage !== 'number' || commissionPercentage < 0 || commissionPercentage > 20) {
        return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Commission percentage must be between 0 and 20', field: 'commissionPercentage' });
    }

    const t = await sequelize.transaction();
    try {
        const group = await ChitGroup.findByPk(req.params.id, { transaction: t, lock: t.LOCK.UPDATE });
        if (!group) {
            await t.rollback();
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Group not found' });
        }
        if (group.status !== 'ACTIVE') {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Auctions can only be run on an ACTIVE group' });
        }

        const inst = await Installment.findOne({
            where: { groupId: group.id, installmentNo: req.params.no },
            transaction: t,
            lock: t.LOCK.UPDATE
        });
        if (!inst) {
            await t.rollback();
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        }
        if (inst.status === 'LOCKED' || inst.status === 'AUCTION_DONE') {
            await t.rollback();
            return res.status(403).json({ errorCode: 'FORBIDDEN', message: `Installment #${inst.installmentNo} is already finalized and cannot be edited` });
        }

        // Enforce chronological order: every earlier installment must already be LOCKED
        const unfinishedPriorCount = await Installment.count({
            where: { groupId: group.id, installmentNo: { [Op.lt]: inst.installmentNo }, status: { [Op.ne]: 'LOCKED' } },
            transaction: t
        });
        if (unfinishedPriorCount > 0) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Earlier installments must be auctioned first' });
        }

        const winnerSubscription = await MemberSubscription.findOne({
            where: { groupId: group.id, memberId: winningMemberId },
            include: [{ model: User, attributes: { exclude: ['passwordHash'] } }],
            transaction: t
        });
        if (!winnerSubscription) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Winning member is not a subscriber of this group', field: 'winningMemberId' });
        }
        if (winnerSubscription.hasWon) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'This member has already won a previous auction', field: 'winningMemberId' });
        }

        if (winningBid > group.chitValue) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Winning bid cannot exceed the chit value', field: 'winningBid' });
        }

        // Calculate Kasaru logic (throws if winningBid is below the company commission)
        let auctionResults;
        try {
            auctionResults = KasaruService.calculateAuction(
                group.chitValue,
                group.subscriberCount,
                winningBid,
                commissionPercentage
            );
        } catch (calcError) {
            await t.rollback();
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: calcError.message });
        }

        inst.winningMemberId = winningMemberId;
        inst.kasaruAmount = auctionResults.kasaruPerMember;
        inst.payoutAmount = auctionResults.winnerPayout;
        inst.companyCommission = auctionResults.companyCommission;
        inst.auctionDate = auctionDate || new Date();
        inst.status = 'LOCKED';
        inst.lockedAt = new Date();
        await inst.save({ transaction: t });

        await MemberSubscription.update(
            { hasWon: true, wonInstallmentNo: inst.installmentNo },
            { where: { groupId: group.id, memberId: winningMemberId }, transaction: t }
        );

        // Create the DUE payment record every subscriber now owes for this installment
        const allMembers = await MemberSubscription.findAll({
            where: { groupId: group.id },
            include: [{ model: User, attributes: { exclude: ['passwordHash'] } }],
            transaction: t
        });
        const paymentsToCreate = allMembers.map(m => ({
            installmentId: inst.id,
            memberId: m.memberId,
            amountPaid: 0,
            amountDue: auctionResults.nextMonthDue,
            status: 'DUE',
            mode: 'CASH',
            paidAt: null
        }));
        await Payment.bulkCreate(paymentsToCreate, { transaction: t });

        await t.commit();

        // Fire-and-forget notifications after the transaction is safely committed;
        // a notification failure must never roll back or fail an already-saved auction result.
        const dividendPool = auctionResults.totalKasaru;
        const dividend = auctionResults.kasaruPerMember;
        for (const sub of allMembers) {
            if (sub.User && sub.User.phone) {
                const isWinner = sub.User.id === winningMemberId;
                let msg = `*Jothi Vel Chits Auction Update*\nGroup: ${group.registerNo} | Installment: #${inst.installmentNo}\n\n`;
                if (isWinner) {
                    msg += `Congratulations ${sub.User.name}! You have won the auction with a discount (Kasaru) of Rs. ${winningBid / 100}.\nYour payout will be Rs. ${auctionResults.winnerPayout / 100}.`;
                } else {
                    msg += `The auction was won by ${winnerSubscription.User.name} for a discount of Rs. ${winningBid / 100}.\nYour dividend is Rs. ${dividend / 100}.\nNext due: Rs. ${auctionResults.nextMonthDue / 100}.`;
                }
                sendWhatsAppMessage(sub.User.phone, msg).catch(() => {});
            }
        }

        res.json({
            installment: inst,
            calculations: auctionResults
        });
    } catch (error) {
        await t.rollback();
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /groups/:id/members
router.get('/:id/members', async (req, res) => {
    try {
        const members = await MemberSubscription.findAll({
            where: { groupId: req.params.id },
            include: [{ model: User, attributes: { exclude: ['passwordHash'] } }]
        });
        res.json(members);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /groups/:id/members
router.post('/:id/members', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { memberId, slotNo } = req.body;
        const groupId = req.params.id;

        const group = await ChitGroup.findByPk(groupId);
        if (!group) {
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Group not found' });
        }

        if (group.status !== 'DRAFT') {
            return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'Members can only be added while the group is in DRAFT status'
            });
        }

        const member = await User.findOne({ where: { id: memberId, role: 'MEMBER' } });
        if (!member) {
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Member not found', field: 'memberId' });
        }

        if (!Number.isInteger(slotNo) || slotNo < 1 || slotNo > group.durationMonths) {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Invalid slot number', field: 'slotNo' });
        }

        // Verify slot unique constraint (also enforced by a DB unique index as a race-condition backstop)
        const existingSlot = await MemberSubscription.findOne({ where: { groupId, slotNo } });
        if (existingSlot) {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Slot already taken', field: 'slotNo' });
        }

        const subscription = await MemberSubscription.create({
            groupId,
            memberId,
            slotNo
        });

        res.status(201).json(subscription);
    } catch (error) {
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Slot already taken', field: 'slotNo' });
        }
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
