package com.qwenton.studio.apex.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = DarkCyanSurface,
    borderColor: Color = CyanBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                border = BorderStroke(borderWidth, borderColor),
                shape = shape
            ),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassmorphicPill(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ObsidianSurface,
    borderColor: Color = CyanBorder,
    shape: Shape = RoundedCornerShape(50),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        content = content
    )
}
