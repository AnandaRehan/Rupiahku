package com.ehan.rupiahku.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ehan.rupiahku.data.model.AppSettings
import com.ehan.rupiahku.data.model.BackupHistoryEntity
import com.ehan.rupiahku.data.model.BillEntity
import com.ehan.rupiahku.data.model.CategoryEntity
import com.ehan.rupiahku.data.model.DashboardSummary
import com.ehan.rupiahku.data.model.TransactionEntity
import com.ehan.rupiahku.data.repository.FinanceRepository
import com.ehan.rupiahku.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository(application)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bills: StateFlow<List<BillEntity>> = repository.allBills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val backupLogs: StateFlow<List<BackupHistoryEntity>> = repository.allBackupLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning: StateFlow<Boolean> = _isBackupRunning.asStateFlow()

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        transactions,
        bills
    ) { txList, billList ->
        var totalIncome = 0.0
        var totalExpense = 0.0
        txList.forEach { tx ->
            if (tx.type == "INCOME") totalIncome += tx.amount
            else if (tx.type == "EXPENSE") totalExpense += tx.amount
        }
        val balance = totalIncome - totalExpense

        val activeBills = billList.filter { !it.isPaid }
        val overdueBills = activeBills.count {
            DateUtils.getBillStatusLabel(it.dueDateMillis, it.isPaid) == DateUtils.BillStatus.OVERDUE
        }

        DashboardSummary(
            totalBalance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            activeBillsCount = activeBills.size,
            overdueBillsCount = overdueBills
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardSummary(0.0, 0.0, 0.0, 0, 0)
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        categoryName: String,
        accountName: String,
        note: String
    ) {
        viewModelScope.launch {
            if (title.isBlank() || amount <= 0) {
                _toastMessage.emit("Harap isi judul dan nominal yang valid")
                return@launch
            }
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    categoryName = categoryName,
                    accountName = accountName,
                    dateMillis = System.currentTimeMillis(),
                    note = note
                )
            )
            _toastMessage.emit("Transaksi berhasil disimpan")

            // Auto cloud backup if setting is real-time
            if (_appSettings.value.autoBackupEnabled && _appSettings.value.backupFrequency == "Real-time") {
                repository.performCloudBackup(isAuto = true)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _toastMessage.emit("Transaksi dihapus")
        }
    }

    fun addBill(
        title: String,
        amount: Double,
        categoryName: String,
        dueDateMillis: Long,
        repeatInterval: String
    ) {
        viewModelScope.launch {
            if (title.isBlank() || amount <= 0) {
                _toastMessage.emit("Harap isi nama tagihan dan nominal")
                return@launch
            }
            repository.insertBill(
                BillEntity(
                    title = title,
                    amount = amount,
                    categoryName = categoryName,
                    dueDateMillis = dueDateMillis,
                    repeatInterval = repeatInterval,
                    isPaid = false
                )
            )
            _toastMessage.emit("Tagihan berhasil didaftarkan")
        }
    }

    fun payBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.payBill(bill)
            _toastMessage.emit("Tagihan '${bill.title}' telah dilunasi!")
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            _toastMessage.emit("Tagihan dihapus")
        }
    }

    fun updateBudgetLimit(categoryName: String, newLimit: Double) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == categoryName }
            if (cat != null) {
                repository.updateCategory(cat.copy(budgetLimit = newLimit))
                _toastMessage.emit("Target anggaran $categoryName diperbarui")
            }
        }
    }

    fun triggerCloudBackup() {
        viewModelScope.launch {
            _isBackupRunning.value = true
            try {
                val log = repository.performCloudBackup(isAuto = false)
                _toastMessage.emit("Backup Cloud Sukses! (${log.recordCount} catatan tersimpan)")
            } catch (e: Exception) {
                _toastMessage.emit("Gagal melakukan backup cloud: ${e.localizedMessage}")
            } finally {
                _isBackupRunning.value = false
            }
        }
    }

    fun restoreCloudData() {
        viewModelScope.launch {
            _isBackupRunning.value = true
            try {
                val success = repository.restoreDataFromCloud()
                if (success) {
                    _toastMessage.emit("Data berhasil dipulihkan dari Cloud!")
                } else {
                    _toastMessage.emit("Tidak ada file backup cloud yang ditemukan")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Gagal memulihkan data: ${e.localizedMessage}")
            } finally {
                _isBackupRunning.value = false
            }
        }
    }

    fun updateAutoBackupSetting(enabled: Boolean, frequency: String) {
        _appSettings.value = _appSettings.value.copy(
            autoBackupEnabled = enabled,
            backupFrequency = frequency
        )
    }

    suspend fun getJsonBackupPayload(): String {
        return repository.getLatestBackupJsonString()
    }
}
