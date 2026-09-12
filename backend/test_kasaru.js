const KasaruService = require('./src/services/kasaruService');

try {
    const results = KasaruService.calculateAuction(100000000, 20, 30000000, 5.0);
    console.log("Auction Results:", results);
    // Expected: baseAmount: 5000000, companyCommission: 5000000, totalKasaru: 25000000
    // kasaruPerMember: 1250000, nextMonthDue: 3750000, winnerPayout: 70000000
} catch (e) {
    console.error(e);
}
