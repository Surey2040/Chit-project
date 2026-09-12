require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { sequelize } = require('./src/models');

const app = express();

app.use(cors());
app.use(express.json());

// Routes
app.use('/auth', require('./src/routes/auth'));
app.use('/groups', require('./src/routes/groups'));
app.use('/members', require('./src/routes/members'));
app.use('/payments', require('./src/routes/payments'));
app.use('/reports', require('./src/routes/reports'));
app.use('/payouts', require('./src/routes/payouts'));
app.use('/installments', require('./src/routes/auctions')); // For auction logic

const PORT = process.env.PORT || 3000;

sequelize.sync({ force: false }).then(() => {
  app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
  });
}).catch(err => {
  console.error('Failed to sync database:', err);
});
