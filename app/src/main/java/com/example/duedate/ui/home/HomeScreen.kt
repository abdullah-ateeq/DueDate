package com.example.duedate.ui.home

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.duedate.domain.DueDateFormatter
import com.example.duedate.domain.DueDateValidator
import com.example.duedate.model.CalculationState
import com.example.duedate.model.DurationPreset
import com.example.duedate.model.DurationUnit
import com.example.duedate.ui.DueDateUiState
import com.example.duedate.ui.DueDateViewModel
import com.example.duedate.util.DateConversions
import com.example.duedate.util.ShareActions
import com.example.duedate.util.rememberAppHaptics
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(viewModel: DueDateViewModel, state: DueDateUiState, snackbarHostState: SnackbarHostState) {
    val formatter = remember { DueDateFormatter() }
    val haptics = rememberAppHaptics(state.settings.hapticsEnabled)
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val onCalculate = {
        focusManager.clearFocus()
        if (state.canCalculate) {
            haptics.confirm()
            viewModel.calculate()
        }
    }

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
            Header()

            StartDateCard(
                date = state.startDate,
                formatter = formatter,
                onPick = { showDatePicker = true },
                onToday = {
                    haptics.tick()
                    viewModel.onTodaySelected()
                }
            )

            val calculationError = state.calculationErrorMessage()
            DurationInput(
                value = state.durationInput,
                errorMessage = state.errorMessage.takeIf { it != calculationError },
                onValueChange = viewModel::onDurationChange,
                onDone = onCalculate
            )

            UnitSelector(
                selected = state.unit,
                formatter = formatter,
                onSelect = {
                    haptics.tick()
                    viewModel.onUnitSelected(it)
                }
            )

            IncludeStartDateRow(
                checked = state.includeStartDate,
                onCheckedChange = {
                    haptics.tick()
                    viewModel.onIncludeStartDateChange(it)
                }
            )

            QuickPresets(
                state = state,
                formatter = formatter,
                onSelect = {
                    haptics.tick()
                    viewModel.onPresetSelected(it)
                }
            )

            CalculateActions(
                enabled = state.canCalculate,
                onCalculate = onCalculate,
                onReset = {
                    haptics.tick()
                    focusManager.clearFocus()
                    viewModel.reset()
                }
            )

            if (calculationError != null && state.result == null) {
                CalculationErrorCard(calculationError)
            }

            ResultSection(
                result = state.result,
                formatter = formatter,
                animationsEnabled = state.settings.animationsEnabled,
                onCopy = {
                    viewModel.copyText()?.let { text ->
                        haptics.confirm()
                        ShareActions.copyToClipboard(context, text)
                        // Android 13+ shows its own clipboard confirmation.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            scope.launch { snackbarHostState.showSnackbar("Result copied") }
                        }
                    }
                },
                onShare = {
                    viewModel.shareText()?.let { text -> ShareActions.share(context, text) }
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        StartDatePickerDialog(
            initialDate = state.startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                haptics.tick()
                viewModel.onStartDateSelected(date)
            }
        )
    }
}

/** A calculation failure (as opposed to an input validation message shown under the field). */
private fun DueDateUiState.calculationErrorMessage(): String? =
    (calculationState as? CalculationState.Error)?.message
        ?.takeIf { it == DueDateValidator.ERROR_INVALID_DATE }

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "DueDate",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = "Calculate an expected date from a start date and duration.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun StartDateCard(
    date: LocalDate,
    formatter: DueDateFormatter,
    onPick: () -> Unit,
    onToday: () -> Unit
) {
    val dateText = formatter.formatDate(date)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClickLabel = "Change start date", onClick = onPick)
                    .clearAndSetSemantics {
                        contentDescription = "Start date, $dateText"
                        role = Role.Button
                    }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(14.dp))
                Column {
                    SectionLabel("START DATE")
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatter.formatDayOfWeek(date.dayOfWeek),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(
                onClick = onToday,
                modifier = Modifier.semantics { contentDescription = "Set start date to today" }
            ) {
                Text("TODAY")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = DateConversions.toPickerMillis(initialDate))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let { onConfirm(DateConversions.fromPickerMillis(it)) } },
                enabled = pickerState.selectedDateMillis != null
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun DurationInput(
    value: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Duration, ${value.ifBlank { "empty" }}" },
        label = { Text("DURATION") },
        placeholder = { Text("30") },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = {
            Text(errorMessage ?: "Whole number from 0 to 1,000,000")
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = MaterialTheme.typography.titleLarge
    )
}

@Composable
private fun UnitSelector(selected: DurationUnit, formatter: DueDateFormatter, onSelect: (DurationUnit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("UNIT")
        val units = DurationUnit.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            units.forEachIndexed { index, unit ->
                val label = formatter.formatUnit(unit)
                val isSelected = unit == selected
                SegmentedButton(
                    selected = isSelected,
                    onClick = { onSelect(unit) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = units.size),
                    icon = {},
                    modifier = Modifier.semantics {
                        contentDescription = "Duration unit, ${label.lowercase()}"
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                    },
                    label = {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun IncludeStartDateRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Include start date in duration"
                    stateDescription = if (checked) "On" else "Off"
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Include start date in duration",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.size(8.dp))
                    StatusPill(text = if (checked) "ON" else "OFF", highlighted = checked)
                }
                Text(
                    text = "Count the start date as the first day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (checked) {
                        "Start date is day 1 · 1 Oct + 1 day = 1 Oct"
                    } else {
                        "Start date is day 0 · 1 Oct + 1 day = 2 Oct"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.size(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
internal fun StatusPill(text: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun QuickPresets(state: DueDateUiState, formatter: DueDateFormatter, onSelect: (DurationPreset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel("QUICK DURATIONS")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(DurationPreset.ALL, key = { "${it.duration}-${it.unit}" }) { preset ->
                val label = formatter.formatDuration(preset.duration, preset.unit)
                val selected = state.unit == preset.unit && state.durationInput.trim() == preset.duration.toString()
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(preset) },
                    label = { Text(label.replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.semantics {
                        contentDescription = "Quick duration, $label"
                        stateDescription = if (selected) "Selected" else "Not selected"
                    }
                )
            }
        }
    }
}

@Composable
private fun CalculateActions(enabled: Boolean, onCalculate: () -> Unit, onReset: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
            onClick = onCalculate,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics { contentDescription = "Calculate expected due date" },
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Outlined.EventAvailable, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("CALCULATE DUE DATE", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("RESET")
        }
    }
}

@Composable
private fun CalculationErrorCard(message: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
            Spacer(Modifier.size(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
