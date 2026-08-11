package com.ehan.rupiahku.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.data.model.DebtEntity
import com.ehan.rupiahku.data.model.DebtPaymentEntity
import com.ehan.rupiahku.ui.components.AddDebtBottomSheet
import com.ehan.rupiahku.ui.components.DebtItemCard
import com.ehan.rupiahku.ui.components.DebtPaymentHistoryDialog
import com.ehan.rupiahku.ui.components.PayDebtDialog
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.ExpenseRed
import com.ehan.rupiahku.util.CurrencyUtils
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    debts: List<DebtEntity>,
    sheetState: SheetState,
    onAddDebtClick: () -> Unit,
    onDismissAddSheet: () -> Unit,
    onAddDebtSubmit: (personName: String, type: String, title: String, totalAmount: Double, dueDateMillis: Long?, note: String) -> Unit,
    onPayDebt: (debt: DebtEntity, amount: Double, note: String, recordAsTx: Boolean) -> Unit,
    onDeleteDebt: (debt: DebtEntity) -> Unit,
    onFetchDebtPayments: (debtId: Long) -> Flow<List<DebtPaymentEntity>>,
    showAddDebtSheet: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Semua") } // "Semua", "Hutang", "Piutang", "Lunas"

    var targetPayDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var targetHistoryDebt by remember { mutableStateOf<DebtEntity?>(null) }

    val historyPaymentsFlow = targetHistoryDebt?.let { onFetchDebtPayments(it.id) }
    val historyPayments by historyPaymentsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    // Summary calculations
    val activeHutangList = debts.filter { it.type == "HUTANG" && !it.isPaidOff }
    val activePiutangList = debts.filter { it.type == "PIUTANG" && !it.isPaidOff }

    val totalRemainingHutang = activeHutangList.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0.0) }
    val totalRemainingPiutang = activePiutangList.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0.0) }

    // Filter items
    val filteredDebts = debts.filter { debt ->
        when (selectedFilter) {
            "Hutang" -> debt.type == "HUTANG" && !debt.isPaidOff
            "Piutang" -> debt.type == "PIUTANG" && !debt.isPaidOff
            "Lunas" -> debt.isPaidOff
            else -> true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDebtClick,
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_debt")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Catatan")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Catat Hutang/Piutang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("debts_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Title
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = "Hutang Piutang",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kelola Hutang & Piutang",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pantau pinjaman, catat cicilan bertahap, dan pelunasan transaksi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary Header Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sisa Hutang Saya
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_hutang"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ExpenseRed.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Hutang",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sisa Hutang Saya",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ExpenseRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyUtils.formatRupiah(totalRemainingHutang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Text(
                                text = "${activeHutangList.size} catatan aktif",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sisa Piutang Saya
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_piutang"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Piutang",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sisa Piutang Saya",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyUtils.formatRupiah(totalRemainingPiutang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${activePiutangList.size} catatan aktif",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Semua", "Hutang", "Piutang", "Lunas")
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(text = filter, fontSize = 12.sp) },
                            modifier = Modifier.testTag("filter_chip_$filter")
                        )
                    }
                }
            }

            // Debt List Items
            if (filteredDebts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum Ada Catatan ${if (selectedFilter != "Semua") selectedFilter else "Hutang/Piutang"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tekan tombol + di bawah untuk menambahkan catatan baru",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredDebts, key = { "debt_${it.id}" }) { debt ->
                    DebtItemCard(
                        debt = debt,
                        onPayClick = { targetPayDebt = debt },
                        onHistoryClick = { targetHistoryDebt = debt },
                        onDeleteClick = { onDeleteDebt(debt) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Debt Bottom Sheet Modal
    if (showAddDebtSheet) {
        AddDebtBottomSheet(
            sheetState = sheetState,
            onDismiss = onDismissAddSheet,
            onSubmit = onAddDebtSubmit
        )
    }

    // Pay Debt Dialog
    if (targetPayDebt != null) {
        PayDebtDialog(
            debt = targetPayDebt!!,
            onDismiss = { targetPayDebt = null },
            onConfirmPayment = { amount, note, recordAsTx ->
                val debt = targetPayDebt!!
                targetPayDebt = null
                onPayDebt(debt, amount, note, recordAsTx)
            }
        )
    }

    // Debt Payment History Dialog
    if (targetHistoryDebt != null) {
        DebtPaymentHistoryDialog(
            debt = targetHistoryDebt!!,
            payments = historyPayments,
            onDismiss = { targetHistoryDebt = null }
        )
    }
}
