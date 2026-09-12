const express = require('express');
const router = express.Router();
const { ChitGroup, Installment, User, MemberSubscription } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { sendWhatsAppMessage } = require('../services/notificationService');

router.use(authenticateToken);

// POST /installments/:id/auction
// Locks the auction for an installment, sets the winner, and notifies the group.
router.post('/:id/auction', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { id } = req.params;
        const { winningMemberId, kasaruAmount } = req.body;

        const installment = await Installment.findByPk(id, { include: [ChitGroup] });
        if (!installment) {
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Installment not found' });
        }

        const winner = await User.findByPk(winningMemberId);
        if (!winner) {
            return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Winner not found' });
        }

        // Calculate 5% Company Commission
        const companyCommission = Math.floor(installment.ChitGroup.chitValue * 0.05);

        // Update installment
        installment.winningMemberId = winningMemberId;
        installment.kasaruAmount = kasaruAmount;
        installment.companyCommission = companyCommission;
        installment.payoutAmount = installment.ChitGroup.chitValue - kasaruAmount;
        installment.status = 'AUCTION_DONE';
        installment.auctionDate = new Date();
        await installment.save();

        // Broadcast to all members in the group
        const subscriptions = await MemberSubscription.findAll({
            where: { groupId: installment.groupId },
            include: [{ model: User }]
        });

        const dividendPool = kasaruAmount - companyCommission;
        const dividend = Math.floor(dividendPool / installment.ChitGroup.subscriberCount);

        subscriptions.forEach(sub => {
            if (sub.User && sub.User.phone) {
                const isWinner = sub.User.id === winningMemberId;
                let msg = `*Jothi Vel Chits Auction Update*\nGroup: ${installment.ChitGroup.registerNo} | Installment: #${installment.installmentNo}\n\n`;
                
                if (isWinner) {
                    msg += `Congratulations ${sub.User.name}! You have won the auction with a discount (Kasaru) of Rs. ${kasaruAmount / 100}.\nYour payout will be Rs. ${installment.payoutAmount / 100}.`;
                } else {
                    msg += `The auction was won by ${winner.name} for a discount of Rs. ${kasaruAmount / 100}.\nYour dividend is Rs. ${dividend / 100}.\nPlease pay your remaining dues on time.`;
                }
                
                // Fire and forget WhatsApp
                sendWhatsAppMessage(sub.User.phone, msg);
            }
        });

        res.json({ message: 'Auction recorded successfully and notifications sent.', data: installment });

    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
