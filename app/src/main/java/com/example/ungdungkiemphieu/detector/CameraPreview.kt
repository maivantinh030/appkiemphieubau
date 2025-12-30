package com.example.ungdungkiemphieu.detector

import androidx.compose.foundation.layout.size
import com.example.ungdungkiemphieu.ui.theme.AppColors
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Trace
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.ApiService
import com.example.ungdungkiemphieu.data.network.AuthManager
import com.example.ungdungkiemphieu.data.network.RetrofitClient
import com.example.ungdungkiemphieu.repository.UploadRepository
import com.example.ungdungkiemphieu.ui.screen.BackgroundDecorations
import com.example.ungdungkiemphieu.ui.theme.AppColors.GreenEnd
import com.example.ungdungkiemphieu.ui.theme.AppColors.GreenStart
import com.example.ungdungkiemphieu.viewmodel.UploadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.io.ByteArrayOutputStream



@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    onCameraController: (LifecycleCameraController) -> Unit = {},
    pollId: Int?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val localStorage = remember { LocalBallotStorage(context) }
    val detectionScope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }
    val apiService = remember { RetrofitClient.apiService }

    // Lưu zoom level để duy trì qua các lần chụp
    val zoomLevel = remember { 0.5f } // 50% zoom

    val cropper = remember { ArUcoDocumentCropper() }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.IMAGE_ANALYSIS
            )
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    if (!hasPermission) {
        CameraPermissionScreen(navController = navController)
        return
    }

    var detectedMarkers by remember { mutableStateOf<List<String>>(emptyList()) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var hasValidMarkers by remember { mutableStateOf(false) }
    var totalMarkersFound by remember { mutableStateOf(0) }
    var processingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<VerifyHmacResponse?>(null) }

    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onCameraController(cameraController)

        onDispose {
            cameraController.unbind()
            cameraExecutor.shutdown()
        }
    }

    // Áp dụng zoom mỗi khi quay lại camera view
    androidx.compose.runtime.LaunchedEffect(capturedBitmap, processingBitmap) {
        if (capturedBitmap == null && processingBitmap == null) {
            // Chỉ set zoom khi đang ở camera view (không ở preview/processing)
            cameraController.setLinearZoom(zoomLevel)
        }
    }

    when {
        capturedBitmap != null -> {
            ImagePreviewScreen(
                bitmap = capturedBitmap!!,
                detectedMarkers = detectedMarkers,
                pollId = pollId,
                hasValidMarkers = hasValidMarkers,
                totalMarkersFound = totalMarkersFound,
                verifyResult = verifyResult,
                onRetake = {
                    capturedBitmap = null
                    detectedMarkers = emptyList()
                    hasValidMarkers = false
                    totalMarkersFound = 0
                    processingBitmap = null
                    isProcessing = false
                    verifyResult = null
                },
                onConfirm = {
                    // Chỉ cho phép lưu nếu có đủ markers
                    if (!hasValidMarkers) {
                        Toast.makeText(
                            context,
                            "⚠️ Không đủ markers! Cần 1 QR + 3 ArUco markers",
                            Toast.LENGTH_LONG
                        ).show()
                        return@ImagePreviewScreen
                    }

                    // Kiểm tra verify result nếu có
                    if (verifyResult != null && !verifyResult!!.verified) {
                        Toast.makeText(
                            context,
                            "❌ Phiếu bầu không hợp lệ! HMAC signature không đúng.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@ImagePreviewScreen
                    }

                    if (pollId == null) {
                        Toast.makeText(context, "Lỗi: Không có poll ID", Toast.LENGTH_SHORT).show()
                        return@ImagePreviewScreen
                    }

                    // Kiểm tra ballot đã được chụp chưa (nếu có ballotId từ verify)
                    val ballotId = verifyResult?.ballot_id
                    if (ballotId != null && localStorage.isBallotAlreadyProcessed(pollId, ballotId)) {
                        Toast.makeText(
                            context,
                            "⚠️ Phiếu bầu này đã được chụp trước đó!\nBallot ID: $ballotId",
                            Toast.LENGTH_LONG
                        ).show()
                        return@ImagePreviewScreen
                    }

                    // Lưu ảnh vào file + thêm vào danh sách pending
                    localStorage.savePendingImage(pollId, capturedBitmap!!, detectedMarkers, ballotId)

                    Toast.makeText(
                        context,
                        "✅ Đã lưu ảnh tạm thành công!\n" +
                                "Đang chờ: ${localStorage.getPendingCount(pollId)} ảnh\n" +
                                "Đã upload: ${localStorage.getUploadedCount(pollId)} ảnh",
                        Toast.LENGTH_LONG
                    ).show()

                    // Quay lại màn hình chụp
                    capturedBitmap = null
                    detectedMarkers = emptyList()
                    hasValidMarkers = false
                    totalMarkersFound = 0
                    processingBitmap = null
                    isProcessing = false
                }
            )
        }
        processingBitmap != null && isProcessing -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = processingBitmap!!.asImageBitmap(),
                    contentDescription = "Ảnh đang quét",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AppColors.OrangeGradientStart)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Đang quét...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        else -> {
            CameraViewScreen(
                cameraController = cameraController,
                isCapturing = isCapturing,
                onCapture = {
                    if (!isCapturing) {
                        isCapturing = true
                        isProcessing = true
                        takePhoto(
                            controller = cameraController,
                            cropper = cropper,
                            onPhotoTaken = { bitmap, markers, isValid, totalFound, verify ->
                                // Chỉ set capturedBitmap, để when condition tự chuyển
                                // Không reset processingBitmap và isProcessing ở đây
                                capturedBitmap = bitmap
                                detectedMarkers = markers
                                hasValidMarkers = isValid
                                totalMarkersFound = totalFound
                                verifyResult = verify
                                isCapturing = false
                                // Sẽ reset khi user retake hoặc confirm
                            },
                            onProcessing = { bitmap ->
                                processingBitmap = bitmap
                                isProcessing = true
                            },
                            context = context,
                            scope = detectionScope,
                            pollId = pollId,
                            apiService = apiService,
                            authManager = authManager
                        )
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun CameraPermissionScreen(navController: NavController) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Camera icon
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
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = AppColors.OrangeGradientStart
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cần quyền truy cập camera",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ứng dụng cần quyền truy cập camera để chụp ảnh phiếu bầu",
                fontSize = 16.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.OrangeGradientStart
                )
            ) {
                Text(
                    text = "Quay lại",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CameraViewScreen(
    cameraController: LifecycleCameraController,
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background màu đen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Camera Preview - chỉ hiển thị trong khung guide
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            )
        }

        // Camera frame border overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            ) {
                EnhancedBallotFrame()
            }
        }

        // Top bar
        CameraTopBar(
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Instructions
        CameraInstructions(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Capture button
        CameraCaptureButton(
            isCapturing = isCapturing,
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        )
    }
}

@Composable
fun CameraBlurOverlay() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate frame dimensions (same as in the frame guide)
        val frameMargin = 32.dp.toPx() * 2 // total horizontal margin
        val frameWidth = canvasWidth - frameMargin
        val frameHeight = frameWidth * 4f / 3f // 3:4 aspect ratio

        // Center position
        val frameLeft = (canvasWidth - frameWidth) / 2
        val frameTop = (canvasHeight - frameHeight) / 2

        // Draw dark overlay everywhere first
        drawRect(
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
            topLeft = Offset.Zero,
            size = size
        )

        // Cut out the center frame area (makes it transparent)
        drawRoundRect(
            color = androidx.compose.ui.graphics.Color.Transparent,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(20.dp.toPx()),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
fun CameraTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Chụp ảnh kiểm phiếu",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun CameraInstructions(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Đưa phiếu bầu vào khung hướng dẫn",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun BoxScope.EnhancedBallotFrame() {
    val cornerLength = 40.dp
    val cornerWidth = 4.dp
    val cornerRadius = 8.dp

    // Frame border
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,

                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GreenStart.copy(alpha = 0.8f),
                        GreenEnd.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    )

    // Enhanced corner brackets
    // Top-left corner
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(cornerLength)
                .height(cornerWidth)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        Box(
            modifier = Modifier
                .width(cornerWidth)
                .height(cornerLength)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }

    // Top-right corner
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(cornerLength)
                .height(cornerWidth)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(cornerWidth)
                .height(cornerLength)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }

    // Bottom-left corner
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(cornerLength)
                .height(cornerWidth)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(cornerWidth)
                .height(cornerLength)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }

    // Bottom-right corner
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(cornerLength)
                .height(cornerWidth)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(cornerWidth)
                .height(cornerLength)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GreenStart, GreenEnd)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

