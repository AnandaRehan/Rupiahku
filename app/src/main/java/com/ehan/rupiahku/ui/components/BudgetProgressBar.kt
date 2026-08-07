package com.ehan.rupiahku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehan.rupiahku.data.model.CategoryEntity
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.ExpenseRed
import com.ehan.rupiahku.ui.theme.GoldAccent
import com.ehan.rupiahku.util.CurrencyUtils

@Composable
fun BudgetProgressBar(
    category: CategoryEntity,
    spentAmount: Double,
    onEditBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = category.budgetLimit
    val ratio = if (budget > 0) (spentAmount / budget).coerceIn(0.0, 1.5).toFloat() else 0f
    val percentage = if (budget > 0) (spentAmount / budget * 100).toInt() else 0

    val barColor = when {
        budget <= 0 -> MaterialTheme.colorScheme.primary
        percentage > 100 -> ExpenseRed
        percentage > 85 -> GoldAccent
        else -> EmeraldPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_item_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (percentage > 100 && budget > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Over budget",
                            tint = ExpenseRed,
                            modifier = Modifier.width(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onEditBudgetClick,
                    modifier = Modifier.testTag("edit_budget_${category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Target",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (ratio / 1.5f).coerceIn(0f, 1f))
                        .background(barColor, RoundedCornerShape(5.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai: " + CurrencyUtils.formatRupiah(spentAmount),
                    fontSize = 12.sp,
                    color = if (percentage > 100 && budget > 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (budget > 0) "Batas: " + CurrencyUtils.formatRupiah(budget) else "Belum diatur",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
