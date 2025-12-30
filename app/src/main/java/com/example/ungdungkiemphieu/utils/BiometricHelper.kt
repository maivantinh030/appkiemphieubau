package com.example.ungdungkiemphieu.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val context: Context) {

    /**
     * Kiểm tra xem thiết bị có hỗ trợ xác thực sinh trắc học không
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // Thiết bị không có phần cứng sinh trắc học
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Phần cứng sinh trắc học không khả dụng
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Người dùng chưa đăng ký sinh trắc học
                false
            }
            else -> false
        }
    }

    /**
     * Kiểm tra trạng thái chi tiết của biometric
     */
    fun getBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNKNOWN_ERROR
        }
    }

    /**
     * Thực hiện xác thực sinh trắc học
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Đăng nhập bằng vân tay",
        subtitle: String = "Sử dụng vân tay để đăng nhập nhanh",
        description: String? = null,
        negativeButtonText: String = "Hủy",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // Người dùng nhấn nút hủy
                            onError("Đã hủy xác thực")
                        }
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            onError("Người dùng đã hủy")
                        }
                        BiometricPrompt.ERROR_CANCELED -> {
                            onError("Xác thực bị hủy")
                        }
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            onError("Đã thử quá nhiều lần. Vui lòng thử lại sau")
                        }
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Đã khóa vĩnh viễn. Vui lòng sử dụng phương thức khác")
                        }
                        else -> {
                            onError(errString.toString())
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)

        description?.let {
            promptInfoBuilder.setDescription(it)
        }

        val promptInfo = promptInfoBuilder.build()
        biometricPrompt.authenticate(promptInfo)
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED,
    UNKNOWN_ERROR
}
