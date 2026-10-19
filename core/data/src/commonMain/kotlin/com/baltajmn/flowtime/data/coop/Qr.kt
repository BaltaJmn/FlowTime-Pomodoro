package com.baltajmn.flowtime.data.coop

import kotlin.math.abs

/**
 * A QR code of versions 1 to 6, byte mode, error correction M (copiado de Chroma, docs/tecnico.md 6.10 de su repo), after the
 * structure of Nayuki's reference encoder. An invite link fits in version 3; nothing longer than
 * version 6 is ever needed, so the version and interleaving tables stop there and no library comes in.
 */
class QrCode internal constructor(val size: Int, private val modules: Array<BooleanArray>) {
    /** True for a dark module. [x] is the column, [y] the row. */
    operator fun get(x: Int, y: Int): Boolean = modules[y][x]
}

// Per version, at level M: data codewords in total, error correction codewords per block, blocks,
// and the position of the one alignment pattern that does not overlap a finder.
private val DATA_CODEWORDS = intArrayOf(0, 16, 28, 44, 64, 86, 108)
private val EC_PER_BLOCK = intArrayOf(0, 10, 16, 26, 18, 24, 16)
private val BLOCKS = intArrayOf(0, 1, 1, 1, 2, 2, 4)
private val ALIGNMENT = intArrayOf(0, 0, 18, 22, 26, 30, 34)

// ISO/IEC 18004 penalty weights.
private const val N1 = 3
private const val N2 = 3
private const val N3 = 40
private const val N4 = 10

/** Null when [text] does not fit in version 6, which no invite link comes near. */
fun qrEncode(text: String): QrCode? {
    val bytes = text.encodeToByteArray()
    val version = (1..6).firstOrNull { 4 + 8 + bytes.size * 8 <= DATA_CODEWORDS[it] * 8 } ?: return null
    val codewords = interleave(version, dataCodewords(version, bytes))
    return Matrix(version).build(codewords)
}

private fun dataCodewords(version: Int, bytes: ByteArray): IntArray {
    val capacity = DATA_CODEWORDS[version] * 8
    val bits = ArrayList<Boolean>(capacity)
    fun append(value: Int, length: Int) {
        for (i in length - 1 downTo 0) bits += (value ushr i) and 1 == 1
    }
    append(0b0100, 4)
    append(bytes.size, 8)
    bytes.forEach { append(it.toInt() and 0xFF, 8) }
    append(0, minOf(4, capacity - bits.size))
    append(0, (8 - bits.size % 8) % 8)
    var pad = 0xEC
    while (bits.size < capacity) {
        append(pad, 8)
        pad = pad xor 0xEC xor 0x11
    }
    return IntArray(bits.size / 8) { i -> (0 until 8).fold(0) { acc, j -> (acc shl 1) or if (bits[i * 8 + j]) 1 else 0 } }
}

private fun interleave(version: Int, data: IntArray): IntArray {
    val blocks = BLOCKS[version]
    val length = data.size / blocks
    val divisor = reedSolomonDivisor(EC_PER_BLOCK[version])
    val parts = (0 until blocks).map { data.copyOfRange(it * length, (it + 1) * length) }
    val ecc = parts.map { reedSolomonRemainder(it, divisor) }
    val out = ArrayList<Int>()
    for (i in 0 until length) parts.forEach { out += it[i] }
    for (i in divisor.indices) ecc.forEach { out += it[i] }
    return out.toIntArray()
}

private fun gfMultiply(x: Int, y: Int): Int {
    var z = 0
    for (i in 7 downTo 0) {
        z = (z shl 1) xor ((z ushr 7) * 0x11D)
        z = z xor (((y ushr i) and 1) * x)
    }
    return z
}

private fun reedSolomonDivisor(degree: Int): IntArray {
    val result = IntArray(degree)
    result[degree - 1] = 1
    var root = 1
    repeat(degree) {
        for (j in 0 until degree) {
            result[j] = gfMultiply(result[j], root)
            if (j + 1 < degree) result[j] = result[j] xor result[j + 1]
        }
        root = gfMultiply(root, 0x02)
    }
    return result
}

private fun reedSolomonRemainder(data: IntArray, divisor: IntArray): IntArray {
    val result = IntArray(divisor.size)
    for (b in data) {
        val factor = b xor result[0]
        result.copyInto(result, 0, 1, result.size)
        result[result.size - 1] = 0
        for (i in result.indices) result[i] = result[i] xor gfMultiply(divisor[i], factor)
    }
    return result
}

