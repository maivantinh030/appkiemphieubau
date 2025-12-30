package com.example.ungdungkiemphieu.data.network

import android.util.Log
import com.example.ungdungkiemphieu.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Tự động refresh access token khi nhận 401 Unauthorized
 */
class TokenAuthenticator(
    private val authManager: AuthManager
) : Authenticator {
    
    override fun authenticate(route: Route?, response: Response): Request? {
        // Tránh infinite loop - nếu request đã thử refresh rồi thì không thử nữa
        if (responseCount(response) >= 2) {
            Log.d("TokenAuthenticator", "Already retried twice, giving up")
            return null
        }
        
        // Nếu là endpoint refresh-token bị 401 thì logout
        if (response.request.url.encodedPath.contains("refresh-token")) {
            Log.d("TokenAuthenticator", "Refresh token failed, logging out")
            runBlocking {
                authManager.logout()
            }
            return null
        }
        
        Log.d("TokenAuthenticator", "Received 401, attempting to refresh token")
        
        return runBlocking {
            try {
                val refreshToken = authManager.getRefreshToken()
                if (refreshToken == null) {
                    Log.e("TokenAuthenticator", "No refresh token available")
                    authManager.logout()
                    return@runBlocking null
                }
                
                // Gọi API refresh token
                val request = RefreshTokenRequest(refreshToken)
                val refreshResponse = RetrofitClient.apiService.refreshToken(request)
                
                if (refreshResponse.success && refreshResponse.accessToken != null) {
                    // Cập nhật access token mới
                    authManager.updateAccessToken(
                        refreshResponse.accessToken,
                        refreshResponse.expiresIn ?: 3600
                    )
                    
                    Log.d("TokenAuthenticator", "Token refreshed successfully")
                    
                    // Retry request với token mới
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                        .build()
                } else {
                    Log.e("TokenAuthenticator", "Token refresh failed")
                    authManager.logout()
                    return@runBlocking null
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Error refreshing token: ${e.message}")
                authManager.logout()
                return@runBlocking null
            }
        }
    }
    
    /**
     * Đếm số lần retry để tránh infinite loop
     */
    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }
}
