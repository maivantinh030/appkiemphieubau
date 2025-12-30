package com.example.ungdungkiemphieu.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.AuthManager
import com.example.ungdungkiemphieu.data.network.RetrofitClient
import com.example.ungdungkiemphieu.repository.VoterRepository
import com.example.ungdungkiemphieu.ui.theme.AppColors
import com.example.ungdungkiemphieu.viewmodel.VoterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoterListScreen(
    pollId: Int?,
    navController: NavController
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val apiService = remember { RetrofitClient.apiService }
    val repository = remember { VoterRepository(apiService, authManager) }
    val viewModel = remember { VoterViewModel(repository) }

    // Collect states
    val voters = viewModel.voters.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val isLoadingMore = viewModel.isLoadingMore.collectAsState().value
    val error = viewModel.error.collectAsState().value
    val hasMore = viewModel.hasMore.collectAsState().value
    val totalCount = viewModel.totalCount.collectAsState().value

    // Local states
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterCheckedIn by remember { mutableStateOf<Boolean?>(null) }
    var showCheckinDialog by remember { mutableStateOf(false) }
    var selectedVoter by remember { mutableStateOf<Voter?>(null) }

    val listState = rememberLazyListState()

    // Tải dữ liệu ban đầu
    LaunchedEffect(pollId) {
        if (pollId != null) {
            viewModel.loadVoters(pollId, refresh = true)
        }
    }

    // Xử lý debounce riêng cho search query
    LaunchedEffect(searchQuery) {
        delay(500) // Debounce 500ms
        debouncedSearchQuery = searchQuery
    }

    // Xử lý tìm kiếm với query đã debounce
    LaunchedEffect(debouncedSearchQuery, filterCheckedIn) {
        if (pollId != null) {
            Log.d("VoterListScreen", "Tìm kiếm với query: '$debouncedSearchQuery', filter: $filterCheckedIn")
            viewModel.loadVoters(
                pollId = pollId,
                search = debouncedSearchQuery.ifBlank { null },
                checkedInFilter = filterCheckedIn,
                refresh = true
            )
        }
    }

    // Xử lý lazy loading - tải thêm dữ liệu khi scroll
    LaunchedEffect(listState, voters, hasMore, isLoadingMore, isLoading) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            Triple(lastVisibleIndex, layoutInfo.totalItemsCount, voters.size)
        }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems, votersSize) ->
                if (lastVisibleIndex != null && votersSize > 0) {
                    val shouldLoadMore = lastVisibleIndex >= votersSize - 5 &&
                            hasMore &&
                            !isLoadingMore &&
                            !isLoading

                    Log.d("VoterListScreen", "Scroll tracking: " +
                            "lastIndex=$lastVisibleIndex, " +
                            "votersSize=$votersSize, " +
                            "totalItems=$totalItems, " +
                            "hasMore=$hasMore, " +
                            "isLoadingMore=$isLoadingMore, " +
                            "isLoading=$isLoading, " +
                            "shouldLoad=$shouldLoadMore")

                    if (shouldLoadMore) {
                        Log.d("VoterListScreen", "Kích hoạt tải thêm dữ liệu")
                        viewModel.loadMore()
                    }
                }
            }
    }

    // Xử lý lỗi
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
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
        BackgroundDecorations()

        Column(modifier = Modifier.fillMaxSize()) {
            // Thanh điều hướng phía trên
            VoterListTopBar(
                voterCount = totalCount,
                onBackClick = { navController.popBackStack() },
                onFilterClick = { showFilterMenu = true }
            )

            // Thanh tìm kiếm
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                activeFilter = filterCheckedIn,
                isLoading = isLoading
            )

            // Danh sách cử tri
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = voters,
                        key = { it.voter_id }
                    ) { voter ->
                        VoterCard(
                            voter = voter,
                            onClick = {
                                selectedVoter = voter
                                showCheckinDialog = true
                            }
                        )
                    }

                    // Hiển thị loading khi tải thêm
                    if (isLoadingMore && voters.isNotEmpty()) {
                        item {
                            LoadingMoreIndicator()
                        }
                    }

                    // Thông báo kết thúc danh sách
                    if (!hasMore && voters.isNotEmpty() && !isLoading) {
                        item {
                            EndOfListMessage(totalCount = totalCount)
                        }
                    }
                }

                // Loading ban đầu
                if (isLoading && voters.isEmpty()) {
                    InitialLoadingIndicator()
                }

                // Trạng thái trống
                if (!isLoading && voters.isEmpty()) {
                    EmptyState(
                        hasFilter = searchQuery.isNotBlank() || filterCheckedIn != null
                    )
                }
            }
        }

        // Menu lọc
        FilterDropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false },
            currentFilter = filterCheckedIn,
            onFilterSelected = { newFilter ->
                filterCheckedIn = newFilter
                showFilterMenu = false
            }
        )

        // Dialog check-in
        if (showCheckinDialog && selectedVoter != null) {
            CheckinDialog(
                voter = selectedVoter!!,
                onDismiss = {
                    showCheckinDialog = false
                    selectedVoter = null
                },
                onConfirm = {
                    if (pollId != null) {
                        handleCheckinAction(
                            viewModel = viewModel,
                            pollId = pollId,
                            voter = selectedVoter!!,
                            context = context,
                            onComplete = {
                                showCheckinDialog = false
                                selectedVoter = null
                            }
                        )
                    }
                }
            )
        }
    }
}

