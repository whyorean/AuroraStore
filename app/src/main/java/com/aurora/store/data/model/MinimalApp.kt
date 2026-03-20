package com.aurora.store.data.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.room.update.Update
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class MinimalApp(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val displayName: String,
    @IgnoredOnParcel
    val icon: Bitmap? = null
) : Parcelable {

    companion object {

        fun fromApp(app: App): MinimalApp = MinimalApp(
            app.packageName,
            app.versionName,
            app.versionCode,
            app.displayName
        )

        fun fromUpdate(update: Update): MinimalApp = MinimalApp(
            update.packageName,
            update.versionName,
            update.versionCode,
            update.displayName
        )
    }
}
