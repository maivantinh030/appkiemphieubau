package com.example.ungdungkiemphieu.detector

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import kotlin.math.max
import kotlin.math.min

class ArUcoDocumentCropper(
    private val minMarkersRequired: Int = 4, // Cần QR + 3 ArUco = 4 markers
    private val padding: Int = 30 // Tăng padding để đảm bảo markers nằm trong
) {

    data class CropResult(
        val croppedBitmap: Bitmap?,
        val success: Boolean,
        val usedMarkerIds: List<Int> = emptyList(),
        val corners: List<Point>? = null,
        val hasQR: Boolean = false,
        val qrData: String? = null,
        val totalMarkersDetected: Int = 0
    )

    private val qrAndArUcoDetector = QRAndArUcoDetector()

    suspend fun cropDocument(bitmap: Bitmap): CropResult {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        // Phát hiện tất cả markers (QR + ArUco)
        val detectResult = qrAndArUcoDetector.detectAllMarkers(bitmap)

        Log.d("CROP", "Total markers found: ${detectResult.totalMarkersFound}")
        Log.d("CROP", "QR: ${detectResult.qrData}, ArUco IDs: ${detectResult.arUcoIds.joinToString()}")

        // Kiểm tra có đủ 4 markers không (1 QR + 3 ArUco)
        if (detectResult.totalMarkersFound < minMarkersRequired) {
            Log.w("CROP", "Not enough markers. Found: ${detectResult.totalMarkersFound}, Required: $minMarkersRequired")
            srcMat.release()
            return CropResult(
                null,
                false,
                detectResult.arUcoIds,
                hasQR = detectResult.qrCorners != null,
                qrData = detectResult.qrData,
                totalMarkersDetected = detectResult.totalMarkersFound
            )
        }

        // Thu thập tất cả các điểm từ QR và ArUco
        val allPoints = mutableListOf<Point>()
        val markerIdsUsed = mutableListOf<Int>()

        // Thêm các góc của QR code
        detectResult.qrCorners?.let { qrCorners ->
            allPoints.addAll(qrCorners)
            Log.d("CROP", "Added QR corners: ${qrCorners.size} points")
        }

        // Thêm các góc của ArUco markers
        for (i in detectResult.arUcoIds.indices) {
            val id = detectResult.arUcoIds[i]
            val cornerMat = detectResult.arUcoCorners.getOrNull(i) ?: continue
            val points = cornerMat.toArray()
            if (points.isNotEmpty()) {
                allPoints.addAll(points)
                markerIdsUsed.add(id)
                Log.d("CROP", "Added ArUco ID $id: ${points.size} points")
            }
        }

        if (allPoints.isEmpty()) {
            Log.w("CROP", "No corner points found")
            srcMat.release()
            return CropResult(
                null,
                false,
                detectResult.arUcoIds,
                hasQR = detectResult.qrCorners != null,
                qrData = detectResult.qrData,
                totalMarkersDetected = detectResult.totalMarkersFound
            )
        }

        // Tìm bounding box chứa tất cả các markers
        val xs = allPoints.map { it.x }
        val ys = allPoints.map { it.y }

        val left = max(0.0, xs.minOrNull()!! - padding)
        val top = max(0.0, ys.minOrNull()!! - padding)
        val right = min(srcMat.cols().toDouble() - 1, xs.maxOrNull()!! + padding)
        val bottom = min(srcMat.rows().toDouble() - 1, ys.maxOrNull()!! + padding)

        val width = (right - left).toInt()
        val height = (bottom - top).toInt()

        Log.d("CROP", "Crop region: left=$left, top=$top, width=$width, height=$height")

        // Crop vùng chứa tất cả markers
        val croppedMat = Mat(srcMat, Rect(left.toInt(), top.toInt(), width, height))

        // Chuyển sang Bitmap
        val croppedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(croppedMat, croppedBitmap)

        // Cleanup
        srcMat.release()
        croppedMat.release()

        return CropResult(
            croppedBitmap = croppedBitmap,
            success = true,
            usedMarkerIds = markerIdsUsed,
            corners = allPoints,
            hasQR = detectResult.qrCorners != null,
            qrData = detectResult.qrData,
            totalMarkersDetected = detectResult.totalMarkersFound
        )
    }
}