// Các composable phụ trợ

@Composable
fun VoterListTopBar(
    voterCount: Int,
    onBackClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nút quay lại
        IconButton(
            onClick = onBackClick,
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
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Tiêu đề và số lượng
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Danh sách cử tri",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            if (voterCount > 0) {
                Text(
                    text = "Tổng: $voterCount cử tri",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }
        }

        // Nút lọc
        IconButton(
            onClick = onFilterClick,
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
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Lọc",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: Boolean?,
    isLoading: Boolean
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.CardBackground
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Tìm theo tên hoặc mã số...",
                            color = AppColors.TextSecondary
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary
                    ),
                    singleLine = true,
                    enabled = true
                )

                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AppColors.PinkGradientStart,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        // Hiển thị bộ lọc đang hoạt động
        if (activeFilter != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lọc: ${if (activeFilter) "Đã check-in" else "Chưa check-in"}",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun VoterCard(
    voter: Voter,
    onClick: () -> Unit
) {
    val backgroundColor = if (voter.has_checked_in) {
        AppColors.GreenStatus.copy(alpha = 0.1f)
    } else {
        AppColors.CardBackground
    }

    val borderColor = if (voter.has_checked_in) {
        AppColors.GreenStatus.copy(alpha = 0.3f)
    } else {
        AppColors.CardBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            VoterAvatar(voter = voter)

            Spacer(modifier = Modifier.width(12.dp))

            // Thông tin cử tri
            VoterInfo(
                voter = voter,
                modifier = Modifier.weight(1f)
            )

            // Icon hành động
            Icon(
                imageVector = if (voter.has_checked_in) {
                    Icons.Default.Undo
                } else {
                    Icons.Default.CheckCircleOutline
                },
                contentDescription = if (voter.has_checked_in) "Hủy check-in" else "Check-in",
                tint = if (voter.has_checked_in) {
                    AppColors.TextSecondary
                } else {
                    AppColors.BlueStatus
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun VoterAvatar(voter: Voter) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (voter.has_checked_in) {
                    AppColors.GreenStatus.copy(alpha = 0.2f)
                } else {
                    AppColors.BlueStatus.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (voter.has_checked_in) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.Person
            },
            contentDescription = null,
            tint = if (voter.has_checked_in) {
                AppColors.GreenStatus
            } else {
                AppColors.BlueStatus
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VoterInfo(
    voter: Voter,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = voter.full_name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Text(
            text = "Mã số: ${voter.code_id}",
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )

        if (voter.has_checked_in && voter.check_in_time != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Check-in: ${formatCheckInTime(voter.check_in_time)}",
                fontSize = 12.sp,
                color = AppColors.GreenStatus
            )
        }
    }
}

@Composable
fun LoadingMoreIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppColors.PinkGradientStart,
                strokeWidth = 2.dp
            )
            Text(
                text = "Đang tải thêm...",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EndOfListMessage(totalCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Đã hiển thị tất cả $totalCount cử tri",
            color = AppColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
fun InitialLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AppColors.PinkGradientStart)
            Text(
                text = "Đang tải danh sách cử tri...",
                color = AppColors.TextSecondary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun EmptyState(hasFilter: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (hasFilter) Icons.Default.SearchOff else Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppColors.TextSecondary.copy(alpha = 0.5f)
            )
            Text(
                text = if (hasFilter) {
                    "Không tìm thấy cử tri phù hợp"
                } else {
                    "Chưa có cử tri nào"
                },
                color = AppColors.TextSecondary,
                fontSize = 16.sp
            )
            if (hasFilter) {
                Text(
                    text = "Hãy thử thay đổi từ khóa tìm kiếm hoặc bộ lọc",
                    color = AppColors.TextSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun FilterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentFilter: Boolean?,
    onFilterSelected: (Boolean?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(AppColors.CardBackground)
    ) {
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentFilter == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.PinkGradientStart
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Tất cả")
                }
            },
            onClick = { onFilterSelected(null) }
        )
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentFilter == false) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.PinkGradientStart
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Chưa check-in")
                }
            },
            onClick = { onFilterSelected(false) }
        )
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentFilter == true) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.PinkGradientStart
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Đã check-in")
                }
            },
            onClick = { onFilterSelected(true) }
        )
    }
}

