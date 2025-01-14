package com.aurora.store.view.epoxy.controller

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.airbnb.epoxy.paging3.PagingDataEpoxyController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * A base class that exposes loading status for [PagingDataEpoxyController]
 *
 * This controller requests a model rebuild when the loading status changes to reflect appropriate
 * data. Consider overriding [addModels] method to show loading placeholders when using this controller.
 */
abstract class BasePagingDataEpoxyController<T : Any>(lifecycleOwner: LifecycleOwner) :
    PagingDataEpoxyController<T>() {

    /**
     * Loading status for the initial refresh, defaults to true
     */
    protected val isLoading = loadStateFlow
        .map { loadState -> loadState.refresh is LoadState.Loading }
        .stateIn(lifecycleOwner.lifecycleScope, SharingStarted.WhileSubscribed(), true)

    init {
        isLoading.onEach {
            requestModelBuild()
        }.launchIn(lifecycleOwner.lifecycleScope)
    }
}
