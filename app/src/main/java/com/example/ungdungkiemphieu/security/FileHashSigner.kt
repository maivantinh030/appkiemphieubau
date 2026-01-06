package com.example.ungdungkiemphieu.security

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
class FileHashSigner {

    companion object {
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA/PSS" // PSS padding!
        private const val TAG = "FileHashSigner"
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
        fun signData(data: ByteArray, privateKey: PrivateKey): String {
            try {
                Log.d(TAG, "Signing data with PSS padding: ${data.size} bytes")

                val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
                val pssSpec = PSSParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    32,
                    1
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

    }
}