package com.example.duedate.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Haptic helper that respects the user's Haptics setting. */
class AppHaptics(private val feedback: HapticFeedback, private val enabled: Boolean) {
    fun confirm() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
}

@Composable
fun rememberAppHaptics(enabled: Boolean): AppHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { AppHaptics(feedback, enabled) }
}
