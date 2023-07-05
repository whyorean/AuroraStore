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

package com.aurora.store.view.ui.commons

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aurora.Constants
import com.aurora.extensions.applyTheme
import com.aurora.extensions.getEmptyActivityBundle
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.view.ui.account.GoogleActivity
import com.aurora.store.view.ui.details.*
import com.aurora.store.view.ui.sheets.NetworkDialogSheet
import com.aurora.store.view.ui.sheets.TOSSheet
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier


abstract class BaseActivity : AppCompatActivity(), NetworkProvider.NetworkListener {

    protected val gson: Gson = GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create()

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeId = Preferences.getInteger(this, PREFERENCE_THEME_TYPE)
        val accentId = Preferences.getInteger(this, PREFERENCE_THEME_ACCENT)
        applyTheme(themeId, accentId)
        super.onCreate(savedInstanceState)
    }

    fun openDetailsActivity(app: App) {
        val intent = Intent(this, AppDetailsFragment::class.java)
        intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app))
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    fun openDetailsMoreActivity(app: App) {
        val intent = Intent(this, DetailsMoreActivity::class.java)
        intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app))
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    fun openDetailsReviewActivity(app: App) {
        val intent = Intent(this, DetailsReviewActivity::class.java)
        intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app))
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    fun openStreamBrowseActivity(browseUrl: String, title: String = "") {
        val intent = if (browseUrl.lowercase().contains("expanded")) {
            Intent(this, ExpandedStreamBrowseFragment::class.java)
        } else if (browseUrl.lowercase().contains("developer")) {
            Intent(this, DevProfileActivity::class.java)
        } else {
            Intent(this, StreamBrowseActivity::class.java)
        }
        intent.putExtra(Constants.BROWSE_EXTRA, browseUrl)
        intent.putExtra(Constants.STRING_EXTRA, title)
        startActivity(intent, getEmptyActivityBundle())
    }

    fun openGoogleActivity() {
        val intent = Intent(this, GoogleActivity::class.java)
        startActivity(intent, getEmptyActivityBundle())
    }

    fun askToReadTOS() {
        runOnUiThread {
            if (!supportFragmentManager.isDestroyed) {
                val sheet = TOSSheet.newInstance()
                sheet.isCancelable = false
                sheet.show(supportFragmentManager, TOSSheet.TAG)
            }
        }
    }

    fun showNetworkConnectivitySheet() {
        runOnUiThread {
            if (!supportFragmentManager.isDestroyed) {
                supportFragmentManager.beginTransaction()
                    .add(NetworkDialogSheet.newInstance(), NetworkDialogSheet.TAG)
                    .commitAllowingStateLoss()
            }
        }
    }

    fun hideNetworkConnectivitySheet() {
        runOnUiThread {
            if (!supportFragmentManager.isDestroyed) {
                val fragment = supportFragmentManager.findFragmentByTag(NetworkDialogSheet.TAG)
                fragment?.let {
                    supportFragmentManager.beginTransaction().remove(fragment)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        NetworkProvider.addListener(this)
    }

    override fun onStop() {
        NetworkProvider.removeListener(this)
        super.onStop()
    }
}
