package com.example.ungdungkiemphieu.data.model


import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Int?,
    val user: User?
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenResponse(
    val success: Boolean,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Int?
)

data class SignupRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String?,

    @SerializedName("last_name")
    val lastName: String?,

    @SerializedName("password")
    val password: String,

    @SerializedName("password_confirm")
    val passwordConfirm: String
)
data class User(
    val id: Int,
    val username: String
)

data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("public_key")
    val publicKey: String
)

enum class UserRole(val displayName: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Quản trị viên"),
    OPERATOR("Nhân viên quét phiếu"),
    VOTER("Cử tri"),
    GUEST("Khách");

    companion object {
        fun fromString(value: String?): UserRole {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: GUEST
        }
    }
}

data class SignupResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("error")
    val error: String?,

    @SerializedName("user")
    val user: SignupUser?
)

// User data trong response
data class SignupUser(
    @SerializedName("id")
    val id: Int,

    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String?,

    @SerializedName("full_name")
    val fullName: String?
)

data class UserInfoResponse(
    val success: Boolean,
    val user: UserInfo?
)

data class UserInfo(
    val id: Int,
    val username: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("full_name")
    val fullName: String?,
    @SerializedName("is_superuser")
    val isSuperuser: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String?
)