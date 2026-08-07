package com.ehan.rupiahku.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.util.CurrencyUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSubmit: (title: String, amount: Double, categoryName: String, dueDateMillis: Long, repeatInterval: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("Tagihan & Utilitas") }
    var repeatInterval by remember { mutableStateOf("Bulanan") }
    var daysInFutureText by remember { mutableStateOf("3") } // days from today

    val intervals = listOf("Sekali", "Mingguan", "Bulanan", "Tahunan")
    val categories = listOf("Tagihan & Utilitas", "Sewa & Properti", "Asuransi & Kesehatan", "Pendidikan", "Cicilan & Kredit", "Lainnya")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Daftarkan Pengingat Tagihan Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Tagihan / Rutinitas") },
                placeholder = { Text("Contoh: Listrik PLN / Indihome / Cicilan Motor") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input
            val formattedDisplay = if (amountText.isNotBlank()) {
                val num = CurrencyUtils.parseRupiahInput(amountText)
                CurrencyUtils.formatRupiah(num)
            } else ""

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                label = { Text("Nominal Tagihan (IDR / Rupiah)") },
                placeholder = { Text("0") },
                prefix = { Text("Rp ") },
                supportingText = { if (formattedDisplay.isNotBlank()) Text("Format: $formattedDisplay") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Due Date Offset Input
            OutlinedTextField(
                value = daysInFutureText,
                onValueChange = { daysInFutureText = it.filter { char -> char.isDigit() } },
                label = { Text("Jatuh Tempo (Berapa hari dari sekarang?)") },
                placeholder = { Text("1, 3, 7, 30") },
                supportingText = { Text("Misal: Isi 1 untuk besok, 0 untuk hari ini") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_days_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Text(
                text = "Kategori Tagihan",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                Column {
                    categories.chunked(2).forEach { chunk ->
                        Row {
                            chunk.forEach { cat ->
                                FilterChip(
                                    selected = categoryName == cat,
                                    onClick = { categoryName = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Repeat Interval Chips
            Text(
                text = "Siklus Pengulangan",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                intervals.forEach { interval ->
                    FilterChip(
                        selected = repeatInterval == interval,
                        onClick = { repeatInterval = interval },
                        label = { Text(interval, fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = CurrencyUtils.parseRupiahInput(amountText)
                    val days = daysInFutureText.toIntOrNull() ?: 1
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_MONTH, days)
                    }
                    onSubmit(title, amt, categoryName, cal.timeInMillis, repeatInterval)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_bill_btn")
            ) {
                Text("Daftarkan Pengingat Tagihan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
