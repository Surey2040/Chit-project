/**
 * Kasaru Service - Core Engine for Chit Fund Calculations
 * All monetary values must be provided in integer paise (1 Rupee = 100 paise)
 */

class KasaruService {
    
    /**
     * Calculates all auction variables for a given month.
     * @param {number} chitValue - Total value of the chit in paise (e.g. 10 Lakhs = 100000000)
     * @param {number} totalSubscribers - Number of members in the chit
     * @param {number} winningBid - The discount amount bidded by the winner in paise (e.g. 3 Lakhs = 30000000)
     * @param {number} companyCommissionPercentage - The % commission the company takes (default 5%)
     * @returns {Object} Calculated values in integer paise
     */
    static calculateAuction(chitValue, totalSubscribers, winningBid, companyCommissionPercentage = 5.0) {
        if (chitValue <= 0 || totalSubscribers <= 0 || winningBid < 0) {
            throw new Error("Invalid auction parameters");
        }

        // 1. Base Amount (மாத தவணை)
        const baseAmount = Math.floor(chitValue / totalSubscribers);

        // 2. Company Commission (கம்பெனி கமிஷன்)
        const companyCommission = Math.floor((chitValue * companyCommissionPercentage) / 100);

        // 3. Total Kasaru / Dividend (மீதமுள்ள தள்ளுபடி)
        const totalKasaru = winningBid - companyCommission;
        
        if (totalKasaru < 0) {
            throw new Error("Winning bid cannot be less than company commission");
        }

        // 4. Kasaru Per Member (ஒருவருக்கு கசரு)
        const kasaruPerMember = Math.floor(totalKasaru / totalSubscribers);

        // 5. Next Month Due (அடுத்த மாத நிலுவை)
        const nextMonthDue = baseAmount - kasaruPerMember;

        // 6. Winner Payout (பணம் எடுப்பவருக்குச் சேரும் தொகை)
        const winnerPayout = chitValue - winningBid;

        return {
            baseAmount,
            companyCommission,
            totalKasaru,
            kasaruPerMember,
            nextMonthDue,
            winnerPayout
        };
    }
}

module.exports = KasaruService;
