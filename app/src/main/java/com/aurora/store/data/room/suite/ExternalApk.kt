/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.suite

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.model.AppFile
import com.aurora.store.data.room.AppFileParceler
import com.aurora.store.util.PackageUtil
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<AppFile, AppFileParceler>
@Entity(tableName = "externalApk")
data class ExternalApk(
    @PrimaryKey
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val displayName: String,
    val iconURL: String,
    val developerName: String,
    var fileList: List<AppFile>
) : Parcelable {

    fun isInstalled(context: Context): Boolean = PackageUtil.isInstalled(context, packageName)
}
