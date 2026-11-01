/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

// Layout as parsed by Play's client, documented by avast/apkverifier
object FrostingVerifier {

    enum class Result {
        VERIFIED,

        MISSING,

        UNKNOWN_KEY,

        INVALID
    }

    // From Play Store's finsky.peer_app_sharing_api.frosting_public_keys
    private val PUBLIC_KEYS = listOf(
        "3059301306072a8648ce3d020106082a8648ce3d03010703420004647dbed7e134edd9c4ac00f72f" +
            "a05b4e76a8854d1b6d7ae2365248ed5c4953e2e3752c0fc97479a68980440328c8f90c92c2f3fe08" +
            "ad50fb45b89a0f2f6ce213"
    )

    private const val FROSTING_BLOCK_ID = 0x2146444e
    private const val V2_BLOCK_ID = 0x7109871a
    private const val EOCD_SIGNATURE = 0x06054b50
    private const val EOCD_MIN_SIZE = 22
    private const val EOCD_CD_SIZE_OFFSET = 12
    private const val EOCD_CD_OFFSET_OFFSET = 16
    private const val MAX_COMMENT_SIZE = 0xFFFF
    private const val SIGNING_BLOCK_FOOTER_SIZE = 24
    private const val SIGNING_BLOCK_MAGIC = "APK Sig Block 42"
    private const val DIGEST_SIZE = 32
    private const val MAX_ENTRIES = 32767

    @OptIn(ExperimentalStdlibApi::class)
    private val publicKeys: List<PublicKey> by lazy {
        val factory = KeyFactory.getInstance("EC")
        PUBLIC_KEYS.map { key -> factory.generatePublic(X509EncodedKeySpec(key.hexToByteArray())) }
    }

    fun verify(file: File): Result = try {
        RandomAccessFile(file, "r").use { verify(it) }
    } catch (_: MalformedException) {
        Result.INVALID
    }

    private fun verify(apk: RandomAccessFile): Result {
        val eocdOffset = findEocd(apk) ?: return Result.MISSING
        val eocd = apk.read(eocdOffset, (apk.length() - eocdOffset).toInt())
        val cdSize = eocd.uint32(EOCD_CD_SIZE_OFFSET)
        val cdOffset = eocd.uint32(EOCD_CD_OFFSET_OFFSET)
        if (cdOffset + cdSize != eocdOffset) return Result.MISSING

        val block = signingBlock(apk, cdOffset) ?: return Result.MISSING
        val frosting = block.pairs[FROSTING_BLOCK_ID] ?: return Result.MISSING
        val entry = parseFrosting(frosting) ?: return Result.MISSING

        val key = publicKeys.getOrNull(entry.keyIndex) ?: return Result.UNKNOWN_KEY
        val signatureValid = Signature.getInstance("SHA256withECDSA").run {
            initVerify(key)
            update(entry.signedData)
            runCatching { verify(entry.signature) }.getOrDefault(false)
        }
        if (!signatureValid) return Result.INVALID

        val digest = apkDigest(apk, block.offset, block.pairs[V2_BLOCK_ID], cdOffset, cdSize, eocd)
        return if (digest.contentEquals(entry.fileSha256)) Result.VERIFIED else Result.INVALID
    }

    private class SigningBlock(val offset: Long, val pairs: Map<Int, ByteArray>)

    private class FrostingEntry(
        val keyIndex: Int,
        val fileSha256: ByteArray,
        val signedData: ByteArray,
        val signature: ByteArray
    )

    private class MalformedException : Exception()

    private fun findEocd(apk: RandomAccessFile): Long? {
        val length = apk.length()
        if (length < EOCD_MIN_SIZE) return null
        val scan = minOf(length, (EOCD_MIN_SIZE + MAX_COMMENT_SIZE).toLong()).toInt()
        val tail = apk.read(length - scan, scan)
        for (offset in scan - EOCD_MIN_SIZE downTo 0) {
            if (tail.int32(offset) != EOCD_SIGNATURE) continue
            val commentLength = tail.uint16(offset + EOCD_MIN_SIZE - 2)
            if (offset + EOCD_MIN_SIZE + commentLength == scan) return length - scan + offset
        }
        return null
    }

