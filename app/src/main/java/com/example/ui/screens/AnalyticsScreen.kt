package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BudgetAlertLevel
import com.example.data.CategorySummary
import com.example.data.StudentFinancialHealth
import com.example.notification.BudgetNotificationHelper
import com.example.ui.components.CategoryUtils
import com.example.ui.components.EditCategoryBudgetDialog
import com.example.ui.theme.CashGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningYellow

@Composable
fun AnalyticsScreen(
    monthlyBudget: Long,
    totalExpense: Long,
    totalIncome: Long,
    categorySummaries: List<CategorySummary>,
    financialHealth: StudentFinancialHealth,
    onUpdateCategoryBudget: (category: String, amount: Long) -> Unit,
    onTestAlert: (level: BudgetAlertLevel, category: String) -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(BudgetNotificationHelper.isNotificationPermissionGranted(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var editingCategory by remember { mutableStateOf<Pair<String, Long>?>(null) }

    if (editingCategory != null) {
        val (cat, curLimit) = editingCategory!!
        EditCategoryBudgetDialog(
            category = cat,
            currentBudget = curLimit,
            onDismiss = { editingCategory = null },
            onSave = { category, newBudget ->
                onUpdateCategoryBudget(category, newBudget)
                editingCategory = null
            }
        )
    }

    val budgetProgress = if (monthlyBudget > 0) {
        (totalExpense.toFloat() / monthlyBudget.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val budgetPercentage = (budgetProgress * 100).toInt()
    val remainingBudget = (monthlyBudget - totalExpense).coerceAtLeast(0L)

    val progressColor = when {
        budgetProgress > 0.85f -> ExpenseRed
        budgetProgress > 0.6f -> WarningYellow
        else -> CashGreen
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analisis & Budgeting Mahasiswa",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pantau target budget & notifikasi pengeluaran per kategori",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Notification System Banner & Quick Test
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_notification_status"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CashGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = CashGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sistem Notifikasi Budget AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasNotificationPermission) "Aktif • Peringatan 80% & 100%" else "Izin notifikasi belum diaktifkan",
                                    fontSize = 11.sp,
                                    color = if (hasNotificationPermission) CashGreen else WarningYellow
                                )
                            }
                        }

                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                                modifier = Modifier.testTag("btn_request_notification_permission")
                            ) {
                                Text(
                                    text = "Aktifkan",
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CashBuddy otomatis mendeteksi transaksi dan memberi notifikasi saat pengeluaran kategori mencapai 80% dan 100% dari limit.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick test buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestAlert(BudgetAlertLevel.WARNING_80, "Makanan") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_alert_80"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tes Notif 80%", fontSize = 11.sp, color = WarningYellow)
                        }

                        OutlinedButton(
                            onClick = { onTestAlert(BudgetAlertLevel.EXCEEDED_100, "Hiburan") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_alert_100"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tes Notif 100%", fontSize = 11.sp, color = ExpenseRed)
                        }
                    }
                }
            }
        }

        // Monthly Budget Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = CashGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Target Total Budget Bulanan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "$budgetPercentage% Terpakai",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = progressColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Terpakai",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CategoryUtils.formatRupiah(totalExpense),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Sisa Budget",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CategoryUtils.formatRupiah(remainingBudget),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (remainingBudget > 0) CashGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily allowance recommendation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rekomendasi jajan: ${CategoryUtils.formatRupiah(financialHealth.safeDailySpend)}/hari untuk ${financialHealth.daysRemainingInMonth} hari ke depan.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Category Budgets & Alert Thresholds Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = CashGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Limit Budget Per Kategori",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Klik 'Ubah' untuk limit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (categorySummaries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada kategori yang dikonfigurasi.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(categorySummaries, key = { it.category }) { cat ->
                CategoryBudgetCardItem(
                    summary = cat,
                    onEditBudget = { category, currentLimit ->
                        editingCategory = Pair(category, currentLimit)
                    }
                )
            }
        }

        // Student Financial Hacks
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = WarningYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tips & Hack Finansial Mahasiswa",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            HackCard(
                title = "Aturan 24 Jam Sebelum Check Out",
                desc = "Kalau mau beli barang non-esensial (baju, game, skincare viral), tunggu 24 jam. Biasanya keinginan impulsif itu hilang setelah sehari!",
                emoji = "⏳"
            )
        }

        item {
            HackCard(
                title = "Rice Cooker Multi-fungsi Kosan",
                desc = "Selain masak nasi, rice cooker bisa buat rebus telur, bikin sup jagung, dan kukus sayur. Hemat biaya makan sampai 40% per bulan!",
                emoji = "🍚"
            )
        }

        item {
            HackCard(
                title = "Manfaatkan Fasilitas Kampus",
                desc = "Isi ulang tumbler di dispenser kampus, pinjam buku di perpus daripada beli baru, dan manfaatkan akun email student untuk diskon software.",
                emoji = "🎓"
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun CategoryBudgetCardItem(
    summary: CategorySummary,
    onEditBudget: (category: String, currentLimit: Long) -> Unit
) {
    val visual = CategoryUtils.getCategoryVisual(summary.category)
    val budgetPercentageInt = (summary.budgetPercentage * 100).toInt()

    val progressColor = when (summary.alertLevel) {
        BudgetAlertLevel.EXCEEDED_100 -> ExpenseRed
        BudgetAlertLevel.WARNING_80 -> WarningYellow
        BudgetAlertLevel.NORMAL -> CashGreen
    }

    val statusBadgeText = when (summary.alertLevel) {
        BudgetAlertLevel.EXCEEDED_100 -> "🚨 Melebihi Budget ($budgetPercentageInt%)"
        BudgetAlertLevel.WARNING_80 -> "⚠️ Waspada ($budgetPercentageInt%)"
        BudgetAlertLevel.NORMAL -> if (summary.totalAmount > 0) "✅ Aman ($budgetPercentageInt%)" else "Belum Ada Pengeluaran"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_budget_card_${summary.category}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (summary.alertLevel != BudgetAlertLevel.NORMAL) {
            androidx.compose.foundation.BorderStroke(1.dp, progressColor.copy(alpha = 0.5f))
        } else null
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(visual.containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = null,
                            tint = visual.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${summary.count} transaksi dicatat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Edit Budget Button
                Button(
                    onClick = { onEditBudget(summary.category, summary.budgetLimit) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("btn_edit_budget_${summary.category}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ubah Budget",
                        tint = CashGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ubah",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spent vs Budget Limit Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Terpakai",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CategoryUtils.formatRupiah(summary.totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (summary.alertLevel == BudgetAlertLevel.EXCEEDED_100) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Batas Budget",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CategoryUtils.formatRupiah(summary.budgetLimit),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { summary.budgetPercentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge & Alert Level Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }

                val remaining = summary.budgetLimit - summary.totalAmount
                Text(
                    text = if (remaining >= 0) "Sisa ${CategoryUtils.formatRupiah(remaining)}" else "Lebih ${CategoryUtils.formatRupiah(-remaining)}",
                    fontSize = 11.sp,
                    color = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HackCard(title: String, desc: String, emoji: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
