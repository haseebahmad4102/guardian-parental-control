package com.example.guardian.parent.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.guardian.core.usage.AppUsage
import com.example.guardian.parent.util.formatMinutes

/** Horizontal bar chart of per-app foreground time, largest first. */
@Composable
fun UsageChart(
    apps: List<AppUsage>,
    maxItems: Int = 8,
    modifier: Modifier = Modifier,
) {
    val shown = apps.take(maxItems)
    val peak = (shown.firstOrNull()?.minutes ?: 0L).coerceAtLeast(1L)

    Column(modifier) {
        shown.forEach { app ->
            UsageBar(app = app, fraction = app.minutes.toFloat() / peak.toFloat())
            Spacer(Modifier.height(10.dp))
        }
        val remaining = apps.size - shown.size
        if (remaining > 0) {
            Text(
                "+ $remaining more app${if (remaining == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UsageBar(app: AppUsage, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            text = formatMinutes(app.minutes),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .width(64.dp)
                .padding(start = 8.dp),
        )
    }
}
