package com.example.guardian.parent.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

const val DEVELOPER_CREDIT = "Developed by Muhammad Haseeb"

/** A colorful vertical brand gradient used behind hero content. */
@Composable
fun BrandGradient(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.primaryContainer,
                        scheme.tertiaryContainer.copy(alpha = 0.35f),
                        scheme.surface,
                    )
                )
            )
    ) { content() }
}

@Composable
fun DevelopedByFooter(modifier: Modifier = Modifier) {
    Text(
        text = DEVELOPER_CREDIT,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}
