package com.example.data

import java.util.UUID

enum class TransactionType(val value: String) {
    EXPENSE("EXPENSE"),
    INCOME("INCOME")
}

data class TransactionItem(
    val type: String, // "EXPENSE" | "INCOME"
    val amount: Long,
    val category: String, // "Makanan", "Transportasi", "Akademik", "Uang Saku", "Hiburan", "Tagihan", "Lainnya"
    val description: String,
    val date: String // "YYYY-MM-DD"
)

data class CashBuddyAiResponse(
    val mode: String, // "TRANSACTION" | "ASSISTANT"
    val transactions: List<TransactionItem>? = null,
    val reply_message: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "NORMAL",
    val parsedTransactions: List<TransactionItem>? = null,
    val savedTransactionIds: List<Long>? = null,
    val isActionHandled: Boolean = false
)

enum class ChatSender {
    USER,
    BUDDY
}

enum class BudgetAlertLevel {
    NORMAL,
    WARNING_80,
    EXCEEDED_100
}

data class BudgetAlertEvent(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val spentAmount: Long,
    val budgetLimit: Long,
    val percentage: Int,
    val level: BudgetAlertLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CategorySummary(
    val category: String,
    val totalAmount: Long,
    val percentage: Float,
    val count: Int,
    val budgetLimit: Long = 0L,
    val budgetPercentage: Float = 0f,
    val alertLevel: BudgetAlertLevel = BudgetAlertLevel.NORMAL
)

data class StudentFinancialHealth(
    val statusTitle: String,
    val statusSubtitle: String,
    val badgeColorHex: Long,
    val iconEmoji: String,
    val safeDailySpend: Long,
    val daysRemainingInMonth: Int
)
