package com.jothivel.chits.ui.agent

import java.text.Normalizer
import java.util.Locale

enum class AssistantIntent {
    HELP,
    TODAY_COLLECTION,
    MONTH_COLLECTION,
    PENDING_SUMMARY,
    PENDING_ABOVE,
    AREA_PENDING,
    CUSTOMER_PENDING,
    CUSTOMER_LEDGER,
    CUSTOMER_DETAILS,
    GROUP_MEMBERS,
    CUSTOMER_SETTLEMENT,
    CUSTOMER_BALANCE,
    CUSTOMER_DELIVERY,
    UNKNOWN
}

data class ParsedAssistantCommand(
    val original: String,
    val normalized: String,
    val intent: AssistantIntent,
    val amount: Long? = null,
    val month: Int? = null,
    val year: Int? = null,
    val area: String? = null,
    val groupReference: String? = null
)

object AssistantCommandParser {
    private val monthNames = mapOf(
        "january" to 0, "jan" to 0, "february" to 1, "feb" to 1,
        "march" to 2, "mar" to 2, "april" to 3, "apr" to 3,
        "may" to 4, "june" to 5, "jun" to 5, "july" to 6, "jul" to 6,
        "august" to 7, "aug" to 7, "september" to 8, "sep" to 8,
        "october" to 9, "oct" to 9, "november" to 10, "nov" to 10,
        "december" to 11, "dec" to 11,
        "ஜனவரி" to 0, "பிப்ரவரி" to 1, "மார்ச்" to 2, "ஏப்ரல்" to 3,
        "மே" to 4, "ஜூன்" to 5, "ஜூலை" to 6, "ஆகஸ்ட்" to 7,
        "செப்டம்பர்" to 8, "அக்டோபர்" to 9, "நவம்பர்" to 10, "டிசம்பர்" to 11
    )

    fun normalize(input: String): String {
        val digits = input.map { char ->
            when (char) {
                '௦' -> '0'; '௧' -> '1'; '௨' -> '2'; '௩' -> '3'; '௪' -> '4'
                '௫' -> '5'; '௬' -> '6'; '௭' -> '7'; '௮' -> '8'; '௯' -> '9'
                else -> char
            }
        }.joinToString("")
        return Normalizer.normalize(digits, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('₹', ' ')
            .replace(Regex("([a-z])\\1{2,}"), "$1")
            .replace(Regex("[^\\p{L}\\p{N}.,-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun parse(input: String): ParsedAssistantCommand {
        val text = normalize(input)
        if (text.isBlank()) return ParsedAssistantCommand(input, text, AssistantIntent.UNKNOWN)
        val amount = extractAmount(text)
        val month = monthNames.entries.firstOrNull { (name, _) -> Regex("(^|\\s)$name($|\\s)").containsMatchIn(text) }?.value
        val year = Regex("\\b(20\\d{2})\\b").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val hasCollection = hasAny(text, "collection", "payment", "received", "vasool", "வசூல்", "கலெக்ஷன்")
        val hasPending = hasAny(text, "pending", "due", "baaki", "baki", "balance pending", "பாக்கி", "நிலுவை")
        val hasDetails = hasAny(text, "details", "detail", "full details", "விவரம்", "தகவல்")
        val groupRef = Regex("(?:chit|group|cits)\\s*(?:no|number)?\\s*[-:]?\\s*([a-z]*-?\\d+)").find(text)?.groupValues?.getOrNull(1)

        val intent = when {
            hasAny(text, "help", "what can you do", "commands", "உதவி") -> AssistantIntent.HELP
            hasCollection && hasAny(text, "today", "innaiku", "indru", "இன்று") -> AssistantIntent.TODAY_COLLECTION
            hasCollection && (month != null || hasAny(text, "last month", "pona maasam", "previous month", "current month", "intha month")) -> AssistantIntent.MONTH_COLLECTION
            hasPending && amount != null && hasAny(text, "above", "mela", "more than", "greater", "மேல") -> AssistantIntent.PENDING_ABOVE
            hasPending && hasAny(text, "area wise", "area", "ooru", "place", "ஏரியா") -> AssistantIntent.AREA_PENDING
            groupRef != null && hasAny(text, "members", "customers", "member", "customer") -> AssistantIntent.GROUP_MEMBERS
            hasAny(text, "settlement", "settled") -> AssistantIntent.CUSTOMER_SETTLEMENT
            hasAny(text, "delivery", "payout") -> AssistantIntent.CUSTOMER_DELIVERY
            hasAny(text, "balance") && !hasPending -> AssistantIntent.CUSTOMER_BALANCE
            hasAny(text, "ledger", "statement", "kanakku", "கணக்கு") -> AssistantIntent.CUSTOMER_LEDGER
            hasPending && isGenericPending(text) -> AssistantIntent.PENDING_SUMMARY
            hasPending -> AssistantIntent.CUSTOMER_PENDING
            hasDetails || hasAny(text, "customer", "member", "phone", "mobile", "address") -> AssistantIntent.CUSTOMER_DETAILS
            hasCollection -> AssistantIntent.MONTH_COLLECTION
            else -> AssistantIntent.UNKNOWN
        }
        val area = if (intent == AssistantIntent.AREA_PENDING) extractArea(text) else null
        return ParsedAssistantCommand(input, text, intent, amount, month, year, area, groupRef)
    }

    private fun isGenericPending(text: String): Boolean {
        val stripped = text.replace(Regex("\\b(total|all|customer|customers|details|detail|list|show|kaatu|sollu|kudu|pending|due|baaki|baki|amount|evlo)\\b"), "").trim()
        return stripped.isBlank() || stripped in setOf("எவ்வளவு", "காட்டு", "சொல்லு", "விவரம்")
    }

    private fun extractAmount(text: String): Long? {
        val match = Regex("(?:₹\\s*)?(\\d[\\d,]*(?:\\.\\d+)?)\\s*(lakh|lakhs|lac|k|thousand)?").findAll(text)
            .mapNotNull { found ->
                val raw = found.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
                if (raw.toLong() in 1900..2100 && found.groupValues[2].isBlank()) return@mapNotNull null
                val multiplier = when (found.groupValues[2]) { "lakh", "lakhs", "lac" -> 100_000.0; "k", "thousand" -> 1_000.0; else -> 1.0 }
                (raw * multiplier).toLong()
            }.toList()
        return match.firstOrNull()
    }

    private fun extractArea(text: String): String? {
        val match = Regex("(?:in|at|area|ooru|place)\\s+([\\p{L} ]+?)(?:\\s+(?:pending|due|details|list|kaatu|sollu)|$)").find(text)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun hasAny(text: String, vararg phrases: String) = phrases.any { text.contains(it) }
}
