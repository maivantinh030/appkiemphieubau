package com.example.ungdungkiemphieu.ui.screen

import com.example.ungdungkiemphieu.ui.theme.AppColors
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.*
import com.example.ungdungkiemphieu.repository.*
import com.example.ungdungkiemphieu.viewmodel.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollSummaryScreen(pollId: Int?, navController: NavController) {
    val context = LocalContext.current
    val storage = remember { LocalBallotStorage(context) }
    val scope = rememberCoroutineScope()

    val authManager = remember { AuthManager(context) }
    val apiService = remember { RetrofitClient.apiService }
    val uploadRepository = remember { UploadRepository(apiService, authManager) }
    val uploadViewModel = remember { UploadViewModel(uploadRepository, storage) }

    var isUploading by remember { mutableStateOf(false) }
    var isScheduling by remember { mutableStateOf(false) }
    var selectedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedImageType by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Trigger state to refresh gallery and stats
    var refreshTrigger by remember { mutableStateOf(0) }

    var galleryTab by remember {
        mutableStateOf(
            when {
                storage.getPendingImages(pollId).isNotEmpty() -> "pending"
                else -> "uploaded"
            }
        )
    }

    val pendingCount by remember(refreshTrigger) { mutableStateOf(storage.getPendingCount(pollId)) }
    val uploadedCount by remember(refreshTrigger) { mutableStateOf(storage.getUploadedCount(pollId)) }
    val total by remember(refreshTrigger) { mutableStateOf(storage.getTotalCount(pollId)) }

    val pendingImages = storage.getPendingImages(pollId)
    val uploadedImages = storage.getUploadedImages(pollId)

    Log.d("PollSummaryScreen", "PollID: $pollId - Refresh: $refreshTrigger")

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

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết kiểm phiếu",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Tiêu đề
                item {
                    Text(
                        text = "Thống kê ảnh đã chụp",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Thẻ thống kê
                item {
                    StatsCardsSection(
                        uploadedCount = uploadedCount,
                        pendingCount = pendingCount,
                        total = total
                    )
                }

                // Gallery (nếu có ảnh)
                if (pendingImages.isNotEmpty() || uploadedImages.isNotEmpty()) {
                    item {
                        SimpleImprovedImagesGalleryCard(
                            pendingImages = pendingImages,
                            uploadedImages = uploadedImages,
                            pendingCount = pendingCount,
                            uploadedCount = uploadedCount,
                            pollId = pollId,
                            storage = storage,
                            selectedTab = galleryTab,
                            onTabChange = { galleryTab = it },
                            onImageClick = { bitmap, type, fileName ->
                                selectedImageBitmap = bitmap
                                selectedImageType = type
                                selectedFileName = fileName
                            }
                        )
                    }
                }

                // Nút chụp tiếp
                item {
                    ContinueCameraButton(
                        onClick = {
                            navController.navigate("camera/$pollId") {
                                popUpTo("pollSummary/$pollId") { inclusive = true }
                            }
                        }
                    )
                }


                // Upload nền (WorkManager)
                item {
                    UploadAllBackgroundButton(
                        pendingCount = pendingCount,
                        isScheduling = isScheduling,
                        onClick = {
                            if (pendingCount > 0 && !isScheduling) {
                                if (pollId == null) {
                                    Toast.makeText(context, "Thiếu pollId để upload nền", Toast.LENGTH_SHORT).show()
                                } else {
                                    isScheduling = true
                                    uploadViewModel.scheduleUploadAllPendingImages(
                                        context = context,
                                        pollId = pollId,
                                        batchSize = 3
                                    )
                                    isScheduling = false
                                    Toast.makeText(context, "Đã lên lịch upload nền", Toast.LENGTH_SHORT).show()
                                }
                            } else if (pendingCount == 0) {
                                Toast.makeText(context, "Không có ảnh nào cần upload", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Banner trạng thái WorkManager
                item {
                    BackgroundUploadStatus(
                        pollId = pollId,
                        uploadViewModel = uploadViewModel,
                        onSucceeded = {
                            refreshTrigger++
                            if (storage.getPendingCount(pollId) == 0) {
                                galleryTab = "uploaded"
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        // Image preview dialog
        if (selectedImageBitmap != null) {
            ImagePreviewDialog(
                bitmap = selectedImageBitmap!!,
                imageType = selectedImageType ?: "unknown",
                fileName = selectedFileName,
                pollId = pollId,
                storage = storage,
                onDismiss = {
                    selectedImageBitmap = null
                    selectedImageType = null
                    selectedFileName = null
                },
                onDeleted = {
                    refreshTrigger++
                    selectedImageBitmap = null
                    selectedImageType = null
                    selectedFileName = null
                }
            )
        }
    }
}

// === PHẦN GALLERY MỚI - ĐƠN GIẢN ===
@Composable
fun SimpleImprovedImagesGalleryCard(
    pendingImages: List<PendingBallotInfo>,
    uploadedImages: List<UploadedBallotInfo>,
    pendingCount: Int,
    uploadedCount: Int,
    pollId: Int?,
    storage: LocalBallotStorage,
    selectedTab: String,
    onTabChange: (String) -> Unit,
    onImageClick: (android.graphics.Bitmap, String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Ảnh kiểm phiếu",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            // Tab selector
            SimpleTabSelector(
                selectedTab = selectedTab,
                onTabChange = onTabChange,
                pendingCount = pendingCount,
                uploadedCount = uploadedCount
            )

            // Images content
            val currentImages: List<Any> = when (selectedTab) {
                "pending" -> pendingImages
                "uploaded" -> uploadedImages
                else -> emptyList()
            }

            if (currentImages.isEmpty()) {
                SimpleEmptyState(selectedTab = selectedTab)
            } else {
                SimpleImagesGrid(
                    images = currentImages,
                    pollId = pollId,
                    storage = storage,
                    type = selectedTab,
                            onImageClick = { bitmap, fileName -> onImageClick(bitmap, selectedTab, fileName) }
                )
            }
        }
    }
}

@Composable
fun SimpleTabSelector(
    selectedTab: String,
    onTabChange: (String) -> Unit,
    pendingCount: Int,
    uploadedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF8FAFC),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Pending tab
        SimpleTabItem(
            text = "Chờ upload ($pendingCount)",
            isSelected = selectedTab == "pending",
            color = Color(0xFFFF9800),
            onClick = { onTabChange("pending") },
            modifier = Modifier.weight(1f)
        )

        // Uploaded tab
        SimpleTabItem(
            text = "Đã upload ($uploadedCount)",
            isSelected = selectedTab == "uploaded",
            color = AppColors.GreenStatus,
            onClick = { onTabChange("uploaded") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SimpleTabItem(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) color else AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleEmptyState(selectedTab: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(
                Color(0xFFF9FAFB),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (selectedTab == "pending")
                    Icons.Default.PhotoCamera
                else
                    Icons.Default.CloudDone,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = AppColors.TextSecondary.copy(alpha = 0.5f)
            )

            Text(
                text = if (selectedTab == "pending")
                    "Chưa có ảnh chờ upload"
                else
                    "Chưa có ảnh đã upload",
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleImagesGrid(
    images: List<Any>,
    pollId: Int?,
    storage: LocalBallotStorage,
    type: String,
    onImageClick: (android.graphics.Bitmap, String) -> Unit
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
    ) {
        items(images) { imageInfo ->
            val fileName = when (imageInfo) {
                is PendingBallotInfo -> imageInfo.fileName
                is UploadedBallotInfo -> imageInfo.fileName
                else -> return@items
            }

            val file = when (type) {
                "pending", "uploaded" -> {
                    val pollDir = java.io.File(context.filesDir, "ballots/poll_$pollId")
                    java.io.File(pollDir, fileName)
                }
                else -> null
            }

            if (file != null && file.exists()) {
                SimpleImageThumbnail(
                    file = file,
                    type = type,
                    onClick = {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            onImageClick(bitmap, fileName)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SimpleImageThumbnail(
    file: java.io.File,
    type: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Box {
            val bitmap = remember {
                BitmapFactory.decodeFile(file.absolutePath)
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Status badge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (type == "uploaded")
                            AppColors.GreenStatus
                        else
                            Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = if (type == "uploaded")
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.padding(3.dp),
                            tint = Color.White
                        )
                    }
                }
            } else {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Lỗi",
                            color = AppColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// === CÁC COMPONENT GỐC GIỮ NGUYÊN ===

@Composable
fun StatsCardsSection(
    uploadedCount: Int,
    pendingCount: Int,
    total: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Chi tiết",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            HorizontalDivider(
                color = AppColors.TextSecondary.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Đã upload",
                    value = uploadedCount.toString(),
                    color = AppColors.GreenStatus
                )
                StatCard(
                    title = "Đang chờ",
                    value = pendingCount.toString(),
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .width(95.dp)
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ContinueCameraButton(
    onClick: () -> Unit
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
            .clickable { onClick() },
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
                        contentDescription = "Chụp tiếp",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Chụp thêm phiếu bầu",
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
fun UploadAllButton(
    pendingCount: Int,
    isUploading: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "upload_button_scale"
    )

    val isEnabled = pendingCount > 0 && !isUploading

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = isEnabled) { onClick() },
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
                                AppColors.PinkGradientStart.copy(alpha = if (isEnabled) 0.1f else 0.05f),
                                AppColors.PinkGradientEnd.copy(alpha = if (isEnabled) 0.1f else 0.05f)
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
                // Upload icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isEnabled) {
                                    listOf(
                                        AppColors.PinkGradientStart,
                                        AppColors.PinkGradientEnd
                                    )
                                } else {
                                    listOf(
                                        AppColors.TextSecondary.copy(alpha = 0.3f),
                                        AppColors.TextSecondary.copy(alpha = 0.3f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isUploading) "Đang upload..." else "Upload tất cả ảnh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEnabled) AppColors.TextPrimary else AppColors.TextSecondary
                    )
                    if (pendingCount > 0) {
                        Text(
                            text = "$pendingCount ảnh đang chờ",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isEnabled) {
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
}

@Composable
fun ImagePreviewDialog(
    bitmap: android.graphics.Bitmap,
    imageType: String,
    fileName: String?,
    pollId: Int?,
    storage: LocalBallotStorage,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .clickable(enabled = false) { },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (imageType) {
                            "pending" -> "Ảnh chờ upload"
                            "uploaded" -> "Ảnh đã upload"
                            else -> "Chi tiết ảnh"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nút xóa chỉ hiển thị khi là pending
                        if (imageType == "pending" && fileName != null && pollId != null) {
                            IconButton(
                                onClick = { showDeleteConfirm = true }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa ảnh",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        IconButton(onClick = { onDismiss() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Image
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Full preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog xác nhận xóa
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Xóa ảnh",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa ảnh này không?",
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName != null && pollId != null) {
                            val success = storage.deletePendingImage(pollId, fileName)
                            if (success) {
                                Toast.makeText(context, "Đã xóa ảnh", Toast.LENGTH_SHORT).show()
                                showDeleteConfirm = false
                                onDeleted()
                            } else {
                                Toast.makeText(context, "Không thể xóa ảnh", Toast.LENGTH_SHORT).show()
                                showDeleteConfirm = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Xóa",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.TextSecondary
                    )
                ) {
                    Text("Hủy")
                }
            },
            containerColor = AppColors.CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}


@Composable
fun UploadAllBackgroundButton(
    pendingCount: Int,
    isScheduling: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "upload_bg_button_scale"
    )

    val isEnabled = pendingCount > 0 && !isScheduling

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = isEnabled) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground
    ) {
        Box {
            // Gradient overlay (green theme)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.GreenStatus.copy(alpha = if (isEnabled) 0.12f else 0.06f),
                                AppColors.GreenStatus.copy(alpha = if (isEnabled) 0.12f else 0.06f)
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
                // Upload icon with green background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.GreenStatus,
                                    AppColors.GreenStatus
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScheduling) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload nền",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isScheduling) "Đang lên lịch..." else "Upload tất cả ảnh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEnabled) AppColors.TextPrimary else AppColors.TextSecondary
                    )
                    if (pendingCount > 0) {
                        Text(
                            text = "$pendingCount ảnh đang chờ",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AppColors.GreenStatus,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundUploadStatus(
    pollId: Int?, 
    uploadViewModel: UploadViewModel,
    onSucceeded: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (pollId == null) return

    var state by remember { mutableStateOf<WorkInfo.State?>(null) }
    var uploaded by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var lastState by remember { mutableStateOf<WorkInfo.State?>(null) }

    LaunchedEffect(pollId) {
        val tag = "ballot_upload_${pollId}"
        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(tag)
            .observe(lifecycleOwner) { infos ->
                val info = infos.lastOrNull()
                if (info != null) {
                    state = info.state
                    val progress = info.progress
                    uploaded = progress.getInt("uploaded", uploaded)
                    total = progress.getInt("total", total)

                    if (state == WorkInfo.State.SUCCEEDED) {
                        // override by final output
                        uploaded = info.outputData.getInt("uploaded_count", uploaded)
                        if (lastState != WorkInfo.State.SUCCEEDED) {
                            onSucceeded()
                        }
                    }
                    lastState = state
                }
            }
    }

    if (state == null) return

    val (label, color) = when (state) {
        WorkInfo.State.RUNNING -> "Đang upload nền" to AppColors.GreenStatus
        WorkInfo.State.ENQUEUED -> "Đã lên lịch, chờ network" to AppColors.TextSecondary
        WorkInfo.State.SUCCEEDED -> "Upload nền hoàn tất" to AppColors.GreenStatus
        WorkInfo.State.FAILED -> "Upload nền thất bại" to Color(0xFFFF5252)
        WorkInfo.State.CANCELLED -> "Upload bị hủy" to AppColors.TextSecondary
        else -> "" to AppColors.TextSecondary
    }

    val percent = if (total > 0) (uploaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (total > 0) {
                        Text(
                            text = "${(percent * 100).toInt()}%",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                    // Nút hủy khi đang chạy hoặc đang chờ
                    if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                        TextButton(
                            onClick = {
                                uploadViewModel.cancelUpload(context, pollId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hủy upload",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hủy",
                                fontSize = 14.sp,
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (total > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$uploaded/$total ảnh",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                    if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                        CircularProgressIndicator(
                            color = color,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = percent,
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = AppColors.TextSecondary.copy(alpha = 0.15f)
                )
            } else {
                if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                    CircularProgressIndicator(
                        color = color,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}