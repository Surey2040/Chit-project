const { Sequelize, DataTypes } = require('sequelize');
const path = require('path');

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../../database.sqlite'),
  logging: false
});

const User = sequelize.define('User', {
  id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
  name: { type: DataTypes.STRING, allowNull: false },
  username: { type: DataTypes.STRING, allowNull: false, unique: true },
  phone: { type: DataTypes.STRING, allowNull: false, unique: true },
  passwordHash: { type: DataTypes.STRING, allowNull: false },
  role: { type: DataTypes.ENUM('ADMIN', 'AGENT', 'MEMBER'), allowNull: false },
  photoUrl: { type: DataTypes.STRING },
  idProofUrl: { type: DataTypes.STRING },
  nomineeName: { type: DataTypes.STRING },
  nomineePhone: { type: DataTypes.STRING },
  isActive: { type: DataTypes.BOOLEAN, defaultValue: true }
});

const ChitGroup = sequelize.define('ChitGroup', {
  id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
  registerNo: { type: DataTypes.STRING, allowNull: false, unique: true },
  chitValue: { type: DataTypes.INTEGER, allowNull: false }, // stored in paise
  durationMonths: { type: DataTypes.INTEGER, allowNull: false },
  subscriberCount: { type: DataTypes.INTEGER, allowNull: false },
  branch: { type: DataTypes.STRING, allowNull: false },
  startDate: { type: DataTypes.DATEONLY, allowNull: false },
  status: { type: DataTypes.ENUM('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED'), defaultValue: 'DRAFT' },
  createdBy: { type: DataTypes.UUID, allowNull: false }
});

const Installment = sequelize.define('Installment', {
  id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
  installmentNo: { type: DataTypes.INTEGER, allowNull: false },
  baseAmount: { type: DataTypes.INTEGER, allowNull: false },
  kasaruAmount: { type: DataTypes.INTEGER, defaultValue: null },
  payoutAmount: { type: DataTypes.INTEGER, defaultValue: null },
  companyCommission: { type: DataTypes.INTEGER, defaultValue: null },
  auctionDate: { type: DataTypes.DATEONLY, defaultValue: null },
  status: { type: DataTypes.ENUM('UPCOMING', 'AUCTION_DONE', 'LOCKED'), defaultValue: 'UPCOMING' },
  lockedAt: { type: DataTypes.DATE, defaultValue: null }
}, {
  indexes: [
    { unique: true, fields: ['groupId', 'installmentNo'] }
  ]
});

const MemberSubscription = sequelize.define('MemberSubscription', {
  id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
  slotNo: { type: DataTypes.INTEGER, allowNull: false },
  hasWon: { type: DataTypes.BOOLEAN, defaultValue: false },
  wonInstallmentNo: { type: DataTypes.INTEGER, defaultValue: null }
}, {
  indexes: [
    { unique: true, fields: ['groupId', 'slotNo'] }
  ]
});

const Payment = sequelize.define('Payment', {
  id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
  amountPaid: { type: DataTypes.INTEGER, allowNull: false },
  amountDue: { type: DataTypes.INTEGER, allowNull: false },
  status: { type: DataTypes.ENUM('PAID', 'PARTIAL', 'DUE', 'OVERDUE'), allowNull: false },
  mode: { type: DataTypes.ENUM('CASH', 'UPI', 'BANK_TRANSFER'), allowNull: false },
  referenceNo: { type: DataTypes.STRING },
  receiptNo: { type: DataTypes.STRING, unique: true },
  paidAt: { type: DataTypes.DATE, allowNull: true }
}, {
  indexes: [
    { unique: true, fields: ['installmentId', 'memberId'] }
  ]
});

const Payout = sequelize.define('Payout', {
  id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
  amount: { type: DataTypes.INTEGER, allowNull: false },
  proofUrl: { type: DataTypes.STRING, allowNull: false },
  disbursedAt: { type: DataTypes.DATE, allowNull: false }
}, {
  indexes: [
    { unique: true, fields: ['installmentId'] }
  ]
});

// Relationships
ChitGroup.hasMany(Installment, { foreignKey: 'groupId' });
Installment.belongsTo(ChitGroup, { foreignKey: 'groupId' });

ChitGroup.hasMany(MemberSubscription, { foreignKey: 'groupId' });
MemberSubscription.belongsTo(ChitGroup, { foreignKey: 'groupId' });

User.hasMany(MemberSubscription, { foreignKey: 'memberId' });
MemberSubscription.belongsTo(User, { foreignKey: 'memberId' });

Installment.hasMany(Payment, { foreignKey: 'installmentId' });
Payment.belongsTo(Installment, { foreignKey: 'installmentId' });

User.hasMany(Payment, { foreignKey: 'memberId' });
Payment.belongsTo(User, { foreignKey: 'memberId' });

User.hasMany(Payment, { foreignKey: 'collectedBy', as: 'Agent' });
Payment.belongsTo(User, { foreignKey: 'collectedBy', as: 'Agent' });

Installment.hasOne(Payout, { foreignKey: 'installmentId' });
Payout.belongsTo(Installment, { foreignKey: 'installmentId' });

User.hasMany(Payout, { foreignKey: 'memberId' });
Payout.belongsTo(User, { foreignKey: 'memberId' });

User.hasMany(Payout, { foreignKey: 'disbursedBy', as: 'Admin' });
Payout.belongsTo(User, { foreignKey: 'disbursedBy', as: 'Admin' });

Installment.belongsTo(User, { foreignKey: 'winningMemberId', as: 'Winner' });

module.exports = {
  sequelize,
  User,
  ChitGroup,
  Installment,
  MemberSubscription,
  Payment,
  Payout
};
