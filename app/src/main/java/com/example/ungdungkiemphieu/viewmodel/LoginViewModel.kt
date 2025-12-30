package com.example.ungdungkiemphieu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ungdungkiemphieu.repository.LoginRepository
import com.example.ungdungkiemphieu.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    fun login(username:String, password:String, onResult:(Boolean,String?)->Unit){
        Log.d("LoginViewModel", "Starting login for user: $username")
        viewModelScope.launch {
            try {
                // Bước 1: Tạo RSA key pair mới (chạy trên background thread)
                val publicKeyBase64 = withContext(Dispatchers.Default) {
                    Log.d("LoginViewModel", "Generating RSA key pair...")
                    KeystoreManager.generateKeyPair()
                    val pubKey = KeystoreManager.getPublicKeyBase64()
                    Log.d("LoginViewModel", "Key pair generated. Public key: ${pubKey.take(50)}...")
                    pubKey
                }
                
                // Bước 2: Gửi login request với public key PEM
                repository.login(username, password, publicKeyBase64)
                Log.d("LoginViewModel", "Login successful for user: $username")
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed: ${e.message}")
                onResult(false, e.message)
            }
        }
    }
    
    /**
     * Đăng nhập bằng biometric - refresh access token
     */
    fun loginWithBiometric(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.refreshAccessToken()
                if (success) {
                    onResult(true, null)
                } else {
                    onResult(false, "Không thể làm mới token")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Biometric login failed: ${e.message}")
                onResult(false, e.message)
            }
        }
    }
    
    /**
     * Kiểm tra xem biometric đã được bật chưa
     */
    fun isBiometricEnabled(): Boolean {
        return repository.isBiometricEnabled()
    }
    
    /**
     * Tắt biometric login
     */
    fun disableBiometric() {
        repository.disableBiometric()
    }
}