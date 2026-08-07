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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.ehan.rupiahku.data.model.BillEntity
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.ExpenseRed
import com.ehan.rupiahku.ui.theme.GoldAccent
import com.ehan.rupiahku.ui.theme.IncomeGreen
import com.ehan.rupiahku.util.CurrencyUtils
import com.ehan.rupiahku.util.DateUtils

@Composable
fun BillReminderCard(
    bill: BillEntity,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = DateUtils.getBillStatusLabel(bill.dueDateMillis, bill.isPaid)

    val (statusBg, statusFg) = when (status) {
        DateUtils.BillStatus.PAID -> Pair(IncomeGreen.copy(alpha = 0.15f), IncomeGreen)
        DateUtils.BillStatus.OVERDUE -> Pair(ExpenseRed.copy(alpha = 0.15f), ExpenseRed)
        DateUtils.BillStatus.DUE_TODAY -> Pair(GoldAccent.copy(alpha = 0.2f), Color(0xFFD87D00))
        DateUtils.BillStatus.DUE_SOON -> Pair(Color(0xFFE3F2FD), Color(0xFF1976D2))
        DateUtils.BillStatus.UPCOMING -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bill_item_${bill.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isPaid) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bill.isPaid) 1.dp else 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusBg,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = "Bill Icon",
                                tint = statusFg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = bill.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bill.categoryName + " • " + bill.repeatInterval,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_bill_${bill.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Tagihan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Jatuh Tempo: " + DateUtils.formatDate(bill.dueDateMillis),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = CurrencyUtils.formatRupiah(bill.amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                    )
                }

                // Status Pill & Action Button
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = status.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (!bill.isPaid) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onPayClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("pay_bill_btn_${bill.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = "Bayar",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Bayar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