@Composable
fun CameraCaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .border(
                    width = 3.dp,
                    color = Color.White,
                    shape = CircleShape
                )
        )

        // Inner button
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable(enabled = !isCapturing) { onClick() },
            shape = CircleShape,
            color = if (isCapturing) AppColors.TextSecondary else Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = AppColors.OrangeGradientStart,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        AppColors.OrangeGradientStart,
                                        AppColors.OrangeGradientEnd
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * ✅ PHIÊN BẢN TỐI UU - SỬ DỤNG COROUTINES
 * Dispatcher.Default giải phóng Main Thread
 *
 * Lợi ích: Giao diện mượt mà, người dùng có thể bấm nút "Hủy"
 * Main Thread rảnh rỗi (Waiting), không bị block
 * GPU Rendering báo màu XANH (Smooth)
 */
private fun takePhoto(
    controller: LifecycleCameraController,
    cropper: ArUcoDocumentCropper,
    onPhotoTaken: (Bitmap, List<String>, Boolean, Int, VerifyHmacResponse?) -> Unit,
    onProcessing: (Bitmap) -> Unit,
    context: Context,
    scope: CoroutineScope,
    pollId: Int?,
    apiService: ApiService,
    authManager: AuthManager
) {
    Log.d("PERF", "MODE: ${PerformanceConfig.mode}")

    // Switch giữa 2 phiên bản dựa trên config
    if (PerformanceConfig.USE_OPTIMIZED_VERSION) {
        takePhotoOptimized(controller, cropper, onPhotoTaken, onProcessing, context, scope, pollId, apiService, authManager)
    } else {
//        takePhotoBlocking(controller, cropper, onPhotoTaken, onProcessing, context)
    }
}

