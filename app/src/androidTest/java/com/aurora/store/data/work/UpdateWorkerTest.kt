package com.aurora.store.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.junit4.MockKRule
import io.mockk.junit5.MockKExtension
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@MockKExtension.ConfirmVerification
@MockKExtension.CheckUnnecessaryStub
@RunWith(AndroidJUnit4::class)
class UpdateWorkerTest {

    @get:Rule
    var hiltAndroidRule = HiltAndroidRule(this)

    @get:Rule
    val mockkRule = MockKRule(this)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltAndroidRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAutoUpdates() {
        val worker = TestListenableWorkerBuilder<UpdateWorker>(context)
            .build()
    }
}
