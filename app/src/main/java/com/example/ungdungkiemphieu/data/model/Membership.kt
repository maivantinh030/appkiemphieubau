package com.example.ungdungkiemphieu.data.model

import com.google.gson.annotations.SerializedName

data class Memberships(
    @SerializedName("poll_id")
    val poll_id: Int = 0,

    val title: String = "",
    val description: String? = null,

    @SerializedName("member_status")
    val member_status: String = ""
)

data class MyPollMembershipResponse(
    val success: Boolean = false,
    val total: Int = 0,
    val count: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val memberships: List<Memberships> = emptyList()
)
