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

package com.aurora.store

import android.Manifest
import android.content.Intent
import android.os.Bundle
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
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityMainBinding
import com.aurora.store.util.Log
import com.aurora.store.util.ViewUtil
import com.aurora.store.util.ViewUtil.getStyledAttribute
import com.aurora.store.util.extensions.isQAndAbove
import com.aurora.store.util.extensions.load
import com.aurora.store.util.extensions.open
import com.aurora.store.view.ui.about.AboutActivity
import com.aurora.store.view.ui.account.AccountActivity
import com.aurora.store.view.ui.all.AppsGamesActivity
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.commons.BlacklistActivity
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.aurora.store.view.ui.preferences.SettingsActivity
import com.aurora.store.view.ui.sale.AppSalesActivity
import com.aurora.store.view.ui.search.SearchSuggestionActivity
import com.aurora.store.view.ui.spoof.SpoofActivity
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions


class MainActivity : BaseActivity() {

    private lateinit var B: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authData: AuthData

    private var lastBackPressed = 0L

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

        checkPermission()
    }

    private fun checkPermission() = runWithPermissions(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) {
        Log.i("External Storage Access Available")

        if (isQAndAbove()) {
            //pickExternalFileDir()
        }
    }

    private fun attachToolbar() {
        B.viewToolbar.imgActionPrimary.setOnClickListener {
            B.drawerLayout.openDrawer(GravityCompat.START, true)
        }

        B.viewToolbar.imgActionSecondary.setOnClickListener {
            val userAppIntent = Intent(this, DownloadActivity::class.java)
            startActivity(userAppIntent, ViewUtil.getEmptyActivityBundle(this))
        }
    }

    private fun attachSearch() {
        B.searchFab.setOnClickListener {
            startActivity(
                Intent(this, SearchSuggestionActivity::class.java),
                ViewUtil.getEmptyActivityBundle(this)
            )
        }
    }

    private fun attachNavigation() {
        val bottomNavigationView: BottomNavigationView = B.navView
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        val backGroundColor = getStyledAttribute(this, android.R.attr.colorBackground)
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
        if (!navController.navigateUp()) {
            if (lastBackPressed + 1000 > System.currentTimeMillis()) {
                super.onBackPressed()
            } else {
                lastBackPressed = System.currentTimeMillis()
                Toast.makeText(this, "Click twice to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {

    }

    private fun pickExternalFileDir() {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT)
        getContentIntent.type = "*/*"
        getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        getContentIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(
            Intent.createChooser(
                getContentIntent,
                "Aurora Store - External Storage Access"
            ), 1337
        )
    }
}