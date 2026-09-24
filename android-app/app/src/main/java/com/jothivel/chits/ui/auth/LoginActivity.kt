package com.jothivel.chits.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.ui.MainHostActivity
import com.jothivel.chits.ui.theme.JothiVelChitsTheme
import com.jothivel.chits.utils.AppPreferences

class LoginActivity : BaseActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var agentLoginViewModel: AgentLoginViewModel
    private lateinit var appPreferences: AppPreferences

    companion object {
        // In-memory session flag — resets when the app process is killed
        // This ensures PIN is asked every time the user re-opens the app
        var isSessionActive: Boolean = false
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val languageCode = com.jothivel.chits.utils.LanguageManager.getLanguage(newBase)
        super.attachBaseContext(com.jothivel.chits.utils.LanguageManager.updateResources(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPreferences = AppPreferences(this)
        
        // ── Flow Logic ───────────────────────────────────────────────────
        // 1. First time ever → isAdminSetup = false → show Setup screen
        //    (user enters name, phone, username, 4-digit PIN)
        //    → saves profile + PIN → marks admin setup done → go to Dashboard
        //
        // 2. Already set up + session active (app still in memory) → skip to Dashboard
        //    (no PIN asked again while app is in memory)
        //
        // 3. Already set up + session NOT active (app was closed/killed) → show PIN screen
        //    (user enters their 4-digit PIN → verify → go to Dashboard)
        
        // The user requested to NEVER show the setup screen, even on fresh installs or different phones.
        // The PIN will default to 1234 until changed in settings.
        val isSetupMode = false
        
        // If admin is set up, or a labour/agent account is cached on this device, AND the user
        // already authenticated in this session, skip to Dashboard
        if ((appPreferences.isAdminSetup() || appPreferences.isAgent()) && isSessionActive) {
            goToDashboard()
            return
        }

        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        agentLoginViewModel = ViewModelProvider(this).get(AgentLoginViewModel::class.java)

        loginViewModel.loginSuccess.observe(this) { success ->
            if (success) {
                // Mark session as active (lives only while process is alive)
                isSessionActive = true
                goToDashboard()
            }
        }

        loginViewModel.loginError.observe(this) { error ->
            error?.let {
                // Error is handled by shake animation in LoginScreen for PIN mode
                // Toast kept as fallback for setup mode
                if (isSetupMode) {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
            }
        }

        agentLoginViewModel.loginSuccess.observe(this) { success ->
            if (success) {
                isSessionActive = true
                goToDashboard()
            }
        }

        setContent {
            JothiVelChitsTheme {
                LoginScreen(
                    viewModel = loginViewModel,
                    agentViewModel = agentLoginViewModel,
                    isSetupMode = isSetupMode,
                    onLanguageToggle = {
                        val currentLang = com.jothivel.chits.utils.LanguageManager.getLanguage(this)
                        val newLang = if (currentLang == "ta") "en" else "ta"
                        com.jothivel.chits.utils.LanguageManager.setLanguage(this, newLang)
                        recreate()
                    }
                )
            }
        }
    }
    
    private fun goToDashboard() {
        startActivity(Intent(this, MainHostActivity::class.java))
        SmoothTransitions.applyFadeTransition(this)
        finish()
    }
}
