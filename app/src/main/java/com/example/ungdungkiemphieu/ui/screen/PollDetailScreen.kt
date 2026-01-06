package com.example.ungdungkiemphieu.ui.screen



import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.ungdungkiemphieu.data.model.Poll
import com.example.ungdungkiemphieu.repository.PollRepository
import com.example.ungdungkiemphieu.ui.theme.AppColors
import com.example.ungdungkiemphieu.viewmodel.PollViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollDetailScreen(
    pollId: Int?,
    navController: NavController
) {
    val repository = remember { PollRepository() }
    val viewModel = remember { PollViewModel(repository) }
    val context = LocalContext.current


    Log.d("PollDetailScreen", "Screen created with pollId: $pollId")

    LaunchedEffect(pollId) {
        Log.d("PollDetailScreen", "LaunchedEffect triggered with pollId: $pollId")
        viewModel.loadPollById(pollId)
    }

    var showRoleDialog by remember { mutableStateOf(false) }
    val poll = viewModel.selectedPoll.collectAsState().value
    Log.d("PollDetailScreen", "Current poll state: $poll, pollId: $pollId")

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
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            PollDetailTopBar(
                onBackClick = { navController.popBackStack() },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Poll details section
                item {
                    PollDetailsSection(poll = poll)
                }

                // Vote results section
                item {
                    VoteResultsSection(poll = poll)
                }

                item {
                    ViewResultsButton(
                        onViewResults = {
                            navController.navigate("poll_result/$pollId")
                        }
                    )
                }
                item {
                    if(poll?.role == "manager" ||poll?.role == "checkin"){
                        VoterListButton(
                            onVoterListClick = {
                                navController.navigate("voter_list_simple/${pollId}")
                            }
                        )
                    }

                }
                item {
                    if (poll?.role != "manager") {
                        RequestRoleUpgradeButton(
                            currentRole = poll?.role,
                            requestedRole = poll?.requested_role_change,
                            onRequestClick = { showRoleDialog = true }
                        )
                    }
                }
                item {
                    if(poll?.role == "manager" || poll?.role == "operator"){
                        SummaryButton(
                            onSummaryClick = {
                                navController.navigate("pollSummary/${pollId}")
                            }
                        )
                    }

                }
                // Camera button (conditional for admin/checker)
                item {
                    if(poll?.role == "manager" ||poll?.role == "operator"){
                        CameraButton(
                            onCameraClick = {
                                navController.navigate("camera/${pollId}")
                            }
                        )
                    }

                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    if (showRoleDialog) {
        RoleSelectionDialog(
            currentRole = poll?.role ?: "user",
            onDismiss = { showRoleDialog = false },
            onRoleSelected = { selectedRole ->
                // TODO: Call API để request role upgrade
                viewModel.requestRoleUpgrade(pollId, selectedRole)
                showRoleDialog = false
                Toast.makeText(context,"Đã gửi yêu cầu thành công", Toast.LENGTH_SHORT).show()
            }
        )
    }

}

@Composable
fun PollDetailTopBar(
    onBackClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = AppColors.CardBackground,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.CardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Chi tiết cuộc bầu cử",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

    }
}
@Composable
fun PollDetailsSection(poll: Poll?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thông tin cuộc bầu cử",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                StatusBadge(status = poll?.status ?: "", membership_status = poll?.member_status)
            }

            HorizontalDivider(
                color = AppColors.TextSecondary.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Details
            DetailRow(
                label = "Tiêu đề",
                value = poll?.title ?: ""
            )

            DetailRow(
                label = "Mô tả",
                value = poll?.description ?: "Không có mô tả"
            )

            DetailRow(
                label = "Thời gian bắt đầu",
                value = formatDateTime(poll?.start_time)
            )

            DetailRow(
                label = "Thời gian kết thúc",
                value = formatDateTime(poll?.end_time)
            )

            DetailRow(
                label = "Người tạo",
                value = "Admin"
            )

            DetailRow(
                label = "Trạng thái",
                value = getStatusText(poll?.status ?: "")
            )
        }
    }
}

