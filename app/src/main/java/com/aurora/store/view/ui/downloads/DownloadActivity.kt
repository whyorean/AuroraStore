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

package com.aurora.store.view.ui.downloads

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.model.DownloadFile
import com.aurora.store.databinding.ActivityDownloadBinding
import com.aurora.store.view.epoxy.views.DownloadViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.sheets.DownloadMenuSheet
import com.tonyodev.fetch2.*

class DownloadActivity : BaseActivity() {

    private lateinit var B: ActivityDownloadBinding
    private lateinit var fetch: Fetch

    private var fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onAdded(download: Download) {
            updateDownloadsList()
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            updateDownloadsList()
        }

        override fun onCompleted(download: Download) {
            updateDownloadsList()
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            updateDownloadsList()
        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {
            updateDownloadsList()
        }

        override fun onPaused(download: Download) {
            updateDownloadsList()
        }

        override fun onResumed(download: Download) {
            updateDownloadsList()
        }

        override fun onCancelled(download: Download) {
            updateDownloadsList()
        }

        override fun onRemoved(download: Download) {
            updateDownloadsList()
        }

        override fun onDeleted(download: Download) {
            updateDownloadsList()
        }
    }

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(B.root)

        attachToolbar()

        fetch = DownloadManager.with(this).fetch
        updateDownloadsList()

        B.swipeRefreshLayout.setOnRefreshListener {
            updateDownloadsList()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::fetch.isInitialized)
            fetch.addListener(fetchListener)
    }

    override fun onPause() {
        if (::fetch.isInitialized)
            fetch.removeListener(fetchListener)
        super.onPause()
    }

    override fun onDestroy() {
        if (::fetch.isInitialized)
            fetch.removeListener(fetchListener)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_download_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_pause_all -> {
                fetch.pauseAll()
                return true
            }
            R.id.action_resume_all -> {
                fetch.resumeAll()
                return true
            }
            R.id.action_cancel_all -> {
                fetch.cancelAll()
                return true
            }
            R.id.action_clear_completed -> {
                fetch.removeAllWithStatus(Status.COMPLETED)
                return true
            }
            R.id.action_force_clear_all -> {
                fetch.deleteAll()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutToolbarAction.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.setTitle(R.string.title_download_manager)
        }
    }

    private fun updateDownloadsList() {
        if (::fetch.isInitialized)
            fetch.getDownloads { downloads ->
                updateController(
                    downloads
                        .filter { it.id != BuildConfig.APPLICATION_ID.hashCode() }
                        .sortedWith { o1, o2 -> o2.created.compareTo(o1.created) }
                        .map { DownloadFile(it) }
                )
            }
    }

    private fun updateController(downloads: List<DownloadFile>) {
        B.recycler.withModels {
            if (downloads.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_downloads")
                        .message(getString(R.string.download_none))
                )
            } else {
                downloads.forEach {
                    add(
                        DownloadViewModel_()
                            .id(it.download.id, it.download.progress, it.download.status.value)
                            .download(it)
                            .click { _ -> openDetailsActivity(it) }
                            .longClick { _ ->
                                openDownloadMenuSheet(it)
                                false
                            }
                    )
                }
            }
        }
        B.swipeRefreshLayout.isRefreshing = false
    }

    private fun openDetailsActivity(downloadFile: DownloadFile) {
        val app: App = gson.fromJson(
            downloadFile.download.extras.getString(Constants.STRING_EXTRA, "{}"),
            App::class.java
        )
        openDetailsActivity(app)
    }

    private fun openDownloadMenuSheet(downloadFile: DownloadFile) {
        with(downloadFile) {
            val fragment = supportFragmentManager.findFragmentByTag(DownloadMenuSheet.TAG)
            if (fragment != null)
                supportFragmentManager.beginTransaction().remove(fragment)

            DownloadMenuSheet().apply {
                arguments = Bundle().apply {
                    putInt(DownloadMenuSheet.DOWNLOAD_ID, download.id)
                    putInt(DownloadMenuSheet.DOWNLOAD_STATUS, download.status.value)
                    putString(DownloadMenuSheet.DOWNLOAD_URL, download.url)
                }
            }.show(
                supportFragmentManager,
                DownloadMenuSheet.TAG
            )
        }
    }
}