package com.example.ungdungkiemphieu.ui.screen

import com.example.ungdungkiemphieu.data.network.AuthManager
import com.example.ungdungkiemphieu.repository.LoginRepository
import com.example.ungdungkiemphieu.ui.theme.AppColors
import com.example.ungdungkiemphieu.viewmodel.LoginViewModel
import com.example.ungdungkiemphieu.utils.BiometricHelper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController



@Composable
fun LoginScreen(
    navController: NavController
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val repository = remember { LoginRepository(authManager) }
    val viewModel = remember { LoginViewModel(repository) }
    val biometricHelper = remember { BiometricHelper(context) }
    val isBiometricAvailable = remember { biometricHelper.canAuthenticate() }
    var isBiometricEnabled by remember { mutableStateOf(viewModel.isBiometricEnabled()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.BackgroundStart,
                        AppColors.BackgroundMid,
                        AppColors.BackgroundEnd
                    )
                )
            )
    ) {
        // Background decorative elements
        BackgroundDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo section
            AppLogoSection()
            Spacer(modifier = Modifier.height(48.dp))

            // Login form
            LoginForm(
                username = username,
                password = password,
                passwordVisible = passwordVisible,
                isLoading = isLoading,
                usernameError = usernameError,
                passwordError = passwordError,
                onUsernameChange = { 
                    username = it
                    usernameError = null // Clear error when user types
                },
                onPasswordChange = { 
                    password = it
                    passwordError = null // Clear error when user types
                },
                onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                onLogin = {
                    // Validate inputs
                    val usernameValidation = validateLoginUsername(username)
                    val passwordValidation = validateLoginPassword(password)
                    
                    if (!usernameValidation.isValid) {
                        usernameError = usernameValidation.errorMessage
                    }
                    if (!passwordValidation.isValid) {
                        passwordError = passwordValidation.errorMessage
                    }
                    
                    // Only proceed if both validations pass
                    if (usernameValidation.isValid && passwordValidation.isValid) {
                        isLoading = true
                        viewModel.login(
                            username = username,
                            password = password
                        ) { success, message ->
                            isLoading = false
                            if (success) {
                                // Token đã được tự động lưu và mã hóa trong AuthManager
                                // Cập nhật trạng thái biometric
                                isBiometricEnabled = viewModel.isBiometricEnabled()
                                
                                Toast.makeText(
                                    context, 
                                    "Đăng nhập thành công! Vân tay đã được kích hoạt.", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                navController.navigate("home"){
                                    popUpTo("login") {
                                        inclusive = true
                                    }
                                }
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                Log.d("LoginScreen", "Login failed: $message")
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric Login Button - Hiển thị khi thiết bị hỗ trợ
            if (isBiometricAvailable) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = AppColors.CardBorder
                )
                
                BiometricLoginSection(
                    enabled = isBiometricEnabled,
                    onBiometricClick = {
                        if (isBiometricEnabled) {
                            biometricHelper.authenticate(
                                activity = context as FragmentActivity,
                                onSuccess = {
                                    isLoading = true
                                    viewModel.loginWithBiometric { success, message ->
                                        isLoading = false
                                        if (success) {
                                            navController.navigate("home") {
                                                popUpTo("login") {
                                                    inclusive = true
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Vui lòng đăng nhập bằng tài khoản/mật khẩu lần đầu để bật tính năng này", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Signup link
            SignupSection(
                onSignupClick = {
                    navController.navigate("signup")
                }
            )
        }
    }
}

@Composable
fun AppLogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App logo icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = AppColors.CardBackground,
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.CardBorder,
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Voting icon with same style as main screen
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(48.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppColors.OrangeGradientStart,
                                AppColors.OrangeGradientEnd
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 8.dp, y = 8.dp)
                        .background(Color.White, RoundedCornerShape(3.dp))
                        .border(2.dp, Color(0xFFFED7AA), RoundedCornerShape(3.dp))
                )
            }

            // Floating particles
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(x = (-32).dp, y = (-32).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFACC15),
                                Color(0xFFF59E0B)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(x = (32).dp, y = (32).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppColors.PinkGradientStart,
                                AppColors.PinkGradientEnd
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App title
        Text(
            text = "Ứng dụng kiểm phiếu bầu",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Đăng nhập để tiếp tục",
            fontSize = 16.sp,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
fun LoginForm(
    username: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    usernameError: String?,
    passwordError: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = {
                Text(
                    text = "Tài khoản",
                    color = AppColors.TextSecondary
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            isError = usernameError != null,
            supportingText = {
                if (usernameError != null) {
                    Text(
                        text = usernameError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.CardBackground,
                unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                focusedBorderColor = AppColors.OrangeGradientStart,
                unfocusedBorderColor = AppColors.CardBorder,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorContainerColor = AppColors.CardBackground,
                focusedLabelColor = AppColors.OrangeGradientStart,
                unfocusedLabelColor = AppColors.TextSecondary,
                errorLabelColor = MaterialTheme.colorScheme.error,
                cursorColor = AppColors.OrangeGradientStart
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = {
                Text(
                    text = "Mật khẩu",
                    color = AppColors.TextSecondary
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            isError = passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(
                        text = passwordError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.CardBackground,
                unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                focusedBorderColor = AppColors.OrangeGradientStart,
                unfocusedBorderColor = AppColors.CardBorder,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorContainerColor = AppColors.CardBackground,
                focusedLabelColor = AppColors.OrangeGradientStart,
                unfocusedLabelColor = AppColors.TextSecondary,
                errorLabelColor = MaterialTheme.colorScheme.error,
                cursorColor = AppColors.OrangeGradientStart
            ),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                        tint = AppColors.TextSecondary
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login button
        LoginButton(
            onClick = onLogin,
            isLoading = isLoading,
            isEnabled = username.isNotBlank() && password.isNotBlank()
        )
    }
}

@Composable
fun LoginButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isEnabled: Boolean
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "login_button_scale"
    )

    Surface(
        modifier = Modifier
            .width(200.dp)
            .height(56.dp)
            .scale(buttonScale)
            .clickable(enabled = isEnabled && !isLoading) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled && !isLoading) AppColors.OrangeGradientStart else AppColors.TextSecondary.copy(alpha = 0.3f),
        shadowElevation = if (isEnabled && !isLoading) 8.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isEnabled && !isLoading) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.OrangeGradientStart,
                                AppColors.OrangeGradientEnd
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.TextSecondary.copy(alpha = 0.3f),
                                AppColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Đang đăng nhập...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "Đăng nhập",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SignupSection(
    onSignupClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bạn chưa có tài khoản?",
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSignupClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = AppColors.OrangeGradientStart
            )
        ) {
            Text(
                text = "Đăng ký ngay",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BiometricLoginSection(
    enabled: Boolean,
    onBiometricClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Hoặc đăng nhập bằng",
            fontSize = 14.sp,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Biometric button
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onBiometricClick),
            shape = CircleShape,
            color = if (enabled) AppColors.CardBackground else AppColors.CardBackground.copy(alpha = 0.5f),
            shadowElevation = if (enabled) 4.dp else 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        1.dp, 
                        if (enabled) AppColors.OrangeGradientStart else AppColors.CardBorder.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Fingerprint icon
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Vân tay",
                    modifier = Modifier.size(36.dp),
                    tint = if (enabled) AppColors.OrangeGradientStart else AppColors.CardBorder.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (enabled) "Vân tay" else "Vân tay (chưa bật)",
            fontSize = 12.sp,
            color = if (enabled) AppColors.TextSecondary else AppColors.TextSecondary.copy(alpha = 0.5f)
        )
    }
}

// Validation data class
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

// Username validation function
fun validateLoginUsername(username: String): ValidationResult {
    return when {
        username.isBlank() -> ValidationResult(
            isValid = false,
            errorMessage = "Vui lòng nhập tài khoản"
        )
        username.length < 3 -> ValidationResult(
            isValid = false,
            errorMessage = "Tài khoản phải có ít nhất 3 ký tự"
        )
        username.length > 50 -> ValidationResult(
            isValid = false,
            errorMessage = "Tài khoản không được quá 50 ký tự"
        )
        !username.matches(Regex("^[a-zA-Z0-9._@-]+$")) -> ValidationResult(
            isValid = false,
            errorMessage = "Tài khoản chỉ được chứa chữ cái, số và các ký tự . _ @ -"
        )
        else -> ValidationResult(isValid = true)
    }
}

// Password validation function
fun validateLoginPassword(password: String): ValidationResult {
    return when {
        password.isBlank() -> ValidationResult(
            isValid = false,
            errorMessage = "Vui lòng nhập mật khẩu"
        )
        password.length < 6 -> ValidationResult(
            isValid = false,
            errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
        )
        password.length > 100 -> ValidationResult(
            isValid = false,
            errorMessage = "Mật khẩu không được quá 100 ký tự"
        )
        else -> ValidationResult(isValid = true)
    }
}