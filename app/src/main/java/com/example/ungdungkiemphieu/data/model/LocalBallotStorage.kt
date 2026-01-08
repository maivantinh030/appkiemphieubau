package com.example.ungdungkiemphieu.data.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

data class PendingBallotInfo(
    val fileName: String,                  // tên file ảnh
    val detectedMarkers: List<String>,     // danh sách marker phát hiện
    val ballotId: Int? = null,             // ID của phiếu bầu (lấy từ QR code)
    val timestamp: Long = System.currentTimeMillis()
)

data class UploadedBallotInfo(
    val fileName: String,                  // tên file ảnh
    val ballotId: Int? = null,             // ID của phiếu bầu (lấy từ QR code)
    val timestamp: Long = System.currentTimeMillis()
)

class LocalBallotStorage(private val context: Context) {

    private val ballotsDir: File = File(context.filesDir, "ballots").apply { mkdirs() }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ballot_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val KEY_PENDING_LIST = "pending_list_poll_"   // danh sách pending theo pollId
        private const val KEY_UPLOADED_COUNT = "uploaded_count_poll_" // số lượng đã upload
        private const val KEY_UPLOADED_LIST = "uploaded_list_poll_"   // danh sách uploaded theo pollId
    }

    /** Lưu ảnh tạm cho một pollId */
    fun savePendingImage(pollId: Int, bitmap: Bitmap, detectedMarkers: List<String>, ballotId: Int? = null): String {
        // Tạo thư mục riêng cho poll
        val pollDir = File(ballotsDir, "poll_$pollId").apply { mkdirs() }

        // Tên file duy nhất
        val fileName = "ballot_${System.currentTimeMillis()}.jpg"
        val file = File(pollDir, fileName)

        // Nén và lưu file
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // chất lượng 90%
        }

        // Lưu metadata vào SharedPreferences
        val key = "$KEY_PENDING_LIST$pollId"
        val currentList = getPendingImages(pollId).toMutableList()

        currentList.add(PendingBallotInfo(fileName, detectedMarkers, ballotId))

        prefs.edit()
            .putString(key, gson.toJson(currentList))
            .apply()

        return file.absolutePath // trả về đường dẫn nếu cần
    }

    /** Lưu ảnh trực tiếp vào danh sách uploaded (dùng cho upload ngay) */
    fun saveAsUploaded(pollId: Int, bitmap: Bitmap, detectedMarkers: List<String>, ballotId: Int? = null): String {
        // Tạo thư mục riêng cho poll
        val pollDir = File(ballotsDir, "poll_$pollId").apply { mkdirs() }

        // Tên file duy nhất
        val fileName = "ballot_${System.currentTimeMillis()}.jpg"
        val file = File(pollDir, fileName)

        // Nén và lưu file
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // chất lượng 90%
        }

        // Thêm trực tiếp vào danh sách uploaded
        incrementUploadedCount(pollId, fileName, ballotId)

        return file.absolutePath // trả về đường dẫn nếu cần
    }

    /** Lấy danh sách ảnh pending của pollId */
    fun getPendingImages(pollId: Int?): List<PendingBallotInfo> {
        val key = "$KEY_PENDING_LIST$pollId"
        val json = prefs.getString(key, null) ?: return emptyList()

        val type = object : TypeToken<List<PendingBallotInfo>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /** Lấy đường dẫn file ảnh từ PendingBallotInfo */
    fun getImageFile(pollId: Int?, info: PendingBallotInfo): File {
        return File(File(ballotsDir, "poll_$pollId"), info.fileName)
    }

    /** Xóa một ảnh pending sau khi upload thành công - chỉ xóa khỏi danh sách, không xóa file */
    fun removePendingImage(pollId: Int?, fileName: String) {
        // Cập nhật danh sách trong prefs - giữ lại file để có thể hiển thị ở tab "đã upload"
        val key = "$KEY_PENDING_LIST$pollId"
        val list = getPendingImages(pollId).toMutableList()
        list.removeAll { it.fileName == fileName }

        if (list.isEmpty()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, gson.toJson(list)).apply()
        }
    }

    /** Xóa hoàn toàn một ảnh pending (xóa cả file và metadata) */
    fun deletePendingImage(pollId: Int?, fileName: String): Boolean {
        try {
            // Xóa file ảnh
            val pollDir = File(ballotsDir, "poll_$pollId")
            val file = File(pollDir, fileName)
            if (file.exists()) {
                file.delete()
            }

            // Xóa khỏi danh sách pending
            val key = "$KEY_PENDING_LIST$pollId"
            val list = getPendingImages(pollId).toMutableList()
            list.removeAll { it.fileName == fileName }

            if (list.isEmpty()) {
                prefs.edit().remove(key).apply()
            } else {
                prefs.edit().putString(key, gson.toJson(list)).apply()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /** Xóa toàn bộ ảnh của một poll (khi cần reset) */
    fun clearAllForPoll(pollId: Int) {
        File(ballotsDir, "poll_$pollId").deleteRecursively()

        prefs.edit()
            .remove("$KEY_PENDING_LIST$pollId")
            .remove("$KEY_UPLOADED_COUNT$pollId")
            .remove("$KEY_UPLOADED_LIST$pollId")
            .apply()
    }

    /** Tăng số lượng đã upload và thêm vào danh sách */
    fun incrementUploadedCount(pollId: Int?, fileName: String, ballotId: Int? = null) {
        val key = "$KEY_UPLOADED_COUNT$pollId"
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()

        // Thêm vào danh sách uploaded
        val listKey = "$KEY_UPLOADED_LIST$pollId"
        val currentList = getUploadedImages(pollId).toMutableList()
        currentList.add(UploadedBallotInfo(fileName, ballotId))
        prefs.edit().putString(listKey, gson.toJson(currentList)).apply()
    }

    /** Lấy danh sách ảnh đã upload */
    fun getUploadedImages(pollId: Int?): List<UploadedBallotInfo> {
        val key = "$KEY_UPLOADED_LIST$pollId"
        val json = prefs.getString(key, null) ?: return emptyList()

        val type = object : TypeToken<List<UploadedBallotInfo>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /** Lấy số lượng đã upload */
    fun getUploadedCount(pollId: Int?): Int {
        return prefs.getInt("$KEY_UPLOADED_COUNT$pollId", 0)
    }

    /** Lấy số lượng ảnh đang chờ upload */
    fun getPendingCount(pollId: Int?): Int {
        return getPendingImages(pollId).size
    }

    /** Tổng số ảnh đã xử lý (uploaded + pending) */
    fun getTotalCount(pollId: Int?): Int {
        return getUploadedCount(pollId) + getPendingCount(pollId)
    }

    /** Kiểm tra ballot đã được xử lý (pending hoặc uploaded) chưa */
    fun isBallotAlreadyProcessed(pollId: Int?, ballotId: Int): Boolean {
        // Check trong pending
        val pendingBallots = getPendingImages(pollId)
        if (pendingBallots.any { it.ballotId == ballotId }) {
            return true
        }

        // Check trong uploaded
        val uploadedBallots = getUploadedImages(pollId)
        if (uploadedBallots.any { it.ballotId == ballotId }) {
            return true
        }

        return false
    }
}