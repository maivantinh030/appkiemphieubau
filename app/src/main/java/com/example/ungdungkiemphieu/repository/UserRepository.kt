package com.example.ungdungkiemphieu.repository



import com.example.ungdungkiemphieu.data.model.UserInfo
import com.example.ungdungkiemphieu.data.network.RetrofitClient.apiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository() {
    suspend fun getCurrentUserSimple(): Result<UserInfo> {
        try {
            val response = apiService.getMe()

            if (response.isSuccessful && response.body()?.success == true) {
                return response.body()?.user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("User data not found"))
            } else {
                return Result.failure(Exception("Failed to get user info: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}