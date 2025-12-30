package com.example.ungdungkiemphieu.repository


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*
import com.example.ungdungkiemphieu.security.FileHashSigner
import com.example.ungdungkiemphieu.security.KeystoreManager
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class UploadRepository(
    private val api: ApiService,
    private val authManager: AuthManager
) {

    suspend fun uploadBallots(
        bitmaps: List<Bitmap>,
        pollId: Int?,
        context: Context
    ): UploadResponse {
        if (bitmaps.isEmpty()) throw IllegalArgumentException("No images to upload")

        val tempFiles = mutableListOf<File>()

        try {
            Log.d("UploadRepository", "Starting multi-upload for poll_id: $pollId, count=${bitmaps.size}")

            // Bước 1: Lấy Private Key từ Keystore
            val privateKey = try {
                KeystoreManager.getPrivateKey()
            } catch (e: Exception) {
                Log.e("UploadRepository", "Cannot get private key. User might not be logged in.", e)
                throw Exception("Không thể lấy private key. Vui lòng đăng nhập lại.", e)
            }

            // Bước 2: Tạo signatures cho từng file
            val signatures = mutableMapOf<String, String>()
            val parts = mutableListOf<MultipartBody.Part>()
            
            for ((index, bitmap) in bitmaps.withIndex()) {
                val fileName = "ballot_${System.currentTimeMillis()}_$index.jpg"
                
                // Log thông tin bitmap trước khi xử lý
                Log.d("UploadRepository", "Processing bitmap #$index: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
                
                // Tạo temp file
                val file = bitmapToFile(bitmap, context, fileName)
                tempFiles.add(file)
                
                // Log thông tin file sau khi tạo
                Log.d("UploadRepository", "File created: $fileName, size=${file.length()} bytes")
                
                // Ký bitmap
                try {
                    val signature = FileHashSigner.signBitmap(bitmap, fileName, privateKey)
                    signatures[fileName] = signature
                    Log.d("UploadRepository", "✓ Signed $fileName")
                    Log.d("UploadRepository", "  Signature (full): $signature")
                    Log.d("UploadRepository", "  File size: ${file.length()} bytes")
                } catch (e: Exception) {
                    Log.e("UploadRepository", "Failed to sign $fileName", e)
                    throw Exception("Không thể ký file $fileName: ${e.message}", e)
                }
                
                // Tạo multipart file part
                val requestFile = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "ballot_files",
                    fileName,
                    requestFile
                )
                parts.add(part)
                Log.d("UploadRepository", "Prepared part #$index: $fileName, size=${file.length()}")
            }

            // Bước 3: Tạo signatures JSON part
            val signaturesJson = Gson().toJson(signatures)
            Log.d("UploadRepository", "===========================================")
            Log.d("UploadRepository", "SENDING TO SERVER:")
            Log.d("UploadRepository", "Total files: ${parts.size}")
            Log.d("UploadRepository", "Signatures JSON: $signaturesJson")
            signatures.forEach { (name, sig) ->
                Log.d("UploadRepository", "  $name -> $sig")
            }
            Log.d("UploadRepository", "===========================================")
            val signaturesRequestBody = signaturesJson.toRequestBody("application/json".toMediaTypeOrNull())

            // Bước 4: Gửi request với signatures + files
            val response = api.uploadBallot(
                pollId = pollId,
                signatures = signaturesRequestBody,
                ballot_files = parts
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d("UploadRepository", "Upload successful: ${response.body()}")
                return response.body()!!
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Upload failed"
                Log.e("UploadRepository", "Upload failed: Code ${response.code()}, Message: $errorMsg")
                throw Exception("Upload failed: $errorMsg")
            }

        } catch (e: Exception) {
            Log.e("UploadRepository", "Upload error: ${e.message}", e)
            throw e
        } finally {
            // Cleanup temporary files
            for (file in tempFiles) {
                if (file.exists()) {
                    file.delete()
                    Log.d("UploadRepository", "Temporary file deleted: ${file.absolutePath}")
                }
            }
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, context: Context, fileName: String? = null): File {
        val file = File(context.cacheDir, fileName ?: "ballot_${System.currentTimeMillis()}.jpg")

        try {
            val outputStream = ByteArrayOutputStream()
            
            // Log trước khi compress
            Log.d("UploadRepository", "Before compress: Bitmap=${bitmap.width}x${bitmap.height}, byteCount=${bitmap.byteCount}")
            
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bitmapData = outputStream.toByteArray()
            
            // Log sau khi compress
            Log.d("UploadRepository", "After compress: JPEG data size=${bitmapData.size} bytes")

            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(bitmapData)
            fileOutputStream.flush()
            fileOutputStream.close()
            outputStream.close()

            Log.d("UploadRepository", "✓ File saved: ${file.name}, size=${file.length()} bytes, path=${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("UploadRepository", "Error creating file: ${e.message}")
            throw e
        }

        return file
    }

    /**
     * Upload single ballot image
     * @param bitmap Image to upload
     * @param pollId Poll ID
     * @param ballotId Optional ballot ID for update
     * @param context Android context
     * @return SingleUploadResponse
     */
    suspend fun uploadSingleBallot(
        bitmap: Bitmap,
        pollId: Int?,
        ballotId: Int? = null,
        context: Context
    ): SingleUploadResponse {
        val tempFile = bitmapToFile(bitmap, context)

        try {
            Log.d("UploadRepository", "Starting single upload for poll_id: $pollId, ballot_id: $ballotId")

            val requestFile = tempFile.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
            val ballotFilePart = MultipartBody.Part.createFormData(
                "ballot_file",
                tempFile.name,
                requestFile
            )

            val ballotIdPart = ballotId?.let {
                MultipartBody.Part.createFormData("ballot_id", it.toString())
            }

            val response = api.uploadSingleBallot(
                pollId = pollId,
                ballot_file = ballotFilePart,
                ballot_id = ballotIdPart
            )
            if (response.isSuccessful && response.body() != null) {
                Log.d("UploadRepository", "Single upload successful: ${response.body()}")
                return response.body()!!
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Upload failed"
                Log.e("UploadRepository", "Single upload failed: Code ${response.code()}, Message: $errorMsg")
                throw Exception("Upload failed: $errorMsg")
            }

        } catch (e: Exception) {
            Log.e("UploadRepository", "Single upload error: ${e.message}", e)
            throw e
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
                Log.d("UploadRepository", "Temporary file deleted: ${tempFile.absolutePath}")
            }
        }
    }
}
