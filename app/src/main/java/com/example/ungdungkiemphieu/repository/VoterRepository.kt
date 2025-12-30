package com.example.ungdungkiemphieu.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*

class VoterRepository(
    private val apiService: ApiService,
    private val authManager: AuthManager
) {
    /**
     * Lấy danh sách cử tri với phân trang và lọc
     */
    suspend fun getVoterList(
        pollId: Int,
        limit: Int = 20,
        offset: Int = 0,
        search: String? = null,
        checkedIn: Boolean? = null
    ): Result<VoterListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("VoterRepository", "Gọi API getVoterList với " +
                        "pollId=$pollId, limit=$limit, offset=$offset, " +
                        "search=$search, checkedIn=$checkedIn")

                // Chuyển đổi Boolean thành String cho API
                val checkedInParam = when (checkedIn) {
                    true -> "true"
                    false -> "false"
                    null -> null
                }

                val response = apiService.getVoterList(
                    pollId = pollId,
                    limit = limit,
                    offset = offset,
                    search = search,
                    checkedIn = checkedInParam
                )

                Log.d("VoterRepository", "API trả về thành công: " +
                        "success=${response.success}, " +
                        "count=${response.count}, " +
                        "voters=${response.voters.size} items")

                Result.success(response)

            } catch (e: Exception) {
                Log.e("VoterRepository", "Lỗi khi gọi API getVoterList", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Thực hiện check-in cho cử tri
     */
    suspend fun checkinVoter(pollId: Int, voterId: Int): Result<VoterCheckinResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("VoterRepository", "Thực hiện check-in cho voter $voterId trong poll $pollId")

                val response = apiService.checkinVoter(
                    pollId = pollId,
                    voterId = voterId,
                )

                Log.d("VoterRepository", "Check-in thành công: ${response.message}")
                Result.success(response)

            } catch (e: Exception) {
                Log.e("VoterRepository", "Lỗi khi check-in", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Hủy check-in cho cử tri
     */
    suspend fun undoCheckinVoter(pollId: Int, voterId: Int): Result<VoterCheckinResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("VoterRepository", "Hủy check-in cho voter $voterId trong poll $pollId")

                val response = apiService.undoCheckinVoter(
                    pollId = pollId,
                    voterId = voterId,
                )

                Log.d("VoterRepository", "Hủy check-in thành công: ${response.message}")
                Result.success(response)

            } catch (e: Exception) {
                Log.e("VoterRepository", "Lỗi khi hủy check-in", e)
                Result.failure(e)
            }
        }
    }

    // ============================================
    // PHẦN TEST - Lấy tất cả voters (không phân trang)
    // ============================================
    
    /**
     * Lấy TẤT CẢ cử tri (không phân trang - dùng để test)
     */
    suspend fun getAllVoters(
        pollId: Int,
        search: String? = null,
        checkedIn: Boolean? = null
    ): Result<VotersAllResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("VoterRepository", "[TEST] Gọi API getAllVoters với " +
                        "pollId=$pollId, search=$search, checkedIn=$checkedIn")

                // Chuyển đổi Boolean thành String cho API
                val checkedInParam = when (checkedIn) {
                    true -> "true"
                    false -> "false"
                    null -> null
                }

                val response = apiService.getAllVoters(
                    pollId = pollId,
                    search = search,
                    checkedIn = checkedInParam
                )

                Log.d("VoterRepository", "[TEST] API trả về thành công: " +
                        "success=${response.success}, " +
                        "count=${response.count}, " +
                        "voters=${response.voters.size} items")

                Result.success(response)

            } catch (e: Exception) {
                Log.e("VoterRepository", "[TEST] Lỗi khi gọi API getAllVoters", e)
                Result.failure(e)
            }
        }
    }
}