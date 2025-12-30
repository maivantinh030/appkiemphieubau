package com.example.ungdungkiemphieu.data.model



data class Voter(
    val voter_id: Int,
    val full_name: String,
    val email: String?,
    val code_id: String,
    val has_checked_in: Boolean,
    val check_in_time: String?,
    val check_in_by: String?
)

data class VoterListResponse(
    val success: Boolean,
    val count: Int,
    val limit: Int,
    val offset: Int,
    val voters: List<Voter>
)

data class VoterCheckinResponse(
    val success: Boolean,
    val message: String,
    val voter: Voter
)