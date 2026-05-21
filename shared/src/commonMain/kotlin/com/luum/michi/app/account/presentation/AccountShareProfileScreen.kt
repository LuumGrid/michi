package com.luum.michi.app.account.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.setPlainText
import kotlinx.coroutines.launch

@Composable
internal fun AccountShareProfileScreen(
    username: String,
    displayName: String,
    avatarUrl: String?,
) {
    val strings = LanguageProvider.strings
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val profileUrl = remember(username) { "https://luum.lat/${username.toProfilePathSegment()}" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(PaddingValues(start = 20.dp, top = 112.dp, end = 20.dp, bottom = 24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AccountShareAvatar(
                    username = username,
                    avatarUrl = avatarUrl,
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.74f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(252.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AccountProfileQrCode(
                        seed = profileUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                    AccountShareAvatar(
                        username = username,
                        avatarUrl = avatarUrl,
                        modifier = Modifier.size(54.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = profileUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    scope.launch { clipboard.setPlainText(profileUrl) }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    painter = PlatformIcons.Share,
                    contentDescription = strings.accountShareProfileAction,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(strings.accountShareProfileAction, maxLines = 1)
            }

            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    painter = PlatformIcons.Download,
                    contentDescription = strings.accountDownloadProfileQrAction,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(strings.accountDownloadProfileQrAction, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AccountProfileQrCode(
    seed: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(seed) { createAccountQrMatrix(seed) }
    val onColor = Color(0xFF111111)
    val offColor = Color.White

    Canvas(modifier = modifier) {
        val moduleSize = size.minDimension / matrix.size

        drawRect(color = offColor, size = size)

        for (row in 0 until matrix.size) {
            for (column in 0 until matrix.size) {
                if (matrix[row, column]) {
                    drawRect(
                        color = onColor,
                        topLeft = Offset(column * moduleSize, row * moduleSize),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}

private fun String.toProfilePathSegment(): String {
    return lowercase()
        .filter { char -> char.isLetterOrDigit() || char == '_' || char == '.' || char == '-' }
        .take(30)
        .ifBlank { "profile" }
}

private data class AccountQrMatrix(
    val size: Int,
    private val modules: BooleanArray,
) {
    operator fun get(row: Int, column: Int): Boolean = modules[row * size + column]
}

private fun createAccountQrMatrix(value: String): AccountQrMatrix {
    val size = AccountQrSize
    val modules = BooleanArray(size * size)
    val reserved = BooleanArray(size * size)

    fun index(row: Int, column: Int) = row * size + column

    fun set(row: Int, column: Int, dark: Boolean, reserve: Boolean = true) {
        if (row !in 0 until size || column !in 0 until size) return
        modules[index(row, column)] = dark
        if (reserve) reserved[index(row, column)] = true
    }

    fun drawFinder(startRow: Int, startColumn: Int) {
        for (row in -1..7) {
            for (column in -1..7) {
                val targetRow = startRow + row
                val targetColumn = startColumn + column
                val dark = row in 0..6 && column in 0..6 &&
                    (row == 0 || row == 6 || column == 0 || column == 6 || (row in 2..4 && column in 2..4))
                set(targetRow, targetColumn, dark)
            }
        }
    }

    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    for (i in 8 until size - 8) {
        val dark = i % 2 == 0
        set(6, i, dark)
        set(i, 6, dark)
    }

    set(size - 8, 8, true)

    reserveFormatAreas(::index, reserved, size)

    val data = createAccountQrData(value.encodeToByteArray().take(AccountQrMaxInputBytes))
    val errorCorrection = createReedSolomonRemainder(data, AccountQrErrorCodewords)
    val codewords = data + errorCorrection
    val dataBits = codewords.flatMap { byte ->
        (7 downTo 0).map { bit -> ((byte.toInt() ushr bit) and 1) == 1 }
    }

    var bitIndex = 0
    var upward = true
    var column = size - 1
    while (column > 0) {
        if (column == 6) column--
        for (step in 0 until size) {
            val row = if (upward) size - 1 - step else step
            for (currentColumn in column downTo column - 1) {
                if (!reserved[index(row, currentColumn)]) {
                    val bit = if (bitIndex < dataBits.size) dataBits[bitIndex] else false
                    set(row, currentColumn, bit xor accountQrMask(row, currentColumn), reserve = false)
                    bitIndex++
                }
            }
        }
        upward = !upward
        column -= 2
    }

    drawFormatBits(::set, size)

    return AccountQrMatrix(size = size, modules = modules)
}

private fun reserveFormatAreas(
    index: (Int, Int) -> Int,
    reserved: BooleanArray,
    size: Int,
) {
    for (i in 0..8) {
        reserved[index(8, i)] = true
        reserved[index(i, 8)] = true
    }
    for (i in 0..7) {
        reserved[index(size - 1 - i, 8)] = true
    }
    for (i in size - 8 until size) {
        reserved[index(8, i)] = true
    }
}

private fun drawFormatBits(
    set: (row: Int, column: Int, dark: Boolean, reserve: Boolean) -> Unit,
    size: Int,
) {
    val formatBits = createAccountQrFormatBits()

    fun bit(index: Int): Boolean = ((formatBits ushr index) and 1) == 1

    for (i in 0..5) set(8, i, bit(i), true)
    set(8, 7, bit(6), true)
    set(8, 8, bit(7), true)
    set(7, 8, bit(8), true)
    for (i in 9..14) set(14 - i, 8, bit(i), true)

    for (i in 0..7) set(size - 1 - i, 8, bit(i), true)
    for (i in 8..14) set(8, size - 15 + i, bit(i), true)
}

private fun createAccountQrData(input: List<Byte>): List<Byte> {
    val bits = mutableListOf<Boolean>()

    fun append(value: Int, width: Int) {
        for (i in width - 1 downTo 0) bits += ((value ushr i) and 1) == 1
    }

    append(0b0100, 4)
    append(input.size, 8)
    input.forEach { byte -> append(byte.toInt() and 0xFF, 8) }

    repeat(minOf(4, AccountQrDataCodewords * 8 - bits.size)) { bits += false }
    while (bits.size % 8 != 0) bits += false

    val data = bits.chunked(8).map { byteBits ->
        byteBits.fold(0) { acc, bit -> (acc shl 1) or if (bit) 1 else 0 }.toByte()
    }.toMutableList()

    var pad = 0
    while (data.size < AccountQrDataCodewords) {
        data += if (pad % 2 == 0) 0xEC.toByte() else 0x11.toByte()
        pad++
    }

    return data
}

private fun createAccountQrFormatBits(): Int {
    val errorCorrectionLevelLow = 0b01
    val mask = 0
    val data = (errorCorrectionLevelLow shl 3) or mask
    var remainder = data
    repeat(10) { remainder = remainder shl 1 }

    for (i in 14 downTo 10) {
        if (((remainder ushr i) and 1) != 0) {
            remainder = remainder xor (0x537 shl (i - 10))
        }
    }

    return ((data shl 10) or remainder) xor 0x5412
}

private fun createReedSolomonRemainder(
    data: List<Byte>,
    degree: Int,
): List<Byte> {
    val divisor = createReedSolomonDivisor(degree)
    val result = IntArray(degree)

    data.forEach { byte ->
        val factor = (byte.toInt() and 0xFF) xor result.first()
        for (i in 0 until degree - 1) {
            result[i] = result[i + 1]
        }
        result[degree - 1] = 0

        for (i in 0 until degree) {
            result[i] = result[i] xor multiplyQrField(divisor[i], factor)
        }
    }

    return result.map { it.toByte() }
}

private fun createReedSolomonDivisor(degree: Int): IntArray {
    val result = IntArray(degree)
    result[degree - 1] = 1
    var root = 1

    repeat(degree) {
        for (i in 0 until degree) {
            result[i] = multiplyQrField(result[i], root)
            if (i + 1 < degree) result[i] = result[i] xor result[i + 1]
        }
        root = multiplyQrField(root, 0x02)
    }

    return result
}

private fun multiplyQrField(left: Int, right: Int): Int {
    var result = 0
    var a = left
    var b = right

    while (b != 0) {
        if ((b and 1) != 0) result = result xor a
        a = a shl 1
        if ((a and 0x100) != 0) a = a xor 0x11D
        b = b ushr 1
    }

    return result and 0xFF
}

private fun accountQrMask(row: Int, column: Int): Boolean = (row + column) % 2 == 0

private const val AccountQrSize = 29
private const val AccountQrDataCodewords = 55
private const val AccountQrErrorCodewords = 15
private const val AccountQrMaxInputBytes = 53

@Composable
private fun AccountShareAvatar(
    username: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier.size(72.dp),
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFC857),
                        Color(0xFFFF5C8A),
                        Color(0xFF6C63FF),
                    ),
                ),
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
