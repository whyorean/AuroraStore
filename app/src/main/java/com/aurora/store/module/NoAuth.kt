/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.module

import javax.inject.Qualifier

/**
 * Marks the account-less half of the object graph: a [com.aurora.gplayapi.PlayClient] whose
 * session provider is pinned to gplayapi's anonymous protocol and never consults Aurora's
 * accounts.
 *
 * Aurora keeps both lanes alive at once. The unqualified client carries the signed-in
 * account and answers for everything that is *about* that account - details, search, the
 * library, reviews, updates. This one carries no account at all and exists for delivery:
 * a shared dispensed account is rate-limited across everyone using it, and a download is
 * the request that hits that limit, so an unbound app's files are resolved account-lessly
 * and the account's quota is spent only where an account is actually required.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuth
