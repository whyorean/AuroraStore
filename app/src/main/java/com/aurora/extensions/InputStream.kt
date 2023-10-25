package com.aurora.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.io.OutputStream

fun InputStream.copyTo(out: OutputStream, streamSize: Long): Flow<Int> {
    return flow {
        var bytesCopied: Long = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = read(buffer)
        while (bytes >= 0) {
            out.write(buffer, 0, bytes)
            bytesCopied += bytes
            bytes = read(buffer)
            // Emit stream progress in percentage
            emit((bytesCopied * 100 / streamSize).toInt())
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()
}
