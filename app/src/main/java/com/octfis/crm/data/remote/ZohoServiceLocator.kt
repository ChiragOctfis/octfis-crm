// app/src/main/java/com/octfis/crm/data/remote/ZohoServiceLocator.kt
package com.octfis.crm.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.octfis.crm.data.remote.dto.FlexibleReminder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object ZohoServiceLocator {

    private lateinit var appContext      : Context
    private lateinit var _tokenStore     : TokenStore
    private lateinit var _catalystAuth   : CatalystAuthManager
    private lateinit var _apiService     : ZohoApiService

    val themePrefs by lazy { ThemePreferenceManager(appContext) }



    fun init(context: Context) {
        appContext    = context.applicationContext
        _tokenStore   = TokenStore(appContext)
        _catalystAuth = CatalystAuthManager(appContext, _tokenStore)
        _apiService   = buildApiService()

    }

    private fun buildApiService(): ZohoApiService {
         val refreshMutex = Mutex()
        // ── Custom Gson — handles Remind_At being String OR Object ────────
        val flexibleReminderAdapter = object : JsonDeserializer<FlexibleReminder?> {
            override fun deserialize(
                json    : JsonElement,
                typeOfT : Type,
                context : JsonDeserializationContext,
            ): FlexibleReminder? {
                return if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    FlexibleReminder(
                        period = obj.get("period")?.takeIf { !it.isJsonNull }?.asString,
                        unit   = obj.get("unit")?.takeIf   { !it.isJsonNull }?.asString,
                    )
                } else {
                    // Zoho sent a String like "15" or "" — ignore it safely
                    null
                }
            }
        }

        val gson = GsonBuilder()
            .registerTypeAdapter(FlexibleReminder::class.java, flexibleReminderAdapter)
            .serializeNulls()
            .create()

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { refreshMutex.withLock {_tokenStore.getJwtToken()} }
                ?: throw IOException("Not logged in — please sign in")

            chain.proceed(
                chain.request().newBuilder()
                    .header("x-jwt-token", "Bearer $token")
                    .build()
            )
        }
        // ── Step 2: On 401, refresh once and retry ────────────────────────
        // OkHttp's Authenticator is the correct hook for reactive token refresh.
        // It is called only on 401, not on every request, keeping the hot path clean.
        val tokenAuthenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                // If we've already retried once (Authorization header present and still 401), give up.
                if (response.request.header("Authorization") != null &&
                    response.priorResponse?.code == 401
                ) {
                    return null  // Triggers IOException("Too many follow-up requests")
                }

                // Serialize refresh across concurrent 401s — only one thread refreshes,
                // the rest wait and then re-read the freshly-saved token.
                val freshToken = runBlocking {
                    refreshMutex.withLock { _tokenStore.getJwtToken() }
                } ?: return null  // null → OkHttp throws; caller surfaces it as an error

                return response.request.newBuilder()
                    .header("x-jwt-token", "Bearer $freshToken")
                    .build()
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(ZohoConstants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ZohoApiService::class.java)

    }

    fun getTokenStore()          = _tokenStore
    fun getCatalystAuthManager() = _catalystAuth
    fun getAuthManager()         = _catalystAuth   // alias so old call sites compile
    fun getApiService()          = _apiService
}