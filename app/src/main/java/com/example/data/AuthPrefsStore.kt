package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "fleet_auth_prefs")

data class SavedAuthPrefs(
    val rememberedEmail: String = "",
    val rememberMe: Boolean = true,
    val lastRiderName: String = "",
    val quickPinHash: String = "",
    val quickPinUid: String = "",
    val savedPhoneEmailPairs: Map<String, String> = emptyMap(),
    val backgroundAlertsEnabled: Boolean = true
)

class AuthPrefsStore(private val context: Context) {

    private object Keys {
        val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LAST_RIDER_NAME = stringPreferencesKey("last_rider_name")
        val QUICK_PIN_HASH = stringPreferencesKey("quick_pin_hash")
        val QUICK_PIN_UID = stringPreferencesKey("quick_pin_uid")
        val PHONE_EMAIL_MAP = stringPreferencesKey("phone_email_map")
        val BACKGROUND_ALERTS_ENABLED = booleanPreferencesKey("background_alerts_enabled")
    }

    val prefsFlow: Flow<SavedAuthPrefs> = context.authDataStore.data.map { prefs ->
        val rawMap = prefs[Keys.PHONE_EMAIL_MAP].orEmpty()
        val parsedMap = rawMap.split(";")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    parts[0] to parts[1]
                } else null
            }
            .toMap()

        SavedAuthPrefs(
            rememberedEmail = prefs[Keys.REMEMBERED_EMAIL].orEmpty(),
            rememberMe = prefs[Keys.REMEMBER_ME] ?: true,
            lastRiderName = prefs[Keys.LAST_RIDER_NAME].orEmpty(),
            quickPinHash = prefs[Keys.QUICK_PIN_HASH].orEmpty(),
            quickPinUid = prefs[Keys.QUICK_PIN_UID].orEmpty(),
            savedPhoneEmailPairs = parsedMap,
            backgroundAlertsEnabled = prefs[Keys.BACKGROUND_ALERTS_ENABLED] ?: true
        )
    }

    suspend fun saveLoginPreference(email: String, rememberMe: Boolean, riderName: String? = null) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.REMEMBER_ME] = rememberMe
            if (rememberMe) {
                prefs[Keys.REMEMBERED_EMAIL] = email.trim()
                if (!riderName.isNullOrBlank()) {
                    prefs[Keys.LAST_RIDER_NAME] = riderName
                }
            } else {
                prefs.remove(Keys.REMEMBERED_EMAIL)
                prefs.remove(Keys.LAST_RIDER_NAME)
            }
        }
    }

    suspend fun linkPhoneToEmail(phone: String, email: String, riderName: String) {
        val digits = phone.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10 || email.isBlank()) return
        context.authDataStore.edit { prefs ->
            val current = prefs[Keys.PHONE_EMAIL_MAP].orEmpty()
                .split(";")
                .mapNotNull { entry ->
                    val parts = entry.split("=", limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        parts[0] to parts[1]
                    } else null
                }
                .toMap()
                .toMutableMap()
            current[digits] = email.trim()
            prefs[Keys.PHONE_EMAIL_MAP] = current.entries.joinToString(";") { "${it.key}=${it.value}" }
            if (riderName.isNotBlank()) {
                prefs[Keys.LAST_RIDER_NAME] = riderName
            }
        }
    }

    suspend fun resolveEmailForPhone(phone: String): String? {
        val digits = phone.filter { it.isDigit() }.takeLast(10)
        return prefsFlow.first().savedPhoneEmailPairs[digits]
    }

    suspend fun setQuickPin(uid: String, pin: String) {
        val hashed = SecurityUtils.sha256Hex("$uid:$pin")
        context.authDataStore.edit { prefs ->
            prefs[Keys.QUICK_PIN_UID] = uid
            prefs[Keys.QUICK_PIN_HASH] = hashed
        }
    }

    suspend fun clearQuickPin() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.QUICK_PIN_UID)
            prefs.remove(Keys.QUICK_PIN_HASH)
        }
    }

    suspend fun setBackgroundAlertsEnabled(enabled: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_ALERTS_ENABLED] = enabled
        }
    }
}
