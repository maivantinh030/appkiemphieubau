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

    private lateinit var authManager: AuthManager
    private lateinit var settingsManager: SettingsManager
    private var onUnauthorized: (() -> Unit)? = null
    private var retrofitInstance: Retrofit? = null
    private var apiServiceInstance: ApiService? = null
    
    fun initialize(
        authManager: AuthManager, 
        settingsManager: SettingsManager,
        onUnauthorizedCallback: (() -> Unit)? = null
    ){
        this.authManager = authManager
        this.settingsManager = settingsManager
        this.onUnauthorized = onUnauthorizedCallback
        Log.d("RetrofitClient", "RetrofitClient initialized, authManager: $authManager")
    }
    
    /**
     * Reset Retrofit instance khi BASE_URL thay đổi
     */
    fun resetInstance() {
        retrofitInstance = null
        apiServiceInstance = null
        Log.d("RetrofitClient", "Retrofit instance reset")
    }
    
    // Logging interceptor để debug request/response
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Tạo Retrofit instance riêng để refresh token (tránh circular dependency)
    private fun getRefreshTokenRetrofit(): Retrofit {
        val baseUrl = settingsManager.getBaseUrl()
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
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
                                // Sử dụng Retrofit instance riêng để tránh circular dependency
                                val refreshService = getRefreshTokenRetrofit().create(ApiService::class.java)
                                val response = refreshService.refreshToken(request)
                                
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

    private fun getClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)  // Thêm logging để debug
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(authManager))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            val baseUrl = settingsManager.getBaseUrl()
            Log.d("RetrofitClient", "Creating Retrofit instance with BASE_URL: $baseUrl")
            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    val apiService: ApiService
        get() {
            if (apiServiceInstance == null) {
                apiServiceInstance = getRetrofit().create(ApiService::class.java)
            }
            return apiServiceInstance!!
        }
}