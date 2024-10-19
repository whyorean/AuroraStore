/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.model

import android.content.Context
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.gplayapi.data.models.File
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.util.CertUtil
import com.google.gson.annotations.SerializedName

data class SelfUpdate(
    @SerializedName("version_name") var versionName: String = String(),
    @SerializedName("version_code") var versionCode: Int = 0,
    @SerializedName("aurora_build") var auroraBuild: String = String(),
    @SerializedName("fdroid_build") var fdroidBuild: String = String(),
    @SerializedName("updated_on") var updatedOn: String = String(),
    val changelog: String = String(),
    val size: Long = 0L,
    val timestamp: Long = 0L
) {
    companion object {
        private const val BASE_URL = "https://gitlab.com/AuroraOSS/AuroraStore/-/raw/master"

        fun toApp(selfUpdate: SelfUpdate, context: Context): App {
            // Keep paths updated with fastlane data on project
            val icon = "fastlane/metadata/android/en-US/images/icon.png"

            val downloadURL = if (CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID)) {
                selfUpdate.fdroidBuild
            } else {
                selfUpdate.auroraBuild
            }

            return App(
                packageName = context.packageName,
                versionCode = selfUpdate.versionCode,
                versionName = selfUpdate.versionName,
                changes = selfUpdate.changelog,
                size = selfUpdate.size,
                updatedOn = selfUpdate.updatedOn,
                displayName = context.getString(R.string.app_name),
                developerName = "Rahul Kumar Patel",
                iconArtwork = Artwork(url = "$BASE_URL/$icon"),
                fileList = mutableListOf(
                    File(
                        name = "${context.packageName}.apk",
                        url = downloadURL,
                        size = selfUpdate.size
                    )
                ),
                isFree = true,
                isInstalled = true
            )
        }
    }
}
