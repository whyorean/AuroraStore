package com.aurora.store.data.model

import android.os.Parcelable
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.room.update.Update
import kotlinx.parcelize.Parcelize

@Parcelize
data class MinimalApp(
    val packageName: String,
    val versionCode: Int,
    val displayName: String
) : Parcelable {

    companion object {

        fun fromApp(app: App): MinimalApp {
            return MinimalApp(app.packageName, app.versionCode, app.displayName)
        }

        fun fromUpdate(update: Update): MinimalApp {
            return MinimalApp(update.packageName, update.versionCode, update.displayName)
        }
    }
}
