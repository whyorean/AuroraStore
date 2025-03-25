package com.aurora.store.data.model

import androidx.annotation.StringRes
import com.aurora.store.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlexusReport(
    @SerialName("data")
    val report: Data?
)

@Serializable
data class Data(
    val name: String,
    val scores: Scores,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class Scores(
    @SerialName("micro_g")
    val microG: Rating = Rating(),
    @SerialName("native")
    val aosp: Rating = Rating()
)

@Serializable
data class Rating(
    val denominator: Float = -1F,
    val numerator: Float = -1F,
    val rating_type: String = String(),
    val total_count: Long = -1
) {
    private val fraction
        get() = if (numerator == -1F && denominator == -1F) -1F else numerator / denominator

    @get:StringRes
    val status: Int
        get() = when {
            fraction == -1F -> R.string.plexus_progress
            fraction == 0F -> R.string.details_compatibility_status_unknown
            fraction >= 0.90 -> R.string.details_compatibility_status_compatible
            fraction >= 0.50 -> R.string.details_compatibility_status_limited
            else -> R.string.details_compatibility_status_unsupported
        }
}
