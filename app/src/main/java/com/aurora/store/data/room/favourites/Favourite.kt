package com.aurora.store.data.room.favourites

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "favourite")
data class Favourite(
    @PrimaryKey
    val packageName: String,
    val displayName: String,
    val iconURL: String,
    val added: Long,
    val mode: Mode
) : Parcelable {

    enum class Mode {
        MANUAL,
        IMPORT
    }
}