/**
 * ✅ IMPLEMENTATION: Phiên bản tối ưu
 */
private fun takePhotoOptimized(
    controller: LifecycleCameraController,
    cropper: ArUcoDocumentCropper,
    onPhotoTaken: (Bitmap, List<String>, Boolean, Int, VerifyHmacResponse?) -> Unit,
    onProcessing: (Bitmap) -> Unit,
    context: Context,
    scope: CoroutineScope,
    pollId: Int?,
    apiService: ApiService,
    authManager: AuthManager
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            @OptIn(ExperimentalGetImage::class)
            override fun onCaptureSuccess(image: ImageProxy) {
                // ⏱️ Bắt đầu đo thời gian
                val totalStartTime = System.currentTimeMillis()

                val original = image.toBitmap()
                image.close()

                // Hiển thị ảnh gốc ngay lập tức để người dùng thấy ảnh đã dừng
                onProcessing(original)

                scope.launch(Dispatchers.Default) {
                    try {
                        Log.d("PERF", "✅ OPTIMIZED: Xử lý TRÊN WORKER THREAD - BẮTĐẦU")
                        val processingStartTime = System.currentTimeMillis()
                        // Dò QR/ArUco trên ảnh gốc để tăng độ ổn định (OpenCV)
                        Trace.beginSection("Optimized:cropDocument")
                        val cropResult = cropper.cropDocument(original)
                        Trace.endSection()
                        val processingTime = System.currentTimeMillis() - processingStartTime
                        Log.d("PERF", "✅ OPTIMIZED: Xử lý markers: ${processingTime}ms")

                        val outputBitmap = cropResult.croppedBitmap

                        // Tạo danh sách thông tin markers
                        val markerInfo = mutableListOf<String>()

                        if (cropResult.hasQR) {
                            markerInfo.add("✓ QR Code${cropResult.qrData?.let { ": $it" } ?: ""}")
                        } else {
                            markerInfo.add("✗ QR Code (chưa phát hiện)")
                        }

                        val arUcoCount = cropResult.usedMarkerIds.size
                        cropResult.usedMarkerIds.forEach { id ->
                            markerInfo.add("✓ ArUco ID: $id")
                        }

                        if (arUcoCount < 3) {
                            markerInfo.add("✗ Cần thêm ${3 - arUcoCount} ArUco markers")
                        }

                        // Thêm thông tin tổng quan
                        val totalFound = cropResult.totalMarkersDetected
                        val isValid = cropResult.success && totalFound >= 4
                        val summary = if (isValid) {
                            "✅ Hợp lệ: $totalFound/4 markers"
                        } else {
                            "⚠️ Thiếu markers: $totalFound/4"
                        }
                        markerInfo.add(0, summary)

                        // Verify HMAC nếu có QR code và pollId
                        var verifyResponse: VerifyHmacResponse? = null
                        if (cropResult.hasQR && cropResult.qrData != null && pollId != null) {
                            try {
                                Log.d("VERIFY", "Parsing QR: ${cropResult.qrData}")
                                val qrCode = BallotQrCode.parse(cropResult.qrData!!)
                                
                                if (qrCode != null) {
                                    Log.d("VERIFY", "QR parsed: ballot_id=${qrCode.ballotId}, hmac=${qrCode.hmacSignature}")
                                    
                                    // Gọi API verify
                                    val response = apiService.verifyHmac(
                                        pollId = pollId,
                                        ballotId = qrCode.ballotId,
                                        request = VerifyHmacRequest(qrCode.hmacSignature)
                                    )
                                    
                                    if (response.isSuccessful && response.body() != null) {
                                        verifyResponse = response.body()
                                        Log.d("VERIFY", "Verify successful: verified=${verifyResponse?.verified}")
                                        
                                        // Thêm thông tin verify vào markerInfo
                                        if (verifyResponse?.verified == true) {
                                            markerInfo.add(1, "✅ Phiếu bầu hợp lệ (verified)")
                                        } else {
                                            markerInfo.add(1, "❌ Phiếu bầu không hợp lệ!")
                                        }
                                    } else {
                                        Log.e("VERIFY", "Verify failed: ${response.errorBody()?.string()}")
                                        markerInfo.add(1, "⚠️ Không thể xác minh phiếu")
                                    }
                                } else {
                                    Log.w("VERIFY", "Invalid QR format: ${cropResult.qrData}")
                                    markerInfo.add(1, "⚠️ QR code không đúng định dạng")
                                }
                            } catch (e: Exception) {
                                Log.e("VERIFY", "Verify error: ${e.message}", e)
                                markerInfo.add(1, "⚠️ Lỗi xác minh: ${e.message}")
                            }
                        }

                        // Dùng ảnh crop được nếu có, ngược lại trả về ảnh gốc
                        val finalBitmap = outputBitmap ?: original

                        val totalTime = System.currentTimeMillis() - totalStartTime
                        Log.d("PERF", "✅ OPTIMIZED: Tổng thời gian: ${totalTime}ms (Markers: ${processingTime}ms)")
                        Log.d("PERF", "✅ OPTIMIZED: Main Thread RẢNH RỖI - KẾT THÚC (Giao diện mượt mà!)")

                        withContext(Dispatchers.Main) {
                            onPhotoTaken(finalBitmap, markerInfo, isValid, totalFound, verifyResponse)
                        }
                    } catch (e: Exception) {
                        Log.e("Camera", "Photo processing failed: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            onPhotoTaken(
                                original,
                                listOf("⚠️ Lỗi xử lý ảnh: ${e.message ?: "không rõ"}"),
                                false,
                                0,
                                null
                            )
                        }
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${exception.message}")
                exception.printStackTrace()
            }
        }
    )
}

