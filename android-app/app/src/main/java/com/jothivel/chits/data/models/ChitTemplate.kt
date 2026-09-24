package com.jothivel.chits.data.models

/** One month's fixed schedule for a plan: what every member pays, the kasaru (discount) baked
 *  into that month's collection, and the payout the prized member would receive. Amounts are in
 *  rupees (not paise) — callers convert when writing to InstallmentEntity. */
data class ChitScheduleRow(val installmentNo: Int, val baseAmount: Int, val kasaruAmount: Int, val payoutAmount: Int)

private fun schedule(vararg rows: Triple<Int, Int, Int>): List<ChitScheduleRow> =
    rows.mapIndexed { index, (base, kasaru, payout) -> ChitScheduleRow(index + 1, base, kasaru, payout) }

/** Fixed 20-month collection schedules, transcribed from the printed chit ledger cards
 *  (ஜோதி வேல் சிட்ஸ், Register No. 214/2020). Month 1 has no kasaru/payout since it's the
 *  registration installment. Each list's base+kasaru sum equals the plan's chit value. */
private object ChitScheduleData {
    val PLAN_50K = schedule(
        Triple(2500, 0, 0), Triple(1750, 750, 33000), Triple(1775, 725, 33500), Triple(1800, 700, 34000),
        Triple(1825, 675, 34500), Triple(1850, 650, 35000), Triple(1875, 625, 35500), Triple(1925, 575, 36500),
        Triple(1975, 525, 37500), Triple(2025, 475, 38500), Triple(2075, 425, 39500), Triple(2125, 375, 40500),
        Triple(2175, 325, 41500), Triple(2225, 275, 42500), Triple(2275, 225, 43500), Triple(2325, 175, 44500),
        Triple(2375, 125, 45500), Triple(2425, 75, 46500), Triple(2475, 25, 47500), Triple(2500, 0, 48500)
    )

    val PLAN_1L = schedule(
        Triple(5000, 0, 0), Triple(3500, 1500, 66000), Triple(3550, 1450, 67000), Triple(3600, 1400, 68000),
        Triple(3650, 1350, 69000), Triple(3700, 1300, 70000), Triple(3750, 1250, 71000), Triple(3850, 1150, 73000),
        Triple(3950, 1050, 75000), Triple(4050, 950, 77000), Triple(4150, 850, 79000), Triple(4250, 750, 81000),
        Triple(4350, 650, 83000), Triple(4450, 550, 85000), Triple(4550, 450, 87000), Triple(4650, 350, 89000),
        Triple(4750, 250, 91000), Triple(4850, 150, 93000), Triple(4950, 50, 95000), Triple(5000, 0, 97000)
    )

    val PLAN_2L = schedule(
        Triple(10000, 0, 0), Triple(7000, 3000, 132000), Triple(7100, 2900, 134000), Triple(7200, 2800, 136000),
        Triple(7300, 2700, 138000), Triple(7400, 2600, 140000), Triple(7500, 2500, 142000), Triple(7700, 2300, 146000),
        Triple(7900, 2100, 150000), Triple(8100, 1900, 154000), Triple(8300, 1700, 158000), Triple(8500, 1500, 162000),
        Triple(8700, 1300, 166000), Triple(8900, 1100, 170000), Triple(9100, 900, 174000), Triple(9300, 700, 178000),
        Triple(9500, 500, 182000), Triple(9700, 300, 186000), Triple(9900, 100, 190000), Triple(10000, 0, 194000)
    )

    val PLAN_3L = schedule(
        Triple(15000, 0, 0), Triple(10500, 4500, 198000), Triple(10650, 4350, 201000), Triple(10800, 4200, 204000),
        Triple(10950, 4050, 207000), Triple(11100, 3900, 210000), Triple(11250, 3750, 213000), Triple(11550, 3450, 219000),
        Triple(11850, 3150, 225000), Triple(12150, 2850, 231000), Triple(12450, 2550, 237000), Triple(12750, 2250, 243000),
        Triple(13050, 1950, 249000), Triple(13350, 1650, 255000), Triple(13650, 1350, 261000), Triple(13950, 1050, 267000),
        Triple(14250, 750, 273000), Triple(14550, 450, 279000), Triple(14850, 150, 285000), Triple(15000, 0, 291000)
    )

    val PLAN_10L = schedule(
        Triple(50000, 0, 0), Triple(37500, 12500, 700000), Triple(38000, 12000, 710000), Triple(38500, 11500, 720000),
        Triple(39000, 11000, 730000), Triple(39500, 10500, 740000), Triple(40000, 10000, 750000), Triple(40500, 9500, 760000),
        Triple(41000, 9000, 770000), Triple(41500, 8500, 780000), Triple(42000, 8000, 790000), Triple(42500, 7500, 800000),
        Triple(43000, 7000, 810000), Triple(44000, 6000, 830000), Triple(45000, 5000, 850000), Triple(46000, 4000, 870000),
        Triple(47000, 3000, 890000), Triple(48000, 2000, 910000), Triple(49000, 1000, 930000), Triple(49500, 500, 950000)
    )
}

enum class ChitTemplate(
    val title: String,
    val chitValue: Int,
    val durationMonths: Int,
    val subscriberCount: Int,
    val baseInstallment: Int,
    /** Month-by-month collection schedule for this plan, or null when no fixed schedule is
     *  known (falls back to a flat chitValue/months installment). */
    val fixedSchedule: List<ChitScheduleRow>? = null
) {
    PLAN_50K("50,000 திட்டம்", 50000, 20, 20, 2500, ChitScheduleData.PLAN_50K),
    PLAN_1L("1 லட்சம் திட்டம்", 100000, 20, 20, 5000, ChitScheduleData.PLAN_1L),
    PLAN_2L("2 லட்சம் திட்டம்", 200000, 20, 20, 10000, ChitScheduleData.PLAN_2L),
    PLAN_3L("3 லட்சம் திட்டம்", 300000, 20, 20, 15000, ChitScheduleData.PLAN_3L),
    PLAN_5L("5 லட்சம் திட்டம்", 500000, 20, 20, 25000, null),
    PLAN_10L("10 லட்சம் திட்டம்", 1000000, 20, 20, 50000, ChitScheduleData.PLAN_10L),
    CUSTOM("Custom", 0, 0, 0, 0, null);

    companion object {
        /** Finds the predefined plan matching a chit value, if any (excludes CUSTOM). */
        fun forChitValue(value: Int): ChitTemplate? = entries.firstOrNull { it != CUSTOM && it.chitValue == value }
    }
}
