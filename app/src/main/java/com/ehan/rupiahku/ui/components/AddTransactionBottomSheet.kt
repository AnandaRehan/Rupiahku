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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.data.model.CategoryEntity
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.ExpenseRed
import com.ehan.rupiahku.ui.theme.IncomeGreen
import com.ehan.rupiahku.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    categories: List<CategoryEntity>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSubmit: (title: String, amount: Double, type: String, categoryName: String, accountName: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var selectedCategory by remember {
        mutableStateOf(categories.firstOrNull { if (type == "INCOME") it.isIncome else !it.isIncome }?.name ?: "Makanan & Minuman")
    }
    var selectedAccount by remember { mutableStateOf("Dompet Utama") }
    var note by remember { mutableStateOf("") }

    val accounts = listOf("Dompet Utama", "BCA", "Mandiri", "GoPay", "OVO", "ShopeePay")

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
                text = "Tambah Catatan Transaksi Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Switcher
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        type = "EXPENSE"
                        selectedCategory = categories.firstOrNull { !it.isIncome }?.name ?: "Makanan & Minuman"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "EXPENSE") ExpenseRed else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == "EXPENSE") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_expense_btn")
                ) {
                    Text("Pengeluaran (-)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        type = "INCOME"
                        selectedCategory = categories.firstOrNull { it.isIncome }?.name ?: "Gaji & Pendapatan"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "INCOME") IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == "INCOME") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_income_btn")
                ) {
                    Text("Pemasukan (+)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Transaksi") },
                placeholder = { Text("Contoh: Makan Siang / Gaji Freelance") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
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
                label = { Text("Nominal (IDR / Rupiah)") },
                placeholder = { Text("0") },
                prefix = { Text("Rp ") },
                supportingText = { if (formattedDisplay.isNotBlank()) Text("Format: $formattedDisplay") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selector Chips
            Text(
                text = "Pilih Kategori",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column {
                    val filteredCat = categories.filter { if (type == "INCOME") it.isIncome else !it.isIncome }
                    filteredCat.chunked(3).forEach { chunk ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            chunk.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.name,
                                    onClick = { selectedCategory = cat.name },
                                    label = { Text(cat.name, fontSize = 12.sp) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account / Wallet Selector
            Text(
                text = "Sumber Dompet / Rekening",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                accounts.chunked(3).forEach { chunk ->
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        chunk.forEach { acc ->
                            FilterChip(
                                selected = selectedAccount == acc,
                                onClick = { selectedAccount = acc },
                                label = { Text(acc, fontSize = 12.sp) },
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan Opsional") },
                placeholder = { Text("Keterangan tambahan...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = CurrencyUtils.parseRupiahInput(amountText)
                    onSubmit(title, amt, type, selectedCategory, selectedAccount, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_tx_btn")
            ) {
                Text("Simpan Transaksi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
