package com.example.ungdungkiemphieu.data.network

import com.example.ungdungkiemphieu.data.model.*

import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class ApiError(
    val error: String,
    val message: String
)

interface ApiService{

    @POST("api/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse
    
    @POST("api/refresh-token/")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse
    
    @GET("api/me/")
    suspend fun getMe(
    ): Response<UserInfoResponse>
    @POST("api/register/")
    suspend fun signup(

        @Body request: SignupRequest
    ): SignupResponse

    @POST("api/logout/")
    suspend fun logout(): Response<Unit>


    @GET("api/polls/")
    suspend fun getPolls(): PollResponse
    @GET("/api/my-poll-memberships/")
    suspend fun getMyPollMemberships(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("status") status: String? = null
    ): MyPollMembershipResponse



    @POST("api/polls/join/")
    suspend fun joinPoll(
        @Body request: JoinPollRequest
    ): JoinPollResponse
    @GET("api/polls/{poll_id}/")
    suspend fun getPollById(
        @Path("poll_id") pollId: Int?
    ): PollDetailResponse



    @Multipart
    @POST("api/polls/{poll_id}/upload-batch/")
    suspend fun uploadBallot(
        @Path("poll_id") pollId: Int?,
        @Part("signatures") signatures: okhttp3.RequestBody,
        @Part ballot_files: List<MultipartBody.Part>
    ): Response<UploadResponse>

    @Multipart
    @POST("api/polls/{poll_id}/upload/")
    suspend fun uploadSingleBallot(
        @Path("poll_id") pollId: Int?,
        @Part ballot_file: MultipartBody.Part,
        @Part ballot_id: MultipartBody.Part? = null
    ): Response<SingleUploadResponse>

    @POST("api/polls/{poll_id}/ballots/{ballot_id}/verify-hmac/")
    suspend fun verifyHmac(
        @Path("poll_id") pollId: Int,
        @Path("ballot_id") ballotId: Int,
        @Body request: VerifyHmacRequest
    ): Response<VerifyHmacResponse>

    @GET("api/polls/{poll_id}/statistics/")
    suspend fun getResultPollById(
        @Path("poll_id") pollId: Int?

    ): CandidateStatsResponse

    @POST("api/polls/{poll_id}/request-role/")
    suspend fun requestRoleUpgrade(
        @Path("poll_id") pollId: Int,
        @Body request: RequestRoleUpgradeRequest
    ): RequestRoleUpgradeResponse

    @GET("api/polls/{poll_id}/voters/")
    suspend fun getVoterList(
        @Path("poll_id") pollId: Int,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("checked_in") checkedIn: String? = null
    ): VoterListResponse

    // API lấy TẤT CẢ voters (không phân trang - dùng để test)
    @GET("api/polls/{poll_id}/voters/all/")
    suspend fun getAllVoters(
        @Path("poll_id") pollId: Int,
        @Query("search") search: String? = null,
        @Query("checked_in") checkedIn: String? = null
    ): VotersAllResponse

    @POST("api/polls/{poll_id}/voters/{voter_id}/checkin/")
    suspend fun checkinVoter(
        @Path("poll_id") pollId: Int,
        @Path("voter_id") voterId: Int,
    ): VoterCheckinResponse

    @POST("api/polls/{poll_id}/voters/{voter_id}/undo-checkin/")
    suspend fun undoCheckinVoter(
        @Path("poll_id") pollId: Int,
        @Path("voter_id") voterId: Int,
    ): VoterCheckinResponse
}