package com.jothivel.chits.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "jothivel_chits_prefs"
        private const val KEY_LOGIN_PIN = "login_pin"
        private const val KEY_IS_ADMIN_SETUP = "is_admin_setup"
        private const val KEY_ADMIN_NAME = "admin_name"
        private const val KEY_ADMIN_PHONE = "admin_phone"
        private const val KEY_ADMIN_USERNAME = "admin_username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"

        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_AGENT = "AGENT"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_AGENT_ID = "agent_id"
        private const val KEY_AGENT_NAME = "agent_name"
        private const val KEY_AGENT_PHONE = "agent_phone"
        private const val KEY_AGENT_PIN_HASH = "agent_pin_hash_cache"
        private const val KEY_AGENT_IS_ACTIVE = "agent_is_active_cache"
        private const val KEY_AGENT_ASSIGNED_GROUPS = "agent_assigned_groups_cache"

        /** Salted SHA-256 hash in the same "v2$salt$hash" format used for the admin PIN. */
        fun hashPin(pin: String): String {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            return "v2\$${Base64.encodeToString(salt, Base64.NO_WRAP)}\$${computeHash(pin, salt)}"
        }

        fun verifyPinHash(pin: String, stored: String): Boolean {
            if (stored.isBlank()) return false
            if (!stored.startsWith("v2$")) return MessageDigest.isEqual(stored.toByteArray(), pin.toByteArray())
            val parts = stored.split('$')
            if (parts.size != 3) return false
            val salt = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
            return MessageDigest.isEqual(parts[2].toByteArray(), computeHash(pin, salt).toByteArray())
        }

        private fun computeHash(pin: String, salt: ByteArray): String {
            var bytes = salt + pin.toByteArray(Charsets.UTF_8)
            repeat(20_000) { bytes = MessageDigest.getInstance("SHA-256").digest(bytes) }
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply()
    }

    fun isAdminSetup(): Boolean {
        return prefs.getBoolean(KEY_IS_ADMIN_SETUP, false)
    }

    fun setAdminSetup(isSetup: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ADMIN_SETUP, isSetup).apply()
    }

    fun getPin(): String {
        return prefs.getString(KEY_LOGIN_PIN, "") ?: ""
    }

    fun savePin(newPin: String) {
        prefs.edit().putString(KEY_LOGIN_PIN, hashPin(newPin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = getPin()
        if (stored.isBlank()) return pin == "1234"
        if (!stored.startsWith("v2$")) {
            val valid = MessageDigest.isEqual(stored.toByteArray(), pin.toByteArray())
            if (valid) savePin(pin)
            return valid
        }
        return verifyPinHash(pin, stored)
    }

    fun saveAdminProfile(name: String, phone: String, username: String) {
        prefs.edit()
            .putString(KEY_ADMIN_NAME, name)
            .putString(KEY_ADMIN_PHONE, phone)
            .putString(KEY_ADMIN_USERNAME, username)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun setLastBackupAt(value: Long = System.currentTimeMillis()) { prefs.edit().putLong(KEY_LAST_BACKUP_AT, value).apply() }
    fun getLastBackupAt(): Long = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)

    // ── Role / Labour (field agent) session ──────────────────────────────
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, ROLE_ADMIN) ?: ROLE_ADMIN
    fun isAgent(): Boolean = getUserRole() == ROLE_AGENT

    fun getAgentId(): String = prefs.getString(KEY_AGENT_ID, "") ?: ""
    fun getAgentName(): String = prefs.getString(KEY_AGENT_NAME, "") ?: ""
    fun getAgentPhone(): String = prefs.getString(KEY_AGENT_PHONE, "") ?: ""
    fun getAgentAssignedGroups(): List<String> =
        (prefs.getString(KEY_AGENT_ASSIGNED_GROUPS, "") ?: "").split(',').filter { it.isNotBlank() }
    fun isAgentActiveCached(): Boolean = prefs.getBoolean(KEY_AGENT_IS_ACTIVE, true)

    /** Persists the agent's identity + cached credentials so login works offline afterwards. */
    fun saveAgentSession(agentId: String, name: String, phone: String, pinHash: String, isActive: Boolean, assignedGroups: List<String>) {
        prefs.edit()
            .putString(KEY_USER_ROLE, ROLE_AGENT)
            .putString(KEY_AGENT_ID, agentId)
            .putString(KEY_AGENT_NAME, name)
            .putString(KEY_AGENT_PHONE, phone)
            .putString(KEY_AGENT_PIN_HASH, pinHash)
            .putBoolean(KEY_AGENT_IS_ACTIVE, isActive)
            .putString(KEY_AGENT_ASSIGNED_GROUPS, assignedGroups.joinToString(","))
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /** Cached PIN hash used to allow agent login while offline; refreshed on every successful online login. */
    fun getCachedAgentPinHash(): String = prefs.getString(KEY_AGENT_PIN_HASH, "") ?: ""

    fun clearAgentSession() {
        prefs.edit()
            .remove(KEY_USER_ROLE)
            .remove(KEY_AGENT_ID)
            .remove(KEY_AGENT_NAME)
            .remove(KEY_AGENT_PHONE)
            .remove(KEY_AGENT_PIN_HASH)
            .remove(KEY_AGENT_IS_ACTIVE)
            .remove(KEY_AGENT_ASSIGNED_GROUPS)
            .apply()
    }
}
