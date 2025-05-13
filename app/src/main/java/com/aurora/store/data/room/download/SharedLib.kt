package com.aurora.store.data.room.download

import android.os.Parcelable
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
import kotlinx.parcelize.Parcelize

@Parcelize
data class SharedLib(
    val packageName: String,
    val versionCode: Long,
    var fileList: List<PlayFile>
) : Parcelable {
    companion object {
        fun fromApp(app: App): SharedLib {
            return SharedLib(
                app.packageName,
                app.versionCode,
                app.fileList.filterNot { it.url.isBlank() }
            )
        }
    }
}
