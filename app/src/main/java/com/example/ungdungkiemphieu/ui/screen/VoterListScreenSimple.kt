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
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * VoterListScreenSimple - Version không phân trang (dùng để test)
 * 
 * Version này tải TẤT CẢ cử tri một lần từ API endpoint /voters/all/
 * Không có lazy loading hoặc pagination
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoterListScreenSimple(
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
    val error = viewModel.error.collectAsState().value
    val totalCount = viewModel.totalCount.collectAsState().value

    // Local states
    var searchQuery by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterCheckedIn by remember { mutableStateOf<Boolean?>(null) }
    var showCheckinDialog by remember { mutableStateOf(false) }
    var selectedVoter by remember { mutableStateOf<Voter?>(null) }
    var isFirstLoad by remember { mutableStateOf(true) }

    // ========================================
    // PHIÊN BẢN KHÔNG CÓ DEBOUNCE (Đang dùng)
    // ========================================
    // Mỗi khi searchQuery thay đổi sẽ gọi API ngay lập tức
    // Có thể gây nhiều request không cần thiết khi người dùng đang gõ
    LaunchedEffect(pollId, searchQuery, filterCheckedIn) {
        if (pollId != null) {
            if (isFirstLoad) {
                Log.d("VoterListScreenSimple", " Tải tất cả cử tri lần đầu")
                viewModel.loadAllVoters(pollId)
                isFirstLoad = false
            } else {
                // Không có delay - gọi API ngay lập tức
                Log.d("VoterListScreenSimple", "[TEST - NO DEBOUNCE] Tìm kiếm ngay: '$searchQuery', filter: $filterCheckedIn")
                viewModel.loadAllVoters(
                    pollId = pollId,
                    search = searchQuery.ifBlank { null },
                    checkedInFilter = filterCheckedIn
                )
            }
        }
    }

    LaunchedEffect(pollId, searchQuery, filterCheckedIn) {
        if (pollId != null) {
            if (isFirstLoad) {
                viewModel.loadAllVoters(pollId)  // ← TẢI TẤT CẢ
            } else {
                viewModel.loadAllVoters(         // ← TẢI TẤT CẢ
                    pollId = pollId,
                    search = searchQuery.ifBlank { null },
                    checkedInFilter = filterCheckedIn
                )
            }
        }
    }
    // ========================================
    // PHIÊN BẢN CÓ DEBOUNCE (Comment để so sánh)
    // ========================================
    // Chờ 500ms sau khi người dùng ngừng gõ mới gọi API
    // Giảm số lượng request, tối ưu hơn
    /*
    LaunchedEffect(pollId, searchQuery, filterCheckedIn) {
        if (pollId != null) {
            if (isFirstLoad) {
                Log.d("VoterListScreenSimple", "[TEST] Tải tất cả cử tri lần đầu")
                viewModel.loadAllVoters(pollId)
                isFirstLoad = false
            } else {
                // Đợi 500ms trước khi gọi API (debounce)
                delay(500)
                Log.d("VoterListScreenSimple", "[TEST - WITH DEBOUNCE] Tìm kiếm sau 500ms: '$searchQuery', filter: $filterCheckedIn")
                viewModel.loadAllVoters(
                    pollId = pollId,
                    search = searchQuery.ifBlank { null },
                    checkedInFilter = filterCheckedIn
                )
            }
        }
    }
    */

    // Xử lý checkin dialog
    if (showCheckinDialog && selectedVoter != null) {
        if (selectedVoter!!.check_in_by != null) {
            // Undo Checkin Dialog
            AlertDialog(
                onDismissRequest = {
                    showCheckinDialog = false
                    selectedVoter = null
                },
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFA726)) },
                title = { Text("Hủy Check-in") },
                text = {
                    Column {
                        Text("Bạn có muốn hủy check-in cho cử tri này?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cử tri: ${selectedVoter!!.full_name}")
                        Text("Mã: ${selectedVoter!!.code_id}")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                        onClick = {
                            pollId?.let { pid ->
                                viewModel.undoCheckin(
                                    pollId = pid,
                                    voterId = selectedVoter!!.voter_id,
                                    onSuccess = {
                                        Toast.makeText(context, "Đã hủy check-in", Toast.LENGTH_SHORT).show()
                                        showCheckinDialog = false
                                        selectedVoter = null
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                        showCheckinDialog = false
                                        selectedVoter = null
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Xác nhận")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCheckinDialog = false
                        selectedVoter = null
                    }) {
                        Text("Hủy")
                    }
                }
            )
        } else {
            // Checkin Dialog
            AlertDialog(
                onDismissRequest = {
                    showCheckinDialog = false
                    selectedVoter = null
                },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
                title = { Text("Check-in") },
                text = {
                    Column {
                        Text("Xác nhận check-in cho cử tri này?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cử tri: ${selectedVoter!!.full_name}")
                        Text("Mã: ${selectedVoter!!.code_id}")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        onClick = {
                            pollId?.let { pid ->
                                viewModel.checkinVoter(
                                    pollId = pid,
                                    voterId = selectedVoter!!.voter_id,
                                    onSuccess = {
                                        Toast.makeText(context, "Check-in thành công", Toast.LENGTH_SHORT).show()
                                        showCheckinDialog = false
                                        selectedVoter = null
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                        showCheckinDialog = false
                                        selectedVoter = null
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Xác nhận")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCheckinDialog = false
                        selectedVoter = null
                    }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "[TEST] Danh sách cử tri",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Không phân trang - ${voters.size} cử tri",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.PrimaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Tìm kiếm theo tên hoặc mã...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (filterCheckedIn) {
                        true -> "Đã check-in"
                        false -> "Chưa check-in"
                        null -> "Tất cả"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppColors.PrimaryColor
                )

                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Lọc")
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tất cả") },
                            onClick = {
                                filterCheckedIn = null
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Đã check-in") },
                            onClick = {
                                filterCheckedIn = true
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Chưa check-in") },
                            onClick = {
                                filterCheckedIn = false
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && voters.isEmpty() -> {
                        // Loading ban đầu
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AppColors.PrimaryColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "[TEST] Đang tải tất cả cử tri...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    error != null && voters.isEmpty() -> {
                        // Hiển thị lỗi
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    error,
                                    color = Color.Red,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    pollId?.let {
                                        viewModel.loadAllVoters(it)
                                    }
                                }) {
                                    Text("Thử lại")
                                }
                            }
                        }
                    }
                    voters.isEmpty() -> {
                        // Không có dữ liệu
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Không có cử tri nào",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        // Hiển thị danh sách (KHÔNG CÓ PAGINATION)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
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

                            // Footer info
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE3F2FD)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = AppColors.PrimaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "[TEST] Đã tải tất cả ${voters.size} cử tri",
                                            color = AppColors.PrimaryColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

