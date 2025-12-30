package com.example.ungdungkiemphieu.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Helper class để mã hóa/giải mã dữ liệu sử dụng Android Keystore
 * Dùng để lưu mật khẩu an toàn cho biometric login
 */
class EncryptionHelper {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "BiometricLoginKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = "]"
        private val keyStore: KeyStore by lazy {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }
        }

        /**
         * Lấy hoặc tạo secret key từ Android Keystore
         */
        private fun getOrCreateSecretKey(): SecretKey {
            // Kiểm tra xem key đã tồn tại chưa
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                return entry.secretKey
            }

            // Tạo key mới
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        /**
         * Mã hóa chuỗi
         */
        fun encrypt(data: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            // Kết hợp IV và dữ liệu đã mã hóa
            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
            return "$ivString$IV_SEPARATOR$encryptedString"
        }

        /**
         * Giải mã chuỗi
         */
        fun decrypt(encryptedData: String): String {
            val parts = encryptedData.split(IV_SEPARATOR)
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid encrypted data format")
            }
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

            val decryptedData = cipher.doFinal(encrypted)
            return String(decryptedData, Charsets.UTF_8)
        }

        /**
         * Xóa key khỏi keystore
         */
        fun deleteKey() {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        }
    }
}
