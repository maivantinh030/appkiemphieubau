package com.example.ungdungkiemphieu.security

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec

/**
 * Class để ký số files/bitmaps
 * - Ký trực tiếp DATA (không hash trước)
 * - Dùng SHA256withRSA/PSS (khớp với Python cryptography PSS padding)
 * - Output: Base64 signature string
 */
class FileHashSigner {

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA/PSS" // PSS padding!
        private const val BUFFER_SIZE = 8192 // 8KB buffer
        private const val TAG = "FileHashSigner"

        /**
         * Hash và ký một file từ URI
         * Đọc file qua stream để tối ưu memory
         *
         * @param uri URI của file cần ký
         * @param privateKey Private key để ký
         * @param context Android context để mở InputStream
         * @return Base64 signature string
         * @throws Exception nếu không thể đọc file hoặc ký
         */
        fun signFile(uri: Uri, privateKey: PrivateKey, context: Context): String {
            var inputStream: InputStream? = null
            try {
                Log.d(TAG, "Signing file from URI: $uri")

                // Mở input stream từ URI
                inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Không thể mở file: $uri")

                // Hash file qua stream
                val hash = hashStream(inputStream)

                // Ký hash
                val signature = signHash(hash, privateKey)

                Log.d(TAG, "File signed successfully. Signature length: ${signature.length}")
                return signature

            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign file", e)
                throw Exception("Không thể ký file: ${e.message}", e)
            } finally {
                inputStream?.close()
            }
        }

        /**
         * Ký trực tiếp một Bitmap (KÝ DATA, KHÔNG KÝ HASH)
         * Compress bitmap sang JPEG trước khi ký
         * Dùng PSS padding khớp với Python server
         *
         * @param bitmap Bitmap cần ký
         * @param fileName Tên file (dùng để log)
         * @param privateKey Private key để ký
         * @param quality JPEG quality (0-100), default 85
         * @return Base64 signature string
         * @throws Exception nếu không thể compress hoặc ký
         */
        fun signBitmap(
            bitmap: Bitmap,
            fileName: String,
            privateKey: PrivateKey,
            quality: Int = 85
        ): String {
            try {
                Log.d(TAG, "========== SIGNING BITMAP: $fileName ==========")
                Log.d(TAG, "Bitmap info: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")

                // Compress bitmap sang JPEG bytes
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val bitmapBytes = outputStream.toByteArray()
                outputStream.close()

                Log.d(TAG, "Bitmap compressed to ${bitmapBytes.size} bytes (quality=$quality)")

                // KÝ TRỰC TIẾP DATA với PSS padding
                val signature = signData(bitmapBytes, privateKey)

                Log.d(TAG, "✓ Bitmap signed successfully")
                Log.d(TAG, "  Data size: ${bitmapBytes.size} bytes")
                Log.d(TAG, "  Signature length: ${signature.length} chars")
                Log.d(TAG, "  Signature (first 50 chars): ${signature.take(50)}...")
                Log.d(TAG, "=============================================")
                return signature

            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign bitmap", e)
                throw Exception("Không thể ký bitmap: ${e.message}", e)
            }
        }

        /**
         * KÝ TRỰC TIẾP DATA với PSS padding (khớp Python server)
         * PSS config: MGF1(SHA256), salt_length = digest_length
         *
         * @param data Byte array cần ký
         * @param privateKey Private key để ký
         * @return Base64 signature string
         * @throws Exception nếu không thể ký
         */
        fun signData(data: ByteArray, privateKey: PrivateKey): String {
            try {
                Log.d(TAG, "Signing data with PSS padding: ${data.size} bytes")

                val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

                // ✅ PSS parameters khớp Python cryptography
                // Python: padding.PSS(mgf=MGF1(SHA256()), salt_length=MAX_LENGTH)
                // Android: salt_length = digest_length (32 bytes cho SHA256)
                val pssSpec = PSSParameterSpec(
                    "SHA-256",                  // Hash algorithm
                    "MGF1",                     // MGF algorithm
                    MGF1ParameterSpec.SHA256,   // MGF1 với SHA-256
                    32,                         // Salt length = digest length (32 bytes)
                    1                           // Trailer field (standard)
                )
                
                signature.setParameter(pssSpec)
                signature.initSign(privateKey)
                signature.update(data)  // Ký trực tiếp data

                val signatureBytes = signature.sign()
                val base64Signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

                Log.d(TAG, "✓ Data signed with PSS:")
                Log.d(TAG, "  - Algorithm: SHA256withRSA/PSS")
                Log.d(TAG, "  - MGF: MGF1(SHA-256)")
                Log.d(TAG, "  - Salt length: 32 bytes (digest length)")
                Log.d(TAG, "  - Result: ${signatureBytes.size} bytes → ${base64Signature.length} chars Base64")
                return base64Signature

            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign data with PSS", e)
                throw Exception("Không thể ký data với PSS: ${e.message}", e)
            }
        }

