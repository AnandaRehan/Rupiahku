package com.ehan.rupiahku.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ehan.rupiahku.MainActivity

object NotificationHelper {

    private const val CHANNEL_BILL_REMINDERS = "channel_bill_reminders"
    private const val CHANNEL_CLOUD_BACKUP = "channel_cloud_backup"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val billChannel = NotificationChannel(
                CHANNEL_BILL_REMINDERS,
                "Pengingat Tagihan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jatuh tempo pembayaran tagihan rutin"
                enableVibration(true)
            }

            val backupChannel = NotificationChannel(
                CHANNEL_CLOUD_BACKUP,
                "Backup Cloud Otomatis",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi status backup data keuangan ke cloud"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(billChannel)
            manager.createNotificationChannel(backupChannel)
        }
    }

    fun showBillReminderNotification(
        context: Context,
        billId: Long,
        billTitle: String,
        amountFormatted: String,
        statusText: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "bills")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            billId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BILL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 Pengingat Tagihan: $billTitle")
            .setContentText("Tagihan $amountFormatted $statusText. Ketuk untuk bayar sekarang.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Tagihan $billTitle sebesar $amountFormatted $statusText. Jangan lupa lakukan pembayaran tepat waktu agar tidak terkena denda.")
            )
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((1000 + billId).toInt(), notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun showBackupSuccessNotification(
        context: Context,
        recordCount: Int,
        timestampFormatted: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "backup")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CLOUD_BACKUP)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("☁️ Backup Cloud Otomatis Berhasil")
            .setContentText("$recordCount catatan keuangan telah tersimpan aman ($timestampFormatted).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(2001, notification)
        } catch (e: SecurityException) {
            // Permission catch
        }
    }
}
