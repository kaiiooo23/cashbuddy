package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.CashGreen

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MainScreen(viewModel: CashBuddyViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val financialHealth by viewModel.financialHealth.collectAsState()
    val categorySummaries by viewModel.categorySummaries.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val studentName by viewModel.studentName.collectAsState()
    val studentCampus by viewModel.studentCampus.collectAsState()
    val activeAlert by viewModel.activeAlert.collectAsState()

    // Handle back press to return to Home Tab
    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectTab(0)
    }

    val navItems = listOf(
        NavItem("Dompet", Icons.Default.AccountBalanceWallet, "nav_item_home"),
        NavItem("CashBuddy AI", Icons.Default.AutoAwesome, "nav_item_chat"),
        NavItem("Analisis", Icons.Default.BarChart, "nav_item_analytics"),
        NavItem("Profil", Icons.Default.Person, "nav_item_profile")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(index) },
                        modifier = Modifier.testTag(item.testTag),
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CashGreen,
                            indicatorColor = CashGreen,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    balance = currentBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    financialHealth = financialHealth,
                    transactions = transactions,
                    studentName = studentName,
                    activeAlert = activeAlert,
                    onDismissAlert = { viewModel.dismissActiveAlert() },
                    onNavigateToChat = { viewModel.selectTab(1) },
                    onQuickInput = { query ->
                        viewModel.sendChatMessage(query)
                        viewModel.selectTab(1)
                    },
                    onAddManual = { type, amount, category, desc, date ->
                        viewModel.addManualTransaction(type, amount, category, desc, date)
                    },
                    onDeleteTransaction = { id ->
                        viewModel.deleteTransaction(id)
                    }
                )

                1 -> ChatScreen(
                    messages = chatMessages,
                    isAiThinking = isAiThinking,
                    onSendMessage = { query ->
                        viewModel.sendChatMessage(query)
                    },
                    onUndoTransactions = { messageId, ids ->
                        viewModel.undoTransactions(messageId, ids)
                    }
                )

                2 -> AnalyticsScreen(
                    monthlyBudget = monthlyBudget,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    categorySummaries = categorySummaries,
                    financialHealth = financialHealth,
                    onUpdateCategoryBudget = { category, amount ->
                        viewModel.updateCategoryBudget(category, amount)
                    },
                    onTestAlert = { level, category ->
                        viewModel.testBudgetAlert(level, category)
                    }
                )

                3 -> ProfileScreen(
                    studentName = studentName,
                    studentCampus = studentCampus,
                    monthlyBudget = monthlyBudget,
                    onUpdateBudget = { newBudget -> viewModel.updateMonthlyBudget(newBudget) },
                    onUpdateProfile = { name, campus -> viewModel.updateProfile(name, campus) },
                    onReloadDemo = { viewModel.reloadDemoData() },
                    onResetAll = { viewModel.resetAllData() }
                )
            }
        }
    }
}
