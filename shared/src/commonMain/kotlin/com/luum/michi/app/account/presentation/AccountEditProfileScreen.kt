package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformBackHandlerSetter
import com.luum.michi.app.core.platform.PlatformIcons

internal data class AccountProfileDraft(
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String,
    val links: List<AccountProfileLink> = emptyList(),
    val gender: AccountProfileGender = AccountProfileGender.PREFER_NOT_TO_SAY,
    val customGender: String = "",
    val email: String = "",
    val isEmailPublic: Boolean = false,
    val birthDate: String = "",
    val isBirthDatePublic: Boolean = false,
)

internal data class AccountProfileLink(
    val title: String,
    val url: String,
)

internal enum class AccountProfileGender {
    MALE,
    FEMALE,
    PREFER_NOT_TO_SAY,
    CUSTOM,
}

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
    val links = remember(initialDraft) { mutableStateListOf<AccountProfileLink>().also { it.addAll(initialDraft.links) } }
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
                    top = 112.dp,
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
                    AccountProfileDraft(
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

@Composable
private fun AccountAddLinkContent(
    onSave: (AccountProfileLink) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, top = 112.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = strings.accountAddLinkAction,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        AccountEditField(
            value = title,
            onValueChange = { title = it },
            label = strings.accountEditLinkTitleLabel,
        )
        AccountEditField(
            value = url,
            onValueChange = { url = it },
            label = strings.accountEditLinkUrlLabel,
        )
        Button(
            onClick = {
                AccountProfileLink(title = title.trim(), url = url.trim()).sanitized()?.let(onSave)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && isSafeProfileUrl(url.trim()),
        ) {
            Text(strings.accountSaveProfileAction)
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.backButton)
        }
    }
}

@Composable
private fun AccountLinksSection(
    links: List<AccountProfileLink>,
    onAddLink: () -> Unit,
) {
    val strings = LanguageProvider.strings

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = strings.accountEditLinksLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        links.forEach { link ->
            AccountLinkRow(link = link)
        }
        AccountAddOptionRow(
            text = strings.accountAddLinkAction,
            onClick = onAddLink,
        )
    }
}

@Composable
private fun AccountLinkRow(link: AccountProfileLink) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = link.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = link.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun AccountAddOptionRow(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = PlatformIcons.Add,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = text,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AccountGenderSelector(
    selected: AccountProfileGender,
    onSelect: (AccountProfileGender) -> Unit,
) {
    val strings = LanguageProvider.strings
    val options = listOf(
        AccountProfileGender.MALE to strings.accountGenderMale,
        AccountProfileGender.FEMALE to strings.accountGenderFemale,
        AccountProfileGender.PREFER_NOT_TO_SAY to strings.accountGenderPreferNotToSay,
        AccountProfileGender.CUSTOM to strings.accountGenderCustom,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = strings.accountEditGenderLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowOptions.forEach { (value, label) ->
                    val isSelected = selected == value
                    OutlinedButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountVisibilityField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPublic: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountEditField(
            value = value,
            onValueChange = onValueChange,
            label = label,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPublic) strings.accountVisibilityPublic else strings.accountVisibilityPrivate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.accountVisibilitySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isPublic,
                onCheckedChange = onVisibilityChange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountBirthDateField(
    value: String,
    onValueChange: (String) -> Unit,
    isPublic: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings
    var showCalendar by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { input ->
                val formattedValue = input.text.toBirthDateInput()
                textFieldValue = TextFieldValue(
                    text = formattedValue,
                    selection = TextRange(formattedValue.length),
                )
                onValueChange(formattedValue)
            },
            label = { Text(strings.accountEditBirthDateLabel) },
            placeholder = { Text("dd/MM/yyyy") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            trailingIcon = {
                IconButton(onClick = { showCalendar = true }) {
                    Icon(
                        painter = PlatformIcons.Calendar,
                        contentDescription = strings.accountSelectDateAction,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPublic) strings.accountVisibilityPublic else strings.accountVisibilityPrivate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.accountVisibilitySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isPublic,
                onCheckedChange = onVisibilityChange,
            )
        }
    }

    if (showCalendar) {
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toDayMonthYear())
                        }
                        showCalendar = false
                    },
                ) {
                    Text(strings.accountSelectDateConfirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text(strings.backButton)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun String.toBirthDateInput(): String {
    val digits = filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append('/')
            append(char)
        }
    }
}

private fun AccountProfileLink.sanitized(): AccountProfileLink? {
    val cleanTitle = title.trim()
    val cleanUrl = url.trim()
    if (cleanTitle.isBlank() || !isSafeProfileUrl(cleanUrl)) return null
    return AccountProfileLink(title = cleanTitle, url = cleanUrl)
}

private fun isSafeProfileUrl(value: String): Boolean {
    if (value.isBlank() || value.length > 2_048) return false
    val lowerValue = value.lowercase()
    if (!lowerValue.startsWith("https://")) return false
    if (lowerValue.any(Char::isWhitespace)) return false
    val hostStart = "https://".length
    val hostEnd = lowerValue.indexOf('/', startIndex = hostStart).let { if (it == -1) lowerValue.length else it }
    val host = lowerValue.substring(hostStart, hostEnd)
    return "." in host && host.none { it == '/' || it == '\\' || it == '@' }
}

private fun Long.toDayMonthYear(): String {
    val epochDay = floorDiv(other = MillisPerDay)
    val (year, month, day) = epochDay.toGregorianDate()
    return "${day.twoDigits()}/${month.twoDigits()}/$year"
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private const val MillisPerDay = 86_400_000L

private fun Long.floorDiv(other: Long): Long {
    var result = this / other
    if ((this xor other) < 0 && result * other != this) result--
    return result
}

private fun Long.toGregorianDate(): Triple<Int, Int, Int> {
    var zeroDay = this + 719_528L
    zeroDay -= 60L
    var adjust = 0L
    if (zeroDay < 0) {
        val adjustCycles = (zeroDay + 1L) / 146_097L - 1L
        adjust = adjustCycles * 400L
        zeroDay += -adjustCycles * 146_097L
    }
    var yearEstimate = (400L * zeroDay + 591L) / 146_097L
    var dayOfYearEstimate = zeroDay - (
        365L * yearEstimate + yearEstimate / 4L - yearEstimate / 100L + yearEstimate / 400L
    )
    if (dayOfYearEstimate < 0) {
        yearEstimate--
        dayOfYearEstimate = zeroDay - (
            365L * yearEstimate + yearEstimate / 4L - yearEstimate / 100L + yearEstimate / 400L
        )
    }
    yearEstimate += adjust
    val marchMonth = (dayOfYearEstimate * 5L + 2L) / 153L
    val month = ((marchMonth + 2L) % 12L + 1L).toInt()
    val day = (dayOfYearEstimate - (marchMonth * 306L + 5L) / 10L + 1L).toInt()
    val year = (yearEstimate + marchMonth / 10L).toInt()
    return Triple(year, month, day)
}

@Composable
private fun AccountEditableAvatar(
    username: String,
    avatarUrl: String?,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = username.take(2).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AccountEditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}
