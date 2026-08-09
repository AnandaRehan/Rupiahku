package com.ehan.rupiahku.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.data.model.BackupHistoryEntity
import com.ehan.rupiahku.data.model.BillEntity
import com.ehan.rupiahku.data.model.DashboardSummary
import com.ehan.rupiahku.data.model.TransactionEntity
import com.ehan.rupiahku.ui.components.BillReminderCard
import com.ehan.rupiahku.ui.components.DashboardHeaderCard
import com.ehan.rupiahku.ui.components.FinancialChartComposable
import com.ehan.rupiahku.ui.components.TransactionItemCard
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.util.DateUtils

@Composable
fun HomeScreen(
    summary: DashboardSummary,
    recentTransactions: List<TransactionEntity>,
    upcomingBills: List<BillEntity>,
    latestBackupLog: BackupHistoryEntity?,
    onAddTransactionClick: () -> Unit,
    onAddBillClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onViewAllBillsClick: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onPayBill: (BillEntity) -> Unit,
    onDeleteBill: (BillEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Balance Header Card
        item {
            val backupTime = if (latestBackupLog != null) DateUtils.formatDateTime(latestBackupLog.timestampMillis) else "Baru saja"
            DashboardHeaderCard(
                summary = summary,
                lastBackupTime = backupTime
            )
        }

        // 2. Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddTransactionClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_quick_add_tx")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Catat",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Catat Transaksi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddBillClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_quick_add_bill")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Tagihan",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Tambah Tagihan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Financial Expense Donut Chart
        item {
            FinancialChartComposable(transactions = recentTransactions)
        }

        // 4. Upcoming Bills Alert Banner / Cards
        if (upcomingBills.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengingat Tagihan Mendatang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = onViewAllBillsClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lihat Semua", fontSize = 11.sp)
                    }
                }
            }

            items(upcomingBills.take(2), key = { "bill_${it.id}" }) { bill ->
                BillReminderCard(
                    bill = bill,
                    onPayClick = { onPayBill(bill) },
                    onDeleteClick = { onDeleteBill(bill) }
                )
            }
        }

        // 5. Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = onViewAllTransactionsClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Semua Transaksi", fontSize = 11.sp)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada transaksi. Ketuk tombol 'Catat Transaksi' di atas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(5), key = { "tx_${it.id}" }) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    onDeleteClick = { onDeleteTransaction(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
