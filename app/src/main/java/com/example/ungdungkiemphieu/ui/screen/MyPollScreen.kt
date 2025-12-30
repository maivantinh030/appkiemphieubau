package com.example.ungdungkiemphieu.ui.screen

import com.example.ungdungkiemphieu.ui.theme.AppColors


import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ungdungkiemphieu.data.model.Poll
import com.example.ungdungkiemphieu.data.network.AuthManager
import com.example.ungdungkiemphieu.repository.*
import com.example.ungdungkiemphieu.viewmodel.*

private val PrimaryBlue = Color(0xFF7BB3D3)   // đúng màu nút login của mày
private val LightBg1 = Color(0xFFF8FBFF)
private val LightBg2 = Color(0xFFE3F0FF)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    CenterAlignedTopAppBar(
        title = { Text("Trang chủ", fontWeight = FontWeight.Medium) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White.copy(alpha = 0.85f),
            titleContentColor = Color(0xFF2D3748)
        ),
        actions = {
            IconButton(onClick = { /* Profile */ }) {
                Icon(Icons.Default.Person, contentDescription = "Tài khoản", tint = Color(0xFF4A5568))
            }
        }
    )
}

@Composable
fun BigActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = PrimaryBlue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(28.dp))
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun JoinPollScreen(navController: NavController) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val repository = remember { PollRepository() }
    val viewModel = remember { PollViewModel(repository) }
    val context = LocalContext.current

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
        // Background decorative elements - same as MeetingAttendanceScreen
        BackgroundDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top bar with back button
            JoinPollTopBar(
                onBackClick = { navController.popBackStack() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero icon - same style as main screen
                JoinPollHeroIcon()

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text(
                    text = "Tham gia cuộc bầu cử",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Nhập mã cuộc bầu cử để tham gia",
                    fontSize = 16.sp,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Input card - same style as other cards
                JoinPollInputCard(
                    textState = textState,
                    onValueChange = { input ->
                        if (input.composition != null) {
                            textState = input
                            return@JoinPollInputCard
                        }
                        var cleaned = input.text.uppercase().replace("[^A-Z0-9]".toRegex(), "")
                        if (cleaned.length > 9) cleaned = cleaned.substring(0, 9)
                        code = cleaned.chunked(3).joinToString("-")
                        textState = TextFieldValue(
                            text = code,
                            selection = TextRange(code.length)
                        )
                        error = false
                    },
                    isError = error,
                    isLoading = loading
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Join button - same style as other action cards
                JoinPollButton(
                    onClick = {
                        if (code.matches(Regex("^[A-Z0-9]{3}-[A-Z0-9]{3}-[A-Z0-9]{3}$"))) {
                            loading = true
                            viewModel.joinPoll(code)
                            navController.navigate("home")
                            Toast.makeText(context, "Đã gửi yêu cầu tham gia", Toast.LENGTH_SHORT).show()
                            loading = false
                        } else {
                            error = true
                        }
                    },
                    isLoading = loading,
                    isEnabled = code.isNotBlank()
                )

                Spacer(modifier = Modifier.weight(1f))
            }


            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun JoinPollTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Tham gia cuộc bầu cử",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.weight(1f))

        // Empty space to balance the layout
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
fun JoinPollHeroIcon() {
    Box(
        modifier = Modifier
            .size(88.dp)
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
        // Vote icon with same style as main screen
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
                    .border(1.5.dp, Color(0xFFFED7AA), RoundedCornerShape(2.dp))
            )
        }

        // Floating particles - same as main screen
        Box(
            modifier = Modifier
                .size(8.dp)
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
}

@Composable
fun JoinPollInputCard(
    textState: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isError: Boolean,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier.padding(28.dp)
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = onValueChange,
                label = {
                    Text(
                        text = "Mã cuộc bầu cử",
                        color = if (isError) MaterialTheme.colorScheme.error else AppColors.TextSecondary
                    )
                },
                placeholder = {
                    Text(
                        text = "VD: ABC-123-XYZ",
                        color = AppColors.TextSecondary.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                isError = isError,
                enabled = !isLoading,
                leadingIcon = {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else AppColors.TextSecondary
                    )
                },
                supportingText = if (isError) {
                    {
                        Text(
                            "Mã không hợp lệ. Vui lòng nhập đúng định dạng ABC-123-XYZ",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor = AppColors.OrangeGradientStart,
                    unfocusedBorderColor = AppColors.CardBorder,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedLabelColor = AppColors.OrangeGradientStart,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    cursorColor = AppColors.OrangeGradientStart
                )
            )
        }
    }
}

@Composable
fun JoinPollButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isEnabled: Boolean
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "button_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale)
            .clickable(enabled = isEnabled && !isLoading) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled && !isLoading) AppColors.OrangeGradientStart else AppColors.TextSecondary.copy(alpha = 0.3f)
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
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Đang kiểm tra...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tham gia ngay",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FBFF)
@Composable
fun JoinPollPreview() {
    MaterialTheme {
        JoinPollScreen(navController = rememberNavController())
    }
}
@Composable
fun MyPoll(
    navController: NavController,
    onPollClick: (Poll) -> Unit = {}
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val repository = remember { PollRepository() }
    val viewModel = remember { PollViewModel(repository) }
    LaunchedEffect(Unit) {
        viewModel.loadPolls()
    }
    val polls by viewModel.polls.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loading by viewModel.loading.collectAsState()
    Log.d("MyPoll", "MyPoll: $polls")


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
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top bar
            MyPollTopBar(
                onBackClick = { navController.popBackStack() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Content
            if(loading && polls.isEmpty()){
                // Loading initial data
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.OrangeGradientStart)
                }
            } else if(polls.isEmpty()){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ){
                    EmptyPollState()
                }
            }else{
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = polls.size,
                        key = { index -> polls[index].poll_id }
                    ) { index ->
                        val poll = polls[index]
                        
                        PollCard(
                            poll = poll,
                            onClick = {
                                if(poll.member_status != "active"){
                                    Toast.makeText(context, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show()
                                    return@PollCard
                                }
                                else{
                                    onPollClick(poll)
                                    navController.navigate("poll_detail/${poll.poll_id}")
                                    Log.d("MyPoll", "MyPoll: $poll")
                                }
                            }
                        )
                        
                        // Trigger load more when approaching the end
                        if (index >= polls.size - 3 && hasMore && !isLoadingMore) {
                            LaunchedEffect(Unit) {
                                viewModel.loadMorePolls()
                            }
                        }
                    }

                    // Loading indicator at bottom
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AppColors.OrangeGradientStart,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            // Poll list


        }
    }
}

