package com.example.ungdungkiemphieu.data.model


data class ApproveRoleBody(
    val action: String  // "approve" hoặc "reject"
)
data class RequestRoleResponse(
    val role: String,
    val user_id: Int
)
data class ApproveRoleResponse(
    val message: String
)
data class RequestRoleBody(
    val role: String   // "operator" hoặc "assistant"
)

