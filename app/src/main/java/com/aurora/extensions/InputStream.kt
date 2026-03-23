package com.aurora.extensions

import com.aurora.store.data.model.DownloadInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.fixedRateTimer

fun InputStream.copyTo(out: OutputStream, streamSize: Long): Flow<DownloadInfo> {
    return flow {
        var bytesCopied: Long = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = read(buffer)

        var lastTotalBytesRead = 0L
        var speed: Long = 0
        @Suppress("KotlinConstantConditions") // False-positive for bytesCopied always being zero
        val timer = fixedRateTimer("timer", true, 0L, 1000) {
            val totalBytesRead = bytesCopied
            speed = totalBytesRead - lastTotalBytesRead
            lastTotalBytesRead = totalBytesRead
        }

        while (bytes >= 0) {
            out.write(buffer, 0, bytes)
            bytesCopied += bytes
            // Emit stream progress in percentage
            emit(DownloadInfo((bytesCopied * 100 / streamSize).toInt(), bytes.toLong(), speed))
            bytes = read(buffer)
        }
        timer.cancel()
    }.flowOn(Dispatchers.IO)
}
