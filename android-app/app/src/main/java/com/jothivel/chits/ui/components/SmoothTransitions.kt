package com.jothivel.chits.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.jothivel.chits.R

/**
 * Smooth Activity transition utilities.
 * Call these after startActivity() or finish() for buttery-smooth navigation.
 */
object SmoothTransitions {
    
    /**
     * Apply forward navigation transition (slide in from right).
     * Call after startActivity().
     */
    fun applyEnterTransition(activity: Activity) {
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    /**
     * Apply back navigation transition (slide out to right).
     * Call after finish().
     */
    fun applyExitTransition(activity: Activity) {
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    /**
     * Apply fade transition (for special moments like login → dashboard).
     * Call after startActivity().
     */
    fun applyFadeTransition(activity: Activity) {
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    
    /**
     * Start an activity with smooth slide transition.
     */
    fun startActivitySmooth(context: Context, intent: Intent) {
        context.startActivity(intent)
        if (context is Activity) {
            applyEnterTransition(context)
        }
    }
    
    /**
     * Finish an activity with smooth slide-back transition.
     */
    fun finishSmooth(activity: Activity) {
        activity.finish()
        applyExitTransition(activity)
    }
}
