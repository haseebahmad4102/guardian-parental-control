package com.example.guardian.child.ui.theme

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

/** The app's signature footer, shown on primary screens. */
const val DEVELOPER_CREDIT = "Developed by Muhammad Haseeb"

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

/** A soft vertical brand gradient used behind hero content. */
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
                        scheme.surface,
                        scheme.surface,
                    )
                )
            )
    ) { content() }
}
