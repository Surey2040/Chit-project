package com.jothivel.chits.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }

    fun onAttach(context: Context): Context {
        val lang = AppPreferences(context).getLanguage()
        return setLocale(context, lang)
    }
}
