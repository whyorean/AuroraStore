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

package com.aurora.store.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.model.Report
import com.aurora.store.view.ui.details.DetailsExodusActivity
import com.aurora.store.view.ui.details.DevAppsActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

object NavigationUtil {
    val gson: Gson = GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create()

    fun openDevAppsActivity(context: Context, app: App) {
        val intent = Intent(
            context,
            DevAppsActivity::class.java
        ).apply {
            putExtra(Constants.STRING_APP, gson.toJson(app))
        }
        val options = ActivityOptions.makeSceneTransitionAnimation(context as AppCompatActivity)
        context.startActivity(intent, options.toBundle())
    }

    fun openExodusActivity(context: Context, app: App, report: Report) {
        val intent = Intent(
            context,
            DetailsExodusActivity::class.java
        ).apply {
            putExtra(Constants.STRING_APP, gson.toJson(app))
            putExtra(Constants.STRING_EXTRA, gson.toJson(report))
        }
        val options = ActivityOptions.makeSceneTransitionAnimation(context as AppCompatActivity)
        context.startActivity(intent, options.toBundle())
    }
}
