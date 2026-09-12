const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
    console.error("FATAL ERROR: JWT_SECRET environment variable is not set.");
    process.exit(1);
}

const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({
            errorCode: 'UNAUTHORIZED',
            message: 'Authentication token is required',
            field: 'authorization_header'
        });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({
                errorCode: 'FORBIDDEN',
                message: 'Invalid or expired authentication token',
                field: 'authorization_header'
            });
        }
        req.user = user;
        next();
    });
};

const requireRole = (roles) => {
    return (req, res, next) => {
        if (!req.user || !roles.includes(req.user.role)) {
            return res.status(403).json({
                errorCode: 'INSUFFICIENT_PERMISSIONS',
                message: `This action requires one of the following roles: ${roles.join(', ')}`,
                field: 'role'
            });
        }
        next();
    };
};

module.exports = {
    authenticateToken,
    requireRole,
    JWT_SECRET
};
