package com.example.ungdungkiemphieu.data.network

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val BASE_URL_KEY = "base_url"
    private val DEFAULT_BASE_URL = "http://192.168.0.102:8000/"
    
    /**
     * Lấy BASE_URL từ SharedPreferences, nếu chưa có thì trả về giá trị mặc định
     */
    fun getBaseUrl(): String {
        return prefs.getString(BASE_URL_KEY, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }
    
    /**
     * Lưu BASE_URL mới vào SharedPreferences
     */
    fun saveBaseUrl(url: String) {
        // Đảm bảo URL có kết thúc bằng "/"
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(BASE_URL_KEY, normalizedUrl).apply()
    }
    
    /**
     * Kiểm tra xem BASE_URL có hợp lệ không
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            val normalizedUrl = if (url.endsWith("/")) url else "$url/"
            java.net.URL(normalizedUrl)
            true
        } catch (e: Exception) {
            false
        }
    }
}

