package com.aurora.store.data.providers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExodusModuleTest {

    @get:Rule
    var hiltAndroidRule = HiltAndroidRule(this)

    @Inject
    lateinit var exodusTrackers: JSONObject

    @Before
    fun setup() {
        hiltAndroidRule.inject()
    }

    @Test
    fun testTrackersJsonIsNotEmpty() {
        assertThat(exodusTrackers.toString()).isNotEmpty()
    }

    @Test
    fun testTrackersJsonContainsTrackers() {
        val trackers = mapOf(
            "com.facebook.flipper" to 392,
            "com.google.analytics." to 48,
            "com.google.firebase.firebase_analytics" to 49,
            "com.google.ads." to 312
        )
        trackers.forEach { (codeSignature, id) ->
            assertThat(
                exodusTrackers.getJSONObject(id.toString())
                    .getString("code_signature")
                    .contains(codeSignature)
            ).isTrue()
        }
    }
}
