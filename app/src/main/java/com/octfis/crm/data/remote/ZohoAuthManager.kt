// app/src/main/java/com/octfis/crm/data/remote/ZohoAuthManager.kt
package com.octfis.crm.data.remote

import android.content.Context

// Kept as a thin wrapper so existing call sites compile.
// All real auth is now handled by CatalystAuthManager.
@Deprecated("Use CatalystAuthManager instead")
class ZohoAuthManager(
    private val context    : Context,
    private val tokenStore : TokenStore,
) {
    fun launchAuthFlow(activityContext: Context) {
        // No-op — Zoho OAuth replaced by Catalyst email/password login

    }

    suspend fun handleCallback(uri: android.net.Uri) = false

    suspend fun getValidToken(): String? = tokenStore.getJwtToken()

    suspend fun logout() = tokenStore.clear()
}