    private fun signingBlock(apk: RandomAccessFile, cdOffset: Long): SigningBlock? {
        if (cdOffset < SIGNING_BLOCK_FOOTER_SIZE) return null
        val footer = apk.read(cdOffset - SIGNING_BLOCK_FOOTER_SIZE, SIGNING_BLOCK_FOOTER_SIZE)
        if (String(footer, 8, 16, Charsets.US_ASCII) != SIGNING_BLOCK_MAGIC) return null

        val size = footer.int64(0)
        val offset = cdOffset - size - 8
        if (size < SIGNING_BLOCK_FOOTER_SIZE || offset < 0) return null
        if (apk.read(offset, 8).int64(0) != size) return null

        val pairs = mutableMapOf<Int, ByteArray>()
        val body = ByteBuffer.wrap(apk.read(offset + 8, (size - SIGNING_BLOCK_FOOTER_SIZE).toInt()))
            .order(ByteOrder.LITTLE_ENDIAN)
        while (body.remaining() >= 12) {
            val pairLength = body.long
            if (pairLength < 4 || pairLength > body.remaining()) throw MalformedException()
            val id = body.int
            pairs[id] = ByteArray((pairLength - 4).toInt()).also { body.get(it) }
        }
        return SigningBlock(offset, pairs)
    }

    private fun parseFrosting(value: ByteArray): FrostingEntry? {
        val reader = VarintReader(value)
        val protoAndMetaSize = reader.varint()
        val signedStart = reader.position
        reader.require(protoAndMetaSize > 0 && protoAndMetaSize <= value.size - signedStart)

        val protobufSize = reader.varint()
        reader.require(protobufSize > 0)
        reader.skip(protobufSize)

        val metadataSize = reader.varint()
        val metadataEnd = reader.position + metadataSize
        reader.require(metadataSize > 0 && metadataEnd <= signedStart + protoAndMetaSize)

        var index = 0
        while (reader.position < metadataEnd && index < MAX_ENTRIES) {
            val entrySize = reader.varint()
            val entryEnd = reader.position + entrySize
            reader.require(entrySize > 0 && entryEnd <= metadataEnd)

            val disabled = reader.varint()
            if (disabled != 0) {
                reader.position = entryEnd
                index++
                continue
            }
            val keyIndex = reader.varint()
            reader.require(entryEnd - reader.position == DIGEST_SIZE)
            val fileSha256 = value.copyOfRange(reader.position, entryEnd)

            return FrostingEntry(
                keyIndex = keyIndex,
                fileSha256 = fileSha256,
                signedData = value.copyOfRange(signedStart, entryEnd),
                signature = signature(value, signedStart + protoAndMetaSize, index)
            )
        }
        return null
    }

    private fun signature(value: ByteArray, start: Int, index: Int): ByteArray {
        val reader = VarintReader(value, start)
        val total = reader.varint()
        val end = reader.position + total
        reader.require(total > 0 && end <= value.size)
        repeat(index) { reader.skip(reader.varint()) }
        val size = reader.varint()
        reader.require(size > 0 && reader.position + size <= end)
        return value.copyOfRange(reader.position, reader.position + size)
    }

    private fun apkDigest(
        apk: RandomAccessFile,
        blockOffset: Long,
        v2Block: ByteArray?,
        cdOffset: Long,
        cdSize: Long,
        eocd: ByteArray
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        apk.digestRange(digest, 0, blockOffset)
        if (v2Block != null) {
            digest.update(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(V2_BLOCK_ID).array()
            )
            digest.update(v2Block)
        }
        apk.digestRange(digest, cdOffset, cdSize)
        val patchedEocd = eocd.copyOf()
        ByteBuffer.wrap(patchedEocd).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(EOCD_CD_OFFSET_OFFSET, blockOffset.toInt())
        digest.update(patchedEocd)
        return digest.digest()
    }

    private class VarintReader(private val bytes: ByteArray, var position: Int = 0) {
        fun varint(): Int {
            var result = 0
            for (shift in 0 until 35 step 7) {
                require(position < bytes.size)
                val byte = bytes[position++].toInt()
                result = result or ((byte and 0x7F) shl shift)
                if (byte and 0x80 == 0) {
                    require(result >= 0)
                    return result
                }
            }
            throw MalformedException()
        }

        fun skip(count: Int) {
            require(count >= 0 && position + count <= bytes.size)
            position += count
        }

        fun require(condition: Boolean) {
            if (!condition) throw MalformedException()
        }
    }

    private fun RandomAccessFile.read(offset: Long, length: Int): ByteArray {
        if (offset < 0 || length < 0 || offset + length > length()) throw MalformedException()
        return ByteArray(length).also {
            seek(offset)
            readFully(it)
        }
    }

    private fun RandomAccessFile.digestRange(digest: MessageDigest, offset: Long, length: Long) {
        seek(offset)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = length
        while (remaining > 0) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw MalformedException()
            digest.update(buffer, 0, read)
            remaining -= read
        }
    }

    private fun ByteArray.uint16(offset: Int): Int =
        ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getShort(offset).toInt() and 0xFFFF

    private fun ByteArray.int32(offset: Int): Int =
        ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getInt(offset)

    private fun ByteArray.uint32(offset: Int): Long = int32(offset).toLong() and 0xFFFFFFFFL

    private fun ByteArray.int64(offset: Int): Long =
        ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getLong(offset)
}
