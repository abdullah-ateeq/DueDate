package com.example.duedate.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.duedate.domain.DueDateFormatter
import com.example.duedate.model.DueDateResult

@Composable
fun ResultSection(
    result: DueDateResult?,
    formatter: DueDateFormatter,
    animationsEnabled: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    if (result == null) {
        ResultPlaceholder()
        return
    }

    // A single 450 ms progress drives four staggered stages: due date, duration summary,
    // day of week / details, and boundary information.
    val progress = remember(result) { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(result, animationsEnabled) {
        if (animationsEnabled) {
            progress.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val stage: (Int) -> Modifier = { index ->
        Modifier.graphicsLayer {
            val start = index * 0.18f
            val local = ((progress.value - start) / 0.46f).coerceIn(0f, 1f)
            alpha = local
            translationY = (1f - local) * 16.dp.toPx()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DueDateHero(result, formatter, stage(0))
        SummaryCard(result, formatter, stage(1))
        BoundaryChips(result, stage(2))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = stage(3).fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Copy due-date calculation" }
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("COPY RESULT")
            }
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Share due-date calculation" }
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("SHARE RESULT")
            }
        }
    }
}

@Composable
private fun ResultPlaceholder() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your expected due date will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DueDateHero(result: DueDateResult, formatter: DueDateFormatter, modifier: Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = formatter.resultAccessibilityText(result)
                },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "EXPECTED DUE DATE",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatter.formatDate(result.dueDate),
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = formatter.formatDayOfWeek(result.dueDayOfWeek).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.size(6.dp))
            StatusPill(
                text = formatter.formatHeadline(result),
                highlighted = true
            )
            Text(
                text = formatter.formatDescription(result),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SummaryCard(result: DueDateResult, formatter: DueDateFormatter, modifier: Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SummaryRow("DURATION", formatter.formatDuration(result.duration, result.unit))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("START", formatter.formatDate(result.startDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("DUE DATE", formatter.formatDate(result.dueDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("DAY OF WEEK", formatter.formatDayOfWeek(result.dueDayOfWeek))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("CALENDAR DAYS", formatter.formatCalendarDays(result).uppercase())
            if (result.includeStartDate && result.duration > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SummaryRow("DAYS COUNTED", "${formatter.formatCount(result.inclusiveCalendarDays)} incl. start date")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("START DATE INCLUDED", if (result.includeStartDate) "On · counted as day 1" else "Off · start is day 0")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(label, Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoundaryChips(result: DueDateResult, modifier: Modifier) {
    val labels = buildList {
        if (result.crossedMonth) add("MONTH CHANGED")
        if (result.crossedYear) add("YEAR CHANGED")
        if (result.crossedLeapDay && !result.isSameDate) add("LEAP DAY INCLUDED")
    }
    if (labels.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
