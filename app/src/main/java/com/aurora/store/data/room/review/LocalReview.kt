/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.review

import androidx.room.Entity
import com.aurora.gplayapi.data.models.Review

/**
 * A user-authored review cached locally so it can be shown immediately after submission,
 * even while Google takes time to publish it. It is kept in sync with the Play API: a review
 * is [synced] once the API has confirmed it exists, after which a subsequent disappearance from
 * the API is treated as a deletion on Play Store and the local copy is dropped.
 *
 * Scoped by [accountEmail] so a previous account's reviews never leak into a different session.
 */
@Entity(tableName = "review", primaryKeys = ["packageName", "accountEmail"])
data class LocalReview(
    val packageName: String,
    val accountEmail: String,
    val title: String,
    val comment: String,
    val rating: Int,
    val commentId: String = String(),
    val userName: String = String(),
    val userPhotoUrl: String = String(),
    val appVersion: String = String(),
    val timeStamp: Long = 0L,
    val synced: Boolean = false
) {

    companion object {
        fun fromReview(
            review: Review,
            packageName: String,
            accountEmail: String,
            synced: Boolean
        ): LocalReview = LocalReview(
            packageName = packageName,
            accountEmail = accountEmail,
            title = review.title,
            comment = review.comment,
            rating = review.rating,
            commentId = review.commentId,
            userName = review.userName,
            userPhotoUrl = review.userPhotoUrl,
            appVersion = review.appVersion,
            timeStamp = review.timeStamp,
            synced = synced
        )

        fun LocalReview.toReview(): Review = Review(
            title = title,
            comment = comment,
            commentId = commentId,
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            appVersion = appVersion,
            rating = rating,
            timeStamp = timeStamp
        )
    }
}
