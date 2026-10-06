/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data

import com.aurora.store.data.room.account.Account
import com.aurora.store.data.room.account.AccountDao
import com.aurora.store.data.room.account.AppAccountBinding
import com.aurora.store.data.room.account.AppAccountBindingDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val bindingDao: AppAccountBindingDao
) {
    val accounts: Flow<List<Account>> = accountDao.accounts()
    val defaultAccount: Flow<Account?> = accountDao.observeDefault()

    suspend fun getAll(): List<Account> = accountDao.getAll()

    suspend fun getDefault(): Account? = accountDao.getDefault()

    suspend fun getById(id: String): Account? = accountDao.getById(id)

    /** Inserts/updates an account. Enforces the single-anonymous invariant. */
    suspend fun upsertAccount(account: Account) {
        if (account.isAnonymous) {
            accountDao.upsert(account.copy(id = Account.ANONYMOUS_ID))
        } else {
            accountDao.upsert(account)
        }
    }

    suspend fun setDefault(id: String) = accountDao.replaceDefault(id)

    /**
     * Removes an account and its bindings. If it was the default, promotes a new default
     * (anonymous preferred, else the oldest remaining). The default promotion is atomic.
     */
    suspend fun removeAccount(id: String) {
        bindingDao.deleteByAccountId(id)
        accountDao.deleteAndEnsureDefault(id)
    }

    /**
     * The account [packageName] was explicitly bound to, or null when it simply uses whichever
     * account is default — or no account at all, which gplayapi answers anonymously. This
     * replaced a `resolveAccountId` that fell back to the default account's id (and to
     * [Account.ANONYMOUS_ID] when there was none): a caller needs to know *whether* an app is
     * pinned to a particular account, and that fallback could not say.
     */
    suspend fun boundAccountId(packageName: String): String? =
        bindingDao.getForPackage(packageName)?.accountId

    /**
     * Records the account an app was installed with. Binding to the current default clears any
     * existing binding (default-account installs are never tagged).
     */
    suspend fun bindApp(packageName: String, accountId: String) {
        val defaultId = accountDao.getDefault()?.id
        if (accountId == defaultId) {
            bindingDao.delete(packageName)
        } else {
            bindingDao.upsert(AppAccountBinding(packageName, accountId))
        }
    }

    suspend fun unbindApp(packageName: String) = bindingDao.delete(packageName)
}
