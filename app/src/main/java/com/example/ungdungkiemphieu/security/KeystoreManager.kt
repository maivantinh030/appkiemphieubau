package com.example.ungdungkiemphieu.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Quản lý RSA Key Pair trong Android Keystore để ký số ballot images
 * - Mỗi lần login tạo key mới (overwrite key cũ)
 * - Private key được bảo vệ trong Keystore, không thể extract
 * - Public key được export sang Base64 để gửi lên server
 */
class KeystoreManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "BallotSigningKey"
        private const val KEY_SIZE = 2048
        private const val TAG = "KeystoreManager"

        /**
         * Tạo RSA Key Pair mới (overwrite nếu đã tồn tại)
         * Dùng khi user login
         * 
         * @return KeyPair mới được tạo
         * @throws Exception nếu không thể tạo key
         */
        fun generateKeyPair(): KeyPair {
            try {
                Log.d(TAG, "Generating new RSA key pair...")
                
                // Xóa key cũ nếu tồn tại
                clearKeys()

                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEYSTORE
                )

                val parameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).apply {
                    setKeySize(KEY_SIZE)
                    setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    
                    // ✅ Dùng PSS padding để khớp với server Python
                    setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1  // Fallback cho compatibility
                    )
                    
                    // Không yêu cầu user authentication để sign
                    // (vì đã authenticate ở login rồi)
                    setUserAuthenticationRequired(false)
                    
                    // Android 9+ (API 28+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setIsStrongBoxBacked(false) // Không dùng hardware security module
                    }
                }.build()

                keyPairGenerator.initialize(parameterSpec)
                val keyPair = keyPairGenerator.generateKeyPair()
                
                Log.d(TAG, "Key pair generated successfully. Public key algorithm: ${keyPair.public.algorithm}")
                return keyPair
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate key pair", e)
                throw Exception("Không thể tạo key pair: ${e.message}", e)
            }
        }

        /**
         * Lấy Private Key từ Keystore để ký
         * 
         * @return PrivateKey
         * @throws Exception nếu key không tồn tại
         */
        fun getPrivateKey(): PrivateKey {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)

                if (!keyStore.containsAlias(KEY_ALIAS)) {
                    throw Exception("Private key không tồn tại. Vui lòng login lại.")
                }

                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
                    ?: throw Exception("Không thể lấy private key entry")

                Log.d(TAG, "Retrieved private key from keystore")
                return entry.privateKey
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get private key", e)
                throw Exception("Không thể lấy private key: ${e.message}", e)
            }
        }

        /**
         * Lấy Public Key từ Keystore
         * 
         * @return PublicKey
         * @throws Exception nếu key không tồn tại
         */
        fun getPublicKey(): PublicKey {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)

                if (!keyStore.containsAlias(KEY_ALIAS)) {
                    throw Exception("Public key không tồn tại. Vui lòng login lại.")
                }

                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
                    ?: throw Exception("Không thể lấy key entry")

                return entry.certificate.publicKey
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get public key", e)
                throw Exception("Không thể lấy public key: ${e.message}", e)
            }
        }

        /**
         * Export Public Key sang PEM format để gửi lên server
         * Format: PEM (BEGIN/END headers + Base64 with line breaks)
         * Tương thích với Python cryptography library
         * 
         * @return String PEM của public key
         * @throws Exception nếu không thể export
         */
        fun getPublicKeyBase64(): String {
            try {
                val publicKey = getPublicKey()
                
                // Encode theo định dạng X.509 (SubjectPublicKeyInfo)
                val publicKeyBytes = publicKey.encoded
                val base64String = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
                
                // Chia Base64 thành các dòng 64 ký tự (chuẩn PEM)
                val base64WithLineBreaks = base64String.chunked(64).joinToString("\n")
                
                // Wrap thành PEM format (dùng \n explicit thay vì triple quotes)
                val pemString = "-----BEGIN PUBLIC KEY-----\n$base64WithLineBreaks\n-----END PUBLIC KEY-----"
                
                Log.d(TAG, "Exported public key to PEM format (${pemString.length} chars)")
                Log.d(TAG, "First 80 chars: ${pemString.take(80)}")
                return pemString
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export public key", e)
                throw Exception("Không thể export public key: ${e.message}", e)
            }
        }

        /**
         * Xóa key khỏi Keystore
         * Dùng khi logout hoặc trước khi tạo key mới
         */
        fun clearKeys() {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)

                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                    Log.d(TAG, "Deleted existing key pair from keystore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear keys", e)
                // Không throw exception vì đây chỉ là cleanup
            }
        }

        /**
         * Kiểm tra xem key đã tồn tại chưa
         * 
         * @return true nếu key tồn tại
         */
        fun hasKey(): Boolean {
            return try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.containsAlias(KEY_ALIAS)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check key existence", e)
                false
            }
        }

        /**
         * Lấy fingerprint của public key (SHA-256 hash)
         * Dùng để debug/verify key
         * 
         * @return String hex của SHA-256 fingerprint
         */
        fun getPublicKeyFingerprint(): String? {
            return try {
                val publicKey = getPublicKey()
                val publicKeyBytes = publicKey.encoded
                
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(publicKeyBytes)
                
                // Convert to hex string
                hash.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get key fingerprint", e)
                null
            }
        }
    }
}