@Composable
fun VoteResultsSection(poll: Poll?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Kết quả kiểm phiếu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            HorizontalDivider(
                color = AppColors.TextSecondary.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Vote statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VoteStatCard(
                    title = "Tổng phiếu bầu",
                    value = poll?.total_ballots?.toString() ?: "0",
                    color = AppColors.BlueStatus
                )
                VoteStatCard(
                    title = "Đã kiểm phiếu",
                    value = poll?.checked_ballots?.toString() ?: "0",
                    color = AppColors.GreenStatus
                )
                VoteStatCard(
                    title = "Chưa kiểm",
                    value = (poll?.total_ballots?.minus(poll.checked_ballots ?: 0))?.toString() ?: "0",
                    color = Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
fun VoteStatCard(
    title: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .width(95.dp)
            .height(95.dp)
        ,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ViewResultsButton(
    onViewResults: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "results_button_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onViewResults() },
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Box {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.PinkGradientStart.copy(alpha = 0.1f),
                                AppColors.PinkGradientEnd.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Results icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.PinkGradientStart,
                                    AppColors.PinkGradientEnd
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Xem kết quả",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Xem chi tiết kiểm phiếu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppColors.PinkGradientStart,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryButton(
    onSummaryClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "summary_button_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onSummaryClick() },
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Box {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6200EE).copy(alpha = 0.1f),
                                Color(0xFF9C27B0).copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Summary icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF6200EE),
                                    Color(0xFF9C27B0)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = "Tổng kết",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Xem tổng kết ảnh đã chụp",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF6200EE),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
@Composable
fun CameraButton(
    onCameraClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "camera_button_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onCameraClick() },
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Box {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.OrangeGradientStart.copy(alpha = 0.1f),
                                AppColors.OrangeGradientEnd.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Camera icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Chụp ảnh kiểm phiếu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppColors.OrangeGradientStart,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RequestRoleUpgradeButton(
    currentRole: String?,
    requestedRole: String?,
    onRequestClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "role_upgrade_button_scale"
    )

    // Kiểm tra xem có đang chờ duyệt không
    val isPending = requestedRole != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = !isPending) {
                if (!isPending) onRequestClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Box {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isPending) {
                                listOf(
                                    Color(0xFFFFA726).copy(alpha = 0.1f),
                                    Color(0xFFFF9800).copy(alpha = 0.1f)
                                )
                            } else {
                                listOf(
                                    Color(0xFF00BCD4).copy(alpha = 0.1f),
                                    Color(0xFF0097A7).copy(alpha = 0.1f)
                                )
                            }
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isPending) {
                                    listOf(
                                        Color(0xFFFFA726),
                                        Color(0xFFFF9800)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF00BCD4),
                                        Color(0xFF0097A7)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPending) {
                            Icons.Default.HourglassEmpty
                        } else {
                            Icons.Default.Upgrade
                        },
                        contentDescription = "Yêu cầu nâng cấp role",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPending) {
                            "Đang chờ duyệt"
                        } else {
                            "Yêu cầu nâng cấp quyền"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )

                    if (isPending) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Yêu cầu nâng lên: ${getRoleDisplayName(requestedRole ?: "")}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                if (!isPending) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionDialog(
    currentRole: String,
    onDismiss: () -> Unit,
    onRoleSelected: (String) -> Unit
) {
    val availableRoles = remember {
        when (currentRole.lowercase()) {
            "user" -> listOf("checkin", "operator", "manager")
            "checkin" -> listOf("operator", "manager")
            "operator" -> listOf("manager")
            else -> emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chọn quyền muốn nâng cấp",
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quyền hiện tại: ${getRoleDisplayName(currentRole)}",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )

                HorizontalDivider(
                    color = AppColors.TextSecondary.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                availableRoles.forEach { role ->
                    RoleOptionCard(
                        role = role,
                        onClick = { onRoleSelected(role) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun RoleOptionCard(
    role: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        border = BorderStroke(1.dp, AppColors.CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Role icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = getRoleColor(role).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getRoleIcon(role),
                    contentDescription = null,
                    tint = getRoleColor(role),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getRoleDisplayName(role),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = getRoleDescription(role),
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun VoterListButton(
    onVoterListClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "voter_list_button_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onVoterListClick() },
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Box {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.1f),
                                Color(0xFF66BB6A).copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF66BB6A)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Checkin cử tri",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Checkin cử tri",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Helper functions
fun getRoleDisplayName(role: String): String {
    return when (role.lowercase()) {
        "user" -> "Thành viên"
        "checkin" -> "Check-in"
        "operator" -> "Operator"
        "manager" -> "Manager"
        else -> role
    }
}

fun getRoleDescription(role: String): String {
    return when (role.lowercase()) {
        "checkin" -> "Quyền check-in thành viên"
        "operator" -> "Quyền kiểm phiếu "
        "manager" -> "Quyền quản lý toàn bộ"
        else -> ""
    }
}

fun getRoleColor(role: String): Color {
    return when (role.lowercase()) {
        "checkin" -> Color(0xFF4CAF50)
        "operator" -> Color(0xFF2196F3)
        "manager" -> Color(0xFFE91E63)
        else -> Color.Gray
    }
}

fun getRoleIcon(role: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (role.lowercase()) {
        "checkin" -> Icons.Default.Check
        "operator" -> Icons.Default.Settings
        "manager" -> Icons.Default.AdminPanelSettings
        else -> Icons.Default.Person
    }
}

// Helper functions (same as original)
fun formatDateTime(dateTime: String?): String {
    // Implementation for formatting datetime
    return dateTime ?: "Chưa xác định"
}

fun getStatusText(status: String): String {
    return when (status.lowercase()) {
        "scheduled" -> "Chờ diễn ra"
        "ongoing" -> "Đang diễn ra"
        "closed" -> "Đã đóng"
        "counted" -> "Đã kiểm phiếu"
        "published" -> "Đã công bố"
        "locked" -> "Đã khoá"
        "cancelled" -> "Đã huỷ"
        else -> status
    }
}

@Preview(showBackground = true)
@Composable
fun PollDetailScreenPreview(){
    val navController = rememberNavController()
    PollDetailScreen(pollId = 1, navController = navController)
}
