/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FrostingVerifierTest {

    @get:Rule
    val folder = TemporaryFolder()

    // Play APKs aren't redistributable: pass one with -Pfrosted.apk=/path/to/base.apk
    private val frostedApk: File? =
        System.getProperty("frosted.apk")?.let(::File)?.takeIf { it.isFile }

    @Test
    fun aZipWithoutSigningBlockHasNoFrosting() {
        val apk = folder.newFile("plain.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write(ByteArray(64))
            zip.closeEntry()
        }

        assertThat(FrostingVerifier.verify(apk)).isEqualTo(FrostingVerifier.Result.MISSING)
    }

    @Test
    fun aPlayServedBaseApkVerifies() {
        assumeTrue(frostedApk != null)

        assertThat(FrostingVerifier.verify(frostedApk!!))
            .isEqualTo(FrostingVerifier.Result.VERIFIED)
    }

    @Test
    fun aBytePatchedAfterPlayIsInvalid() {
        assumeTrue(frostedApk != null)
        val apk = folder.newFile("tampered.apk")
        frostedApk!!.copyTo(apk, overwrite = true)
        RandomAccessFile(apk, "rw").use { file ->
            file.seek(1024)
            val byte = file.read()
            file.seek(1024)
            file.write(byte xor 0xFF)
        }

        assertThat(FrostingVerifier.verify(apk)).isEqualTo(FrostingVerifier.Result.INVALID)
    }
}
