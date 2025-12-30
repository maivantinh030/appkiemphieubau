package com.example.ungdungkiemphieu.ui.screen


import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import com.example.ungdungkiemphieu.data.network.*
import com.example.ungdungkiemphieu.repository.*
import com.example.ungdungkiemphieu.ui.theme.AppColors
import com.example.ungdungkiemphieu.viewmodel.*

data class PasswordValidation(
    val hasMinLength: Boolean = false,
    val hasUpperCase: Boolean = false,
    val hasNumber: Boolean = false,
    val hasSpecialChar: Boolean = false
) {
    fun isValid() = hasMinLength && hasUpperCase && hasNumber && hasSpecialChar
}

fun validatePassword(password: String): PasswordValidation {
    return PasswordValidation(
        hasMinLength = password.length >= 6,
        hasUpperCase = password.any { it.isUpperCase() },
        hasNumber = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() }
    )
}

data class ValidationErrors(
    val usernameError: String? = null,
    val emailError: String? = null,
    val fullNameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

fun validateUsername(username: String): String? {
    return when {
        username.isBlank() -> "Tên đăng nhập không được để trống"
        username.length < 3 -> "Tên đăng nhập phải có ít nhất 3 ký tự"
        username.length > 20 -> "Tên đăng nhập không được quá 20 ký tự"
        !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Tên đăng nhập chỉ chứa chữ cái, số và dấu gạch dưới"
        else -> null
    }
}

fun validateEmail(email: String): String? {
    if (email.isEmpty()) return null // Email không bắt buộc
    return when {
        !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> "Email không hợp lệ"
        else -> null
    }
}
fun validateFullName(fullName: String): String? {
    return when {
        fullName.isBlank() -> "Họ và tên không được để trống"
        fullName.length < 2 -> "Họ và tên phải có ít nhất 2 ký tự"
        fullName.length > 50 -> "Họ và tên không được quá 50 ký tự"
        else -> null
    }
}


fun validateConfirmPassword(password: String, confirmPassword: String): String? {
    return when {
        confirmPassword.isBlank() -> "Vui lòng xác nhận mật khẩu"
        password != confirmPassword -> "Mật khẩu xác nhận không khớp"
        else -> null
    }
}

@Composable
fun SignupScreen(
    navController: NavController
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordValidation by remember { mutableStateOf(PasswordValidation()) }
    var validationErrors by remember { mutableStateOf(ValidationErrors()) }
    var touchedFields by remember { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val repository = remember { SignupRepository(authManager) }
    val viewModel = remember { SignupViewModel(repository) }
    val scrollState = rememberScrollState()

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
        BackgroundDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top bar with back button
            SignupTopBar(
                onBackClick = { navController.popBackStack() }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Logo section
                SignupLogoSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Signup form
                SignupForm(
                    username = username,
                    email = email,
                    fullName = fullName,
                    password = password,
                    confirmPassword = confirmPassword,
                    passwordVisible = passwordVisible,
                    confirmPasswordVisible = confirmPasswordVisible,
                    isLoading = isLoading,
                    passwordValidation = passwordValidation,
                    validationErrors = validationErrors,
                    onUsernameChange = { 
                        username = it
                        if (touchedFields.contains("username")) {
                            validationErrors = validationErrors.copy(usernameError = validateUsername(it))
                        }
                    },
                    onEmailChange = { 
                        email = it
                        if (touchedFields.contains("email")) {
                            validationErrors = validationErrors.copy(emailError = validateEmail(it))
                        }
                    },
                    onFullNameChange = { 
                        fullName = it
                        if (touchedFields.contains("fullName")) {
                            validationErrors = validationErrors.copy(fullNameError = validateFullName(it))
                        }
                    },
                    onPasswordChange = { 
                        password = it
                        passwordValidation = validatePassword(it)
                        if (touchedFields.contains("confirmPassword") && confirmPassword.isNotEmpty()) {
                            validationErrors = validationErrors.copy(
                                confirmPasswordError = validateConfirmPassword(it, confirmPassword)
                            )
                        }
                    },
                    onConfirmPasswordChange = { 
                        confirmPassword = it
                        if (touchedFields.contains("confirmPassword")) {
                            validationErrors = validationErrors.copy(
                                confirmPasswordError = validateConfirmPassword(password, it)
                            )
                        }
                    },
                    onUsernameFocusLost = {
                        touchedFields = touchedFields + "username"
                        validationErrors = validationErrors.copy(usernameError = validateUsername(username))
                    },
                    onEmailFocusLost = {
                        touchedFields = touchedFields + "email"
                        validationErrors = validationErrors.copy(emailError = validateEmail(email))
                    },
                    onFullNameFocusLost = {
                        touchedFields = touchedFields + "fullName"
                        validationErrors = validationErrors.copy(fullNameError = validateFullName(fullName))
                    },
                    onConfirmPasswordFocusLost = {
                        touchedFields = touchedFields + "confirmPassword"
                        validationErrors = validationErrors.copy(
                            confirmPasswordError = validateConfirmPassword(password, confirmPassword)
                        )
                    },
                    onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                    onConfirmPasswordVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
                    onSignup = {
                        // Validate tất cả các trường
                        val usernameError = validateUsername(username)
                        val emailError = validateEmail(email)
                        val fullNameError = validateFullName(fullName)
                        val confirmPasswordError = validateConfirmPassword(password, confirmPassword)
                        
                        validationErrors = ValidationErrors(
                            usernameError = usernameError,
                            emailError = emailError,
                            fullNameError = fullNameError,
                            confirmPasswordError = confirmPasswordError
                        )
                        
                        touchedFields = setOf("username", "email", "fullName", "confirmPassword")
                        
                        // Chỉ submit nếu tất cả validation đều pass
                        if (usernameError == null && emailError == null && fullNameError == null && confirmPasswordError == null && passwordValidation.isValid()) {
                            isLoading = true
                            viewModel.signup(
                                username = username,
                                email = email,
                                fullName = fullName,
                                password = password,
                                confirmPassword = confirmPassword
                            ) { success, message ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    Log.d("SignupScreen", "Signup failed: $message")
                                }
                            }
                        } else {
                            Toast.makeText(context, "Vui lòng kiểm tra lại thông tin đăng ký", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login link
                LoginSection(
                    onLoginClick = { navController.popBackStack() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SignupTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = AppColors.CardBackground.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = AppColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Tạo tài khoản mới",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
fun SignupLogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = AppColors.CardBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.CardBorder,
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(40.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppColors.OrangeGradientStart,
                                AppColors.OrangeGradientEnd
                            )
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = 6.dp, y = 6.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .border(2.dp, Color(0xFFFED7AA), RoundedCornerShape(2.dp))
                )
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = (-28).dp, y = (-28).dp)
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
                    .size(6.dp)
                    .offset(x = (28).dp, y = (28).dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Đăng ký tài khoản",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tạo tài khoản để sử dụng ứng dụng",
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
fun SignupForm(
    username: String,
    email: String,
    fullName: String,
    password: String,
    confirmPassword: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    isLoading: Boolean,
    passwordValidation: PasswordValidation,
    validationErrors: ValidationErrors,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onUsernameFocusLost: () -> Unit,
    onEmailFocusLost: () -> Unit,
    onFullNameFocusLost: () -> Unit,
    onConfirmPasswordFocusLost: () -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onConfirmPasswordVisibilityChange: () -> Unit,
    onSignup: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Username field
        Column {
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = {
                    Text(
                        text = "Tên đăng nhập *",
                        color = AppColors.TextSecondary
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = validationErrors.usernameError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.CardBackground,
                    unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                    focusedBorderColor = AppColors.OrangeGradientStart,
                    unfocusedBorderColor = AppColors.CardBorder,
                    errorBorderColor = Color(0xFFEF4444),
                    errorContainerColor = AppColors.CardBackground,
                    focusedLabelColor = AppColors.OrangeGradientStart,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    errorLabelColor = Color(0xFFEF4444),
                    cursorColor = AppColors.OrangeGradientStart
                ),
                supportingText = validationErrors.usernameError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        Column {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = {
                    Text(
                        text = "Email (không bắt buộc)",
                        color = AppColors.TextSecondary
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = validationErrors.emailError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.CardBackground,
                    unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                    focusedBorderColor = AppColors.OrangeGradientStart,
                    unfocusedBorderColor = AppColors.CardBorder,
                    errorBorderColor = Color(0xFFEF4444),
                    errorContainerColor = AppColors.CardBackground,
                    focusedLabelColor = AppColors.OrangeGradientStart,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    errorLabelColor = Color(0xFFEF4444),
                    cursorColor = AppColors.OrangeGradientStart
                ),
                supportingText = validationErrors.emailError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full Name field
        Column {
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = {
                    Text(
                        text = "Họ và tên *",
                        color = AppColors.TextSecondary
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = validationErrors.fullNameError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.CardBackground,
                    unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                    focusedBorderColor = AppColors.OrangeGradientStart,
                    unfocusedBorderColor = AppColors.CardBorder,
                    errorBorderColor = Color(0xFFEF4444),
                    errorContainerColor = AppColors.CardBackground,
                    focusedLabelColor = AppColors.OrangeGradientStart,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    errorLabelColor = Color(0xFFEF4444),
                    cursorColor = AppColors.OrangeGradientStart
                ),
                supportingText = validationErrors.fullNameError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = {
                Text(
                    text = "Mật khẩu *",
                    color = AppColors.TextSecondary
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.CardBackground,
                unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                focusedBorderColor = AppColors.OrangeGradientStart,
                unfocusedBorderColor = AppColors.CardBorder,
                focusedLabelColor = AppColors.OrangeGradientStart,
                unfocusedLabelColor = AppColors.TextSecondary,
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

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm password field
        Column {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = {
                    Text(
                        text = "Xác nhận mật khẩu *",
                        color = AppColors.TextSecondary
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = validationErrors.confirmPasswordError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.CardBackground,
                    unfocusedContainerColor = AppColors.CardBackground.copy(alpha = 0.8f),
                    focusedBorderColor = AppColors.OrangeGradientStart,
                    unfocusedBorderColor = AppColors.CardBorder,
                    errorBorderColor = Color(0xFFEF4444),
                    errorContainerColor = AppColors.CardBackground,
                    focusedLabelColor = AppColors.OrangeGradientStart,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    errorLabelColor = Color(0xFFEF4444),
                    cursorColor = AppColors.OrangeGradientStart
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onConfirmPasswordVisibilityChange) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            tint = AppColors.TextSecondary
                        )
                    }
                },
                supportingText = validationErrors.confirmPasswordError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Password requirements
        if (password.isNotEmpty()) {
            PasswordRequirements(
                validation = passwordValidation,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "* Mật khẩu phải đáp ứng các yêu cầu bên dưới:",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Signup button
        SignupButton(
            onClick = onSignup,
            isLoading = isLoading,
            isEnabled = username.isNotBlank() && 
                       fullName.isNotBlank() &&
                       password.isNotBlank() && 
                       confirmPassword.isNotBlank() && 
                       passwordValidation.isValid() &&
                       validationErrors.usernameError == null &&
                       validationErrors.emailError == null &&
                       validationErrors.fullNameError == null &&
                       validationErrors.confirmPasswordError == null
        )
    }
}

@Composable
fun PasswordRequirements(
    validation: PasswordValidation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = "Yêu cầu mật khẩu:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        PasswordRequirementItem(
            text = "Ít nhất 6 ký tự",
            isMet = validation.hasMinLength
        )
        
        PasswordRequirementItem(
            text = "Có ít nhất 1 chữ hoa",
            isMet = validation.hasUpperCase
        )
        
        PasswordRequirementItem(
            text = "Có ít nhất 1 số",
            isMet = validation.hasNumber
        )
        
        PasswordRequirementItem(
            text = "Có ít nhất 1 ký tự đặc biệt",
            isMet = validation.hasSpecialChar
        )
    }
}

@Composable
fun PasswordRequirementItem(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (isMet) Color(0xFF10B981) else AppColors.TextSecondary.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isMet) Color(0xFF10B981) else AppColors.TextSecondary,
            fontWeight = if (isMet) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun SignupButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isEnabled: Boolean
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "signup_button_scale"
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
                        text = "Đang đăng ký...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "Đăng ký",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LoginSection(
    onLoginClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bạn đã có tài khoản?",
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onLoginClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = AppColors.OrangeGradientStart
            )
        ) {
            Text(
                text = "Đăng nhập ngay",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}