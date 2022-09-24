/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
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

package com.aurora.store

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.aurora.Constants
import com.aurora.extensions.*
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityMainBinding
import com.aurora.store.util.CertUtil.isFDroidApp
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.view.ui.about.AboutActivity
import com.aurora.store.view.ui.account.AccountActivity
import com.aurora.store.view.ui.all.AppsGamesActivity
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.commons.BlacklistActivity
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.aurora.store.view.ui.preferences.SettingsActivity
import com.aurora.store.view.ui.sale.AppSalesActivity
import com.aurora.store.view.ui.search.SearchSuggestionActivity
import com.aurora.store.view.ui.sheets.SelfUpdateSheet
import com.aurora.store.view.ui.spoof.SpoofActivity
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.successUi


class MainActivity : BaseActivity() {

    private lateinit var B: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authData: AuthData

    private var lastBackPressed = 0L

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {

    }

    companion object {
        @JvmStatic
        private fun matchDestination(
            @NonNull destination: NavDestination?,
            @IdRes destId: Int
        ): Boolean {
            var currentDestination = destination
            while (currentDestination?.id != destId && currentDestination?.parent != null) {
                currentDestination = currentDestination.parent
            }
            return currentDestination?.id == destId
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivityMainBinding.inflate(layoutInflater)

        setContentView(B.root)

        authData = AuthProvider.with(this).getAuthData()

        attachToolbar()
        attachNavigation()
        attachDrawer()
        attachSearch()

        if (!Preferences.getBoolean(this, Preferences.PREFERENCE_TOS_READ)) {
            askToReadTOS()
        }

        /*Check only if download to external storage is enabled*/
        if (Preferences.getBoolean(this, Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)) {
            if (isRAndAbove()) {
                checkExternalStorageManagerPermission()
            } else {
                checkExternalStorageAccessPermission()
            }
        }

        /* Check self update only for stable release, skip debug & nightlies*/
        if (BuildConfig.APPLICATION_ID == Constants.APP_ID)
            checkSelfUpdate()
    }

    private fun attachToolbar() {
        B.viewToolbar.imgActionPrimary.setOnClickListener {
            B.drawerLayout.openDrawer(GravityCompat.START, true)
        }

        B.viewToolbar.imgActionSecondary.setOnClickListener {
            open(DownloadActivity::class.java)
        }
    }

    private fun attachSearch() {
        B.searchFab.setOnClickListener {
            startActivity(
                Intent(this, SearchSuggestionActivity::class.java),
                getEmptyActivityBundle()
            )
        }
    }

    private fun attachNavigation() {
        val bottomNavigationView: BottomNavigationView = B.navView
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        val backGroundColor = getStyledAttributeColor(android.R.attr.colorBackground)
        bottomNavigationView.setBackgroundColor(ColorUtils.setAlphaComponent(backGroundColor, 245))

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if (item.itemId == bottomNavigationView.selectedItemId)
                return@setOnNavigationItemSelectedListener false
            NavigationUI.onNavDestinationSelected(item, navController)
            true
        }

        navController.addOnDestinationChangedListener { _: NavController?, destination: NavDestination?, _: Bundle? ->
            val menu: Menu = bottomNavigationView.menu
            val size: Int = menu.size()
            for (i in 0 until size) {
                val item: MenuItem = menu.getItem(i)
                if (matchDestination(destination, item.itemId)) {
                    item.isChecked = true
                }
            }
        }

        val defaultTab = Preferences.getInteger(this, Preferences.PREFERENCE_DEFAULT_SELECTED_TAB)

        when (defaultTab) {
            0 -> navController.navigate(R.id.navigation_apps)
            1 -> navController.navigate(R.id.navigation_games)
            2 -> navController.navigate(R.id.navigation_updates)
        }
    }

    private fun attachDrawer() {
        val headerView: View = B.navigation.getHeaderView(0)

        headerView.let {
            it.findViewById<ImageView>(R.id.img)?.load(R.mipmap.ic_launcher) {
                transform(RoundedCorners(8))
            }
            it.findViewById<TextView>(R.id.txt_name)?.text = getString(R.string.app_name)
            it.findViewById<TextView>(R.id.txt_email)?.text =
                ("v${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}")
        }

        B.navigation.setNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_apps_games -> {
                    open(AppsGamesActivity::class.java)
                }
                R.id.menu_apps_sale -> {
                    open(AppSalesActivity::class.java)
                }
                R.id.menu_blacklist_manager -> {
                    open(BlacklistActivity::class.java)
                }
                R.id.menu_download_manager -> {
                    open(DownloadActivity::class.java)
                }
                R.id.menu_spoof_manager -> {
                    open(SpoofActivity::class.java)
                }
                R.id.menu_account_manager -> {
                    open(AccountActivity::class.java)
                }
                R.id.menu_settings -> {
                    open(SettingsActivity::class.java)
                }
                R.id.menu_about -> {
                    open(AboutActivity::class.java)
                }
            }
            false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onBackPressed() {
        if (B.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            B.drawerLayout.close()
        } else if (!navController.navigateUp()) {
            if (Preferences.getBoolean(this, Preferences.PREFERENCE_QUICK_EXIT)) {
                super.onBackPressed()
            } else {
                if (lastBackPressed + 1000 > System.currentTimeMillis()) {
                    super.onBackPressed()
                } else {
                    lastBackPressed = System.currentTimeMillis()
                    Toast.makeText(this, getString(R.string.toast_double_press_to_exit), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkExternalStorageAccessPermission() = runWithPermissions(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) {
        Log.i("Required permissions available")
    }

    private fun checkExternalStorageManagerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager())
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun checkSelfUpdate() {
        task {
            HttpClient.getPreferredClient().get(Constants.UPDATE_URL, mapOf())
        } successUi { playResponse ->
            if (playResponse.isSuccessful) {
                val selfUpdate = gson.fromJson(
                    String(playResponse.responseBytes),
                    SelfUpdate::class.java
                )

                selfUpdate?.let { it ->
                    if (it.versionCode > BuildConfig.VERSION_CODE) {
                        if (isFDroidApp(this, BuildConfig.APPLICATION_ID)) {
                            if (it.fdroidBuild.isNotEmpty()) {
                                showUpdatesSheet(it)
                            }
                        } else if (it.auroraBuild.isNotEmpty()) {
                            showUpdatesSheet(it)
                        } else {
                            Log.i(getString(R.string.details_no_updates))
                        }
                    } else {
                        Log.i(getString(R.string.details_no_updates))
                    }
                }
            } else {
                Log.e("Failed to check self update")
            }
        }
    }

    private fun showUpdatesSheet(selfUpdate: SelfUpdate) {
        if (!supportFragmentManager.isDestroyed) {
            val sheet = SelfUpdateSheet.newInstance(selfUpdate)
            sheet.isCancelable = false
            sheet.show(supportFragmentManager, SelfUpdateSheet.TAG)
        }
    }
}
