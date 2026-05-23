package com.luum.michi.app.account.presentation.util

private const val AccountQrSize = 29
private const val AccountQrDataCodewords = 55
private const val AccountQrErrorCodewords = 15
private const val AccountQrMaxInputBytes = 53

internal data class AccountQrMatrix(
    val size: Int,
    private val modules: BooleanArray,
) {
    operator fun get(row: Int, column: Int): Boolean = modules[row * size + column]
}

internal fun String.toProfilePathSegment(): String =
    lowercase()
        .filter { char -> char.isLetterOrDigit() || char == '_' || char == '.' || char == '-' }
        .take(30)
        .ifBlank { "profile" }

internal fun createAccountQrMatrix(value: String): AccountQrMatrix {
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
