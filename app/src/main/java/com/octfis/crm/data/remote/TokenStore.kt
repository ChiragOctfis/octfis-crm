package com.octfis.crm.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("zoho_token_store")

class TokenStore(private val context: Context) {

    private val accessKey  = stringPreferencesKey(ZohoConstants.KEY_ACCESS_TOKEN)
    private val refreshKey = stringPreferencesKey(ZohoConstants.KEY_REFRESH_TOKEN)
    private val expiresKey = longPreferencesKey(ZohoConstants.KEY_EXPIRES_AT)

    // ── New Catalyst JWT keys ─────────────────────────────────────────────────
    private val jwtKey        = stringPreferencesKey("jwt_token")
    private val userNameKey   = stringPreferencesKey("user_name")
    private val userEmailKey  = stringPreferencesKey("user_email")
    private val userRoleKey   = stringPreferencesKey("user_role")
    private val userFieldKey  = stringPreferencesKey("user_field_value")

    // ── JWT methods ───────────────────────────────────────────────────────────

    suspend fun saveJwtToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[jwtKey] = token
        }
    }

    suspend fun getJwtToken(): String? =
        context.dataStore.data.map { it[jwtKey] }.firstOrNull()

    suspend fun isLoggedIn(): Boolean = getJwtToken() != null

    suspend fun saveUserInfo(
        name       : String,
        email      : String,
        role       : String,
        fieldValue : String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[userNameKey]  = name
            prefs[userEmailKey] = email
            prefs[userRoleKey]  = role
            prefs[userFieldKey] = fieldValue
        }
    }

    suspend fun getUserName(): String? =
        context.dataStore.data.map { it[userNameKey] }.firstOrNull()

    suspend fun getUserEmail(): String? =
        context.dataStore.data.map { it[userEmailKey] }.firstOrNull()

    suspend fun getUserRole(): String? =
        context.dataStore.data.map { it[userRoleKey] }.firstOrNull()

    suspend fun getUserFieldValue(): String? =
        context.dataStore.data.map { it[userFieldKey] }.firstOrNull()

    suspend fun clear() = context.dataStore.edit { it.clear() }

    suspend fun save(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L - 60_000L
        context.dataStore.edit { prefs ->
            prefs[accessKey]  = accessToken
            prefs[refreshKey] = refreshToken
            prefs[expiresKey] = expiresAt
        }
    }

    suspend fun saveAccessOnly(accessToken: String, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L - 60_000L
        context.dataStore.edit { prefs ->
            prefs[accessKey]  = accessToken
            prefs[expiresKey] = expiresAt
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.map { it[accessKey] }.firstOrNull()

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.map { it[refreshKey] }.firstOrNull()

    suspend fun isExpired(): Boolean {
        val exp = context.dataStore.data.map { it[expiresKey] ?: 0L }.firstOrNull() ?: 0L
        return System.currentTimeMillis() > exp
    }

   // suspend fun isLoggedIn(): Boolean = getRefreshToken() != null

   // suspend fun clear() = context.dataStore.edit { it.clear() }
}