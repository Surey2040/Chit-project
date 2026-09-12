package com.jothivel.chits.ui.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.jothivel.chits.utils.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
