package com.aurora.store.data.model

import androidx.annotation.DrawableRes
import com.aurora.store.R

data class PermissionGroupInfo(
    val name: String = "unknown",
    @DrawableRes var icon: Int = R.drawable.ic_permission_unknown,
    val label: String = "UNDEFINED"
)
