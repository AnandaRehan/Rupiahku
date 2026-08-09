package com.ehan.rupiahku.ui.screens

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ehan.rupiahku.data.model.AppSettings
import com.ehan.rupiahku.data.model.BackupHistoryEntity

@Composable
fun BackupScreen(
    appSettings: AppSettings,
    backupLogs: List<BackupHistoryEntity>,
    isBackupRunning: Boolean,
    onExportToFileUri: (Uri) -> Unit,
    onImportFromFileUri: (Uri) -> Unit,
    onTriggerLocalBackup: () -> Unit,
    onRestoreLocalBackup: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onFetchJsonPayload: suspend () -> String,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        appSettings = appSettings,
        backupLogs = backupLogs,
        isBackupRunning = isBackupRunning,
        onExportToFileUri = onExportToFileUri,
        onImportFromFileUri = onImportFromFileUri,
        onTriggerLocalBackup = onTriggerLocalBackup,
        onRestoreLocalBackup = onRestoreLocalBackup,
        onAutoBackupToggle = onAutoBackupToggle,
        onFetchJsonPayload = onFetchJsonPayload,
        modifier = modifier
    )
}

