package com.ehan.rupiahku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ehan.rupiahku.data.model.CategoryEntity
import com.ehan.rupiahku.data.model.TransactionEntity
import com.ehan.rupiahku.ui.components.BudgetProgressBar
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.util.CurrencyUtils

@Composable
fun BudgetsScreen(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    onUpdateBudgetLimit: (categoryName: String, newLimit: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryForEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var editAmountInput by remember { mutableStateOf("") }

    val expenseCategories = categories.filter { !it.isIncome }
    val expenseTransactions = transactions.filter { it.type == "EXPENSE" }

    val categorySpentMap = expenseTransactions
        .groupBy { it.categoryName }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val totalBudget = expenseCategories.sumOf { it.budgetLimit }
    val totalSpent = expenseTransactions.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Target Anggaran Bulanan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Kelola batas pengeluaran per kategori untuk menjaga keuangan tetap hemat",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Total Budget Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ringkasan Total Anggaran",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyUtils.formatRupiah(totalSpent) + " / " + CurrencyUtils.formatRupiah(totalBudget),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (totalSpent > totalBudget && totalBudget > 0) MaterialTheme.colorScheme.error else EmeraldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(expenseCategories, key = { "cat_${it.id}" }) { cat ->
                val spent = categorySpentMap[cat.name] ?: 0.0
                BudgetProgressBar(
                    category = cat,
                    spentAmount = spent,
                    onEditBudgetClick = {
                        selectedCategoryForEdit = cat
                        editAmountInput = cat.budgetLimit.toLong().toString()
                    }
                )
            }
        }
    }

    // Edit Budget Limit Dialog
    if (selectedCategoryForEdit != null) {
        val cat = selectedCategoryForEdit!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForEdit = null },
            title = { Text("Atur Batas Anggaran (${cat.name})") },
            text = {
                Column {
                    Text(
                        text = "Masukkan batas pengeluaran bulanan dalam Rupiah:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editAmountInput,
                        onValueChange = { editAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Batas Anggaran (Rp)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_budget_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = editAmountInput.toDoubleOrNull() ?: 0.0
                        onUpdateBudgetLimit(cat.name, limit)
                        selectedCategoryForEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Simpan Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
