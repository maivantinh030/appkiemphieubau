package com.example.ungdungkiemphieu.data.model

import com.google.gson.annotations.SerializedName

data class Poll(
    var poll_id: Int,
    var title: String,
    var description: String?,
    val status: String,
    val start_time: String?,
    val end_time: String?,
    val total_ballots: Int,
    val checked_ballots: Int,
    val candidates: List<CandidateResponse>,
    val created_by:String,
    var member_status: String?,
    var role: String = "user",
    var requested_role_change: String? = null
)

// Lightweight poll model used by statistics endpoint
data class PollStats(
    val poll_id: Int,
    val title: String,
    val description: String?,
    val status: String
)

data class PollResponse(
    val success: Boolean,
    val count: Int,
    val polls: List<Poll>
)

data class JoinPollRequest(
    val access_code: String
)

data class JoinPollResponse(
    val success: String,
    val message: String,
    val status: String,
    val poll: Poll
)
data class PollDetailResponse(
    val success: Boolean,
    val poll: Poll
)

data class MemberInfo(
    val id: Int,
    val username: String,
    val role: String
)
data class CandidateResponse(
    val candidate_id: Int,
    val name: String,
    val description: String?
)

data class candidatestats(
    val name: String,
    val count: Int
)
data class CandidateStatsResponse(
    val success: Boolean,
    val poll: PollStats,
    val total_ballots: Int,
    @SerializedName("candidate_stats") val stats: List<candidatestats>
)

data class RequestRoleUpgradeRequest(
    val requested_role: String
)

// Response từ API
data class RequestRoleUpgradeResponse(
    val success: Boolean,
    val message: String,
    val requested_role: String?
)