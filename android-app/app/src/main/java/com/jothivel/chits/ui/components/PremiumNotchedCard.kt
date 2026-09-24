package com.jothivel.chits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Premium maroon dashboard card with real inward notches cut into both side edges.
 *
 * The notches are part of the Shape outline, so shadows, clipping, gradient background,
 * and child layout all respect the same path.
 */
class SideNotchedRoundedShape(
    private val cornerRadius: Dp = 30.dp,
    private val notchRadius: Dp = 16.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val corner = with(density) { cornerRadius.toPx() }
            .coerceAtMost(size.width / 2f)
            .coerceAtMost(size.height / 2f)
        val maxNotch = ((size.height - (corner * 2f)) / 2f).coerceAtLeast(0f)
        val notch = with(density) { notchRadius.toPx() }
            .coerceAtMost(size.width / 3f)
            .coerceAtMost(maxNotch)
        val centerY = size.height / 2f
        val kappa = 0.55228475f

        val path = Path().apply {
            moveTo(corner, 0f)
            lineTo(size.width - corner, 0f)
            quadraticBezierTo(size.width, 0f, size.width, corner)
            lineTo(size.width, centerY - notch)
            if (notch > 0f) {
                cubicTo(
                    size.width - (notch * kappa),
                    centerY - notch,
                    size.width - notch,
                    centerY - (notch * kappa),
                    size.width - notch,
                    centerY
                )
                cubicTo(
                    size.width - notch,
                    centerY + (notch * kappa),
                    size.width - (notch * kappa),
                    centerY + notch,
                    size.width,
                    centerY + notch
                )
            }
            lineTo(size.width, size.height - corner)
            quadraticBezierTo(size.width, size.height, size.width - corner, size.height)
            lineTo(corner, size.height)
            quadraticBezierTo(0f, size.height, 0f, size.height - corner)
            lineTo(0f, centerY + notch)
            if (notch > 0f) {
                cubicTo(
                    notch * kappa,
                    centerY + notch,
                    notch,
                    centerY + (notch * kappa),
                    notch,
                    centerY
                )
                cubicTo(
                    notch,
                    centerY - (notch * kappa),
                    notch * kappa,
                    centerY - notch,
                    0f,
                    centerY - notch
                )
            }
            lineTo(0f, corner)
            quadraticBezierTo(0f, 0f, corner, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun PremiumNotchedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 30.dp,
    notchRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(30.dp),
    gradient: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5E1F2F),
            Color(0xFF74263A),
            Color(0xFF4A1622)
        )
    ),
    content: @Composable () -> Unit
) {
    val shape = SideNotchedRoundedShape(cornerRadius = cornerRadius, notchRadius = notchRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0xFF4A1622).copy(alpha = .16f),
                spotColor = Color(0xFF4A1622).copy(alpha = .18f)
            )
            .clip(shape)
            .background(gradient)
            .padding(contentPadding)
    ) {
        content()
    }
}
