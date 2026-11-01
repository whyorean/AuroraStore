/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.module

import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.android.AndroidLogger
import com.aurora.gplayapi.engine.OkHttpEngine
import com.aurora.gplayapi.net.HttpEngine
import com.aurora.gplayapi.session.DefaultPlaySessions
import com.aurora.gplayapi.session.PlaySessions
import com.aurora.gplayapi.web.WebPlayClient
import com.aurora.store.data.providers.AuroraAuthSessionSource
import com.aurora.store.data.providers.SpoofProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Wires the gplayapi v4 object graph: one [PlayClient] whose [PlaySessions] resolves per
 * call, replacing what v3 needed twelve captured-`AuthData` helpers for (spec defect #1).
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayClientModule {

    @Singleton
    @Provides
    fun providesPlayHttpEngine(okHttpClient: OkHttpClient): HttpEngine = OkHttpEngine(okHttpClient)

    /**
     * `device`/`locale` are suppliers, not fixed values: [SpoofProvider] can change under a
     * user at runtime, and `PlayClient` lives for the whole process, so each call re-reads
     * the active spoof rather than freezing whatever it was at graph construction.
     */
    @Singleton
    @Provides
    fun providesPlaySessions(
        engine: HttpEngine,
        spoofProvider: SpoofProvider,
        authSessionSource: AuroraAuthSessionSource
    ): PlaySessions = DefaultPlaySessions(
        engine = engine,
        device = { spoofProvider.deviceProfile() },
        locale = { spoofProvider.locale },
        additionalLocales = { spoofProvider.additionalLocales },
        authenticated = authSessionSource,
        logger = AndroidLogger
    )

    @Singleton
    @Provides
    fun providesPlayClient(engine: HttpEngine, sessions: PlaySessions): PlayClient =
        PlayClient(engine, sessions, AndroidLogger)

    /**
     * The account-less session provider. `authenticated = null` is gplayapi's documented way
     * to pin anonymous mode: it never reads an account, so this provider keeps minting and
     * caching its own [com.aurora.gplayapi.session.AnonymousSession] alongside whatever
     * account the unqualified provider above is serving.
     */
    @NoAuth
    @Singleton
    @Provides
    fun providesNoAuthPlaySessions(engine: HttpEngine, spoofProvider: SpoofProvider): PlaySessions =
        DefaultPlaySessions(
            engine = engine,
            device = { spoofProvider.deviceProfile() },
            locale = { spoofProvider.locale },
            additionalLocales = { spoofProvider.additionalLocales },
            authenticated = null,
            logger = AndroidLogger
        )

    @NoAuth
    @Singleton
    @Provides
    fun providesNoAuthPlayClient(engine: HttpEngine, @NoAuth sessions: PlaySessions): PlayClient =
        PlayClient(engine, sessions, AndroidLogger)

    /**
     * `locale` is a supplier now that [WebPlayClient] exposes one, for the same reason
     * `device`/`locale` above are: [SpoofProvider] can change under a user at runtime, and
     * this client is meant to live for the whole process, so each request re-reads the
     * active content-language setting rather than freezing whatever it was when the graph
     * was built.
     */
    @Singleton
    @Provides
    fun providesWebPlayClient(engine: HttpEngine, spoofProvider: SpoofProvider): WebPlayClient =
        WebPlayClient(engine, locale = { spoofProvider.locale })
}
