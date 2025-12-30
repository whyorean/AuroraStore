package com.aurora.store.module

import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.gplayapi.helpers.ExpandedBrowseHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.web.WebAppDetailsHelper
import com.aurora.gplayapi.helpers.web.WebCategoryStreamHelper
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import com.aurora.gplayapi.helpers.web.WebTopChartsHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.SpoofProvider
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
    ): AppDetailsHelper = AppDetailsHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesStreamHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): StreamHelper = StreamHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesExpandedBrowseHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): ExpandedBrowseHelper = ExpandedBrowseHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesCategoryHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): CategoryHelper = CategoryHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesReviewsHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): ReviewsHelper = ReviewsHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesSearchHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): SearchHelper = SearchHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesPurchaseHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): PurchaseHelper = PurchaseHelper(authProvider.authData!!)
        .using(httpClient)

    @Singleton
    @Provides
    fun providesWebStreamHelperInstance(
        spoofProvider: SpoofProvider,
        httpClient: IHttpClient
    ): WebStreamHelper = WebStreamHelper()
        .using(httpClient)
        .with(spoofProvider.locale)

    @Singleton
    @Provides
    fun providesWebDataSafetyHelperInstance(
        spoofProvider: SpoofProvider,
        httpClient: IHttpClient
    ): WebDataSafetyHelper = WebDataSafetyHelper()
        .using(httpClient)
        .with(spoofProvider.locale)

    @Singleton
    @Provides
    fun providesWebSearchHelperInstance(
        spoofProvider: SpoofProvider,
        httpClient: IHttpClient
    ): WebSearchHelper = WebSearchHelper()
        .using(httpClient)
        .with(spoofProvider.locale)

    @Singleton
    @Provides
    fun providesWebCategoryStreamHelperInstance(
        spoofProvider: SpoofProvider,
        httpClient: IHttpClient
    ): WebCategoryStreamHelper = WebCategoryStreamHelper()
        .using(httpClient)
        .with(spoofProvider.locale)

    @Singleton
    @Provides
    fun providesWebTopChartsHelperInstance(
        spoofProvider: SpoofProvider,
        httpClient: IHttpClient
    ): WebTopChartsHelper = WebTopChartsHelper()
        .using(httpClient)
        .with(spoofProvider.locale)

    @Singleton
    @Provides
    fun providesWebAppDetailsHelperInstance(
        spoofProvider: SpoofProvider,
        httpClient: IHttpClient
    ): WebAppDetailsHelper = WebAppDetailsHelper()
        .using(httpClient)
        .with(spoofProvider.locale)
}
