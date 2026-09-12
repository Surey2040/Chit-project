package com.jothivel.chits.data.models

enum class ChitTemplate(
    val title: String,
    val chitValue: Int,
    val durationMonths: Int,
    val subscriberCount: Int,
    val baseInstallment: Int
) {
    PLAN_50K("50,000 திட்டம்", 50000, 20, 20, 2500),
    PLAN_1L("1 லட்சம் திட்டம்", 100000, 20, 20, 5000),
    PLAN_2L("2 லட்சம் திட்டம்", 200000, 20, 20, 10000),
    PLAN_3L("3 லட்சம் திட்டம்", 300000, 20, 20, 15000),
    PLAN_5L("5 லட்சம் திட்டம்", 500000, 20, 20, 25000),
    PLAN_10L("10 லட்சம் திட்டம்", 1000000, 20, 20, 50000),
    CUSTOM("Custom", 0, 0, 0, 0)
}
