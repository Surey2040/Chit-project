package com.jothivel.chits.ui.collections

import android.os.Bundle
import android.widget.Toast
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.utils.LanguageManager
import com.jothivel.chits.data.repository.PaymentRepository
import com.jothivel.chits.ui.groups.GroupViewModel
import com.jothivel.chits.ui.members.MemberViewModel
import com.jothivel.chits.ui.theme.JothiVelChitsTheme

class CollectionEntryActivity : BaseActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val languageCode = LanguageManager.getLanguage(newBase)
        super.attachBaseContext(LanguageManager.updateResources(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentViewModel = ViewModelProvider(this)[PaymentViewModel::class.java]
        val groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        val memberViewModel = ViewModelProvider(this)[MemberViewModel::class.java]

        setContent {
            JothiVelChitsTheme {
                CollectionEntryScreen(
                    paymentViewModel = paymentViewModel,
                    groupViewModel = groupViewModel,
                    memberViewModel = memberViewModel,
                    onBackClick = { SmoothTransitions.finishSmooth(this) },
                    onPaymentSaved = { receiptNo ->
                        runOnUiThread {
                            Toast.makeText(this@CollectionEntryActivity, "Payment Saved. Receipt: $receiptNo", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { message ->
                        runOnUiThread {
                            Toast.makeText(this@CollectionEntryActivity, "Error: $message", Toast.LENGTH_LONG).show()
                        }
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
