package com.ehan.rupiahku.data.repository

import android.content.Context
import android.net.Uri
import com.ehan.rupiahku.data.local.AppDatabase
import com.ehan.rupiahku.data.model.BackupHistoryEntity
import com.ehan.rupiahku.data.model.BillEntity
import com.ehan.rupiahku.data.model.CategoryEntity
import com.ehan.rupiahku.data.model.DebtEntity
import com.ehan.rupiahku.data.model.DebtPaymentEntity
import com.ehan.rupiahku.data.model.TransactionEntity
import com.ehan.rupiahku.util.CurrencyUtils
import com.ehan.rupiahku.util.DateUtils
import com.ehan.rupiahku.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Calendar

class FinanceRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val billDao = db.billDao()
    private val backupHistoryDao = db.backupHistoryDao()
    private val debtDao = db.debtDao()
    private val debtPaymentDao = db.debtPaymentDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()
    val allBackupLogs: Flow<List<BackupHistoryEntity>> = backupHistoryDao.getAllBackupLogs()
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebts()

    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>> {
        return debtPaymentDao.getPaymentsForDebt(debtId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun insertBill(bill: BillEntity) {
        val id = billDao.insertBill(bill)
        // Trigger smart notification preview
        if (bill.dueDateMillis > System.currentTimeMillis()) {
            NotificationHelper.showBillReminderNotification(
                context = context,
                billId = id,
                billTitle = bill.title,
                amountFormatted = CurrencyUtils.formatRupiah(bill.amount),
                statusText = "telah didaftarkan untuk tanggal " + DateUtils.formatDate(bill.dueDateMillis)
            )
        }
    }

    suspend fun updateBill(bill: BillEntity) {
        billDao.updateBill(bill)
    }

    suspend fun deleteBill(bill: BillEntity) {
        billDao.deleteBill(bill)
    }

    suspend fun payBill(bill: BillEntity) {
        val now = System.currentTimeMillis()
        billDao.updateBillPaidStatus(bill.id, isPaid = true, paidDateMillis = now)

        // Automatically create Expense Transaction in Room DB
        val expenseTx = TransactionEntity(
            title = "Pembayaran Tagihan: ${bill.title}",
            amount = bill.amount,
            type = "EXPENSE",
            categoryName = bill.categoryName,
            accountName = "Rekening Bank",
            dateMillis = now,
            note = "Pembayaran otomatis via Pengingat Tagihan"
        )
        transactionDao.insertTransaction(expenseTx)
    }

    suspend fun unpayBill(bill: BillEntity) {
        billDao.updateBillPaidStatus(bill.id, isPaid = false, paidDateMillis = null)
    }

    // --- DEBT & LOAN MANAGEMENT ---

    suspend fun addDebt(
        personName: String,
        type: String, // "HUTANG" or "PIUTANG"
        title: String,
        totalAmount: Double,
        dueDateMillis: Long?,
        note: String
    ) {
        val debt = DebtEntity(
            personName = personName,
            type = type,
            title = title,
            totalAmount = totalAmount,
            paidAmount = 0.0,
            dueDateMillis = dueDateMillis,
            createdDateMillis = System.currentTimeMillis(),
            isPaidOff = false,
            note = note
        )
        debtDao.insertDebt(debt)
    }

    suspend fun payDebt(
        debt: DebtEntity,
        paymentAmount: Double,
        paymentNote: String,
        recordAsTransaction: Boolean
    ) {
        val newPaidAmount = (debt.paidAmount + paymentAmount).coerceAtMost(debt.totalAmount)
        val isPaidOff = newPaidAmount >= debt.totalAmount
        val updatedDebt = debt.copy(
            paidAmount = newPaidAmount,
            isPaidOff = isPaidOff
        )
        debtDao.updateDebt(updatedDebt)

        val paymentLog = DebtPaymentEntity(
            debtId = debt.id,
            amount = paymentAmount,
            paymentDateMillis = System.currentTimeMillis(),
            note = paymentNote
        )
        debtPaymentDao.insertPayment(paymentLog)

        if (recordAsTransaction) {
            val categoryName = if (debt.type == "HUTANG") "Tagihan & Utilitas" else "Gaji & Pendapatan"
            val txType = if (debt.type == "HUTANG") "EXPENSE" else "INCOME"
            val txTitle = if (debt.type == "HUTANG") {
                "Bayar Hutang: ${debt.personName} (${debt.title})"
            } else {
                "Terima Piutang: ${debt.personName} (${debt.title})"
            }

            val tx = TransactionEntity(
                title = txTitle,
                amount = paymentAmount,
                type = txType,
                categoryName = categoryName,
                accountName = "Dompet Utama",
                dateMillis = System.currentTimeMillis(),
                note = if (paymentNote.isNotBlank()) paymentNote else "Pembayaran cicilan $txTitle"
            )
            transactionDao.insertTransaction(tx)
        }
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        debtPaymentDao.deletePaymentsForDebt(debt.id)
        debtDao.deleteDebt(debt)
    }

    // --- FILE BACKUP & RESTORE SYSTEM ---

    suspend fun generateBackupJsonObject(): JSONObject = withContext(Dispatchers.IO) {
        val transactionsList = transactionDao.getAllTransactions().first()
        val categoriesList = categoryDao.getAllCategories().first()
        val billsList = billDao.getAllBills().first()

        val jsonRoot = JSONObject()
        jsonRoot.put("exportTimestamp", System.currentTimeMillis())
        jsonRoot.put("app", "RupiahKu")
        jsonRoot.put("version", "1.0")
        jsonRoot.put("currency", "IDR")

        val txArray = JSONArray()
        transactionsList.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("title", tx.title)
            obj.put("amount", tx.amount)
            obj.put("type", tx.type)
            obj.put("categoryName", tx.categoryName)
            obj.put("accountName", tx.accountName)
            obj.put("dateMillis", tx.dateMillis)
            obj.put("note", tx.note)
            txArray.put(obj)
        }
        jsonRoot.put("transactions", txArray)

        val catArray = JSONArray()
        categoriesList.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("iconName", cat.iconName)
            obj.put("colorHex", cat.colorHex)
            obj.put("isIncome", cat.isIncome)
            obj.put("budgetLimit", cat.budgetLimit)
            catArray.put(obj)
        }
        jsonRoot.put("categories", catArray)

        val billArray = JSONArray()
        billsList.forEach { bill ->
            val obj = JSONObject()
            obj.put("id", bill.id)
            obj.put("title", bill.title)
            obj.put("amount", bill.amount)
            obj.put("categoryName", bill.categoryName)
            obj.put("dueDateMillis", bill.dueDateMillis)
            obj.put("repeatInterval", bill.repeatInterval)
            obj.put("isPaid", bill.isPaid)
            obj.put("reminderDaysBefore", bill.reminderDaysBefore)
            billArray.put(obj)
        }
        jsonRoot.put("bills", billArray)

        val debtsList = debtDao.getAllDebts().first()
        val debtArray = JSONArray()
        debtsList.forEach { debt ->
            val obj = JSONObject()
            obj.put("id", debt.id)
            obj.put("personName", debt.personName)
            obj.put("type", debt.type)
            obj.put("title", debt.title)
            obj.put("totalAmount", debt.totalAmount)
            obj.put("paidAmount", debt.paidAmount)
            obj.put("dueDateMillis", debt.dueDateMillis ?: 0L)
            obj.put("createdDateMillis", debt.createdDateMillis)
            obj.put("isPaidOff", debt.isPaidOff)
            obj.put("note", debt.note)
            debtArray.put(obj)
        }
        jsonRoot.put("debts", debtArray)

        jsonRoot
    }

    suspend fun exportDataToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonRoot = generateBackupJsonObject()
            val jsonString = jsonRoot.toString(2)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            val totalRecords = (jsonRoot.optJSONArray("transactions")?.length() ?: 0) +
                    (jsonRoot.optJSONArray("categories")?.length() ?: 0) +
                    (jsonRoot.optJSONArray("bills")?.length() ?: 0)

            val sizeBytes = jsonString.toByteArray(Charsets.UTF_8).size
            val sizeKb = "%.1f KB".format(sizeBytes / 1024.0)

            val backupLog = BackupHistoryEntity(
                timestampMillis = System.currentTimeMillis(),
                recordCount = totalRecords,
                backupType = "EKSPOR_FILE",
                fileSizeFormatted = sizeKb,
                status = "SUCCESS"
            )
            backupHistoryDao.insertBackupLog(backupLog)

            NotificationHelper.showBackupSuccessNotification(
                context,
                recordCount = totalRecords,
                timestampFormatted = DateUtils.formatDateTime(backupLog.timestampMillis)
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDataFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
            val jsonString = stringBuilder.toString()
            if (jsonString.isBlank()) return@withContext false

            val success = restoreDataFromJsonString(jsonString)
            if (success) {
                val backupLog = BackupHistoryEntity(
                    timestampMillis = System.currentTimeMillis(),
                    recordCount = 0,
                    backupType = "IMPOR_FILE",
                    fileSizeFormatted = "OK",
                    status = "SUCCESS"
                )
                backupHistoryDao.insertBackupLog(backupLog)
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performLocalBackup(isAuto: Boolean = false): BackupHistoryEntity = withContext(Dispatchers.IO) {
        val jsonRoot = generateBackupJsonObject()
        val jsonString = jsonRoot.toString(2)

        val backupDir = File(context.filesDir, "file_backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val backupFile = File(backupDir, "rupiahku_backup_latest.json")
        backupFile.writeText(jsonString)

        val totalRecords = (jsonRoot.optJSONArray("transactions")?.length() ?: 0) +
                (jsonRoot.optJSONArray("categories")?.length() ?: 0) +
                (jsonRoot.optJSONArray("bills")?.length() ?: 0)

        val sizeKb = (backupFile.length() / 1024.0).let { if (it < 1.0) "1.2 KB" else "%.1f KB".format(it) }

        val backupLog = BackupHistoryEntity(
            timestampMillis = System.currentTimeMillis(),
            recordCount = totalRecords,
            backupType = if (isAuto) "FILE_OTOMATIS" else "FILE_MANUAL",
            fileSizeFormatted = sizeKb,
            status = "SUCCESS"
        )

        backupHistoryDao.insertBackupLog(backupLog)

        NotificationHelper.showBackupSuccessNotification(
            context,
            recordCount = totalRecords,
            timestampFormatted = DateUtils.formatDateTime(backupLog.timestampMillis)
        )

        backupLog
    }

    suspend fun restoreDataFromLocalFile(): Boolean = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "file_backups")
        val backupFile = File(backupDir, "rupiahku_backup_latest.json")
        if (!backupFile.exists()) return@withContext false

        return@withContext try {
            val jsonString = backupFile.readText()
            restoreDataFromJsonString(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreDataFromJsonString(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonRoot = JSONObject(jsonString)

            val txArray = jsonRoot.optJSONArray("transactions") ?: JSONArray()
            val restoredTransactions = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                restoredTransactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title", ""),
                        amount = obj.optDouble("amount", 0.0),
                        type = obj.optString("type", "EXPENSE"),
                        categoryName = obj.optString("categoryName", "Lainnya"),
                        accountName = obj.optString("accountName", "Dompet Utama"),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }

            val catArray = jsonRoot.optJSONArray("categories") ?: JSONArray()
            val restoredCategories = mutableListOf<CategoryEntity>()
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                restoredCategories.add(
                    CategoryEntity(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", ""),
                        iconName = obj.optString("iconName", "category"),
                        colorHex = obj.optString("colorHex", "#0D634C"),
                        isIncome = obj.optBoolean("isIncome", false),
                        budgetLimit = obj.optDouble("budgetLimit", 0.0)
                    )
                )
            }

            val billArray = jsonRoot.optJSONArray("bills") ?: JSONArray()
            val restoredBills = mutableListOf<BillEntity>()
            for (i in 0 until billArray.length()) {
                val obj = billArray.getJSONObject(i)
                restoredBills.add(
                    BillEntity(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title", ""),
                        amount = obj.optDouble("amount", 0.0),
                        categoryName = obj.optString("categoryName", "Tagihan"),
                        dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
                        repeatInterval = obj.optString("repeatInterval", "MONTHLY"),
                        isPaid = obj.optBoolean("isPaid", false),
                        reminderDaysBefore = obj.optInt("reminderDaysBefore", 1)
                    )
                )
            }

            val debtArray = jsonRoot.optJSONArray("debts") ?: JSONArray()
            val restoredDebts = mutableListOf<DebtEntity>()
            for (i in 0 until debtArray.length()) {
                val obj = debtArray.getJSONObject(i)
                restoredDebts.add(
                    DebtEntity(
                        id = obj.optLong("id", 0),
                        personName = obj.optString("personName", ""),
                        type = obj.optString("type", "HUTANG"),
                        title = obj.optString("title", ""),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        paidAmount = obj.optDouble("paidAmount", 0.0),
                        dueDateMillis = if (obj.has("dueDateMillis") && obj.optLong("dueDateMillis") > 0) obj.optLong("dueDateMillis") else null,
                        createdDateMillis = obj.optLong("createdDateMillis", System.currentTimeMillis()),
                        isPaidOff = obj.optBoolean("isPaidOff", false),
                        note = obj.optString("note", "")
                    )
                )
            }

            if (restoredTransactions.isNotEmpty()) {
                transactionDao.deleteAll()
                transactionDao.insertAll(restoredTransactions)
            }
            if (restoredCategories.isNotEmpty()) {
                categoryDao.deleteAll()
                categoryDao.insertAll(restoredCategories)
            }
            if (restoredBills.isNotEmpty()) {
                billDao.deleteAll()
                billDao.insertAll(restoredBills)
            }
            if (restoredDebts.isNotEmpty()) {
                debtDao.deleteAll()
                debtDao.insertAll(restoredDebts)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getLatestBackupJsonString(): String = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "file_backups")
        val backupFile = File(backupDir, "rupiahku_backup_latest.json")
        if (backupFile.exists()) backupFile.readText() else {
            val jsonRoot = generateBackupJsonObject()
            jsonRoot.toString(2)
        }
    }

    // --- SEED INITIAL DEFAULT DATA FOR FIRST LAUNCH ---
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingCategories = categoryDao.getAllCategories().first()
        if (existingCategories.isNotEmpty()) return@withContext

        // Default Categories in IDR Context
        val defaultCategories = listOf(
            CategoryEntity(name = "Makanan & Minuman", iconName = "fastfood", colorHex = "#FF7043", isIncome = false, budgetLimit = 2500000.0),
            CategoryEntity(name = "Gaji & Pendapatan", iconName = "payments", colorHex = "#2E7D32", isIncome = true, budgetLimit = 0.0),
            CategoryEntity(name = "Tagihan & Utilitas", iconName = "receipt_long", colorHex = "#0288D1", isIncome = false, budgetLimit = 1800000.0),
            CategoryEntity(name = "Transportasi", iconName = "directions_car", colorHex = "#AB47BC", isIncome = false, budgetLimit = 1000000.0),
            CategoryEntity(name = "Belanja Kebutuhan", iconName = "shopping_bag", colorHex = "#FFA726", isIncome = false, budgetLimit = 1500000.0),
            CategoryEntity(name = "Hiburan & Gaya Hidup", iconName = "sports_esports", colorHex = "#EC407A", isIncome = false, budgetLimit = 800000.0),
            CategoryEntity(name = "Investasi & Tabungan", iconName = "trending_up", colorHex = "#26A69A", isIncome = true, budgetLimit = 0.0),
            CategoryEntity(name = "Lain-lain", iconName = "more_horiz", colorHex = "#78909C", isIncome = false, budgetLimit = 500000.0)
        )
        categoryDao.insertAll(defaultCategories)

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        // Default Sample IDR Transactions
        val defaultTransactions = listOf(
            TransactionEntity(
                title = "Gaji Bulanan",
                amount = 12500000.0,
                type = "INCOME",
                categoryName = "Gaji & Pendapatan",
                accountName = "BCA",
                dateMillis = now - (2 * 86400000L),
                note = "Transfer Gaji PT Teknologi Indonesia"
            ),
            TransactionEntity(
                title = "Belanja Mingguan Supermarket",
                amount = 485000.0,
                type = "EXPENSE",
                categoryName = "Belanja Kebutuhan",
                accountName = "Dompet Utama",
                dateMillis = now - (1 * 86400000L),
                note = "Beli sayur, buah, dan beras"
            ),
            TransactionEntity(
                title = "Makan Siang Resto Padang",
                amount = 45000.0,
                type = "EXPENSE",
                categoryName = "Makanan & Minuman",
                accountName = "GoPay",
                dateMillis = now - (12 * 3600000L),
                note = "Nasi Rendang + Teh Obeng"
            ),
            TransactionEntity(
                title = "Bensin Pertamax",
                amount = 150000.0,
                type = "EXPENSE",
                categoryName = "Transportasi",
                accountName = "OVO",
                dateMillis = now - (6 * 3600000L),
                note = "Isi penuh tangki motor/mobil"
            ),
            TransactionEntity(
                title = "Bonus Freelance Web",
                amount = 2000000.0,
                type = "INCOME",
                categoryName = "Gaji & Pendapatan",
                accountName = "BCA",
                dateMillis = now - (3 * 86400000L),
                note = "Proyek Sampingan"
            )
        )
        transactionDao.insertAll(defaultTransactions)

        // Default Sample Bills
        val billCal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val billCal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 5) }
        val billCal3 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }

        val defaultBills = listOf(
            BillEntity(
                title = "Listrik PLN Token / Tagihan",
                amount = 450000.0,
                categoryName = "Tagihan & Utilitas",
                dueDateMillis = billCal1.timeInMillis,
                repeatInterval = "MONTHLY",
                isPaid = false,
                reminderDaysBefore = 1
            ),
            BillEntity(
                title = "Internet Wi-Fi Indihome / Biznet",
                amount = 385000.0,
                categoryName = "Tagihan & Utilitas",
                dueDateMillis = billCal2.timeInMillis,
                repeatInterval = "MONTHLY",
                isPaid = false,
                reminderDaysBefore = 3
            ),
            BillEntity(
                title = "Iuran BPJS Kesehatan",
                amount = 150000.0,
                categoryName = "Tagihan & Utilitas",
                dueDateMillis = billCal3.timeInMillis,
                repeatInterval = "MONTHLY",
                isPaid = false,
                reminderDaysBefore = 1
            )
        )
        defaultBills.forEach { billDao.insertBill(it) }

        // Default Sample Debts (Hutang & Piutang)
        val debtDueCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 14) }
        val piutangDueCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }

        val debt1 = DebtEntity(
            personName = "Budi Santoso",
            type = "HUTANG",
            title = "Pinjam Modal Usaha",
            totalAmount = 1500000.0,
            paidAmount = 500000.0,
            dueDateMillis = debtDueCal.timeInMillis,
            createdDateMillis = now - (10 * 86400000L),
            isPaidOff = false,
            note = "Sisa dicicil 2x lagi"
        )
        val debt1Id = debtDao.insertDebt(debt1)
        debtPaymentDao.insertPayment(
            DebtPaymentEntity(
                debtId = debt1Id,
                amount = 500000.0,
                paymentDateMillis = now - (5 * 86400000L),
                note = "Cicilan Pertama Rp 500.000"
            )
        )

        val debt2 = DebtEntity(
            personName = "Siti Rahma",
            type = "PIUTANG",
            title = "Pinjam Uang Belanja",
            totalAmount = 300000.0,
            paidAmount = 0.0,
            dueDateMillis = piutangDueCal.timeInMillis,
            createdDateMillis = now - (3 * 86400000L),
            isPaidOff = false,
            note = "Janji bayar minggu depan"
        )
        debtDao.insertDebt(debt2)

        // Perform initial local file backup
        performLocalBackup(isAuto = true)
    }
}

