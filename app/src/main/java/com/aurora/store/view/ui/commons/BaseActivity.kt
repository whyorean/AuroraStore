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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aurora.extensions.applyTheme
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
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
