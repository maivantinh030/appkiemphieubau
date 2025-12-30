package com.example.ungdungkiemphieu.repository


import android.util.Log
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*


class LoginRepository(
    private val authManager: AuthManager
){
    suspend fun login(username: String, password: String, publicKey: String): LoginResponse {
        Log.d("LoginRepository", "login() called with username: $username")
        Log.d("LoginRepository", "Public key length: ${publicKey} chars")
        val loginRequest = LoginRequest(username, password, publicKey)
        val response = RetrofitClient.apiService.login(loginRequest)
        Log.d("LoginRepository", "API call completed. Response: $response")
        // Lưu tokens (refresh token sẽ tự động được mã hóa)
        authManager.saveLoginData(response)
        return response
    }
    
    /**
     * Làm mới access token bằng refresh token
     */
    suspend fun refreshAccessToken(): Boolean {
        return try {
            val refreshToken = authManager.getRefreshToken()
            if (refreshToken == null) {
                Log.e("LoginRepository", "No refresh token available")
                return false
            }
            
            val request = RefreshTokenRequest(refreshToken)
            val response = RetrofitClient.apiService.refreshToken(request)
            
            if (response.success && response.accessToken != null) {
                authManager.updateAccessToken(
                    response.accessToken,
                    response.expiresIn ?: 3600
                )
                Log.d("LoginRepository", "Access token refreshed successfully")
                true
            } else {
                Log.e("LoginRepository", "Failed to refresh token")
                false
            }
        } catch (e: Exception) {
            Log.e("LoginRepository", "Error refreshing token: ${e.message}")
            false
        }
    }
    
    // ========== BIOMETRIC FUNCTIONS ==========
    
    /**
     * Kiểm tra biometric đã được bật chưa
     */
    fun isBiometricEnabled(): Boolean {
        return authManager.isBiometricEnabled()
    }
    
    /**
     * Tắt biometric
     */
    fun disableBiometric() {
        authManager.disableBiometric()
    }
}
