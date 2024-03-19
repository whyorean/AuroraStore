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

package com.aurora.store.viewmodel.editorschoice

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.editor.EditorChoiceBundle
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class EditorChoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = EditorChoiceViewModel::class.java.simpleName

    private val authData: AuthData = AuthProvider
        .with(context)
        .getAuthData()

    private val streamHelper: StreamHelper = StreamHelper(authData)
        .using(HttpClient.getPreferredClient(context))

    val liveData: MutableLiveData<List<EditorChoiceBundle>> = MutableLiveData()

    fun getEditorChoiceStream(category: StreamHelper.Category) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val editorChoiceBundle = streamHelper.getEditorChoiceStream(category)
                    liveData.postValue(editorChoiceBundle)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed fetching list of categories", exception)
                }
            }
        }
    }
}
