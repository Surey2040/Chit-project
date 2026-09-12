const express = require('express');
const router = express.Router();
const { User } = require('../models');
const { authenticateToken, requireRole } = require('../middleware/auth');
const bcrypt = require('bcrypt');

router.use(authenticateToken);

// GET /members
router.get('/', async (req, res) => {
    try {
        const members = await User.findAll({ where: { role: 'MEMBER' } });
        res.json(members);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// POST /members (admin only)
router.post('/', requireRole(['ADMIN']), async (req, res) => {
    try {
        const { name, phone, nomineeName, nomineePhone, photoUrl, idProofUrl } = req.body;
        
        // Generate a random PIN or default password (in real app, send OTP)
        const defaultPassword = 'member123';
        const passwordHash = await bcrypt.hash(defaultPassword, 10);

        const newMember = await User.create({
            name,
            phone,
            passwordHash,
            role: 'MEMBER',
            nomineeName,
            nomineePhone,
            photoUrl,
            idProofUrl
        });

        // Hide password hash from response
        const { passwordHash: _, ...memberData } = newMember.toJSON();
        res.status(201).json(memberData);
    } catch (error) {
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Phone number already exists', field: 'phone' });
        }
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// GET /members/:id
router.get('/:id', async (req, res) => {
    try {
        const member = await User.findOne({
            where: { id: req.params.id, role: 'MEMBER' },
            attributes: { exclude: ['passwordHash'] }
        });
        if (!member) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Member not found' });
        res.json(member);
    } catch (error) {
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

// PUT /members/:id
router.put('/:id', requireRole(['ADMIN']), async (req, res) => {
    try {
        const member = await User.findOne({ where: { id: req.params.id, role: 'MEMBER' } });
        if (!member) return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Member not found' });
        
        const { name, phone, nomineeName, nomineePhone, isActive } = req.body;
        
        // Don't update password here
        await member.update({ name, phone, nomineeName, nomineePhone, isActive });
        
        const { passwordHash: _, ...memberData } = member.toJSON();
        res.json(memberData);
    } catch (error) {
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Phone number already exists', field: 'phone' });
        }
        res.status(500).json({ errorCode: 'SERVER_ERROR', message: error.message });
    }
});

module.exports = router;
