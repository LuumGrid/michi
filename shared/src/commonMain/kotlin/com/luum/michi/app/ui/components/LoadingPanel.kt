package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.network.LocalNetworkThrottle

/**
 * Centered first-load spinner + label, shared by the load-state containers and
 * the list/calendar screens. The one loading block of the app.
 *
 * While the transport's rate limiter is actually holding requests (not merely a
 * slow network), it shows [waitingLabel] as a secondary line under the primary
 * label, so a throttled load never looks stuck.
 */
@Composable
internal fun LoadingPanel(
    label: String,
    waitingLabel: String,
    modifier: Modifier = Modifier,
) {
    val waiting by LocalNetworkThrottle.current.waitingRequests.collectAsState()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (waiting > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = waitingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
