package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.components.AccountAddLinkContent
import com.luum.michi.app.account.presentation.components.AccountBirthDateField
import com.luum.michi.app.account.presentation.components.AccountEditField
import com.luum.michi.app.account.presentation.components.AccountEditableAvatar
import com.luum.michi.app.account.presentation.components.AccountGenderSelector
import com.luum.michi.app.account.presentation.components.AccountLinksSection
import com.luum.michi.app.account.presentation.components.AccountVisibilityField
import com.luum.michi.app.account.presentation.model.AccountProfileDraft
import com.luum.michi.app.account.presentation.model.AccountProfileGender
import com.luum.michi.app.account.presentation.model.AccountProfileLink
import com.luum.michi.app.account.presentation.util.isSafeProfileUrl
import com.luum.michi.app.account.presentation.util.sanitized
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformBackHandlerSetter

@Composable
internal fun AccountEditProfileScreen(
    initialDraft: AccountProfileDraft,
    onSave: (AccountProfileDraft) -> Unit,
    onBackHandlerChange: PlatformBackHandlerSetter,
) {
    val strings = LanguageProvider.strings
    var username by remember(initialDraft) { mutableStateOf(initialDraft.username) }
    var displayName by remember(initialDraft) { mutableStateOf(initialDraft.displayName) }
    var avatarUrl by remember(initialDraft) { mutableStateOf(initialDraft.avatarUrl.orEmpty()) }
    var bio by remember(initialDraft) { mutableStateOf(initialDraft.bio) }
    val links = remember(initialDraft) {
        mutableStateListOf<AccountProfileLink>().apply { addAll(initialDraft.links) }
    }
    var gender by remember(initialDraft) { mutableStateOf(initialDraft.gender) }
    var customGender by remember(initialDraft) { mutableStateOf(initialDraft.customGender) }
    var email by remember(initialDraft) { mutableStateOf(initialDraft.email) }
    var isEmailPublic by remember(initialDraft) { mutableStateOf(initialDraft.isEmailPublic) }
    var birthDate by remember(initialDraft) { mutableStateOf(initialDraft.birthDate) }
    var isBirthDatePublic by remember(initialDraft) { mutableStateOf(initialDraft.isBirthDatePublic) }
    var showAvatarUrlField by remember(initialDraft) { mutableStateOf(false) }
    var showAddLink by remember(initialDraft) { mutableStateOf(false) }

    DisposableEffect(showAddLink) {
        onBackHandlerChange(if (showAddLink) ({ showAddLink = false }) else null)
        onDispose { onBackHandlerChange(null) }
    }

    if (showAddLink) {
        AccountAddLinkContent(
            onSave = { link ->
                links.add(link)
                showAddLink = false
            },
            onCancel = { showAddLink = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccountEditableAvatar(
            username = username,
            avatarUrl = avatarUrl.ifBlank { null },
        )

        TextButton(onClick = { showAvatarUrlField = !showAvatarUrlField }) {
            Text(
                text = strings.accountChangeProfilePhotoAction,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountEditField(
                value = displayName,
                onValueChange = { displayName = it },
                label = strings.accountEditNameLabel,
            )
            AccountEditField(
                value = username,
                onValueChange = { username = it },
                label = strings.accountEditUsernameLabel,
            )
            AccountEditField(
                value = bio,
                onValueChange = { bio = it },
                label = strings.accountEditBioLabel,
                minLines = 3,
            )

            AccountLinksSection(
                links = links,
                onAddLink = { showAddLink = true },
            )

            AccountGenderSelector(
                selected = gender,
                onSelect = { gender = it },
            )

            if (gender == AccountProfileGender.CUSTOM) {
                AccountEditField(
                    value = customGender,
                    onValueChange = { customGender = it },
                    label = strings.accountEditCustomGenderLabel,
                )
            }

            AccountVisibilityField(
                value = email,
                onValueChange = { email = it },
                label = strings.accountEditEmailLabel,
                isPublic = isEmailPublic,
                onVisibilityChange = { isEmailPublic = it },
            )

            AccountBirthDateField(
                value = birthDate,
                onValueChange = { birthDate = it },
                isPublic = isBirthDatePublic,
                onVisibilityChange = { isBirthDatePublic = it },
            )

            if (showAvatarUrlField) {
                AccountEditField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = strings.accountEditAvatarUrlLabel,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                onSave(
                    initialDraft.copy(
                        username = username.trim(),
                        displayName = displayName.trim(),
                        avatarUrl = avatarUrl.trim().takeIf(::isSafeProfileUrl),
                        bio = bio.trim(),
                        links = links.mapNotNull(AccountProfileLink::sanitized),
                        gender = gender,
                        customGender = customGender.trim(),
                        email = email.trim(),
                        isEmailPublic = isEmailPublic,
                        birthDate = birthDate.trim(),
                        isBirthDatePublic = isBirthDatePublic,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && displayName.isNotBlank(),
        ) {
            Text(strings.accountSaveProfileAction)
        }
    }
}
