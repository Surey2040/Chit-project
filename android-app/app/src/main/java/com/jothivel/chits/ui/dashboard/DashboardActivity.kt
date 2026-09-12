package com.jothivel.chits.ui.dashboard

import android.content.Intent
import android.os.Bundle
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import com.jothivel.chits.ui.auth.LoginActivity
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.utils.AppPreferences
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.ui.theme.JothiVelChitsTheme

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if admin has set up PIN — if not, redirect to login/setup
        val appPreferences = AppPreferences(this)
        if (!appPreferences.isAdminSetup()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        setContent {
            JothiVelChitsTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun finish() {
        super.finish()
        SmoothTransitions.applyExitTransition(this)
    }
}