/**
 * ❌ PHIÊN BẢN CHƯA TỐI UU - BLOCK MAIN THREAD
 * Dùng để so sánh hiệu suất với phiên bản tối ưu
 *
 * Hiện tượng: Giao diện sẽ ĐỨNG HÌNH (Freeze) trong 500-1000ms
 * Main Thread bị block, không thể cuộn hay bấm nút khác
 * GPU Rendering sẽ báo màu ĐỎ (Janky)
 */
private fun takePhotoBlocking(
    controller: LifecycleCameraController,
    cropper: ArUcoDocumentCropper,
    onPhotoTaken: (Bitmap, List<String>, Boolean, Int) -> Unit,
    onProcessing: (Bitmap) -> Unit,
    context: Context
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            @OptIn(ExperimentalGetImage::class)
            override fun onCaptureSuccess(image: ImageProxy) {
                val totalStartTime = System.currentTimeMillis()

                val original = image.toBitmap()
                image.close()
                onProcessing(original)

                // ❌ ĐÂY LÀ VẤN ĐỀ: Xử lý TRỰC TIẾP trên Main Thread (Synchronous)
                // Giao diện sẽ ĐỨNG HÌNH cho đến khi hoàn thành
                try {
                    Log.d("PERF", "⚠️ CHẠY TRÊN MAIN THREAD - GỌI @@@@ BẮTĐẦU")
                    val processingStartTime = System.currentTimeMillis()
                    // ✅ Dùng runBlocking để gọi suspend function
                    // Chạy detection trên ảnh gốc để OpenCV QR hoạt động ổn định
                    Trace.beginSection("Blocking:cropDocument")
                    val cropResult = runBlocking { cropper.cropDocument(original) } // ❌ BLOCK ĐÂY!
                    Trace.endSection()
                    val processingTime = System.currentTimeMillis() - processingStartTime

                    Log.d("PERF", "❌ BLOCKING: Xử lý markers: ${processingTime}ms")

                    val outputBitmap = cropResult.croppedBitmap

                    val markerInfo = mutableListOf<String>()

                    if (cropResult.hasQR) {
                        markerInfo.add("✓ QR Code${cropResult.qrData?.let { ": $it" } ?: ""}")
                        Log.d("PERF", "✅ QR Code: ${cropResult.qrData}")
                    } else {
                        markerInfo.add("✗ QR Code (chưa phát hiện)")
                    }

                    val arUcoCount = cropResult.usedMarkerIds.size
                    cropResult.usedMarkerIds.forEach { id ->
                        markerInfo.add("✓ ArUco ID: $id")
                    }

                    if (arUcoCount < 3) {
                        markerInfo.add("✗ Cần thêm ${3 - arUcoCount} ArUco markers")
                    }

                    val totalFound = cropResult.totalMarkersDetected
                    val isValid = cropResult.success && totalFound >= 4
                    val summary = if (isValid) {
                        "✅ Hợp lệ: $totalFound/4 markers"
                    } else {
                        "⚠️ Thiếu markers: $totalFound/4"
                    }
                    markerInfo.add(0, summary)

                    // Dùng ảnh crop được nếu có, ngược lại trả về ảnh gốc
                    val finalBitmap = outputBitmap ?: original

                    val totalTime = System.currentTimeMillis() - totalStartTime
                    Log.d("PERF", "❌ BLOCKING: Tổng thời gian: ${totalTime}ms")
                    Log.d("PERF", "❌ BLOCKING: Main Thread BỊ BLOCK - GỌI @@@@ KẾT THÚC (Giao diện đứng hình!)")

                    // Gọi callback TRÊN MAIN THREAD
                    onPhotoTaken(finalBitmap, markerInfo, isValid, totalFound)
                } catch (e: Exception) {
                    Log.e("Camera", "Photo processing failed: ${e.message}", e)
                    onPhotoTaken(
                        original,
                        listOf("⚠️ Lỗi xử lý ảnh: ${e.message ?: "không rõ"}"),
                        false,
                        0
                    )
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${exception.message}")
                exception.printStackTrace()
            }
        }
    )
}


