package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CashBuddyAiService
import com.example.data.BudgetAlertEvent
import com.example.data.BudgetAlertLevel
import com.example.data.CashBuddyDatabase
import com.example.data.CashBuddyRepository
import com.example.data.CategorySummary
import com.example.data.ChatMessage
import com.example.data.ChatSender
import com.example.data.StudentFinancialHealth
import com.example.data.TransactionEntity
import com.example.data.TransactionItem
import com.example.notification.BudgetNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CashBuddyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CashBuddyRepository
    private val aiService = CashBuddyAiService()

    val allTransactions: StateFlow<List<TransactionEntity>>
    val totalExpense: StateFlow<Long>
    val totalIncome: StateFlow<Long>
    val currentBalance: StateFlow<Long>

    private val _monthlyBudget = MutableStateFlow(2_000_000L)
    val monthlyBudget: StateFlow<Long> = _monthlyBudget.asStateFlow()

    private val _categoryBudgets = MutableStateFlow<Map<String, Long>>(emptyMap())
    val categoryBudgets: StateFlow<Map<String, Long>> = _categoryBudgets.asStateFlow()

    private val _activeAlert = MutableStateFlow<BudgetAlertEvent?>(null)
    val activeAlert: StateFlow<BudgetAlertEvent?> = _activeAlert.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<BudgetAlertEvent>>(emptyList())
    val alertHistory: StateFlow<List<BudgetAlertEvent>> = _alertHistory.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Dompet, 1: AI Chat, 2: Analisis, 3: Profil
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _userQuery = MutableStateFlow("")
    val userQuery: StateFlow<String> = _userQuery.asStateFlow()

    private val _studentName = MutableStateFlow("Sobat Kampus")
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _studentCampus = MutableStateFlow("Universitas Indonesia")
    val studentCampus: StateFlow<String> = _studentCampus.asStateFlow()

    init {
        val db = CashBuddyDatabase.getDatabase(application)
        repository = CashBuddyRepository(db.transactionDao(), application)

        BudgetNotificationHelper.initNotificationChannel(application)

        allTransactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        totalExpense = repository.totalExpense.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0L
        )

        totalIncome = repository.totalIncome.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0L
        )

        currentBalance = combine(totalIncome, totalExpense) { income, expense ->
            income - expense
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

        _monthlyBudget.value = repository.getMonthlyBudget()
        _studentName.value = repository.getStudentName()
        _studentCampus.value = repository.getStudentCampus()
        _categoryBudgets.value = repository.getAllCategoryBudgets()

        // Initialize initial welcome message in chat
        initializeWelcomeChat()

        // Populate initial demo data if first launch
        viewModelScope.launch {
            if (!repository.isDemoInitialized()) {
                repository.populateDemoData()
            }
        }
    }

    private fun initializeWelcomeChat() {
        val welcomeMsg = ChatMessage(
            sender = ChatSender.BUDDY,
            message = "Halo Sobat Kampus! 👋 Aku CashBuddy AI, asisten keuangan pribadi kamu.\n\n" +
                    "Ketik aja pengeluaran atau pemasukan kamu pakai bahasa santai, misal:\n" +
                    "• 'Beli kopi 25rb'\n" +
                    "• 'Dapat uang saku 500rb'\n" +
                    "• 'Makan nasi padang 18k'\n\n" +
                    "Atau tanya tips hemat: 'Sisa uang saku 200rb cukup ga buat seminggu?' 💡\n\n" +
                    "🔔 Sekarang aku juga otomatis memantau limit budget kategori kamu (80% & 100%)!",
            mode = "ASSISTANT"
        )
        _chatMessages.value = listOf(welcomeMsg)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun onUserQueryChange(query: String) {
        _userQuery.value = query
    }

    val financialHealth: StateFlow<StudentFinancialHealth> by lazy {
        combine(currentBalance, totalExpense, monthlyBudget) { balance, expense, budget ->
            repository.calculateFinancialHealth(balance, expense, budget)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            repository.calculateFinancialHealth(0L, 0L, 2_000_000L)
        )
    }

    val categorySummaries: StateFlow<List<CategorySummary>> by lazy {
        combine(allTransactions, totalExpense, _categoryBudgets) { transactions, totalExp, budgets ->
            val expenseTx = transactions.filter { it.type == "EXPENSE" }
            val grouped = expenseTx.groupBy { it.category }

            // Ensure all known categories are presented
            val allCategoryNames = (CashBuddyRepository.DEFAULT_CATEGORY_BUDGETS.keys + grouped.keys).distinct()

            allCategoryNames.map { cat ->
                val list = grouped[cat] ?: emptyList()
                val sum = list.sumOf { it.amount }
                val pctOfTotal = if (totalExp > 0) (sum.toFloat() / totalExp.toFloat()) else 0f
                val limit = budgets[cat] ?: repository.getCategoryBudget(cat)
                val budgetPct = if (limit > 0) sum.toFloat() / limit.toFloat() else 0f

                val alertLevel = when {
                    budgetPct >= 1.0f -> BudgetAlertLevel.EXCEEDED_100
                    budgetPct >= 0.8f -> BudgetAlertLevel.WARNING_80
                    else -> BudgetAlertLevel.NORMAL
                }

                CategorySummary(
                    category = cat,
                    totalAmount = sum,
                    percentage = pctOfTotal,
                    count = list.size,
                    budgetLimit = limit,
                    budgetPercentage = budgetPct,
                    alertLevel = alertLevel
                )
            }.sortedByDescending { it.totalAmount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun updateCategoryBudget(category: String, amount: Long) {
        repository.setCategoryBudget(category, amount)
        _categoryBudgets.value = repository.getAllCategoryBudgets()
    }

    fun dismissActiveAlert() {
        _activeAlert.value = null
    }

    fun testBudgetAlert(level: BudgetAlertLevel, category: String = "Makanan") {
        val budget = repository.getCategoryBudget(category)
        val spent = when (level) {
            BudgetAlertLevel.EXCEEDED_100 -> (budget * 1.15).toLong()
            BudgetAlertLevel.WARNING_80 -> (budget * 0.82).toLong()
            BudgetAlertLevel.NORMAL -> (budget * 0.5).toLong()
        }
        val pct = (spent.toDouble() / budget * 100).toInt()
        val formattedSpent = String.format(Locale.GERMANY, "Rp %,d", spent)
        val formattedLimit = String.format(Locale.GERMANY, "Rp %,d", budget)

        val message = when (level) {
            BudgetAlertLevel.EXCEEDED_100 ->
                "🚨 Peringatan: Pengeluaran $category ($formattedSpent) telah MELEBIHI batas budget $formattedLimit ($pct%). Mode hemat wajib aktif!"
            BudgetAlertLevel.WARNING_80 ->
                "⚠️ Waspada: Pengeluaran $category sudah mencapai $formattedSpent ($pct%) dari target budget $formattedLimit. Rem jajan dulu ya!"
            BudgetAlertLevel.NORMAL ->
                "ℹ️ Update: Pengeluaran $category masih aman di $formattedSpent dari budget $formattedLimit."
        }

        val alert = BudgetAlertEvent(
            category = category,
            spentAmount = spent,
            budgetLimit = budget,
            percentage = pct,
            level = level,
            message = message
        )

        sendNotificationAndPostAlert(alert)
    }

    private fun sendNotificationAndPostAlert(alert: BudgetAlertEvent) {
        BudgetNotificationHelper.sendBudgetNotification(getApplication(), alert)
        _activeAlert.value = alert
        _alertHistory.value = listOf(alert) + _alertHistory.value
    }

    fun sendChatMessage(rawInput: String) {
        val input = rawInput.trim()
        if (input.isBlank()) return

        val userMsg = ChatMessage(
            sender = ChatSender.USER,
            message = input
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _userQuery.value = ""

        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                val aiResponse = aiService.processPrompt(input)
                val savedIds = mutableListOf<Long>()
                var triggeredAlert: BudgetAlertEvent? = null

                if (aiResponse.mode == "TRANSACTION" && !aiResponse.transactions.isNullOrEmpty()) {
                    for (item in aiResponse.transactions) {
                        if (item.type == "EXPENSE") {
                            val currentCategorySpent = allTransactions.value
                                .filter { it.type == "EXPENSE" && it.category.equals(item.category, ignoreCase = true) }
                                .sumOf { it.amount }

                            val alert = repository.checkBudgetAlert(item.category, currentCategorySpent, item.amount)
                            if (alert != null) {
                                triggeredAlert = alert
                                sendNotificationAndPostAlert(alert)
                            }
                        }

                        val id = repository.insertTransaction(item)
                        savedIds.add(id)
                    }
                }

                val finalReply = if (triggeredAlert != null) {
                    "${aiResponse.reply_message}\n\n${triggeredAlert.message}"
                } else {
                    aiResponse.reply_message
                }

                val buddyMsg = ChatMessage(
                    sender = ChatSender.BUDDY,
                    message = finalReply,
                    mode = aiResponse.mode,
                    parsedTransactions = aiResponse.transactions,
                    savedTransactionIds = if (savedIds.isNotEmpty()) savedIds else null
                )
                _chatMessages.value = _chatMessages.value + buddyMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    sender = ChatSender.BUDDY,
                    message = "Aduh, koneksi lagi ngadat dikit bro. Tapi santai, coba ulangi lagi ya! 🔄",
                    mode = "ASSISTANT"
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun undoTransactions(messageId: String, ids: List<Long>) {
        viewModelScope.launch {
            for (id in ids) {
                repository.deleteTransaction(id)
            }
            _chatMessages.value = _chatMessages.value.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        isActionHandled = true,
                        message = "${msg.message}\n\n*(Transaksi telah dibatalkan & dihapus dari saldo)*"
                    )
                } else {
                    msg
                }
            }
        }
    }

    fun addManualTransaction(
        type: String,
        amount: Long,
        category: String,
        description: String,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            if (type == "EXPENSE") {
                val currentCategorySpent = allTransactions.value
                    .filter { it.type == "EXPENSE" && it.category.equals(category, ignoreCase = true) }
                    .sumOf { it.amount }

                val alert = repository.checkBudgetAlert(category, currentCategorySpent, amount)
                if (alert != null) {
                    sendNotificationAndPostAlert(alert)
                }
            }
            repository.insertManual(type, amount, category, description, date)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateMonthlyBudget(newBudget: Long) {
        _monthlyBudget.value = newBudget
        repository.setMonthlyBudget(newBudget)
    }

    fun updateProfile(name: String, campus: String) {
        _studentName.value = name
        _studentCampus.value = campus
        repository.setStudentName(name)
        repository.setStudentCampus(campus)
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAll()
            initializeWelcomeChat()
        }
    }

    fun reloadDemoData() {
        viewModelScope.launch {
            repository.clearAll()
            repository.populateDemoData()
            initializeWelcomeChat()
        }
    }
}
