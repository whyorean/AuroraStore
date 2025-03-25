/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A generic paging source that is supposed to be able to load any type of data
 *
 * Consider calling [createPager] method to create an instance of [Pager] instead of interacting
 * with the class directly.
 * @param block Data to load into the pager
 */
class GenericPagingSource<T : Any>(
    private val block: suspend (Int) -> List<T>
) : PagingSource<Int, T>() {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20

        /**
         * Method to dynamically create and manage pager objects
         * @param pageSize Size of the page
         * @param enablePlaceholders Whether placeholders should be shown
         * @param data Data to load into the pager
         */
        fun <T : Any> createPager(
            pageSize: Int = DEFAULT_PAGE_SIZE,
            enablePlaceholders: Boolean = true,
            data: suspend (Int) -> List<T>
        ): Pager<Int, T> = Pager(
            config = PagingConfig(enablePlaceholders = enablePlaceholders, pageSize = pageSize),
            pagingSourceFactory = { GenericPagingSource(data) }
        )
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        return try {
            withContext(Dispatchers.IO) {
                val data = block(page)
                val totalPages = if (data.isNotEmpty()) page + 1 else page
                LoadResult.Page(
                    data = data,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (page == totalPages) null else totalPages,
                    itemsAfter = if (page == totalPages) 0 else DEFAULT_PAGE_SIZE
                )
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
