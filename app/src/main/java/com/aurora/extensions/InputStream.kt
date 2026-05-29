package com.aurora.extensions

import com.aurora.store.data.model.DownloadInfo
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.fixedRateTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

fun InputStream.copyTo(out: OutputStream, streamSize: Long): Flow<DownloadInfo> = flow {
    var bytesCopied: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var lastTotalBytesRead: Long = 0
    var speed: Long = 0

    @Suppress("KotlinConstantConditions") // False-positive for bytesCopied always being zero
    val timer = fixedRateTimer("timer", true, 0L, 1000) {
        val totalBytesRead = bytesCopied
        speed = totalBytesRead - lastTotalBytesRead
        lastTotalBytesRead = totalBytesRead
    }

    // Cancel the timer even when the collector aborts mid-stream (e.g. the download is
    // stopped), otherwise the timer thread would leak.
    try {
        var bytes = read(buffer)
        while (bytes >= 0) {
            out.write(buffer, 0, bytes)
            out.flush()

            bytesCopied += bytes
            // Emit stream progress in percentage
            emit(DownloadInfo((bytesCopied * 100 / streamSize).toInt(), bytes.toLong(), speed))
            bytes = read(buffer)
        }
    } finally {
        timer.cancel()
    }
}.flowOn(Dispatchers.IO)
