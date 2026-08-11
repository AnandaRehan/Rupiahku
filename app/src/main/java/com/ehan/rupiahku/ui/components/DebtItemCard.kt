package com.ehan.rupiahku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.data.model.DebtEntity
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.ExpenseRed
import com.ehan.rupiahku.ui.theme.GoldAccent
import com.ehan.rupiahku.util.CurrencyUtils
import com.ehan.rupiahku.util.DateUtils

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    onPayClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHutang = debt.type == "HUTANG"
    val remainingAmount = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0.0)
    val progress = (debt.paidAmount / debt.totalAmount).coerceIn(0.0, 1.0).toFloat()
    val percentText = (progress * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Type Badge, Name, Title, and Delete Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (debt.isPaidOff) EmeraldPrimary.copy(alpha = 0.15f)
                                else if (isHutang) ExpenseRed.copy(alpha = 0.15f)
                                else EmeraldPrimary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Person",
                            tint = if (debt.isPaidOff) EmeraldPrimary
                            else if (isHutang) ExpenseRed
                            else EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (debt.isPaidOff) EmeraldPrimary
                                else if (isHutang) ExpenseRed
                                else EmeraldPrimary
                            ) {
                                Text(
                                    text = if (debt.isPaidOff) "LUNAS" else if (isHutang) "HUTANG SAYA" else "PIUTANG",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (debt.dueDateMillis != null && !debt.isPaidOff) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Due Date",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = DateUtils.formatShortDate(debt.dueDateMillis),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = debt.personName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = debt.title,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("btn_delete_debt_${debt.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar & Payment Status
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progres Pelunasan ($percentText%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sisa: ${CurrencyUtils.formatRupiah(remainingAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (debt.isPaidOff) EmeraldPrimary else if (isHutang) ExpenseRed else EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (debt.isPaidOff) EmeraldPrimary else if (isHutang) ExpenseRed else EmeraldPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {}
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dibayar: ${CurrencyUtils.formatRupiah(debt.paidAmount)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total: ${CurrencyUtils.formatRupiah(debt.totalAmount)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan: ${debt.note}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Bayar Cicilan & Riwayat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!debt.isPaidOff) {
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHutang) ExpenseRed else EmeraldPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_pay_debt_${debt.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Pay",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isHutang) "Bayar Cicil" else "Terima Cicil", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_history_debt_${debt.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Riwayat Cicil", fontSize = 12.sp)
                }
            }
        }
    }
}
