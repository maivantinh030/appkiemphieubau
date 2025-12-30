package com.example.ungdungkiemphieu

import android.R
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.ForegroundInfo
import androidx.core.app.NotificationCompat
import com.example.ungdungkiemphieu.data.model.LocalBallotStorage
import com.example.ungdungkiemphieu.data.network.AuthManager
import com.example.ungdungkiemphieu.data.network.RetrofitClient
import com.example.ungdungkiemphieu.repository.UploadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.collections.mapNotNull
import kotlin.runCatching
import kotlin.takeIf
import kotlin.to

class BallotUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val localStorage = LocalBallotStorage(appContext)
    private val uploadRepository = UploadRepository(
        api = RetrofitClient.apiService,
        authManager = AuthManager(appContext)
    )

    private val channelId = "ballot_upload_channel"
    private val notificationId = 1001

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pollId = inputData.getInt("poll_id", -1).takeIf { it != -1 }
        val batchSize = inputData.getInt("batch_size", 3)
        if (pollId == null) {
            Log.e("BallotUploadWorker", "Invalid poll_id")
            return@withContext Result.failure()
        }
        try {
            val pendingInfos = localStorage.getPendingImages(pollId)
            if (pendingInfos.isEmpty()) {
                Log.d("BallotUploadWorker", "No pending images to upload")
                return@withContext Result.success(workDataOf("uploaded_count" to 0))
            }
            try {
                setForeground(createForegroundInfo(0, pendingInfos.size, "Đang upload nền"))
            } catch (e: Exception) {
                Log.w("BallotUploadWorker", "Cannot set foreground service: ${e.message}")
            }
            setProgress(workDataOf("uploaded" to 0, "total" to pendingInfos.size))
            var uploadedCount = 0
            var hasError = false
            var lastError: Exception? = null
            for (chunk in pendingInfos.chunked(batchSize)) {
                try {
                    val bitmaps = chunk.mapNotNull { info ->
                        val file = localStorage.getImageFile(pollId, info)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }
                    if (bitmaps.isEmpty()) continue
                    val response =
                        uploadRepository.uploadBallots(bitmaps, pollId, applicationContext)
                    chunk.forEach { info ->
                        runCatching { localStorage.incrementUploadedCount(pollId, info.fileName) }
                        runCatching { localStorage.removePendingImage(pollId, info.fileName) }
                    }
                    // Free RAM
                    bitmaps.forEach { it.recycle() }
                    uploadedCount += chunk.size

                    Log.d(
                        "BallotUploadWorker",
                        "Uploaded chunk: ${chunk.size}, total=$uploadedCount"
                    )
                    setProgress(
                        workDataOf(
                            "uploaded" to uploadedCount,
                            "total" to pendingInfos.size
                        )
                    )
                    try {
                        setForeground(
                            createForegroundInfo(
                                uploadedCount,
                                pendingInfos.size,
                                "Đang upload nền"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("BallotUploadWorker", "Cannot update foreground notification: ${e.message}")
                    }
                    
                } catch (e: Exception) {
                    Log.e("BallotUploadWorker", "Chunk upload failed: ${e.message}", e)
                    hasError = true
                    lastError = e
                    break  // Dừng upload các chunk còn lại
                }
            }
            
            // Kiểm tra kết quả
            if (hasError) {
                Log.e("BallotUploadWorker", "Worker stopped due to error, will retry")
                // Hiển thị notification lỗi
                showCompletionNotification(uploadedCount, false, "Upload bị gián đoạn: ${lastError?.message}")
                // Retry toàn bộ job
                return@withContext Result.retry()
            }
            // Hiển thị notification thành công
            showCompletionNotification(uploadedCount, true, "Upload thành công $uploadedCount ảnh")
            
            Result.success(workDataOf("uploaded_count" to uploadedCount))
            
        } catch (e: Exception) {
            Log.e("BallotUploadWorker", "Worker crashed: ${e.message}", e)
            // Hiển thị noti thất bại
            showCompletionNotification(0, false, "Lỗi không mong đợi: ${e.message}")
            
            Result.retry()
        }
    }

    private fun createForegroundInfo(uploaded: Int, total: Int, title: String): ForegroundInfo {
        createNotificationChannel()
        val text = if (total > 0) "$uploaded/$total ảnh" else title
        val progress = if (total > 0) (uploaded * 100 / total) else 0

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, total == 0)
            .build()

        return ForegroundInfo(
            notificationId,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Upload nền phiếu bầu",
                NotificationManager.IMPORTANCE_DEFAULT // Hiển thị notification rõ ràng
            )
            channel.description = "Tiến trình upload phiếu bầu nền"
            channel.enableVibration(true) // Rung nhẹ khi upload xong
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun showCompletionNotification(uploadedCount: Int, success: Boolean, message: String) {
        createNotificationChannel()
        
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(if (success) "✅ Upload hoàn tất" else "❌ Upload thất bại")
            .setContentText(message)
            .setSmallIcon(if (success) R.drawable.stat_sys_upload_done else R.drawable.stat_notify_error)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(notificationId + 1, notification)
    }
}
