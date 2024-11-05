package com.aurora.store.module

import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.gplayapi.helpers.ExpandedBrowseHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.web.WebCategoryStreamHelper
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import com.aurora.gplayapi.helpers.web.WebTopChartsHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module to instantiate singleton components for different helpers from gplayapi library
 */
@Module
@InstallIn(SingletonComponent::class)
object HelperModule {

    @Singleton
    @Provides
    fun providesAppDetailsHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): AppDetailsHelper {
        return AppDetailsHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesStreamHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): StreamHelper {
        return StreamHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesExpandedBrowseHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): ExpandedBrowseHelper {
        return ExpandedBrowseHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesCategoryHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): CategoryHelper {
        return CategoryHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesReviewsHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): ReviewsHelper {
        return ReviewsHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesSearchHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): SearchHelper {
        return SearchHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesPurchaseHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): PurchaseHelper {
        return PurchaseHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesWebStreamHelperInstance(httpClient: IHttpClient): WebStreamHelper {
        return WebStreamHelper().using(httpClient)
    }

    @Singleton
    @Provides
    fun providesWebDataSafetyHelperInstance(httpClient: IHttpClient): WebDataSafetyHelper {
        return WebDataSafetyHelper().using(httpClient)
    }

    @Singleton
    @Provides
    fun providesWebSearchHelperInstance(httpClient: IHttpClient): WebSearchHelper {
        return WebSearchHelper().using(httpClient)
    }

    @Singleton
    @Provides
    fun providesWebCategoryStreamHelperInstance(httpClient: IHttpClient): WebCategoryStreamHelper {
        return WebCategoryStreamHelper().using(httpClient)
    }

    @Singleton
    @Provides
    fun providesWebTopChartsHelperInstance(httpClient: IHttpClient): WebTopChartsHelper {
        return WebTopChartsHelper().using(httpClient)
    }
}