@Composable
fun CheckinDialog(
    voter: Voter,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (voter.has_checked_in) "Hủy check-in?" else "Xác nhận check-in",
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Cử tri: ${voter.full_name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Mã số: ${voter.code_id}",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )

                if (voter.has_checked_in) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bạn có chắc muốn hủy check-in cho cử tri này?",
                        fontSize = 14.sp,
                        color = Color(0xFFF44336)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (voter.has_checked_in) {
                        Color(0xFFF44336)
                    } else {
                        AppColors.GreenStatus
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (voter.has_checked_in) "Hủy check-in" else "Check-in",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
}

// Hàm xử lý hành động check-in
fun handleCheckinAction(
    viewModel: VoterViewModel,
    pollId: Int,
    voter: Voter,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    if (voter.has_checked_in) {
        // Hủy check-in
        viewModel.undoCheckin(
            pollId = pollId,
            voterId = voter.voter_id,
            onSuccess = {
                Toast.makeText(context, "Đã hủy check-in cho ${voter.full_name}", Toast.LENGTH_SHORT).show()
                onComplete()
            },
            onError = { errorMsg ->
                Toast.makeText(context, "Lỗi: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        // Check-in
        viewModel.checkinVoter(
            pollId = pollId,
            voterId = voter.voter_id,
            onSuccess = {
                Toast.makeText(context, "Check-in thành công cho ${voter.full_name}", Toast.LENGTH_SHORT).show()
                onComplete()
            },
            onError = { errorMsg ->
                Toast.makeText(context, "Lỗi: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Hàm hỗ trợ định dạng thời gian
fun formatCheckInTime(isoTime: String): String {
    return try {
        // Parse định dạng ISO: 2024-12-15T10:30:00
        val parts = isoTime.split("T")
        if (parts.size == 2) {
            val time = parts[1].substring(0, 5) // HH:mm
            time
        } else {
            isoTime
        }
    } catch (e: Exception) {
        isoTime
    }
}

// Component BackgroundDecorations (placeholder - cần định nghĩa trong project của bạn)