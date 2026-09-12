const assert = require('assert');

// Core Calculation Logic from the App Requirements
function calculateDividend(kasaruAmount, subscriberCount) {
    if (!kasaruAmount || !subscriberCount) return 0;
    // Paise integer math to avoid rounding errors
    return Math.floor(kasaruAmount / subscriberCount);
}

function calculateOrganizerCommission(chitValue) {
    // 5% commission per month
    return chitValue * 0.05;
}

function calculateNetPayable(baseAmount, dividend) {
    return baseAmount - dividend;
}

// Tests
console.log("Starting Financial Calculations Unit Tests...");

try {
    // Test 1: Dividend Calculation (Integer/Paise test)
    // Kasaru: Rs. 1000.00 (100000 paise), Subscribers: 20 -> Dividend should be Rs. 50.00 (5000 paise)
    let kasaruAmountPaise = 100000; 
    let subscribers = 20;
    let dividend = calculateDividend(kasaruAmountPaise, subscribers);
    assert.strictEqual(dividend, 5000, "Dividend calculation failed for clean division.");

    // Test 2: Dividend Rounding Down
    // Kasaru: Rs. 1000.00 (100000 paise), Subscribers: 3 -> Dividend: 33333.33... should round down to 33333 paise (Rs. 333.33)
    kasaruAmountPaise = 100000;
    subscribers = 3;
    dividend = calculateDividend(kasaruAmountPaise, subscribers);
    assert.strictEqual(dividend, 33333, "Dividend calculation failed to properly floor fractional paise.");

    // Test 3: Organizer Commission
    // Chit Value: Rs. 1,00,000 (10000000 paise) -> 5% Commission = 500000 paise (Rs. 5000)
    let chitValuePaise = 10000000;
    let commission = calculateOrganizerCommission(chitValuePaise);
    assert.strictEqual(commission, 500000, "Organizer commission calculation failed.");

    // Test 4: Net Payable Amount
    // Base Amount: Rs. 5000 (500000 paise)
    // Dividend: Rs. 333.33 (33333 paise)
    // Net Payable = 466667 paise (Rs. 4666.67)
    let baseAmountPaise = 500000;
    let netPayable = calculateNetPayable(baseAmountPaise, 33333);
    assert.strictEqual(netPayable, 466667, "Net payable calculation failed.");

    console.log("✅ All 4 financial calculation tests PASSED successfully.");
    console.log("Floating-point rounding errors are successfully mitigated by using paise integers.");

} catch (err) {
    console.error("❌ Test Failed!");
    console.error(err);
    process.exit(1);
}
