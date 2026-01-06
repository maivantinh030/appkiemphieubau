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

class KeystoreManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "BallotSigningKey"
        private const val KEY_SIZE = 2048
        private const val TAG = "KeystoreManager"
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
                    setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1  // Fallback cho compatibility
                    )
                    setUserAuthenticationRequired(false)
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

    }
}
