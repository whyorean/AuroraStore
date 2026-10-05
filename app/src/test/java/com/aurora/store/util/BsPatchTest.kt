/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BsPatchTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val old = "The quick brown fox".toByteArray()

    @Test
    fun rebuildsTheNewFileFromTheOldOneAndTheGzippedPatch() {
        val new = "The quick red fox jumps!".toByteArray()
        val patch = patch(
            newSize = new.size.toLong(),
            // "The quick " against the old file, then "red" verbatim, then skip "brown".
            Record(diff(new, 0, 0, 10), new.copyOfRange(10, 13), seek = 5),
            // " fox" against the old file's " fox", then " jumps" running past its end, where
            // bsdiff sums against zero.
            Record(diff(new, 13, 15, 10), new.copyOfRange(23, 24), seek = -100)
        )

        assertThat(apply(patch)).isEqualTo(new)
    }

    @Test
    fun rejectsARecordThatOverrunsTheDeclaredNewSize() {
        val patch = patch(newSize = 4, Record(ByteArray(3), ByteArray(3), seek = 0))

        assertThrows(BsPatch.MalformedPatchException::class.java) { apply(patch) }
    }

    private class Record(val diff: ByteArray, val extra: ByteArray, val seek: Long)

    private fun diff(new: ByteArray, newStart: Int, oldStart: Int, length: Int): ByteArray =
        ByteArray(length) { i ->
            val oldByte = old.getOrElse(oldStart + i) { 0 }
            (new[newStart + i] - oldByte).toByte()
        }

    private fun patch(newSize: Long, vararg records: Record): ByteArray {
        val raw = ByteArrayOutputStream()
        raw.write("ENDSLEY/BSDIFF43".toByteArray())
        raw.write(offset(newSize))
        for (record in records) {
            raw.write(offset(record.diff.size.toLong()))
            raw.write(offset(record.extra.size.toLong()))
            raw.write(offset(record.seek))
            raw.write(record.diff)
            raw.write(record.extra)
        }
        val gzipped = ByteArrayOutputStream()
        GZIPOutputStream(gzipped).use { it.write(raw.toByteArray()) }
        return gzipped.toByteArray()
    }

    private fun offset(value: Long): ByteArray {
        val magnitude = if (value < 0) -value else value
        val bytes = ByteArray(8) { i -> (magnitude ushr (8 * i)).toByte() }
        if (value < 0) bytes[7] = (bytes[7].toInt() or 0x80).toByte()
        return bytes
    }

    private fun apply(patch: ByteArray): ByteArray {
        val oldFile = folder.newFile().apply { writeBytes(old) }
        val patchFile = folder.newFile().apply { writeBytes(patch) }
        val output = File(folder.root, "new")
        BsPatch.applyFile(oldFile, patchFile, output)
        return output.readBytes()
    }
}
