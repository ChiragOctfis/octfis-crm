// app/src/main/java/com/octfis/crm/data/remote/ZohoServiceLocator.kt
package com.octfis.crm.data.remote

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ZohoServiceLocator {

    private lateinit var appContext      : Context
    private lateinit var _tokenStore     : TokenStore
    private lateinit var _catalystAuth   : CatalystAuthManager
    private lateinit var _apiService     : ZohoApiService

    val themePrefs by lazy { ThemePreferenceManager(appContext) }

    private const val CATALYST_BASE_URL =
        "https://crm-mobile-app-927349475.development.catalystserverless.com/"

    fun init(context: Context) {
        appContext    = context.applicationContext
        _tokenStore   = TokenStore(appContext)
        _catalystAuth = CatalystAuthManager(appContext, _tokenStore)
        _apiService   = buildApiService()
    }

    private fun buildApiService(): ZohoApiService {

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { _tokenStore.getJwtToken() }
                ?: throw IOException("Not logged in — please sign in")

            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            )
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(CATALYST_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZohoApiService::class.java)
    }

    fun getTokenStore()          = _tokenStore
    fun getCatalystAuthManager() = _catalystAuth
    fun getAuthManager()         = _catalystAuth   // alias so old call sites compile
    fun getApiService()          = _apiService
}