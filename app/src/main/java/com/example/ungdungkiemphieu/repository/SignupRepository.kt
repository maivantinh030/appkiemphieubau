package com.example.ungdungkiemphieu.repository

import android.util.Log
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*

class SignupRepository(
    private val authManager: AuthManager
) {
    suspend fun signup(
        username: String,
        email: String,
        fullName: String,
        password: String,
        confirmPassword: String
    ): SignupResponse {
        Log.d("SignupRepository", "signup() called with username: $username")

        val signupRequest = SignupRequest(
            username = username,
            email = email,
            lastName = fullName,
            password = password,
            passwordConfirm = confirmPassword
        )

        Log.d("SignupRepository", "SignupRequest created: $signupRequest")
        Log.d("SignupRepository", "About to call api.signup()")

        val response = RetrofitClient.apiService.signup(signupRequest)

        Log.d("SignupRepository", "API call completed. Response: $response")

        return response
    }
}