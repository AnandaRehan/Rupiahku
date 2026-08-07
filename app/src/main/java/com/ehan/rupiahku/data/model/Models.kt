package com.ehan.rupiahku.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val categoryName: String,
    val accountName: String = "Dompet Utama", // e.g. "Dompet Utama", "BCA", "GoPay", "OVO"
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String, // e.g., "fastfood", "work", "directions_car", "receipt", "shopping_bag"
    val colorHex: String,
    val isIncome: Boolean = false,
    val budgetLimit: Double = 0.0 // 0.0 means no budget set
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryName: String,
    val dueDateMillis: Long,
    val repeatInterval: String = "MONTHLY", // "ONCE", "WEEKLY", "MONTHLY", "YEARLY"
    val isPaid: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val lastPaidDateMillis: Long? = null
)

@Entity(tableName = "backup_history")
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val recordCount: Int,
    val backupType: String = "CLOUD_AUTO", // "CLOUD_AUTO", "CLOUD_MANUAL"
    val fileSizeFormatted: String = "0 KB",
    val status: String = "SUCCESS" // "SUCCESS", "FAILED"
)

data class AppSettings(
    val autoBackupEnabled: Boolean = true,
    val backupFrequency: String = "Harian", // "Real-time", "Harian", "Mingguan"
    val backupAccountEmail: String = "user@cloud.backup",
    val notificationEnabled: Boolean = true,
    val currencyCode: String = "IDR"
)

data class DashboardSummary(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val activeBillsCount: Int,
    val overdueBillsCount: Int
)

data class BackupExportData(
    val exportDateMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val currency: String = "IDR",
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val bills: List<BillEntity>,
    val backupLogs: List<BackupHistoryEntity>
)
