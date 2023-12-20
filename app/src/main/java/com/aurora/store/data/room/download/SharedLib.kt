package com.aurora.store.data.room.download

import android.os.Parcelable
import com.aurora.gplayapi.data.models.App
import kotlinx.parcelize.Parcelize

@Parcelize
data class SharedLib(
    val packageName: String,
    val versionCode: Int
) : Parcelable {
    companion object {
        fun fromApp(app: App): SharedLib {
            return SharedLib(app.packageName, app.versionCode)
        }
    }
}
