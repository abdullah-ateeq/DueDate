package com.example.duedate.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.duedate.model.ThemeMode
import com.example.duedate.ui.DueDateUiState
import com.example.duedate.ui.DueDateViewModel
import com.example.duedate.ui.home.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(state: DueDateUiState, viewModel: DueDateViewModel, snackbarHostState: SnackbarHostState) {
    val settings = state.settings
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })

            SettingsGroup("APPEARANCE") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    val modes = ThemeMode.entries
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
                GroupDivider()
                SwitchRow(
                    title = "Dynamic color",
                    description = if (dynamicColorSupported) "Use colors from your wallpaper." else "Requires Android 12 or later.",
                    checked = settings.dynamicColor && dynamicColorSupported,
                    enabled = dynamicColorSupported,
                    onCheckedChange = viewModel::setDynamicColor
                )
                GroupDivider()
                SwitchRow(
                    title = "Animations",
                    description = "Animate results and the splash screen.",
                    checked = settings.animationsEnabled,
                    onCheckedChange = viewModel::setAnimationsEnabled
                )
                GroupDivider()
                SwitchRow(
                    title = "Haptics",
                    description = "Vibrate subtly on key actions.",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = viewModel::setHapticsEnabled
                )
            }

            SettingsGroup("CALCULATION") {
                SwitchRow(
                    title = "Include start date in duration",
                    description = "Count the start date as the first day. When off, the start date is day 0 " +
                        "(1 Oct + 1 day = 2 Oct). When on, 1 Oct + 1 day = 1 Oct, and month or year durations " +
                        "end the day before the calendar date (1 Oct + 1 month = 31 Oct).",
                    checked = state.includeStartDate,
                    onCheckedChange = viewModel::onIncludeStartDateChange
                )
                GroupDivider()
                SwitchRow(
                    title = "Preserve last configuration",
                    description = "Restore the start date, duration, unit and start-date setting when the app opens.",
                    checked = settings.preserveLastConfiguration,
                    onCheckedChange = viewModel::setPreserveLastConfiguration
                )
            }

            SettingsGroup("HISTORY") {
                SwitchRow(
                    title = "Confirm history deletion",
                    description = "Ask before deleting a single calculation.",
                    checked = settings.confirmHistoryDelete,
                    onCheckedChange = viewModel::setConfirmHistoryDelete
                )
                GroupDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.history.isNotEmpty(), role = Role.Button) { showClearDialog = true }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Clear History",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (state.history.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    Text(
                        if (state.history.isEmpty()) "No recent calculations." else "${state.history.size} recent calculations saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Clear all recent calculations?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearHistory()
                    scope.launch { snackbarHostState.showSnackbar("History cleared") }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, Modifier.padding(start = 4.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .semantics(mergeDescendants = true) { stateDescription = if (checked) "On" else "Off" }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