@Composable
fun MyPollTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            text = "Cuộc bầu cử của tôi",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
fun EmptyPollState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bạn chưa tham gia cuộc họp nào",
            fontSize = 16.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PollCard(
    poll: Poll,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "poll_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status badge at the top
            StatusBadge(
                status = poll.status,
                membership_status = poll.member_status,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = poll.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            if (!poll.description.isNullOrBlank()) {
                Text(
                    text = poll.description!!,
                    fontSize = 15.sp,
                    color = AppColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    membership_status: String?,
    modifier: Modifier = Modifier
) {
    Log.d("StatusBadge", "Status: $status")
    Log.d("StatusBadge", "Membership Status: $membership_status")
    val (color, displayText) = if (membership_status == "pending") {
        Pair(Color(0xFF757575), "Chờ duyệt")
    } else if (membership_status == "rejected") {
        Pair(Color(0xFFF44336), "Từ chối")
    } else {
        // Nếu membership_status là "active" hoặc null → hiển thị trạng thái cuộc bầu cử
        when (status.lowercase()) {
            "scheduled" -> Pair(Color(0xFF2196F3), "Chờ diễn ra")
            "ongoing" -> Pair(Color(0xFF4CAF50), "Đang diễn ra")
            "closed" -> Pair(Color(0xFF757575), "Đã đóng")
            "counted" -> Pair(Color(0xFF9C27B0), "Đã kiểm phiếu")
            "published" -> Pair(Color(0xFF00BCD4), "Đã công bố")
            "locked" -> Pair(Color(0xFF795548), "Đã khoá")
            "cancelled" -> Pair(Color(0xFFF44336), "Đã huỷ")
            else -> Pair(Color(0xFF9E9E9E), status)
        }
    }

    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )

        Text(
            text = displayText,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FBFF)
@Composable
fun MyPollPreview() {
    MaterialTheme {
        MyPoll(navController = rememberNavController())
    }
}