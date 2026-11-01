/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.preferences

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.isTAndAbove
import com.aurora.store.data.providers.SpoofProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val spoofProvider: SpoofProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val initialPrimary = spoofProvider.locale

    private val _languages = MutableStateFlow(spoofProvider.languages)
    val languages = _languages.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _primaryChanged = MutableStateFlow(false)
    val primaryChanged = _primaryChanged.asStateFlow()

    private val searchIndex: Map<Locale, String> by lazy {
        spoofProvider.availableLocales.associateWith { locale ->
            normalize(
                listOf(
                    locale.displayName,
                    locale.getDisplayName(locale),
                    locale.toLanguageTag(),
                    locale.language,
                    locale.country
                ).joinToString(" ")
            )
        }
    }

    val available = combine(_languages, _query) { selected, query ->
        val tokens = normalize(query).split(' ').filter { it.isNotBlank() }
        spoofProvider.availableLocales.filter { locale ->
            locale !in selected && tokens.all { it in searchIndex.getValue(locale) }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(query: String) {
        _query.value = query
    }

    fun add(locale: Locale) = update { it + locale }

    fun remove(locale: Locale) = update { list -> if (list.size > 1) list - locale else list }

    fun move(from: Int, to: Int) = update { list ->
        if (from !in list.indices || to !in list.indices) return@update list
        list.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun update(transform: (List<Locale>) -> List<Locale>) {
        val updated = transform(_languages.value)
        if (updated == _languages.value) return
        _languages.value = updated
        spoofProvider.setLanguages(updated)
        _primaryChanged.value = updated.first() != initialPrimary
    }

    // Recreates the activity, so it waits until the user leaves rather than running mid-drag
    fun applyAppLanguage() {
        if (!isTAndAbove) return
        val manager = context.getSystemService<LocaleManager>() ?: return
        val primary = _languages.value.first()
        if (manager.applicationLocales[0] != primary) {
            manager.applicationLocales = LocaleList(primary)
        }
    }

    override fun onCleared() {
        applyAppLanguage()
    }

    private fun normalize(text: String): String = Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .replace('ı', 'i')
        .replace(SEPARATORS, " ")
        .replace(WHITESPACE, " ")
        .trim()

    private companion object {
        val DIACRITICS = Regex("\\p{Mn}+")
        val SEPARATORS = Regex("[-_()]")
        val WHITESPACE = Regex("\\s+")
    }
}