private class Matrix(val version: Int) {
    val size = 17 + 4 * version
    val modules = Array(size) { BooleanArray(size) }
    val function = Array(size) { BooleanArray(size) }

    fun build(codewords: IntArray): QrCode {
        drawFunctionPatterns()
        drawCodewords(codewords)
        // Every mask is tried and the one with the lowest penalty kept, as the standard asks.
        val best = (0 until 8).minBy { mask ->
            applyMask(mask)
            drawFormatBits(mask)
            penalty().also { applyMask(mask) }
        }
        applyMask(best)
        drawFormatBits(best)
        return QrCode(size, modules)
    }

    private fun set(x: Int, y: Int, dark: Boolean) {
        modules[y][x] = dark
        function[y][x] = true
    }

    private fun drawFunctionPatterns() {
        for (i in 0 until size) {
            set(6, i, i % 2 == 0)
            set(i, 6, i % 2 == 0)
        }
        drawFinder(3, 3)
        drawFinder(size - 4, 3)
        drawFinder(3, size - 4)
        if (version >= 2) drawAlignment(ALIGNMENT[version], ALIGNMENT[version])
        drawFormatBits(0)
    }

    private fun drawFinder(x: Int, y: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val distance = maxOf(abs(dx), abs(dy))
                if (x + dx in 0 until size && y + dy in 0 until size) set(x + dx, y + dy, distance != 2 && distance != 4)
            }
        }
    }

    private fun drawAlignment(x: Int, y: Int) {
        for (dy in -2..2) for (dx in -2..2) set(x + dx, y + dy, maxOf(abs(dx), abs(dy)) != 1)
    }

    /** Level M is 00 in the format bits, so only the mask shows. */
    private fun drawFormatBits(mask: Int) {
        val data = mask
        var rem = data
        repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
        val bits = ((data shl 10) or rem) xor 0x5412
        fun bit(i: Int) = (bits ushr i) and 1 == 1

        for (i in 0..5) set(8, i, bit(i))
        set(8, 7, bit(6))
        set(8, 8, bit(7))
        set(7, 8, bit(8))
        for (i in 9 until 15) set(14 - i, 8, bit(i))

        for (i in 0 until 8) set(size - 1 - i, 8, bit(i))
        for (i in 8 until 15) set(8, size - 15 + i, bit(i))
        set(8, size - 8, true)
    }

    private fun drawCodewords(data: IntArray) {
        var i = 0
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vertical in 0 until size) {
                for (j in 0 until 2) {
                    val x = right - j
                    val upward = (right + 1) and 2 == 0
                    val y = if (upward) size - 1 - vertical else vertical
                    if (!function[y][x] && i < data.size * 8) {
                        modules[y][x] = (data[i ushr 3] ushr (7 - (i and 7))) and 1 == 1
                        i++
                    }
                }
            }
            right -= 2
        }
    }

    private fun applyMask(mask: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                val invert = when (mask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0
                    5 -> x * y % 2 + x * y % 3 == 0
                    6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                }
                if (!function[y][x] && invert) modules[y][x] = !modules[y][x]
            }
        }
    }

    private fun penalty(): Int {
        var result = 0
        for (horizontal in listOf(true, false)) {
            for (a in 0 until size) {
                var runColor = false
                var run = 0
                val history = IntArray(7)
                for (b in 0 until size) {
                    val dark = if (horizontal) modules[a][b] else modules[b][a]
                    if (dark == runColor) {
                        run++
                        if (run == 5) {
                            result += N1
                        } else if (run > 5) {
                            result++
                        }
                    } else {
                        addHistory(run, history)
                        if (!runColor) result += finderLike(history) * N3
                        runColor = dark
                        run = 1
                    }
                }
                if (runColor) {
                    addHistory(run, history)
                    run = 0
                }
                addHistory(run + size, history)
                result += finderLike(history) * N3
            }
        }
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val c = modules[y][x]
                if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) result += N2
            }
        }
        val dark = modules.sumOf { row -> row.count { it } }
        val total = size * size
        val k = (abs(dark * 20 - total * 10) + total - 1) / total - 1
        return result + k * N4
    }

    private fun addHistory(length: Int, history: IntArray) {
        val run = if (history[0] == 0) length + size else length
        history.copyInto(history, 1, 0, history.size - 1)
        history[0] = run
    }

    private fun finderLike(h: IntArray): Int {
        val n = h[1]
        val core = n > 0 && h[2] == n && h[3] == n * 3 && h[4] == n && h[5] == n
        return (if (core && h[0] >= n * 4 && h[6] >= n) 1 else 0) + (if (core && h[6] >= n * 4 && h[0] >= n) 1 else 0)
    }
}
