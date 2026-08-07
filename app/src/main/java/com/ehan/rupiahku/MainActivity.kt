package com.ehan.rupiahku

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehan.rupiahku.ui.FinanceViewModel
import com.ehan.rupiahku.ui.components.AddBillBottomSheet
import com.ehan.rupiahku.ui.components.AddTransactionBottomSheet
import com.ehan.rupiahku.ui.screens.BackupScreen
import com.ehan.rupiahku.ui.screens.BillsScreen
import com.ehan.rupiahku.ui.screens.BudgetsScreen
import com.ehan.rupiahku.ui.screens.HomeScreen
import com.ehan.rupiahku.ui.screens.TransactionsScreen
import com.ehan.rupiahku.ui.theme.EmeraldPrimary
import com.ehan.rupiahku.ui.theme.RupiahKuTheme
import com.ehan.rupiahku.util.NotificationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class NavTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavTab("home", "Dasbor", Icons.Default.Home)
    object Transactions : NavTab("transactions", "Transaksi", Icons.Default.ReceiptLong)
    object Bills : NavTab("bills", "Tagihan", Icons.Default.NotificationsActive)
    object Budgets : NavTab("budgets", "Anggaran", Icons.Default.AccountBalance)
    object Backup : NavTab("backup", "Backup", Icons.Default.CloudDone)
}

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        setContent {
            RupiahKuTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf<NavTab>(NavTab.Home) }

    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val backupLogs by viewModel.backupLogs.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isBackupRunning by viewModel.isBackupRunning.collectAsStateWithLifecycle()

    var showAddTxSheet by remember { mutableStateOf(false) }
    var showAddBillSheet by remember { mutableStateOf(false) }

    val addTxSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addBillSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Listen to Toast Messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val tabs = listOf(
                    NavTab.Home,
                    NavTab.Transactions,
                    NavTab.Bills,
                    NavTab.Budgets,
                    NavTab.Backup
                )

                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab.route == tab.route,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(text = tab.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.Home -> {
                    HomeScreen(
                        summary = summary,
                        recentTransactions = transactions,
                        upcomingBills = bills.filter { !it.isPaid },
                        latestBackupLog = backupLogs.firstOrNull(),
                        onAddTransactionClick = { showAddTxSheet = true },
                        onAddBillClick = { showAddBillSheet = true },
                        onViewAllTransactionsClick = { currentTab = NavTab.Transactions },
                        onViewAllBillsClick = { currentTab = NavTab.Bills },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onPayBill = { viewModel.payBill(it) },
                        onDeleteBill = { viewModel.deleteBill(it) }
                    )
                }

                NavTab.Transactions -> {
                    TransactionsScreen(
                        transactions = transactions,
                        onAddTransactionClick = { showAddTxSheet = true },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }

                NavTab.Bills -> {
                    BillsScreen(
                        bills = bills,
                        onAddBillClick = { showAddBillSheet = true },
                        onPayBill = { viewModel.payBill(it) },
                        onDeleteBill = { viewModel.deleteBill(it) }
                    )
                }

                NavTab.Budgets -> {
                    BudgetsScreen(
                        categories = categories,
                        transactions = transactions,
                        onUpdateBudgetLimit = { catName, limit ->
                            viewModel.updateBudgetLimit(catName, limit)
                        }
                    )
                }

                NavTab.Backup -> {
                    BackupScreen(
                        appSettings = appSettings,
                        backupLogs = backupLogs,
                        isBackupRunning = isBackupRunning,
                        onBackupNowClick = { viewModel.triggerCloudBackup() },
                        onRestoreClick = { viewModel.restoreCloudData() },
                        onAutoBackupToggle = { enabled ->
                            viewModel.updateAutoBackupSetting(enabled, appSettings.backupFrequency)
                        },
                        onUpdateFrequency = { freq ->
                            viewModel.updateAutoBackupSetting(appSettings.autoBackupEnabled, freq)
                        },
                        onFetchJsonPayload = { viewModel.getJsonBackupPayload() }
                    )
                }
            }

            // Bottom Sheet for Adding Transaction
            if (showAddTxSheet) {
                AddTransactionBottomSheet(
                    categories = categories,
                    sheetState = addTxSheetState,
                    onDismiss = { showAddTxSheet = false },
                    onSubmit = { title, amount, type, categoryName, accountName, note ->
                        viewModel.addTransaction(title, amount, type, categoryName, accountName, note)
                        scope.launch { addTxSheetState.hide() }.invokeOnCompletion {
                            showAddTxSheet = false
                        }
                    }
                )
            }

            // Bottom Sheet for Adding Bill Reminder
            if (showAddBillSheet) {
                AddBillBottomSheet(
                    sheetState = addBillSheetState,
                    onDismiss = { showAddBillSheet = false },
                    onSubmit = { title, amount, categoryName, dueDateMillis, repeatInterval ->
                        viewModel.addBill(title, amount, categoryName, dueDateMillis, repeatInterval)
                        scope.launch { addBillSheetState.hide() }.invokeOnCompletion {
                            showAddBillSheet = false
                        }
                    }
                )
            }
        }
    }
}
