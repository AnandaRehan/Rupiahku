package com.ehan.rupiahku.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ehan.rupiahku.data.model.BackupHistoryEntity
import com.ehan.rupiahku.data.model.BillEntity
import com.ehan.rupiahku.data.model.CategoryEntity
import com.ehan.rupiahku.data.model.DebtEntity
import com.ehan.rupiahku.data.model.DebtPaymentEntity
import com.ehan.rupiahku.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BillEntity::class,
        BackupHistoryEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun billDao(): BillDao
    abstract fun backupHistoryDao(): BackupHistoryDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rupiahku_finance_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