@Composable
fun ImagePreviewScreen(
    bitmap: Bitmap,
    detectedMarkers: List<String> = emptyList(),
    pollId: Int?,
    hasValidMarkers: Boolean = false,
    totalMarkersFound: Int = 0,
    verifyResult: VerifyHmacResponse? = null,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val localStorage = remember { LocalBallotStorage(context) }
    val authManager = remember { AuthManager(context) }
    val apiService = remember { RetrofitClient.apiService }
    val uploadRepository = remember { UploadRepository(apiService, authManager) }
    val uploadViewModel = remember { UploadViewModel(uploadRepository, localStorage) }

    var isUploading by remember { mutableStateOf(false) }

    val pendingCount = pollId?.let { localStorage.getPendingCount(it) } ?: 0
    val canUploadAll = pendingCount > 0 && pollId != null && !isUploading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Hiển thị ảnh đã chụp
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Ảnh phiếu bầu",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Lớp overlay tối
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Tiêu đề và thông tin markers
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kiểm tra ảnh phiếu bầu",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hiển thị từng marker một
            if (detectedMarkers.isNotEmpty()) {
                detectedMarkers.forEach { markerInfo ->
                    val isError = markerInfo.contains("✗") || markerInfo.contains("⚠️")
                    val isSuccess = markerInfo.contains("✅")

                    Text(
                        text = markerInfo,
                        color = when {
                            isError -> Color(0xFFFF9800) // Orange for warnings
                            isSuccess -> Color(0xFF4CAF50) // Green for success
                            else -> Color.White
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (markerInfo.startsWith("✅") || markerInfo.startsWith("⚠️"))
                                FontWeight.Bold
                            else
                                FontWeight.Normal
                        ),
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Hiển thị cảnh báo nếu không đủ markers
            if (!hasValidMarkers) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Không thể lưu ảnh này\nVui lòng chụp lại với đủ markers",
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Các nút hành động
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Nút chụp lại
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = onRetake,
                    modifier = Modifier.size(64.dp),
                    containerColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chụp lại",
                        modifier = Modifier.size(28.dp),
                        tint = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Chụp lại",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Nút upload ngay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!isUploading && hasValidMarkers && pollId != null) {
                            // Kiểm tra ballot đã được chụp chưa
                            val ballotId = verifyResult?.ballot_id
                            if (ballotId != null && localStorage.isBallotAlreadyProcessed(pollId, ballotId)) {
                                Toast.makeText(
                                    context,
                                    "⚠️ Phiếu bầu này đã được chụp trước đó!\nBallot ID: $ballotId",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@FloatingActionButton
                            }

                            isUploading = true
                            uploadViewModel.uploadSingleImage(
                                bitmap = bitmap,
                                pollId = pollId,
                                context = context
                            ) { success, message ->
                                isUploading = false
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (success) {
                                    // Lưu ảnh vào local storage và đánh dấu đã upload
                                    localStorage.saveAsUploaded(pollId, bitmap, detectedMarkers, ballotId)
                                    
                                    // Quay về camera để chụp tiếp
                                    onRetake()
                                }
                            }
                        }
                    },
                                    
                    modifier = Modifier.size(64.dp),
                    containerColor = if (hasValidMarkers && !isUploading) Color.White else Color.Gray,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.Blue,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload ngay",
                            modifier = Modifier.size(28.dp),
                            tint = if (hasValidMarkers) Color.Blue else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isUploading) "Đang tải..." else "Upload",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Nút xác nhận (chỉ active nếu có đủ markers)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = onConfirm,
                    modifier = Modifier.size(64.dp),
                    containerColor = if (hasValidMarkers) Color.White else Color.Gray,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Xác nhận",
                        modifier = Modifier.size(28.dp),
                        tint = if (hasValidMarkers) Color(0xFF4CAF50) else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hasValidMarkers) "Lưu" else "Không hợp lệ",
                    color = if (hasValidMarkers) Color.White else Color(0xFFFF9800),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (!hasValidMarkers) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CameraScreenPreview(){
    CameraScreen(modifier = Modifier, navController = rememberNavController(), pollId = 1)
}
//@Preview(showBackground = true)
//@Composable
//fun ImagePreviewScreenPreview() {
//    val context = LocalContext.current
//    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_background)
//
//    val sampleMarkers = listOf(
//        "✅ Hợp lệ: 4/4 markers",
//        "✓ QR Code: demo-qr",
//        "✓ ArUco ID: 12",
//        "✓ ArUco ID: 34",
//        "✓ ArUco ID: 56"
//    )
//
//    ImagePreviewScreen(
//        bitmap = bitmap,
//        detectedMarkers = sampleMarkers,
//        pollId = 1,
//        hasValidMarkers = true,
//        totalMarkersFound = 4,
//        onRetake = {},
//        onConfirm = {}
//    )
//}

@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val jpegBytes = out.toByteArray()

    var bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

    val rotation = imageInfo.rotationDegrees
    if (rotation != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    return bitmap
}

/**
 * Crop bitmap to match the camera guide frame (3:4 aspect ratio, center crop)
 */
fun cropToGuideFrame(bitmap: Bitmap): Bitmap {
    val targetRatio = 3f / 4f // Width/Height của khung guide
    val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

    // Nếu đã đúng tỷ lệ (hoặc gần đúng), không cần crop
    if (kotlin.math.abs(currentRatio - targetRatio) < 0.01f) {
        return bitmap
    }

    val (cropWidth, cropHeight) = if (currentRatio > targetRatio) {
        // Ảnh rộng hơn, crop chiều rộng
        val newWidth = (bitmap.height * targetRatio).toInt()
        newWidth to bitmap.height
    } else {
        // Ảnh cao hơn, crop chiều cao
        val newHeight = (bitmap.width / targetRatio).toInt()
        bitmap.width to newHeight
    }

    val x = (bitmap.width - cropWidth) / 2
    val y = (bitmap.height - cropHeight) / 2

    return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
}