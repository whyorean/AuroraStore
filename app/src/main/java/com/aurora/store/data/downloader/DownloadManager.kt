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

package com.aurora.store.data.downloader

import android.content.Context
import com.aurora.store.data.SingletonHolder
import com.aurora.store.util.Preferences
import com.tonyodev.fetch2.BuildConfig
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DefaultStorageResolver
import com.tonyodev.fetch2core.getFileTempDir

class DownloadManager private constructor(var context: Context) {

    companion object : SingletonHolder<DownloadManager, Context>(::DownloadManager)

    var fetch: Fetch

    init {
        fetch = Fetch.getInstance(getFetchConfiguration(context))
    }

    fun getFetchInstance(): Fetch {
        return fetch
    }

    private fun getFetchConfiguration(context: Context): FetchConfiguration {
        return FetchConfiguration.Builder(context)
            .enableLogging(BuildConfig.DEBUG)
            .enableHashCheck(true)
            .enableFileExistChecks(true)
            .enableRetryOnNetworkGain(true)
            .enableAutoStart(true)
            .setAutoRetryMaxAttempts(3)
            .setProgressReportingInterval(3000)
            .setNamespace(BuildConfig.APPLICATION_ID)
            .setStorageResolver(DefaultStorageResolver(context, getFileTempDir(context)))
            .build()
    }

    fun isDownloading(fetchGroup: FetchGroup): Boolean {
        return fetchGroup.downloadingDownloads.isNotEmpty()
                || fetchGroup.queuedDownloads.isNotEmpty()
                || fetchGroup.addedDownloads.isNotEmpty()
    }

    fun isCanceled(fetchGroup: FetchGroup): Boolean {
        return fetchGroup.cancelledDownloads.isNotEmpty()
                || fetchGroup.removedDownloads.isNotEmpty()
                || fetchGroup.deletedDownloads.isNotEmpty()
    }

    fun updateOngoingDownloads(
        fetch: Fetch, packageList: MutableList<String?>, download: Download,
        fetchListener: FetchListener?
    ) {
        if (packageList.contains(download.tag)) {
            val packageName = download.tag
            if (packageName != null) {
                val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(context, packageName.hashCode())
                groupIDsOfPackageName.forEach {
                    fetch.deleteGroup(it)
                }
                packageList.remove(packageName)
            }
        }
        if (packageList.size == 0) {
            fetch.removeListener(fetchListener!!)
        }
    }
}
