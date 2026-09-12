require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { sequelize } = require('./src/models');

const app = express();

const allowedOrigins = (process.env.CORS_ALLOWED_ORIGINS || '')
  .split(',')
  .map(o => o.trim())
  .filter(Boolean);

if (allowedOrigins.length === 0 && process.env.NODE_ENV === 'production') {
  console.error('FATAL ERROR: CORS_ALLOWED_ORIGINS must be set in production.');
  process.exit(1);
}

app.use(cors({
  origin: allowedOrigins.length > 0 ? allowedOrigins : true // unrestricted only outside production
}));
app.use(express.json());

// Routes
app.use('/auth', require('./src/routes/auth'));
app.use('/groups', require('./src/routes/groups'));
app.use('/members', require('./src/routes/members'));
app.use('/payments', require('./src/routes/payments'));
app.use('/reports', require('./src/routes/reports'));
app.use('/payouts', require('./src/routes/payouts'));

const PORT = process.env.PORT || 3000;

sequelize.sync({ force: false }).then(() => {
  app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
  });
}).catch(err => {
  console.error('Failed to sync database:', err);
});