        /**
         * Hash và ký byte array (CŨ - giữ lại để tương thích)
         *
         * @param data Byte array cần ký
         * @param privateKey Private key để ký
         * @return Base64 signature string
         * @throws Exception nếu không thể ký
         */
        fun signBytes(data: ByteArray, privateKey: PrivateKey): String {
            try {
                val hash = hashBytes(data)
                return signHash(hash, privateKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign bytes", e)
                throw Exception("Không thể ký bytes: ${e.message}", e)
            }
        }

        /**
         * Hash một InputStream bằng SHA-256
         * Đọc qua buffer để không tốn nhiều RAM
         *
         * @param inputStream InputStream cần hash
         * @return ByteArray hash (32 bytes)
         * @throws Exception nếu lỗi đọc stream
         */
        private fun hashStream(inputStream: InputStream): ByteArray {
            try {
                val digest = MessageDigest.getInstance(HASH_ALGORITHM)
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }

                val hash = digest.digest()
                Log.d(TAG, "Stream hashed. Hash length: ${hash.size} bytes")
                return hash

            } catch (e: Exception) {
                Log.e(TAG, "Failed to hash stream", e)
                throw Exception("Không thể hash stream: ${e.message}", e)
            }
        }

        /**
         * Hash một ByteArray bằng SHA-256
         *
         * @param data ByteArray cần hash
         * @return ByteArray hash (32 bytes)
         * @throws Exception nếu lỗi hash
         */
        private fun hashBytes(data: ByteArray): ByteArray {
            try {
                val digest = MessageDigest.getInstance(HASH_ALGORITHM)
                val hash = digest.digest(data)
                Log.d(TAG, "Bytes hashed. Input: ${data.size} bytes, Hash: ${hash.size} bytes")
                return hash
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hash bytes", e)
                throw Exception("Không thể hash bytes: ${e.message}", e)
            }
        }

        /**
         * Ký một hash bằng RSA Private Key với PSS parameters khớp Python
         *
         * @param hash ByteArray hash cần ký (thường là 32 bytes SHA-256)
         * @param privateKey Private key để ký
         * @return Base64 signature string
         * @throws Exception nếu không thể ký
         */
        private fun signHash(hash: ByteArray, privateKey: PrivateKey): String {
            try {
                val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

                // Config PSS parameters khớp Python
                val pssParams = PSSParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    32,
                    1
                )

                signature.setParameter(pssParams)
                signature.initSign(privateKey)
                signature.update(hash)

                val signatureBytes = signature.sign()
                val base64Signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

                Log.d(TAG, "Hash signed with PSS. Signature: ${signatureBytes.size} bytes -> ${base64Signature.length} chars")
                return base64Signature

            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign hash", e)
                throw Exception("Không thể ký hash: ${e.message}", e)
            }
        }

        /**
         * Verify signature với PSS parameters khớp Python
         *
         * @param data Dữ liệu gốc
         * @param signatureBase64 Signature cần verify
         * @param publicKey Public key để verify
         * @return true nếu signature hợp lệ
         */
        fun verifySignatureWithPSS(
            data: ByteArray,
            signatureBase64: String,
            publicKey: java.security.PublicKey
        ): Boolean {
            return try {
                val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)

                val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

                // PSS parameters khớp Python
                val pssParams = PSSParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    32,
                    1
                )

                signature.setParameter(pssParams)
                signature.initVerify(publicKey)
                signature.update(data)  // Verify trực tiếp data (không hash trước)

                val isValid = signature.verify(signatureBytes)
                Log.d(TAG, "PSS Signature verification: $isValid")
                isValid

            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify PSS signature", e)
                false
            }
        }

        /**
         * Verify signature (method cũ - dùng để test local)
         *
         * @param data Dữ liệu gốc
         * @param signatureBase64 Signature cần verify
         * @param publicKey Public key để verify
         * @return true nếu signature hợp lệ
         */
        @Deprecated("Use verifySignatureWithPSS for better Python compatibility")
        fun verifySignature(
            data: ByteArray,
            signatureBase64: String,
            publicKey: java.security.PublicKey
        ): Boolean {
            return try {
                val hash = hashBytes(data)
                val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)

                val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
                signature.initVerify(publicKey)
                signature.update(hash)

                val isValid = signature.verify(signatureBytes)
                Log.d(TAG, "Signature verification: $isValid")
                isValid

            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify signature", e)
                false
            }
        }

        /**
         * Lấy hex string của hash (dùng để debug)
         *
         * @param data Dữ liệu cần hash
         * @return Hex string của SHA-256 hash
         */
        fun getHashHex(data: ByteArray): String {
            return try {
                val hash = hashBytes(data)
                hash.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get hash hex", e)
                ""
            }
        }
    }
}