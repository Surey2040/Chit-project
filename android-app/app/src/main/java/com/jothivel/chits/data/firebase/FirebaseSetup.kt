package com.jothivel.chits.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Central, crash-safe entry point to Firebase. The Labour/Agent feature must keep working
 * (in admin-only mode) on installs that don't have app/google-services.json yet, so every
 * caller goes through [firestoreOrNull] instead of calling FirebaseFirestore.getInstance() directly.
 */
object FirebaseSetup {
    private const val TAG = "FirebaseSetup"

    @Volatile private var initialized = false
    @Volatile private var configured = true

    fun ensureInitialized(context: Context): Boolean {
        if (initialized) return configured
        synchronized(this) {
            if (initialized) return configured
            configured = try {
                val apps = FirebaseApp.getApps(context.applicationContext)
                val app = if (apps.isEmpty()) {
                    FirebaseApp.initializeApp(context.applicationContext)
                } else {
                    apps.first()
                }
                if (app == null) {
                    Log.w(TAG, "Firebase not configured. Add app/google-services.json to enable Labour sync.")
                    false
                } else {
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase not configured. Add app/google-services.json to enable Labour sync.", e)
                false
            }
            initialized = true
        }
        return configured
    }

    /** Returns null (instead of throwing) when Firebase isn't configured on this install. */
    fun firestoreOrNull(context: Context): FirebaseFirestore? {
        if (!ensureInitialized(context)) return null
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore.getInstance() failed", e)
            null
        }
    }

    /**
     * Firestore security rules require `request.auth != null` for every read/write - this
     * signs the device in anonymously (once; cached across app restarts by the Firebase SDK)
     * before handing back the Firestore instance, so callers never hit a PERMISSION_DENIED
     * from an unauthenticated request. Every real read/write path in this feature should go
     * through this instead of [firestoreOrNull].
     *
     * NOTE: anonymous auth only proves "some caller of the Firebase SDK", not a verified agent
     * identity - it stops casual scraping/bots but not a determined attacker who reverse
     * engineers the app. Real protection requires server-verified custom-claim auth (a Cloud
     * Function that checks the PIN and mints a token) - deferred pending that decision.
     */
    suspend fun firestoreIfSignedIn(context: Context): FirebaseFirestore? {
        val firestore = firestoreOrNull(context) ?: return null
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            firestore
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            null
        }
    }
}
