package com.example.ungdungkiemphieu.viewmodel
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ungdungkiemphieu.BallotUploadWorker
import com.example.ungdungkiemphieu.data.model.LocalBallotStorage
import com.example.ungdungkiemphieu.data.model.UploadResponse
import com.example.ungdungkiemphieu.repository.UploadRepository

import kotlinx.coroutines.launch

class UploadViewModel(
    private val uploadRepository: UploadRepository,
    private val localStorage: LocalBallotStorage // inject từ ngoài
) : ViewModel() {

    // Trạng thái upload (có thể mở rộng với LiveData/StateFlow nếu cần UI feedback)
    var isUploading = false
        private set

    var uploadResult: UploadResponse? = null
        private set

    var uploadError: String? = null
        private set

    fun uploadSingleImage(
        bitmap: Bitmap,
        pollId: Int?,
        context: Context,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            if (isUploading) {
                onComplete(false, "Đang upload, vui lòng đợi...")
                return@launch
            }

            isUploading = true
            uploadError = null

            try {
                Log.d("UploadVM", "Starting single image upload for poll_id: $pollId")

                val response = uploadRepository.uploadSingleBallot(
                    bitmap = bitmap,
                    pollId = pollId,
                    ballotId = null, // Tạo mới, không update
                    context = context
                )

                Log.d("UploadVM", "Single upload successful: ballot_id=${response.ballot_id}, is_update=${response.is_update}")
                onComplete(true, response.message)

            } catch (e: Exception) {
                uploadError = e.message ?: "Lỗi không xác định"
                Log.e("UploadVM", "Single upload failed", e)
                onComplete(false, "Upload thất bại: ${e.message}")
            } finally {
                isUploading = false
            }
        }
    }

    /**
     * Upload tất cả ảnh pending của một pollId
     */
    fun uploadAllPendingImages(
        pollId: Int?,
        context: Context,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            if (isUploading) {
                onComplete(false, "Đang upload, vui lòng đợi...")
                return@launch
            }

            isUploading = true
            uploadResult = null
            uploadError = null

            try {
                // 1. Lấy danh sách ảnh pending
                val pendingInfos = localStorage.getPendingImages(pollId)
                if (pendingInfos.isEmpty()) {
                    onComplete(true, "Không có ảnh nào đang chờ upload")
                    return@launch
                }

                // 2. Đọc file → Bitmap
                val bitmaps = mutableListOf<Bitmap>()
                for (info in pendingInfos) {
                    val file = localStorage.getImageFile(pollId, info)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            bitmaps.add(bitmap)
                        } else {
                            Log.e("UploadVM", "Không đọc được bitmap từ file: ${file.absolutePath}")
                        }
                    } else {
                        Log.e("UploadVM", "File không tồn tại: ${file.absolutePath}")
                    }
                }

                if (bitmaps.isEmpty()) {
                    onComplete(false, "Không thể đọc ảnh nào để upload")
                    return@launch
                }

                // 3. Gọi repository upload nhiều ảnh
                val response = uploadRepository.uploadBallots(bitmaps, pollId, context)

                // 4. Thành công → xóa pending + tăng uploaded count
                uploadResult = response

                // Xóa tất cả ảnh pending đã upload thành công
                for (info in pendingInfos) {
                    localStorage.incrementUploadedCount(pollId, info.fileName)
                    localStorage.removePendingImage(pollId, info.fileName)
                }

                onComplete(true, "Upload thành công ${bitmaps.size} ảnh!")

            } catch (e: Exception) {
                uploadError = e.message ?: "Lỗi không xác định"
                Log.e("UploadVM", "Upload failed", e)
                onComplete(false, "Upload thất bại: ${e.message}")
            } finally {
                isUploading = false
            }
        }
    }

    /**
     * Schedule background upload via WorkManager with network constraint and exponential backoff
     * Worker sẽ TỰ ĐỘNG chạy khi có mạng trở lại mà KHÔNG CẦN mở app
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleUploadAllPendingImages(
        context: Context,
        pollId: Int,
        batchSize: Int = 3
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Chờ có mạng
            .setRequiresBatteryNotLow(false) // Upload ngay cả khi pin yếu
            .setRequiresStorageNotLow(true) // Cần đủ storage
            .build()

        val request = OneTimeWorkRequestBuilder<BallotUploadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("poll_id" to pollId, "batch_size" to batchSize))
            // Retry với backoff tăng dần: 30s, 60s, 120s, 240s...
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                java.time.Duration.ofSeconds(30)
            )
            .addTag("ballot_upload_${pollId}")
            .build()

        // ExistingWorkPolicy.KEEP: Nếu đã có work đang chờ thì giữ nguyên
        WorkManager.getInstance(context).enqueueUniqueWork(
            "upload_ballots_${pollId}",
            ExistingWorkPolicy.KEEP,
            request
        )

        // Thông báo đã lên lịch (chờ mạng) để người dùng biết
        showScheduledNotification(context, pollId)
    }

    private fun showScheduledNotification(context: Context, pollId: Int) {
        val channelId = "ballot_upload_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Upload nền phiếu bầu",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Tiến trình upload phiếu bầu nền"
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Đã lên lịch upload nền")
            .setContentText("Chờ mạng để tiếp tục (poll $pollId)")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1000 + pollId, notification)
    }

    /**
     * Hủy quá trình upload nền đang chạy
     */
    fun cancelUpload(context: Context, pollId: Int) {
        val workName = "upload_ballots_${pollId}"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
        Log.d("UploadViewModel", "Cancelled upload work: $workName")
    }
}