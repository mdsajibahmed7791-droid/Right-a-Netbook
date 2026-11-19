package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val NotebookBackgroundBrush: Brush = Brush.verticalGradient(
    colors = listOf(NavyDeep, NavyMid, TealDeep)
)

private val NotebookDarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = NavyDeep,
    primaryContainer = TealSoft,
    onPrimaryContainer = TextPrimary,
    secondary = TealBright,
    onSecondary = NavyDeep,
    secondaryContainer = GlassSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarm,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = GlassSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = NavyDeep,
    surfaceContainerLow = NavyMid,
    surfaceContainer = GlassSurface,
    surfaceContainerHigh = GlassSurfaceElevated,
    surfaceContainerHighest = GlassSurfaceHighest,
    outline = GlassBorder,
    outlineVariant = GlassBorderTeal,
    scrim = ScrimDark,
    error = ErrorCoral,
    onError = NavyDeep,
    errorContainer = ErrorCoralTint,
    onErrorContainer = ErrorCoral,
)

private val NotebookShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun NotebookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NotebookDarkColorScheme,
        typography = NotebookTypography,
        shapes = NotebookShapes,
        content = content,
    )
}

@Composable
fun NotebookGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = NotebookBackgroundBrush)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Soft ambient radial teal glow at bottom-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TealPrimary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * 0.78f, h * 0.92f),
                        radius = w * 0.85f,
                    ),
                    center = Offset(w * 0.78f, h * 0.92f),
                    radius = w * 0.85f,
                )

                // Faint glowing teal wave lines near the bottom
                val wave1 = Path().apply {
                    moveTo(0f, h * 0.86f)
                    cubicTo(w * 0.28f, h * 0.79f, w * 0.65f, h * 0.94f, w, h * 0.83f)
                }
                val wave2 = Path().apply {
                    moveTo(0f, h * 0.91f)
                    cubicTo(w * 0.35f, h * 0.97f, w * 0.68f, h * 0.82f, w, h * 0.90f)
                }
                val wave3 = Path().apply {
                    moveTo(0f, h * 0.95f)
                    cubicTo(w * 0.30f, h * 0.88f, w * 0.72f, h * 0.98f, w, h * 0.93f)
                }

                drawPath(
                    path = wave1,
                    color = TealBright.copy(alpha = 0.14f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = wave1,
                    color = TealPrimary.copy(alpha = 0.28f),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = wave2,
                    color = TealPrimary.copy(alpha = 0.16f),
                    style = Stroke(width = 6f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = wave2,
                    color = TealBright.copy(alpha = 0.22f),
                    style = Stroke(width = 1.6f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = wave3,
                    color = TealPrimary.copy(alpha = 0.15f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round),
                )
            },
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    errorBorder: Boolean = false,
    cornerRadius: Dp = 18.dp,
    containerColor: Color = GlassSurface,
    content: @Composable () -> Unit,
) {
    val borderColor = when {
        errorBorder -> ErrorCoral
        highlighted -> GlassBorderTeal
        else -> GlassBorder
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(if (errorBorder || highlighted) 1.25.dp else 1.dp, borderColor),
        content = content,
    )
}

@Composable
fun TealGlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val ringColor = if (enabled) TealBright.copy(alpha = 0.35f) else Color.Transparent
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = TealPrimary,
            contentColor = NavyDeep,
            disabledContainerColor = TealPrimary.copy(alpha = 0.35f),
            disabledContentColor = NavyDeep.copy(alpha = 0.6f),
        ),
        contentPadding = contentPadding,
        modifier = modifier
            .heightIn(min = 48.dp)
            .border(width = 2.dp, color = ringColor, shape = CircleShape),
        content = content,
    )
}

@Composable
fun GlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = GlassBorderTeal,
    contentColor: Color = TextPrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (enabled) borderColor else GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GlassSurfaceElevated,
            contentColor = contentColor,
        ),
        contentPadding = contentPadding,
        modifier = modifier.heightIn(min = 48.dp),
        content = content,
    )
}
