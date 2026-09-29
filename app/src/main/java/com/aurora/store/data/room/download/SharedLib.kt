/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.download

import android.os.Parcelable
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.AppFile
import com.aurora.store.data.room.AppFileParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@TypeParceler<AppFile, AppFileParceler>
data class SharedLib(
    val packageName: String,
    val versionCode: Long,
    var fileList: List<AppFile>
) : Parcelable {
    companion object {
        fun fromApp(app: App): SharedLib = SharedLib(
            app.packageName,
            app.versionCode,
            app.fileList.filterNot { it.url.isBlank() }
        )
    }
}
