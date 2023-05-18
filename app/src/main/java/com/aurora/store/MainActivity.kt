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
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
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
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.successUi


class MainActivity : BaseActivity() {

    private lateinit var B: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authData: AuthData

    private var lastBackPressed = 0L

    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) toast(R.string.permissions_denied)
        }

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
            destination: NavDestination?,
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
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        /* Check self update only for stable release, skip debug & nightlies*/
        if (BuildConfig.APPLICATION_ID == Constants.APP_ID) checkSelfUpdate()

        onBackPressedDispatcher.addCallback(this) {
            if (!B.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                if (Preferences.getBoolean(this@MainActivity, Preferences.PREFERENCE_QUICK_EXIT)) {
                    finish()
                } else {
                    if (lastBackPressed + 1000 > System.currentTimeMillis()) {
                        finish()
                    } else {
                        lastBackPressed = System.currentTimeMillis()
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.toast_double_press_to_exit),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                B.drawerLayout.close()
            }
        }
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
        bottomNavigationView.setupWithNavController(navController)

        val backGroundColor = getStyledAttributeColor(android.R.attr.colorBackground)
        bottomNavigationView.apply {
            setBackgroundColor(ColorUtils.setAlphaComponent(backGroundColor, 245))
            val alphaColor = ColorUtils.setAlphaComponent(this@MainActivity.accentColor(), 100)
            itemActiveIndicatorColor = ColorStateList.valueOf(alphaColor)
        }

        val defaultTab = Preferences.getInteger(this, Preferences.PREFERENCE_DEFAULT_SELECTED_TAB)
        val navigationList =
            listOf(R.id.navigation_apps, R.id.navigation_games, R.id.navigation_updates)
        bottomNavigationView.selectedItemId = navigationList[defaultTab]
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
