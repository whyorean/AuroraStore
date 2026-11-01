/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream

/**
 * Applies the binary patches Play serves as `GZIPPED_BSDIFF`: a gzip stream holding the
 * `ENDSLEY/BSDIFF43` layout, which is the 16-byte magic, the new file's size, then
 * control/diff/extra records with no further compression. Classic `bspatch` reads only the
 * bzip2-compressed `BSDIFF40` layout and rejects these.
 *
 * Every record is `add`, `copy`, `seek` (8-byte sign-magnitude little-endian each), then `add`
 * diff bytes, each summed with the old file's byte at the same position, then `copy` bytes taken
 * verbatim, after which the old position moves by `seek`.
 *
 * Streams throughout: memory use is a few fixed buffers whatever the file sizes.
 */
object BsPatch {

    private const val MAGIC = "ENDSLEY/BSDIFF43"
    private const val BUFFER_SIZE = 64 * 1024

    /** The patch is not a well-formed `ENDSLEY/BSDIFF43` patch, or does not fit its own header. */
    class MalformedPatchException(message: String) : IOException(message)

    /**
     * Rebuilds the new file into [output] from the [old] file and the [patch] file, which is
     * either gzip-wrapped as Play serves it or already unwrapped. Both arrive: an HTTP client
     * may strip a `Content-Encoding: gzip` layer from a fresh download but cannot from a resumed
     * byte range, so the first bytes decide.
     *
     * [checkStopped] runs at least once per 64 KB written and may throw to abandon the work.
     *
     * @param expectedSize when positive, the size the new file must have; a patch whose header
     *   declares another size is rejected before anything is written.
     * @throws MalformedPatchException the patch is malformed.
     * @throws IOException a file cannot be read or written, or the patch is truncated.
     */
    fun applyFile(
        old: File,
        patch: File,
        output: File,
        expectedSize: Long = 0L,
        checkStopped: () -> Unit = {}
    ) {
        RandomAccessFile(old, "r").use { oldFile ->
            BufferedInputStream(patch.inputStream(), BUFFER_SIZE).use { raw ->
                raw.mark(2)
                val first = raw.read()
                val second = raw.read()
                raw.reset()
                val gzipped = first == 0x1f && second == 0x8b
                val patchStream = if (gzipped) GZIPInputStream(raw, BUFFER_SIZE) else raw
                patchStream.use {
                    BufferedOutputStream(FileOutputStream(output), BUFFER_SIZE).use { out ->
                        apply(oldFile, patchStream, out, expectedSize, checkStopped)
                    }
                }
            }
        }
    }

    /**
     * Rebuilds the new file into [output] from [old] and the uncompressed [patch] stream.
     *
     * @param expectedSize when positive, the size the header must declare for the new file.
     * @throws MalformedPatchException the patch is malformed.
     * @throws IOException a file cannot be read or written, or the patch is truncated.
     */
    fun apply(
        old: RandomAccessFile,
        patch: InputStream,
        output: OutputStream,
        expectedSize: Long = 0L,
        checkStopped: () -> Unit = {}
    ) {
        val header = ByteArray(MAGIC.length)
        readFully(patch, header, header.size)
        if (String(header, Charsets.US_ASCII) != MAGIC) {
            throw MalformedPatchException("Not an ENDSLEY/BSDIFF43 patch")
        }

        val word = ByteArray(8)
        val newSize = readOffset(patch, word)
        if (newSize < 0) throw MalformedPatchException("Negative new size $newSize")
        if (expectedSize > 0 && newSize != expectedSize) {
            throw MalformedPatchException("Patch builds $newSize bytes, expected $expectedSize")
        }

        val oldSize = old.length()
        val data = ByteArray(BUFFER_SIZE)
        val oldData = ByteArray(BUFFER_SIZE)
        var newPos = 0L
        var oldPos = 0L

        while (newPos < newSize) {
            checkStopped()
            val add = readOffset(patch, word)
            val copy = readOffset(patch, word)
            val seek = readOffset(patch, word)
            if (add < 0 || copy < 0) {
                throw MalformedPatchException("Negative record length ($add, $copy)")
            }
            if (add > newSize - newPos || copy > newSize - newPos - add) {
                throw MalformedPatchException("Record overruns the new size $newSize")
            }

            var remaining = add
            while (remaining > 0) {
                checkStopped()
                val count = minOf(remaining, BUFFER_SIZE.toLong()).toInt()
                readFully(patch, data, count)
                readOld(old, oldSize, oldPos, oldData, count)
                for (i in 0 until count) {
                    data[i] = (data[i] + oldData[i]).toByte()
                }
                output.write(data, 0, count)
                oldPos = addExact(oldPos, count.toLong())
                remaining -= count
            }
            newPos += add

            remaining = copy
            while (remaining > 0) {
                checkStopped()
                val count = minOf(remaining, BUFFER_SIZE.toLong()).toInt()
                readFully(patch, data, count)
                output.write(data, 0, count)
                remaining -= count
            }
            newPos += copy

            oldPos = addExact(oldPos, seek)
        }
    }

    /**
     * Fills [into] with the old file's [count] bytes from [position]. bsdiff defines a byte
     * outside the old file as contributing nothing to the sum, so those positions read as zero
     * rather than failing: a valid patch's diff run may start before or run past the old file.
     */
    private fun readOld(
        old: RandomAccessFile,
        oldSize: Long,
        position: Long,
        into: ByteArray,
        count: Int
    ) {
        into.fill(0, 0, count)
        val start = maxOf(position, 0L)
        val end = minOf(addExact(position, count.toLong()), oldSize)
        if (start >= end) return
        old.seek(start)
        old.readFully(into, (start - position).toInt(), (end - start).toInt())
    }

    private fun readOffset(input: InputStream, word: ByteArray): Long {
        readFully(input, word, 8)
        var magnitude = 0L
        for (i in 6 downTo 0) {
            magnitude = (magnitude shl 8) or (word[i].toLong() and 0xFF)
        }
        magnitude = magnitude or ((word[7].toLong() and 0x7F) shl 56)
        return if (word[7].toInt() and 0x80 != 0) -magnitude else magnitude
    }

    private fun readFully(input: InputStream, into: ByteArray, count: Int) {
        var read = 0
        while (read < count) {
            val n = input.read(into, read, count - read)
            if (n < 0) throw EOFException("Patch truncated")
            read += n
        }
    }

    // Math.addExact is API 24.
    private fun addExact(a: Long, b: Long): Long {
        val sum = a + b
        if ((a xor sum) and (b xor sum) < 0) {
            throw MalformedPatchException("Old position overflows")
        }
        return sum
    }
}
