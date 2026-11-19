package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSurfaceHighest
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun LottieLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    message: String? = null,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_loading_pulse),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(size)
                .testTag("lottie_loading_animation"),
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LottieLoadingCard(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            LottieLoadingIndicator(
                size = 84.dp,
                message = message ?: stringResource(R.string.loading),
            )
        }
    }
}

@Composable
fun LottieButtonSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_loading_pulse),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size),
    )
}

@Composable
fun LottieTaskCompleteOverlay(
    message: String?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = !message.isNullOrBlank()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_task_success),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = visible,
        restartOnPlay = true,
    )

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            delay(1350L)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(initialOffsetY = { it / 3 }) +
            scaleIn(initialScale = 0.92f),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
        modifier = modifier,
    ) {
        GlassCard(
            highlighted = true,
            cornerRadius = 20.dp,
            containerColor = GlassSurfaceHighest,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .testTag("lottie_task_complete_banner"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
        }
    }
}
