package com.aurora.store.view.ui.details

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aurora.store.R

class EmptyAppDetailsActivity: AppCompatActivity(R.layout.activity_details) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val validSchemes = listOf("market", "http", "https")

        if (intent != null && validSchemes.any { it == intent.scheme }) {
            if (intent.data!!.getQueryParameter("id").isNullOrEmpty()) {
                finishAfterTransition()
            } else {
                // Construct a new intent manually to avoid accepting extras from external apps
                Intent(this, AppDetailsActivity::class.java).also { extIntent ->
                    extIntent.data = intent.data
                    startActivity(extIntent)
                }
            }

        }
    }
}
