package com.example.ungdungkiemphieu.data.network

import android.content.Context
import android.util.Log
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.utils.EncryptionHelper


class AuthManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "access_token"
    private val REFRESH_TOKEN_KEY = "refresh_token"
    private val TOKEN_EXPIRY_KEY = "token_expiry_time"
    private val USER_ID = "user_id"
    private val USERNAME = "username"
    private val USER_ROLE = "role"
    
    // Biometric keys - Bây giờ lưu refresh token thay vì password
    private val BIOMETRIC_ENABLED = "biometric_enabled"
    private val BIOMETRIC_REFRESH_TOKEN = "biometric_refresh_token"
    
    // Session keys
    private val SESSION_ACTIVE = "session_active"
    private val LAST_BACKGROUND_TIME = "last_background_time"
    
    // Token expiry time in milliseconds (default 24 hours from now)
    private val TOKEN_EXPIRY_DURATION = 24 * 60 * 60 * 1000L
    
    // Session timeout (khi app bị đóng quá lâu) - 5 phút
    private val SESSION_TIMEOUT = 5 * 60 * 1000L

    fun saveLoginData(response: LoginResponse){
        response.accessToken?.let{ token ->
            saveToken(token)
            // Save token expiry time (convert seconds to milliseconds)
            val expiryTime = System.currentTimeMillis() + (response.expiresIn ?: 3600) * 1000L
            prefs.edit().putLong(TOKEN_EXPIRY_KEY, expiryTime).apply()
        }
        // Lưu refresh token (mã hóa)
        response.refreshToken?.let { refreshToken ->
            saveRefreshToken(refreshToken)
        }
        response.user?.let{ saveUser(it) }
        // Đánh dấu session active sau khi login thành công
        setSessionActive(true)
    }
    
    private fun saveRefreshToken(refreshToken: String) {
        try {
            val encryptedRefreshToken = EncryptionHelper.encrypt(refreshToken)
            prefs.edit().apply {
                putString(REFRESH_TOKEN_KEY, encryptedRefreshToken)
                putBoolean(BIOMETRIC_ENABLED, true) // Tự động bật biometric khi có refresh token
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getAccessToken():String? {
        // ✅ Trả token ngay, không logout khi hết hạn
        // Để TokenAuthenticator tự động refresh khi server trả 401
        return prefs.getString(TOKEN_KEY, null)
    }

    private fun saveToken(token:String){
        prefs.edit().putString(TOKEN_KEY,token).apply()
    }
    
    private fun isTokenValid(): Boolean {
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0)
        val currentTime = System.currentTimeMillis()
        return expiryTime > currentTime
    }
    
    private fun saveUser(user: User){
        with(prefs.edit()){
            putInt(USER_ID,user.id)
            putString(USERNAME,user.username)
            apply()
        }
    }
    
    fun isLoggedIn(): Boolean {
        // ✅ Chỉ check có token và refresh token, không check expiry
        // Nếu token hết hạn, Authenticator sẽ tự động refresh
        val hasToken = prefs.getString(TOKEN_KEY, null) != null
        val hasRefreshToken = prefs.getString(REFRESH_TOKEN_KEY, null) != null
        return hasToken && hasRefreshToken
    }

    fun logout(){
        clearSession() // Xóa session trước
        prefs.edit().clear().apply()
    }
    
    // ========== BIOMETRIC FUNCTIONS ==========
    
    /**
     * Kiểm tra xem biometric đã được bật hay chưa (có refresh token được mã hóa)
     */
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(BIOMETRIC_ENABLED, false) && 
               prefs.getString(REFRESH_TOKEN_KEY, null) != null
    }
    
    /**
     * Lấy refresh token đã được mã hóa (để dùng với biometric)
     */
    fun getEncryptedRefreshToken(): String? {
        return prefs.getString(REFRESH_TOKEN_KEY, null)
    }
    
    /**
     * Giải mã và lấy refresh token
     */
    fun getRefreshToken(): String? {
        if (!isBiometricEnabled()) {
            return null
        }
        
        return try {
            val encryptedToken = prefs.getString(REFRESH_TOKEN_KEY, null)
            if (encryptedToken != null) {
                EncryptionHelper.decrypt(encryptedToken)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Nếu giải mã thất bại, xóa dữ liệu biometric
            clearBiometricData()
            null
        }
    }
    
    /**
     * Cập nhật access token mới (sau khi refresh)
     */
    fun updateAccessToken(newAccessToken: String, expiresIn: Int) {
        saveToken(newAccessToken)
        val expiryTime = System.currentTimeMillis() + expiresIn * 1000L
        prefs.edit().putLong(TOKEN_EXPIRY_KEY, expiryTime).apply()
        Log.d("AuthManager", "Access token updated, new expiry: $expiryTime")
    }
    
    /**
     * Kiểm tra xem access token có hết hạn không
     */
    fun isAccessTokenExpired(): Boolean {
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0)
        val currentTime = System.currentTimeMillis()
        return expiryTime <= currentTime
    }
    
    /**
     * Kiểm tra xem access token có sắp hết hạn không (còn < 5 phút)
     */
    fun isAccessTokenExpiring(): Boolean {
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val bufferTime = 5 * 60 * 1000L // 5 phút
        return (expiryTime - currentTime) < bufferTime
    }
    
    /**
     * Check và refresh token nếu cần (gọi từ MainActivity khi mở app)
     */
    suspend fun checkAndRefreshTokenIfNeeded(): Boolean {
        // Nếu chưa login, không cần refresh
        if (!isLoggedIn()) {
            Log.d("AuthManager", "Not logged in, skip token check")
            return false
        }
        
        // Nếu token còn hạn lâu (> 5 phút), không cần refresh
        if (!isAccessTokenExpiring() && !isAccessTokenExpired()) {
            Log.d("AuthManager", "Token still valid, no refresh needed")
            return true
        }
        
        Log.d("AuthManager", "Token expiring/expired, attempting refresh...")
        
        return try {
            val refreshToken = getRefreshToken()
            if (refreshToken == null) {
                Log.e("AuthManager", "No refresh token available")
                logout()
                return false
            }
            
            // Gọi API refresh token
            val request = RefreshTokenRequest(refreshToken)
            val response = RetrofitClient.apiService.refreshToken(request)
            
            if (response.success && response.accessToken != null) {
                updateAccessToken(response.accessToken, response.expiresIn ?: 3600)
                Log.d("AuthManager", "✓ Token refreshed successfully on app start")
                true
            } else {
                Log.e("AuthManager", "Token refresh failed: ${response}")
                logout()
                false
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error refreshing token: ${e.message}")
            // Không logout ngay, để Authenticator xử lý sau
            false
        }
    }
    
    /**
     * Xóa thông tin biometric
     */
    fun clearBiometricData() {
        with(prefs.edit()) {
            remove(REFRESH_TOKEN_KEY)
            remove(BIOMETRIC_ENABLED)
            apply()
        }
    }
    
    /**
     * Tắt biometric login
     */
    fun disableBiometric() {
        clearBiometricData()
        EncryptionHelper.deleteKey()
    }
    
    // ========== SESSION MANAGEMENT ==========
    
    /**
     * Đánh dấu session đang active (khi user đang dùng app)
     */
    fun setSessionActive(active: Boolean) {
        prefs.edit().putBoolean(SESSION_ACTIVE, active).apply()
    }
    
    /**
     * Kiểm tra xem có session active không
     */
    fun isSessionActive(): Boolean {
        return prefs.getBoolean(SESSION_ACTIVE, false)
    }
    
    /**
     * Lưu thời gian app vào background
     */
    fun markAppInBackground() {
        prefs.edit().apply {
            putLong(LAST_BACKGROUND_TIME, System.currentTimeMillis())
            putBoolean(SESSION_ACTIVE, false)
            apply()
        }
    }
    
    /**
     * Kiểm tra xem có cần xác thực lại không
     * Returns true nếu cần xác thực lại
     */
    fun needsReauthentication(): Boolean {
        // Nếu chưa đăng nhập, không cần re-auth (sẽ vào màn login)
        if (!isLoggedIn()) {
            return false
        }
        
        // Nếu session đang active, không cần re-auth
        if (isSessionActive()) {
            return false
        }
        
        // Kiểm tra thời gian background
        val lastBackgroundTime = prefs.getLong(LAST_BACKGROUND_TIME, 0)
        if (lastBackgroundTime == 0L) {
            // Lần đầu tiên mở app sau khi cài đặt
            return false
        }
        
        // Nếu app bị background (tức là user gạt app ra ngoài)
        // thì luôn yêu cầu xác thực lại
        return true
    }
    
    /**
     * Xóa trạng thái session (khi logout hoặc app bị kill)
     */
    fun clearSession() {
        prefs.edit().apply {
            putBoolean(SESSION_ACTIVE, false)
            remove(LAST_BACKGROUND_TIME)
            apply()
        }
    }
}