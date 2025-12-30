package com.example.ungdungkiemphieu.ui.screen


// Colors.kt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
// MeetingAttendanceScreen.kt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*
import com.example.ungdungkiemphieu.repository.UserRepository
import com.example.ungdungkiemphieu.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.util.Calendar


@Composable
fun MeetingAttendanceScreen(
    navController: NavController
) {
    var activeCard by remember { mutableStateOf<String?>(null) }
    val greeting = remember { getGreeting() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    val authManager = remember { AuthManager(context) }

    var userName by remember { mutableStateOf("...") }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    // Lấy thông tin user khi screen được load
    LaunchedEffect(Unit) {
        scope.launch {
            userRepository.getCurrentUserSimple().fold(
                onSuccess = { userInfo ->
                    userName = userInfo.fullName ?: userInfo.username
                    isLoading = false
                },
                onFailure = {
                    userName = "Người dùng"
                    isLoading = false
                }
            )
        }
    }

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
                .padding(20.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        color = AppColors.TextTertiary
                    )
                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        AppColors.OrangeGradientStart,
                                        AppColors.OrangeGradientEnd
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showProfileMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 4.dp, y = (-4).dp)
                            .align(Alignment.TopEnd)
                            .background(AppColors.GreenStatus, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Hero section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()

            ) {
                // Main icon
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Main container
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = AppColors.CardBackground,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = AppColors.CardBorder,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vote icon representation
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
                    }

                    // Floating particles
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .offset(x = (-32).dp, y = (-32).dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFACC15), Color(0xFFF59E0B))
                                ),
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "Ứng dụng kiểm phiếu bầu",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Features row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureItem("Nhanh chóng", AppColors.GreenStatus)

                    FeatureItem("Chính xác", AppColors.BlueStatus)

                    FeatureItem("An toàn", AppColors.PinkGradientStart)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Action cards
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "Tham gia cuộc họp",
                    subtitle = "Nhập mã phòng để tham gia cuộc họp trực tuyến",
                    icon = Icons.Default.VideoCall,
                    gradientColors = listOf(AppColors.OrangeGradientStart, AppColors.OrangeGradientEnd),
                    isActive = activeCard == "join_poll",
                    activeCard = "join_poll",
                    navController = navController
                )

                ActionCard(
                    title = "Lịch sử tham gia",
                    subtitle = "Xem danh sách các cuộc họp đã tham gia",
                    icon = Icons.Default.History,
                    gradientColors = listOf(AppColors.PinkGradientStart, AppColors.PinkGradientEnd),
                    isActive = activeCard == "my_poll",
                    activeCard = "my_poll",
                    navController = navController
                )
            }
            Spacer(modifier = Modifier.weight(1f))


        }

        // Profile menu modal
        if (showProfileMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showProfileMenu = false }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = AppColors.CardBackground
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                AppColors.OrangeGradientStart,
                                                AppColors.OrangeGradientEnd
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = userName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "Thành viên",
                                    fontSize = 14.sp,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        HorizontalDivider(color = AppColors.CardBorder)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logout button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showProfileMenu = false
                                    showLogoutDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Đăng xuất",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Xác nhận đăng xuất",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?",
                        color = AppColors.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            authManager.logout()
                            showLogoutDialog = false
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        )
                    ) {
                        Text("Đăng xuất", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Hủy", color = AppColors.TextSecondary)
                    }
                },
                containerColor = AppColors.CardBackground
            )
        }
    }
}

@Composable
fun BackgroundDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Decorative blur circles
        Box(
            modifier = Modifier
                .size(256.dp)
                .offset(x = (-64).dp, y = (-64).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x28EC4899), // pink-200/40
                            Color(0x28FB923C), // orange-200/40
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(32.dp)
        )

        Box(
            modifier = Modifier
                .size(256.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 64.dp, y = 64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x28FDE047), // yellow-200/40
                            Color(0x28FB923C), // amber-200/40
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(32.dp)
        )
    }
}


@Composable
fun FeatureItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )
    }
}


@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    isActive: Boolean,
    activeCard: String ,
    navController: NavController
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                navController.navigate(activeCard)
            },
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.verticalGradient(gradientColors),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..10 -> "Chào buổi sáng"
        in 11..13 -> "Chào buổi trưa"
        in 14..17 -> "Chào buổi chiều"
        in 18..21 -> "Chào buổi tối"
        else -> "Chào buổi đêm"
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    MeetingAttendanceScreen(navController = rememberNavController())
}
