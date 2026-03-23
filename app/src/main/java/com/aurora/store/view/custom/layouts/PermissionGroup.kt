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
package com.aurora.store.view.custom.layouts

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aurora.extensions.showDialog
import com.aurora.store.R
import com.aurora.store.data.model.PermissionGroupInfo
import java.util.Locale

class PermissionGroup @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var permissionGroupInfo: PermissionGroupInfo
    private lateinit var packageManager: PackageManager

    private val permissionMap: MutableMap<String, String> = HashMap()

    constructor(context: Context?, permissionGroupInfo: PermissionGroupInfo) : this(context) {
        if (context != null) {
            inflate(context, R.layout.layout_permission, this)

            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )

            this.packageManager = context.packageManager
            this.permissionGroupInfo = permissionGroupInfo

            val imageView = findViewById<ImageView>(R.id.img)
            imageView.setImageDrawable(getPermissionGroupIcon(permissionGroupInfo))
        }
    }

    fun addPermission(permissionInfo: PermissionInfo, currentPerms: List<String> = emptyList()) {
        val title = permissionInfo.loadLabel(packageManager)
        val description = permissionInfo.loadDescription(packageManager)

        permissionMap[getReadableLabel(title.toString(), permissionInfo.packageName)] =
            if (description.isNullOrEmpty())
                "No description"
            else
                description.toString()

        val permissionLabels: List<String> = ArrayList(permissionMap.keys)
        val permissionLabelsView = findViewById<LinearLayout>(R.id.permission_labels)
        permissionLabelsView.removeAllViews()

        permissionLabels
            .filter { it.isNotEmpty() }
            .sortedBy { it }
            .forEach {
                addPermissionLabel(
                    permissionLabelsView,
                    it,
                    permissionMap[it],
                    if (currentPerms.isNotEmpty()) permissionInfo.name !in currentPerms else false
                )
            }
    }

    private fun addPermissionLabel(
        permissionLabelsView: LinearLayout,
        label: String,
        description: String?,
        isNewPerm: Boolean = false
    ) {
        val textView = TextView(context)
        textView.text = label
        if (isNewPerm) textView.setTextColor(ContextCompat.getColor(context, R.color.colorGreen))
        textView.setOnClickListener {
            var title: String = permissionGroupInfo.label

            if (title.contains("UNDEFINED")) {
                title = "Android"
            }

            title = title.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.getDefault())
                } else {
                    it.toString()
                }
            }

            context.showDialog(title, description)
        }

        permissionLabelsView.addView(textView)
    }

    private fun getPermissionGroupIcon(permissionGroupInfo: PermissionGroupInfo): Drawable? {
        return ContextCompat.getDrawable(context, permissionGroupInfo.icon)
    }

    private fun getReadableLabel(label: String, packageName: String): String {
        val prefixes: MutableList<String> = mutableListOf(
            "android",
            packageName
        )

        if (label.contains("UNDEFINED")) {
            return "Android"
        }

        prefixes
            .map { "$it.permission." }
            .forEach {
                if (label.startsWith(it)) {
                    return it.replace(it, "")
                        .replace("_", " ")
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase(Locale.getDefault())
                            } else {
                                it.toString()
                            }
                        }
                }
            }

        return label.replaceFirstChar {
            it.titlecase(Locale.getDefault())
        }
    }
}
