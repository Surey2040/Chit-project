package com.jothivel.chits.ui.agent

import android.content.Context
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.InstallmentEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.data.local.entity.PaymentEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class LocalAssistantEngine(context: Context) {
    private val db = AppDatabase.getDatabase(context.applicationContext)

    fun answer(message: String): String {
        val command = AssistantCommandParser.parse(message)
        return when (command.intent) {
            AssistantIntent.HELP -> help()
            AssistantIntent.TODAY_COLLECTION -> collectionForDay(System.currentTimeMillis(), "Innaiku")
            AssistantIntent.MONTH_COLLECTION -> collectionForMonth(command)
            AssistantIntent.PENDING_SUMMARY -> pendingSummary()
            AssistantIntent.PENDING_ABOVE -> pendingAbove(command.amount ?: 0L)
            AssistantIntent.AREA_PENDING -> areaPending(command)
            AssistantIntent.GROUP_MEMBERS -> groupMembers(command)
            AssistantIntent.CUSTOMER_PENDING,
            AssistantIntent.CUSTOMER_LEDGER,
            AssistantIntent.CUSTOMER_DETAILS,
            AssistantIntent.CUSTOMER_SETTLEMENT,
            AssistantIntent.CUSTOMER_BALANCE,
            AssistantIntent.CUSTOMER_DELIVERY -> customerAnswer(command)
            AssistantIntent.UNKNOWN -> "Request clear-a puriyala. ‘Gopal pending evlo?’, ‘DEMO-C022 ledger kaatu’, ‘5000-ku mela pending’, அல்லது ‘innaiku collection’ madhiri கேளுங்கள்."
        }
    }

    private fun customerAnswer(command: ParsedAssistantCommand): String {
        return when (val resolution = resolveCustomer(command.normalized)) {
            CustomerResolution.None -> "Matching customer கிடைக்கவில்லை. Name, exact customer code அல்லது 10-digit mobile number use பண்ணுங்க."
            is CustomerResolution.Many -> buildString {
                append("${resolution.members.size} customers match ஆகுறாங்க. Correct customer-ஐ code/mobile வைத்து சொல்லுங்க:")
                resolution.members.take(6).forEach { append("\n${it.name} • ${it.id} • ${it.phone ?: "No mobile"} • ${it.city ?: "No area"}") }
            }
            is CustomerResolution.One -> formatCustomer(command.intent, account(resolution.member))
        }
    }

    private fun formatCustomer(intent: AssistantIntent, account: Account): String {
        val member = account.member
        val groupName = account.group?.registerNo ?: member.selectedChitId ?: "Unavailable"
        return when (intent) {
            AssistantIntent.CUSTOMER_PENDING -> buildString {
                append("${member.name} (${member.id}) pending ${money(account.pending)}. Paid ${money(account.paid)} / Payable ${money(account.payable)}.")
                if (account.pendingInstallments.isNotEmpty()) append("\nPending installments: ${account.pendingInstallments.take(8).joinToString { "$it TH" }}")
            }
            AssistantIntent.CUSTOMER_LEDGER -> "${member.name} (${member.id})\nChit: $groupName • Ticket: ${member.ticketNo ?: "-"}\nPayable: ${money(account.payable)}\nPaid: ${money(account.paid)}\nPending: ${money(account.pending)}\nPayments: ${account.payments.size}"
            AssistantIntent.CUSTOMER_DETAILS -> "${member.name} (${member.id})\nMobile: ${member.phone ?: "Unavailable"}\nAddress: ${listOfNotNull(member.addressLine, member.city, member.pincode).joinToString(", ").ifBlank { "Unavailable" }}\nChit: $groupName • Ticket: ${member.ticketNo ?: "-"}\nJoining: ${member.joiningDate ?: "Unavailable"} • Due: ${member.dueDate ?: "Unavailable"}"
            AssistantIntent.CUSTOMER_SETTLEMENT -> "${member.name} (${member.id}) settlement/payable ${money(account.payable)}. Paid ${money(account.paid)}; pending ${money(account.pending)}."
            AssistantIntent.CUSTOMER_BALANCE -> "${member.name} (${member.id}) balance ${money(account.paid - account.payable)} (Paid − Payable). Pending ${money(account.pending)}."
            AssistantIntent.CUSTOMER_DELIVERY -> "${member.name} (${member.id})-க்கு delivery amount field current Room schema-ல் இல்லை. நான் amount invent பண்ணமாட்டேன்."
            else -> help()
        }
    }

    private fun pendingSummary(): String {
        val accounts = db.memberDao().getAllMembersSync().map(::account).filter { it.pending > 0 }.sortedByDescending { it.pending }
        if (accounts.isEmpty()) return "Room DB-ல் pending customers இல்லை."
        return buildString {
            append("Total pending ${money(accounts.sumOf { it.pending })} • ${accounts.size} customers")
            accounts.take(8).forEach { append("\n${it.member.name} (${it.member.id}) • ${money(it.pending)}") }
            if (accounts.size > 8) append("\nமேலும் ${accounts.size - 8} customers இருக்காங்க.")
        }
    }

    private fun pendingAbove(amount: Long): String {
        val accounts = db.memberDao().getAllMembersSync().map(::account).filter { it.pending > amount }.sortedByDescending { it.pending }
        if (accounts.isEmpty()) return "${money(amount)}-க்கு மேல pending customer யாரும் இல்லை."
        return buildString {
            append("${money(amount)}-க்கு மேல ${accounts.size} customers • Total ${money(accounts.sumOf { it.pending })}")
            accounts.take(10).forEach { append("\n${it.member.name} (${it.member.id}) • ${money(it.pending)}") }
        }
    }

    private fun areaPending(command: ParsedAssistantCommand): String {
        val area = command.area
        if (area.isNullOrBlank()) return "எந்த area pending வேணும்? Example: Manapparai area pending."
        val accounts = db.memberDao().getAllMembersSync().filter {
            it.city?.contains(area, true) == true || it.addressLine?.contains(area, true) == true
        }.map(::account).filter { it.pending > 0 }.sortedByDescending { it.pending }
        if (accounts.isEmpty()) return "‘$area’ area-வில் pending customer கிடைக்கவில்லை."
        return buildString {
            append("$area area pending ${money(accounts.sumOf { it.pending })} • ${accounts.size} customers")
            accounts.take(8).forEach { append("\n${it.member.name} (${it.member.id}) • ${money(it.pending)}") }
        }
    }

    private fun groupMembers(command: ParsedAssistantCommand): String {
        val ref = command.groupReference ?: return "Chit/group number சொல்லுங்க. Example: Cits-2 members."
        val groups = db.groupDao().getAllGroupsSync().filter {
            normalizeKey(it.id) == normalizeKey(ref) || normalizeKey(it.registerNo) == normalizeKey(ref) || it.registerNo?.contains(ref, true) == true
        }
        if (groups.isEmpty()) return "‘$ref’ chit/group கிடைக்கவில்லை."
        if (groups.size > 1) return "Multiple groups match: ${groups.joinToString { it.registerNo ?: it.id }}. Exact group சொல்லுங்க."
        val group = groups.first()
        val members = db.memberDao().getAllMembersSync().filter { it.selectedChitId == group.id }
        return "${group.registerNo ?: group.id} • ${members.size} members" + members.take(10).joinToString("") { "\n${it.name} (${it.id}) • Ticket ${it.ticketNo ?: "-"}" }
    }

    private fun collectionForDay(timestamp: Long, label: String): String {
        val start = Calendar.getInstance().apply { timeInMillis = timestamp; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val payments = db.paymentDao().getAllPaymentsSync().filter { it.paidAt in start until end }
        return "$label total collection ${money(payments.sumOf { it.amountPaid } / 100)} • ${payments.size} payment entries."
    }

    private fun collectionForMonth(command: ParsedAssistantCommand): String {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        when {
            command.month != null -> { calendar.set(Calendar.MONTH, command.month); calendar.set(Calendar.YEAR, command.year ?: now.get(Calendar.YEAR)) }
            command.normalized.contains("last month") || command.normalized.contains("pona maasam") || command.normalized.contains("previous month") -> calendar.add(Calendar.MONTH, -1)
            else -> Unit
        }
        calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val payments = db.paymentDao().getAllPaymentsSync().filter { it.paidAt in start until calendar.timeInMillis }
        val title = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(start)
        return "$title collection ${money(payments.sumOf { it.amountPaid } / 100)} • ${payments.size} payment entries."
    }

    private fun resolveCustomer(text: String): CustomerResolution {
        val members = db.memberDao().getAllMembersSync()
        val compact = normalizeKey(text)
        val code = Regex("(?:demo[- ]?)?c[- ]?\\d{1,4}").find(text)?.value?.let(::normalizeKey)
        if (code != null) {
            val exact = members.filter { normalizeKey(it.id) == code }
            return resolution(exact)
        }
        val mobile = Regex("(?<!\\d)[6-9]\\d{9}(?!\\d)").find(text)?.value
        if (mobile != null) return resolution(members.filter { it.phone?.filter(Char::isDigit) == mobile })

        val fullNameMatches = members.filter { member ->
            val name = AssistantCommandParser.normalize(member.name ?: "")
            name.isNotBlank() && Regex("(^|\\s)${Regex.escape(name)}($|\\s)").containsMatchIn(text)
        }
        if (fullNameMatches.isNotEmpty()) return resolution(fullNameMatches)

        val queryTokens = text.split(' ').filter { token -> token.length >= 3 && token !in CUSTOMER_STOP_WORDS && token.none(Char::isDigit) }
        if (queryTokens.isEmpty()) return CustomerResolution.None
        val scored = members.map { member ->
            val nameTokens = AssistantCommandParser.normalize(member.name ?: "").split(' ')
            val exactTokens = queryTokens.count { it in nameTokens }
            val prefixTokens = queryTokens.count { q -> nameTokens.any { it.startsWith(q) || q.startsWith(it) } }
            val fuzzy = queryTokens.count { q -> nameTokens.any { levenshtein(q, it) <= if (q.length >= 6) 2 else 1 } }
            member to (exactTokens * 30 + prefixTokens * 20 + fuzzy * 10)
        }
        val max = scored.maxOfOrNull { it.second } ?: 0
        if (max < 20) return CustomerResolution.None
        return resolution(scored.filter { it.second == max }.map { it.first })
    }

    private fun account(member: MemberEntity): Account {
        val group = member.selectedChitId?.let { db.groupDao().getGroupByIdSync(it) }
        val installments = member.selectedChitId?.let { db.installmentDao().getInstallmentsForGroupSync(it) }.orEmpty()
        val payments = db.paymentDao().getPaymentsByMemberSync(member.id)
        val payablePaise = if (installments.isNotEmpty()) installments.sumOf { it.baseAmount.toLong() - (it.kasaruAmount ?: 0).toLong() }
            else group?.chitValue?.toLong() ?: ((member.installmentAmount?.toLongOrNull() ?: 0L) * 20 * 100)
        val paidPaise = payments.sumOf { it.amountPaid }
        val paidByInstallment = payments.groupBy { it.installmentId?.toIntOrNull() }.mapValues { entry -> entry.value.sumOf { it.amountPaid } }
        val pendingInstallments = installments.filter { installment ->
            val due = installment.baseAmount.toLong() - (installment.kasaruAmount ?: 0).toLong()
            (paidByInstallment[installment.installmentNo] ?: 0L) < due
        }.map { it.installmentNo }
        return Account(member, group, installments, payments, payablePaise / 100, paidPaise / 100, ((payablePaise - paidPaise).coerceAtLeast(0)) / 100, pendingInstallments)
    }

    private fun resolution(matches: List<MemberEntity>): CustomerResolution = when (matches.size) {
        0 -> CustomerResolution.None
        1 -> CustomerResolution.One(matches.first())
        else -> CustomerResolution.Many(matches.sortedBy { it.name })
    }

    private fun help() = "நான் செய்யக்கூடியவை:\n• Gopal pending evlo?\n• DEMO-C022 ledger kaatu\n• 5000-ku mela pending\n• Manapparai area pending\n• August 2026 collection\n• Cits-2 members"
    private fun money(value: Long): String = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
    private fun normalizeKey(value: String?) = value.orEmpty().lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        left.forEachIndexed { i, l ->
            val current = IntArray(right.length + 1); current[0] = i + 1
            right.forEachIndexed { j, r -> current[j + 1] = min(min(current[j] + 1, previous[j + 1] + 1), previous[j] + if (l == r) 0 else 1) }
            previous = current
        }
        return previous[right.length]
    }

    private data class Account(
        val member: MemberEntity,
        val group: ChitGroupEntity?,
        val installments: List<InstallmentEntity>,
        val payments: List<PaymentEntity>,
        val payable: Long,
        val paid: Long,
        val pending: Long,
        val pendingInstallments: List<Int>
    )

    private sealed interface CustomerResolution {
        data object None : CustomerResolution
        data class One(val member: MemberEntity) : CustomerResolution
        data class Many(val members: List<MemberEntity>) : CustomerResolution
    }

    companion object {
        private val CUSTOMER_STOP_WORDS = setOf(
            "customer", "member", "details", "detail", "pending", "ledger", "statement", "amount", "balance", "settlement", "delivery",
            "oda", "evlo", "evalo", "sollu", "solu", "kaatu", "kattu", "kudu", "full", "phone", "mobile", "number", "please", "show", "get", "tell"
        )
    }
}
