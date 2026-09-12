package com.jothivel.chits.ui.groups

import android.os.Bundle
import android.widget.Toast
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.utils.LanguageManager
import com.jothivel.chits.ui.theme.JothiVelChitsTheme

class AuctionEntryActivity : BaseActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val languageCode = LanguageManager.getLanguage(newBase)
        super.attachBaseContext(LanguageManager.updateResources(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ideally, these would come from intent extras (e.g., from GroupDetailActivity)
        val chitValue = 100000000L // 10 Lakhs (in paise)
        val subscriberCount = 20

        setContent {
            JothiVelChitsTheme {
                AuctionEntryScreen(
                    chitValue = chitValue,
                    subscriberCount = subscriberCount,
                    onBackClick = { SmoothTransitions.finishSmooth(this) },
                    onSaveClick = { winningBid, winningMemberId ->
                        // To do: Api Call POST /groups/:id/installments/:no/auction
                        Toast.makeText(this, "Auction Recorded", Toast.LENGTH_SHORT).show()
                        SmoothTransitions.finishSmooth(this)
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        SmoothTransitions.applyExitTransition(this)
    }
}
