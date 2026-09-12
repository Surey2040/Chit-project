const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { User } = require('../models');
const { JWT_SECRET } = require('../middleware/auth');

// Simple in-memory brute-force guard for /auth/login.
// Keyed by IP+phone so one bad actor can't lock out other users, and cleared on success.
// NOTE: this resets on server restart and does not share state across multiple server
// instances - for a multi-instance production deployment, back this with Redis instead.
const LOGIN_ATTEMPT_LIMIT = 5;
const LOGIN_ATTEMPT_WINDOW_MS = 15 * 60 * 1000;
const loginAttempts = new Map();

function loginRateLimiter(req, res, next) {
    const key = `${req.ip}:${req.body && req.body.phone}`;
    const now = Date.now();
    const entry = loginAttempts.get(key);

    if (entry && now - entry.firstAttempt < LOGIN_ATTEMPT_WINDOW_MS && entry.count >= LOGIN_ATTEMPT_LIMIT) {
        const retryAfterSeconds = Math.ceil((LOGIN_ATTEMPT_WINDOW_MS - (now - entry.firstAttempt)) / 1000);
        return res.status(429).json({
            errorCode: 'TOO_MANY_ATTEMPTS',
            message: `Too many login attempts. Try again in ${retryAfterSeconds} seconds.`
        });
    }
    next();
}

function recordLoginFailure(req) {
    const key = `${req.ip}:${req.body && req.body.phone}`;
    const now = Date.now();
    const entry = loginAttempts.get(key);
    if (!entry || now - entry.firstAttempt >= LOGIN_ATTEMPT_WINDOW_MS) {
        loginAttempts.set(key, { count: 1, firstAttempt: now });
    } else {
        entry.count += 1;
    }
}

function clearLoginFailures(req) {
    loginAttempts.delete(`${req.ip}:${req.body && req.body.phone}`);
}

// POST /auth/login (public)
router.post('/login', loginRateLimiter, async (req, res) => {
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
            recordLoginFailure(req);
            return res.status(401).json({
                errorCode: 'UNAUTHORIZED',
                message: 'Invalid credentials',
                field: 'credentials'
            });
        }

        const validPassword = await bcrypt.compare(password, user.passwordHash);
        if (!validPassword) {
            recordLoginFailure(req);
            return res.status(401).json({
                errorCode: 'UNAUTHORIZED',
                message: 'Invalid credentials',
                field: 'credentials'
            });
        }

        clearLoginFailures(req);

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

// One-time admin provisioning. Only reachable outside production, and only while no
// admin exists yet - this must never be exposed publicly with guessable credentials.
router.post('/seed-admin', async (req, res) => {
    if (process.env.NODE_ENV === 'production') {
        return res.status(404).json({ errorCode: 'NOT_FOUND', message: 'Not found' });
    }
    try {
        const adminExists = await User.findOne({ where: { role: 'ADMIN' } });
        if (adminExists) {
            return res.status(400).json({ message: 'Admin already exists' });
        }

        const { phone, username } = req.body;
        if (!phone || !username) {
            return res.status(400).json({
                errorCode: 'VALIDATION_ERROR',
                message: 'phone and username are required to seed the admin account'
            });
        }

        const generatedPassword = crypto.randomBytes(9).toString('base64url');
        const passwordHash = await bcrypt.hash(generatedPassword, 10);
        await User.create({
            name: 'Admin',
            username,
            phone,
            passwordHash,
            role: 'ADMIN'
        });

        res.status(201).json({
            message: 'Admin seeded successfully. Save this password now - it will not be shown again.',
            username,
            phone,
            password: generatedPassword
        });
    } catch (error) {
        if (error.name === 'SequelizeUniqueConstraintError') {
            return res.status(400).json({ errorCode: 'VALIDATION_ERROR', message: 'Username or phone already in use' });
        }
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
