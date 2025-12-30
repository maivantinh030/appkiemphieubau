package com.example.ungdungkiemphieu.repository




import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*

class PollRepository {
    // Cache đúng cách: dùng StateFlow (giữ sống suốt đời app nếu inject singleton)
    private val _pollsCache = MutableStateFlow<List<Poll>>(emptyList())
    val pollsCache: StateFlow<List<Poll>> = _pollsCache


    suspend fun getPolls(
        forceRefresh: Boolean = false,
        limit: Int = 20,
        offset: Int = 0,
        status: String? = null
    ): MyPollMembershipResponse {
        // Nếu offset = 0, clear cache (refresh from start)
        if (offset == 0 && forceRefresh) {
            _pollsCache.value = emptyList()
        }

        try {
            val response = RetrofitClient.apiService.getMyPollMemberships(
                limit = limit,
                offset = offset,
                status = status
            )
            Log.d("PollRepository", "Response: $response")
            if (response.success) {
                val polls = mutableListOf<Poll>()
                for (membership in response.memberships) {
                    try {
                        // Lấy đầy đủ thông tin poll từ API
                        val fullPoll = getPollById(membership.poll_id)
                        // Cập nhật member_status từ membership response
                        fullPoll.member_status = membership.member_status
                        polls.add(fullPoll)
                    } catch (e: Exception) {
                        Log.e("PollRepository", "Error loading poll ${membership.poll_id}: ${e.message}")
                        // Fallback: tạo poll từ membership data nếu không thể load full info
                        val poll = Poll(
                            poll_id = membership.poll_id,
                            title = membership.title,
                            description = membership.description,
                            member_status = membership.member_status,
                            status = "unknown",
                            start_time = null,
                            end_time = null,
                            total_ballots = 0,
                            checked_ballots = 0,
                            candidates = emptyList(),
                            created_by = ""
                        )
                        polls.add(poll)
                    }
                }
                
                // Append hoặc replace cache
                if (offset == 0) {
                    _pollsCache.value = polls
                } else {
                    _pollsCache.value = _pollsCache.value + polls
                }
                
                Log.d("PollRepository", "Polls loaded: ${polls.size}, total cache: ${_pollsCache.value.size}")
                return response.copy(memberships = emptyList()) // Return metadata only
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        return MyPollMembershipResponse(success = false)
    }
    suspend fun joinPoll(accessCode: String): JoinPollResponse {
        try {
            val request = JoinPollRequest(accessCode)
            Log.d("JoinPollRepository", "Request: $request")
            val response = RetrofitClient.apiService.joinPoll(request)
            Log.d("JoinPollRepository", "Response: $response")
            return response
        }
        catch(e: Exception){
            e.printStackTrace()
            throw e
        }

    }
    suspend fun getPollById(pollId: Int?): Poll {
        try {
            Log.d("PollRepository", "Fetching poll with ID: $pollId")
            val response = RetrofitClient.apiService.getPollById(pollId)
            Log.d("PollRepository", "Response: $response")

            if (response.success) {
                Log.d("PollRepository", "Poll fetched: ${response.poll}")
                return response.poll
            } else {
                throw Exception("API returned success=false")
            }
        } catch (e: Exception) {
            Log.e("PollRepository", "Error fetching poll", e)
            throw e
        }
    }

    suspend fun getResultPollById(pollId: Int?): CandidateStatsResponse {
        try{
            Log.d("PollRepository", "Calling getResultPollById with pollId=$pollId")
            val response = RetrofitClient.apiService.getResultPollById(pollId)
            Log.d("PollRepository", "Stats response: success=${response.success}, total=${response.total_ballots}, items=${response.stats.size}")
            return response
        }
        catch (e: Exception){
            Log.e("PollRepository", "getResultPollById error", e)
            throw e
        }
    }
    suspend fun requestRoleUpgrade(pollId: Int, requestedRole: String): Result<RequestRoleUpgradeResponse> {
        return withContext(Dispatchers.IO) {
            try {

                val request = RequestRoleUpgradeRequest(requested_role = requestedRole)
                val response = RetrofitClient.apiService.requestRoleUpgrade(
                    pollId = pollId,
                    request = request
                )
                Log.d("PollRepository", "Request role upgrade success: ${response.message}")
                Result.success(response)

            } catch (e: Exception) {
                Log.e("PollRepository", "Request role upgrade error", e)
                Result.failure(e)
            }
        }
    }

}
