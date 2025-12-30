package com.example.ungdungkiemphieu.data.model


data class UploadResponse(
    val success: Boolean,
    val total_ballots: Int,
    val succeeded: Int,
    val failed: Int,
    val message: String,
    val result: List<UploadResult>
)

data class UploadResult(
    val ballot_file: String,
    val success: Boolean
)

// Response cho upload đơn lẻ
data class SingleUploadResponse(
    val success: Boolean,
    val ballot_id: Int,
    val is_update: Boolean,
    val message: String
)

// Request/Response cho HMAC verification
data class VerifyHmacRequest(
    val hmac_signature: String
)

data class VerifyHmacResponse(
    val success: Boolean,
    val verified: Boolean,
    val ballot_id: Int,
    val message: String
)

// Response cho API lấy tất cả voters (không phân trang - dùng để test)
data class VotersAllResponse(
    val success: Boolean,
    val count: Int,
    val voters: List<Voter>
)

// QR Code data parsed
data class BallotQrCode(
    val prefix: Int,
    val ballotId: Int,
    val hmacSignature: String
) {
    companion object {
        /**
         * Parse QR code format: "0:30:e729a6083f6a"
         * @return BallotQrCode or null if invalid format
         */
        fun parse(qrContent: String): BallotQrCode? {
            try {
                val parts = qrContent.split(":")
                if (parts.size != 3) return null
                
                val prefix = parts[0].toIntOrNull() ?: return null
                val ballotId = parts[1].toIntOrNull() ?: return null
                val hmacSignature = parts[2]
                
                if (hmacSignature.isBlank()) return null
                
                return BallotQrCode(prefix, ballotId, hmacSignature)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
