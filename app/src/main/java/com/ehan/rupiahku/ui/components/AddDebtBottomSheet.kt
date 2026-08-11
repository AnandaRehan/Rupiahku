package com.ehan.rupiahku.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.ExpenseRed
import com.ehan.rupiahku.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSubmit: (
        personName: String,
        type: String,
        title: String,
        totalAmount: Double,
        dueDateMillis: Long?,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current

    var personName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("HUTANG") } // "HUTANG" or "PIUTANG"
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dueDateMillis = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("add_debt_sheet")
        ) {
            Text(
                text = "Tambah Catatan Hutang / Piutang",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Catat hutang Anda ke orang lain atau piutang yang dipinjam orang",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Debt Type Selection (HUTANG vs PIUTANG)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedType == "HUTANG",
                    onClick = { selectedType = "HUTANG" },
                    label = {
                        Text(
                            text = "Saya Berhutang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_chip_hutang")
                )

                FilterChip(
                    selected = selectedType == "PIUTANG",
                    onClick = { selectedType = "PIUTANG" },
                    label = {
                        Text(
                            text = "Piutang (Orang Berhutang)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_chip_piutang")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nama Kontak / Orang
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text("Nama Kontak / Orang") },
                placeholder = { Text("cth. Budi, Bank BCA, Siti") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Person")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_debt_person")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Keterangan / Judul
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Keterangan Singkat") },
                placeholder = { Text("cth. Pinjam Modal Usaha, Bayar Kontrakan") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Title, contentDescription = "Title")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_debt_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Nominal Total
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                label = { Text("Total Nominal (Rp)") },
                placeholder = { Text("0") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.PriceCheck, contentDescription = "Amount")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_debt_amount")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Date Picker for Due Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Due Date",
                    tint = EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Jatuh Tempo (Opsional)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (dueDateMillis != null) DateUtils.formatShortDate(dueDateMillis!!) else "Belum ditentukan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Catatan Tambahan
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                placeholder = { Text("Catatan khusus...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Note")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_debt_note")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(personName, selectedType, title, parsedAmount, dueDateMillis, note)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "HUTANG") ExpenseRed else EmeraldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_debt")
            ) {
                Text(
                    text = "Simpan Catatan ${if (selectedType == "HUTANG") "Hutang" else "Piutang"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
