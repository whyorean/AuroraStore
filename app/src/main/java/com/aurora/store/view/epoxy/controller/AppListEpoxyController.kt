package com.aurora.store.view.epoxy.controller

import androidx.lifecycle.LifecycleOwner
import com.airbnb.epoxy.EpoxyModel
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_

/**
 * Epoxy controller to load an app list with pagination
 * @param onClick Listener for clicks
 */
class AppListEpoxyController(
    lifecycleOwner: LifecycleOwner,
    private val onClick: (App) -> Unit
) : BasePagingDataEpoxyController<App>(lifecycleOwner) {

    override fun buildItemModel(currentPosition: Int, item: App?): EpoxyModel<*> {
        return if (item != null) {
            AppListViewModel_()
                .id(item.id)
                .app(item)
                .click { _ -> onClick(item) }
        } else {
            AppListViewShimmerModel_()
                .id("shimmer_$currentPosition")
        }
    }

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (isLoading.value) {
            repeat(20) { add(AppListViewShimmerModel_().id("shimmer_$it")) }
        } else {
            if (models.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .icon(R.drawable.ic_round_search)
                        .message(R.string.details_no_app_match)
                )
            } else {
                super.addModels(models)
            }
        }
    }
}
