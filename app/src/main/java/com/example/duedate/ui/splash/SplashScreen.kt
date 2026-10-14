package com.example.duedate.ui.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private data class SplashStep(val text: String, val isTitle: Boolean = false)

// Illustrative sample only (14 Oct + 30 days = 13 Nov); the app itself always uses LocalDate.now().
private val steps = listOf(
    SplashStep("14 OCT"),
    SplashStep("+30 DAYS"),
    SplashStep("13 NOV"),
    SplashStep("DueDate", isTitle = true)
)

private const val STEP_MILLIS = 280L
private const val HOLD_MILLIS = 180L

@Composable
fun SplashScreen(animationsEnabled: Boolean, onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(if (animationsEnabled) 0 else steps.lastIndex) }
    val finish by rememberUpdatedState(onFinished)

    // 3 x 280 ms steps + 180 ms hold on the title, plus the final fade ≈ 1.2 s in total.
    LaunchedEffect(Unit) {
        while (step < steps.lastIndex) {
            delay(STEP_MILLIS)
            step++
        }
        delay(if (animationsEnabled) HOLD_MILLIS + 200 else 1000)
        finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "DueDate" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AnimatedContent(
                targetState = steps[step],
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 })
                        .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
                },
                label = "splash-step"
            ) { current ->
                Box(Modifier.height(64.dp), contentAlignment = Alignment.Center) {
                    if (current.isTitle) {
                        Text(
                            text = current.text,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = current.text,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
