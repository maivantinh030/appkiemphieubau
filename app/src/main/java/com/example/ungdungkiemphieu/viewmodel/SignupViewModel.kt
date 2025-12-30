package com.example.ungdungkiemphieu.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ungdungkiemphieu.repository.SignupRepository
import kotlinx.coroutines.launch

class SignupViewModel(private val repository: SignupRepository) : ViewModel() {

    fun signup(
        username: String,
        email: String,
        fullName: String,
        password: String,
        confirmPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Validate input
        if (username.isBlank()) {
            onResult(false, "Vui lòng nhập tên đăng nhập")
            return
        }

        if (password.isBlank()) {
            onResult(false, "Vui lòng nhập mật khẩu")
            return
        }

        if (confirmPassword.isBlank()) {
            onResult(false, "Vui lòng xác nhận mật khẩu")
            return
        }

        if (password != confirmPassword) {
            onResult(false, "Mật khẩu xác nhận không khớp")
            return
        }

        if (password.length < 6) {
            onResult(false, "Mật khẩu phải có ít nhất 6 ký tự")
            return
        }

        Log.d("SignupViewModel", "Starting signup for user: $username")

        viewModelScope.launch {
            try {
                Log.d("SignupViewModel", "Calling repository.signup()")
                val response = repository.signup(
                    username = username,
                    email = email,
                    fullName = fullName,
                    password = password,
                    confirmPassword = confirmPassword
                )

                Log.d("SignupViewModel", "Signup successful for user: $username")
                Log.d("SignupViewModel", "Response: $response")

                if (response.success) {
                    onResult(true, response.message)
                } else {
                    onResult(false, response.message ?: "Đăng ký thất bại")
                }
            } catch (e: Exception) {
                Log.e("SignupViewModel", "Signup failed", e)
                onResult(false, e.message ?: "Đã xảy ra lỗi khi đăng ký")
            }
        }
    }
}