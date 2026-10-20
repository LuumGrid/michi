package com.luum.michi.app.notifications.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.ui.model.NotificationRowItem
import com.luum.michi.app.notifications.ui.model.submissionStatusLabel
import com.luum.michi.app.ui.components.AppButton
import com.luum.michi.app.ui.components.ButtonEmphasis
import com.luum.michi.app.ui.components.ModalSheet

/**
 * Page-scoped detail sheet for media-change / merge / deletion and submission
 * notifications. The row text is the title; each non-blank detail field is a
 * labelled block. Only a real entity target gets the primary "Open" action —
 * web links and inert targets just show the details.
 */
@Composable
internal fun NotificationDetailsSheet(
    item: NotificationRowItem,
    strings: LanguageStrings,
    onDismiss: () -> Unit,
    onOpenTarget: () -> Unit,
) {
    val details = item.details ?: return
    val footer: (@Composable RowScope.() -> Unit)? = if (item.target.isOpenable()) {
        {
            AppButton(
                label = strings.notificationDetailOpenAction,
                onClick = onOpenTarget,
                emphasis = ButtonEmphasis.Primary,
            )
        }
    } else {
        null
    }
    ModalSheet(
        title = item.text,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        footer = footer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            details.reason?.let {
                NotificationDetailField(strings.notificationDetailReasonLabel, it)
            }
            details.notes?.let {
                NotificationDetailField(strings.notesLabel, it)
            }
            details.status?.let {
                NotificationDetailField(strings.statusLabel, submissionStatusLabel(it, strings))
            }
            if (details.affectedTitles.isNotEmpty()) {
                NotificationDetailField(
                    label = strings.notificationDetailAffectedTitlesLabel,
                    value = details.affectedTitles.joinToString("\n"),
                )
            }
        }
    }
}

/** Uppercase label above the value, the same read-only block used by info panels. */
@Composable
private fun NotificationDetailField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Only real entities can be pushed as a detail page. */
private fun NotificationTarget.isOpenable(): Boolean = when (this) {
    is NotificationTarget.Media,
    is NotificationTarget.Character,
    is NotificationTarget.Staff -> true
    is NotificationTarget.Web,
    NotificationTarget.None -> false
}
