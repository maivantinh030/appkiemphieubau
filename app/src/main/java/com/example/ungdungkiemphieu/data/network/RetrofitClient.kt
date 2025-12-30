package com.example.ungdungkiemphieu.data.network


import android.util.Log
import com.example.ungdungkiemphieu.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient{
    private const val BASE_URL = "http://10.184.78.135:8000/"
    private lateinit var authManager: AuthManager
    private var onUnauthorized: (() -> Unit)? = null
    
    fun initialize(authManager: AuthManager, onUnauthorizedCallback: (() -> Unit)? = null){
        this.authManager = authManager
        this.onUnauthorized = onUnauthorizedCallback
        Log.d("RetrofitClient", "RetrofitClient initialized, authManager: $authManager")
    }
    
    // Logging interceptor để debug request/response
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor kiểm tra và refresh token trước khi gửi request
    private val authInterceptor: Interceptor
        get() = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            // Kiểm tra token có sắp hết hạn không (trừ endpoint refresh-token)
            if (!chain.request().url.encodedPath.contains("refresh-token")) {
                if (authManager.isAccessTokenExpiring()) {
                    Log.d("RetrofitClient", "Access token expiring, refreshing...")
                    runBlocking {
                        try {
                            val refreshToken = authManager.getRefreshToken()
                            if (refreshToken != null) {
                                val request = RefreshTokenRequest(refreshToken)
                                val response = apiService.refreshToken(request)
                                
                                if (response.success && response.accessToken != null) {
                                    authManager.updateAccessToken(
                                        response.accessToken,
                                        response.expiresIn ?: 3600
                                    )
                                    Log.d("RetrofitClient", "Token refreshed proactively")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RetrofitClient", "Proactive token refresh failed: ${e.message}")
                        }
                    }
                }
            }
            
            // Gắn token vào header
            authManager.getAccessToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            chain.proceed(requestBuilder.build())
        }

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)  // Thêm logging để debug
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(authManager))
            .connectTimeout(500, TimeUnit.SECONDS)
            .readTimeout(500, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy{
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}