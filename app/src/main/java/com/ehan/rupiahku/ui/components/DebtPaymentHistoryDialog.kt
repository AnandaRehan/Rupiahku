package com.ehan.rupiahku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.data.model.DebtEntity
import com.ehan.rupiahku.data.model.DebtPaymentEntity
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.util.CurrencyUtils
import com.ehan.rupiahku.util.DateUtils

@Composable
fun DebtPaymentHistoryDialog(
    debt: DebtEntity,
    payments: List<DebtPaymentEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Riwayat Cicilan Pembayaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${debt.personName} • ${debt.title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Total Dibayar: ${CurrencyUtils.formatRupiah(debt.paidAmount)} dari ${CurrencyUtils.formatRupiah(debt.totalAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (payments.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat cicilan yang dicatat.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(payments, key = { "payment_${it.id}" }) { payment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = DateUtils.formatDateTime(payment.paymentDateMillis),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (payment.note.isNotBlank()) {
                                            Text(
                                                text = payment.note,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Text(
                                        text = "+ ${CurrencyUtils.formatRupiah(payment.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Tutup")
            }
        }
    )
}
