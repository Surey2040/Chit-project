const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { User } = require('../models');
const { JWT_SECRET } = require('../middleware/auth');

// POST /auth/login (public)
router.post('/login', async (req, res) => {
    try {
        const { phone, username, password } = req.body;

        if (!phone || !username || !password) {
            return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'Phone, username, and password are required',
                field: 'body'
            });
        }

        const user = await User.findOne({ where: { phone, username, isActive: true } });
        if (!user) {
            return res.status(401).json({
                errorCode: 'UNAUTHORIZED',
                message: 'Invalid credentials',
                field: 'credentials'
            });
        }

        const validPassword = await bcrypt.compare(password, user.passwordHash);
        if (!validPassword) {
            return res.status(401).json({
                errorCode: 'UNAUTHORIZED',
                message: 'Invalid credentials',
                field: 'credentials'
            });
        }

        // Generate short-lived access token (15 mins) and payload with role
        const payload = {
            id: user.id,
            phone: user.phone,
            username: user.username,
            role: user.role
        };
        
        const accessToken = jwt.sign(payload, JWT_SECRET, { expiresIn: '15m' });
        const refreshToken = jwt.sign(payload, JWT_SECRET, { expiresIn: '30d' });

        res.json({
            accessToken,
            refreshToken,
            user: {
                id: user.id,
                username: user.username,
                role: user.role,
                phone: user.phone
            }
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({
            errorCode: 'SERVER_ERROR',
            message: 'An unexpected error occurred during login',
            field: null
        });
    }
});

// POST /auth/refresh-token (public)
router.post('/refresh-token', async (req, res) => {
    try {
        const { refreshToken } = req.body;
        if (!refreshToken) {
            return res.status(401).json({ message: 'Refresh token required' });
        }

        jwt.verify(refreshToken, JWT_SECRET, (err, decoded) => {
            if (err) {
                return res.status(403).json({ message: 'Invalid or expired refresh token' });
            }

            const payload = {
                id: decoded.id,
                phone: decoded.phone,
                username: decoded.username,
                role: decoded.role
            };

            const accessToken = jwt.sign(payload, JWT_SECRET, { expiresIn: '15m' });
            res.json({ accessToken });
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: 'Server error' });
    }
});

// Seed an initial Admin user for dev purposes
router.post('/seed-admin', async (req, res) => {
    try {
        const adminExists = await User.findOne({ where: { role: 'ADMIN' } });
        if (adminExists) {
            return res.status(400).json({ message: 'Admin already exists' });
        }
        
        const passwordHash = await bcrypt.hash('admin123', 10);
        const admin = await User.create({
            username: 'admin',
            phone: '9999999999',
            passwordHash,
            role: 'ADMIN'
        });
        
        res.status(201).json({ message: 'Admin seeded successfully. Login with 9999999999 / admin / admin123' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
