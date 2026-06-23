// app/src/main/java/com/octfis/crm/data/remote/CatalystAuthManager.kt
package com.octfis.crm.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class LoginResponse(
    val token          : String,
    val name           : String,
    val email          : String,
    val role           : String,
    val userFieldValue : String,
)

class CatalystAuthManager(
    private val context    : Context,
    private val tokenStore : TokenStore,
) {
    private val http = OkHttpClient()


    suspend fun login(
        email    : String,
        password : String,
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("email",    email.trim())
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("${ZohoConstants.BASE_URL}/server/appauth/login")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = http.newCall(request).execute()
            val raw      = response.body!!.string()
            Log.d("CatalystAuth", "login response [${response.code}]: $raw")

            val json = JSONObject(raw)
            if (!json.getBoolean("success")) {
                error(json.optString("message", "Login failed"))
            }

            val token    = json.getString("token")
            val userJson = json.getJSONObject("user")

            val loginResponse = LoginResponse(
                token          = token,
                name           = userJson.getString("name"),
                email          = userJson.getString("email"),
                role           = userJson.getString("role"),
                userFieldValue = userJson.getString("userFieldValue"),
            )
            Log.d("CatalystAuth", "login response [${response.code}]: $raw")

            // Persist to DataStore
            tokenStore.saveJwtToken(token)
            tokenStore.saveUserInfo(
                name       = loginResponse.name,
                email      = loginResponse.email,
                role       = loginResponse.role,
                fieldValue = loginResponse.userFieldValue,
            )

            // Populate in-memory session
            SessionManager.currentUser = UserSession(
                name           = loginResponse.name,
                email          = loginResponse.email,
                role           = loginResponse.role,
                userFieldValue = loginResponse.userFieldValue,
            )

            loginResponse
        }
    }

    suspend fun restoreSession(): Boolean {
        val token      = tokenStore.getJwtToken()      ?: return false
        val name       = tokenStore.getUserName()      ?: return false
        val email      = tokenStore.getUserEmail()     ?: return false
        val role       = tokenStore.getUserRole()      ?: return false
        val fieldValue = tokenStore.getUserFieldValue() ?: return false

        SessionManager.currentUser = UserSession(
            name           = name,
            email          = email,
            role           = role,
            userFieldValue = fieldValue,
        )
        return true
    }

    suspend fun logout() {
        SessionManager.clear()
        tokenStore.clear()
    }
}