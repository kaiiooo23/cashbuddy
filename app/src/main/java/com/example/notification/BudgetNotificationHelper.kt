package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BudgetAlertEvent
import com.example.data.BudgetAlertLevel
import com.example.ui.components.CategoryUtils

object BudgetNotificationHelper {

    const val CHANNEL_ID = "budget_alerts_channel"
    private const val CHANNEL_NAME = "Peringatan Budget Mahasiswa"
    private const val CHANNEL_DESCRIPTION = "Notifikasi saat pengeluaran kategori mencapai 80% atau melebihi limit budget"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendBudgetNotification(context: Context, alert: BudgetAlertEvent): Boolean {
        initNotificationChannel(context)

        if (!isNotificationPermissionGranted(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = when (alert.level) {
            BudgetAlertLevel.EXCEEDED_100 -> "🚨 Budget ${alert.category} Terlampaui! (100%)"
            BudgetAlertLevel.WARNING_80 -> "⚠️ Peringatan Budget ${alert.category} (80%)"
            BudgetAlertLevel.NORMAL -> "ℹ️ Update Budget ${alert.category}"
        }

        val spentFormatted = CategoryUtils.formatRupiah(alert.spentAmount)
        val limitFormatted = CategoryUtils.formatRupiah(alert.budgetLimit)

        val contentText = when (alert.level) {
            BudgetAlertLevel.EXCEEDED_100 ->
                "Pengeluaran ${alert.category} sebesar $spentFormatted telah melebihi budget $limitFormatted (${alert.percentage}%). Rem jajan dulu ya!"
            BudgetAlertLevel.WARNING_80 ->
                "Pengeluaran ${alert.category} sudah mencapai $spentFormatted (${alert.percentage}%) dari budget $limitFormatted. Waspada jangan sampai boncos!"
            BudgetAlertLevel.NORMAL ->
                "Pengeluaran ${alert.category} saat ini $spentFormatted dari budget $limitFormatted."
        }

        val smallIcon = android.R.drawable.ic_dialog_alert

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationId = alert.category.hashCode() * 31 + alert.level.ordinal

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
