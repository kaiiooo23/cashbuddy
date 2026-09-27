package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.CashGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningYellow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CashBuddyRepository(
    private val transactionDao: TransactionDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cashbuddy_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_STUDENT_NAME = "student_name"
        private const val KEY_STUDENT_CAMPUS = "student_campus"
        private const val KEY_DEMO_INITIALIZED = "demo_initialized"
        private const val DEFAULT_MONTHLY_BUDGET = 2_000_000L // 2 Juta Rupiah default budget

        val DEFAULT_CATEGORY_BUDGETS = mapOf(
            "Makanan" to 800_000L,
            "Transportasi" to 250_000L,
            "Akademik" to 200_000L,
            "Hiburan" to 250_000L,
            "Tagihan" to 700_000L,
            "Lainnya" to 150_000L
        )
    }

    fun getCategoryBudget(category: String): Long {
        val defaultVal = DEFAULT_CATEGORY_BUDGETS[category] ?: 200_000L
        return prefs.getLong("category_budget_$category", defaultVal)
    }

    fun setCategoryBudget(category: String, amount: Long) {
        prefs.edit().putLong("category_budget_$category", amount).apply()
    }

    fun getAllCategoryBudgets(): Map<String, Long> {
        return DEFAULT_CATEGORY_BUDGETS.keys.associateWith { getCategoryBudget(it) }
    }

    fun checkBudgetAlert(
        category: String,
        currentSpent: Long,
        addedAmount: Long
    ): BudgetAlertEvent? {
        val budget = getCategoryBudget(category)
        if (budget <= 0) return null

        val previousSpent = currentSpent
        val newSpent = currentSpent + addedAmount

        val prevPct = (previousSpent.toDouble() / budget * 100).toInt()
        val newPct = (newSpent.toDouble() / budget * 100).toInt()

        val formattedSpent = String.format(Locale.GERMANY, "Rp %,d", newSpent)
        val formattedLimit = String.format(Locale.GERMANY, "Rp %,d", budget)

        return when {
            prevPct < 100 && newPct >= 100 -> {
                BudgetAlertEvent(
                    category = category,
                    spentAmount = newSpent,
                    budgetLimit = budget,
                    percentage = newPct,
                    level = BudgetAlertLevel.EXCEEDED_100,
                    message = "🚨 Peringatan: Pengeluaran $category ($formattedSpent) telah MELEBIHI batas budget $formattedLimit ($newPct%). Mode hemat wajib aktif!"
                )
            }
            prevPct < 80 && newPct >= 80 -> {
                BudgetAlertEvent(
                    category = category,
                    spentAmount = newSpent,
                    budgetLimit = budget,
                    percentage = newPct,
                    level = BudgetAlertLevel.WARNING_80,
                    message = "⚠️ Waspada: Pengeluaran $category sudah mencapai $formattedSpent ($newPct%) dari target budget $formattedLimit. Rem jajan dulu ya!"
                )
            }
            else -> null
        }
    }

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(10)
    val totalExpense: Flow<Long> = transactionDao.getTotalExpense().map { it ?: 0L }
    val totalIncome: Flow<Long> = transactionDao.getTotalIncome().map { it ?: 0L }

    suspend fun insertTransaction(item: TransactionItem): Long {
        val entity = TransactionEntity(
            type = item.type,
            amount = item.amount,
            category = item.category,
            description = item.description,
            date = item.date,
            timestamp = System.currentTimeMillis()
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun insertManual(
        type: String,
        amount: Long,
        category: String,
        description: String,
        date: String
    ): Long {
        val entity = TransactionEntity(
            type = type,
            amount = amount,
            category = category,
            description = description,
            date = date,
            timestamp = System.currentTimeMillis()
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun clearAll() {
        transactionDao.clearAll()
    }

    fun getMonthlyBudget(): Long {
        return prefs.getLong(KEY_MONTHLY_BUDGET, DEFAULT_MONTHLY_BUDGET)
    }

    fun setMonthlyBudget(amount: Long) {
        prefs.edit().putLong(KEY_MONTHLY_BUDGET, amount).apply()
    }

    fun getStudentName(): String {
        return prefs.getString(KEY_STUDENT_NAME, "Mahasiswa Pejuang") ?: "Mahasiswa Pejuang"
    }

    fun setStudentName(name: String) {
        prefs.edit().putString(KEY_STUDENT_NAME, name).apply()
    }

    fun getStudentCampus(): String {
        return prefs.getString(KEY_STUDENT_CAMPUS, "Universitas Indonesia") ?: "Universitas Indonesia"
    }

    fun setStudentCampus(campus: String) {
        prefs.edit().putString(KEY_STUDENT_CAMPUS, campus).apply()
    }

    fun isDemoInitialized(): Boolean {
        return prefs.getBoolean(KEY_DEMO_INITIALIZED, false)
    }

    fun setDemoInitialized(value: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_INITIALIZED, value).apply()
    }

    suspend fun populateDemoData() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val threeDaysAgo = dateFormat.format(calendar.time)

        val demoList = listOf(
            TransactionEntity(
                type = "INCOME",
                amount = 2_000_000L,
                category = "Uang Saku",
                description = "Kiriman Uang Saku Ortu Awal Bulan",
                date = threeDaysAgo,
                timestamp = System.currentTimeMillis() - 86400000 * 3
            ),
            TransactionEntity(
                type = "EXPENSE",
                amount = 650_000L,
                category = "Tagihan",
                description = "Bayar Kosan Bulanan + Wifi",
                date = threeDaysAgo,
                timestamp = System.currentTimeMillis() - 86400000 * 3 + 3600000
            ),
            TransactionEntity(
                type = "EXPENSE",
                amount = 22_000L,
                category = "Makanan",
                description = "Nasi Padang Rendang + Es Teh",
                date = yesterday,
                timestamp = System.currentTimeMillis() - 86400000 + 7200000
            ),
            TransactionEntity(
                type = "EXPENSE",
                amount = 15_000L,
                category = "Transportasi",
                description = "Bensin Pertalite Motor",
                date = yesterday,
                timestamp = System.currentTimeMillis() - 86400000 + 10800000
            ),
            TransactionEntity(
                type = "EXPENSE",
                amount = 35_000L,
                category = "Akademik",
                description = "Print Makalah & Jilid Skripsi",
                date = today,
                timestamp = System.currentTimeMillis() - 3600000
            ),
            TransactionEntity(
                type = "EXPENSE",
                amount = 18_000L,
                category = "Makanan",
                description = "Kopi Susu Gula Aren",
                date = today,
                timestamp = System.currentTimeMillis()
            )
        )
        transactionDao.insertAll(demoList)
        setDemoInitialized(true)
    }

    fun calculateFinancialHealth(
        balance: Long,
        totalExpense: Long,
        monthlyBudget: Long
    ): StudentFinancialHealth {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

        val safeDaily = if (balance > 0) balance / daysRemaining else 0L

        val budgetUsageRatio = if (monthlyBudget > 0) totalExpense.toFloat() / monthlyBudget else 0.5f

        val (title, subtitle, color, emoji) = when {
            balance <= 50_000L -> Quadruple(
                "Mode Mie Instan 🚨",
                "Krisis dompet! Bertahanlah kawan, seduh mie & cari traktiran!",
                0xFFEF4444, // Red
                "🍜"
            )
            budgetUsageRatio > 0.85f || balance < 200_000L -> Quadruple(
                "Mode Waspada ⚠️",
                "Budget menipis. Rem jajan kopi & nongkrong dulu ya!",
                0xFFF59E0B, // Yellow
                "⚠️"
            )
            budgetUsageRatio > 0.5f -> Quadruple(
                "Aman Terkendali 👍",
                "Pengeluaran wajar. Jaga batas jajan harian kamu!",
                0xFF10B981, // Green
                "😎"
            )
            else -> Quadruple(
                "Sultan Awal Bulan 🟢",
                "Saldo gemuk! Tapi tetap ingat nabung buat akhir bulan.",
                0xFF00D632, // CashGreen
                "🤑"
            )
        }

        return StudentFinancialHealth(
            statusTitle = title,
            statusSubtitle = subtitle,
            badgeColorHex = color,
            iconEmoji = emoji,
            safeDailySpend = safeDaily,
            daysRemainingInMonth = daysRemaining
        )
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
