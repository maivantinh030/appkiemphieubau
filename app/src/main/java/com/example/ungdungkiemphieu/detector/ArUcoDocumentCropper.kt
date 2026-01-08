package com.example.ungdungkiemphieu.detector

import android.graphics.Bitmap
import android.graphics.Matrix
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
        // Phát hiện tất cả markers (QR + ArUco) trên ảnh gốc
        val detectResult = qrAndArUcoDetector.detectAllMarkers(bitmap)

        Log.d("CROP", "Total markers found: ${detectResult.totalMarkersFound}")
        Log.d("CROP", "QR: ${detectResult.qrData}, ArUco IDs: ${detectResult.arUcoIds.joinToString()}")

        // Kiểm tra có đủ 4 markers không (1 QR + 3 ArUco)
        if (detectResult.totalMarkersFound < minMarkersRequired) {
            Log.w("CROP", "Not enough markers. Found: ${detectResult.totalMarkersFound}, Required: $minMarkersRequired")
            return CropResult(
                null,
                false,
                detectResult.arUcoIds,
                hasQR = detectResult.qrCorners != null,
                qrData = detectResult.qrData,
                totalMarkersDetected = detectResult.totalMarkersFound
            )
        }

        // Xác định và xoay ảnh để QR ở góc trên bên trái
        val (orientedBitmap, rotationAngle) = orientImageWithQRTopLeft(bitmap, detectResult.qrCorners)
        
        // Phát hiện lại markers trên ảnh đã xoay (nếu có xoay)
        val finalDetectResult = if (rotationAngle != 0) {
            Log.d("CROP", "Re-detecting markers after rotation: $rotationAngle degrees")
            qrAndArUcoDetector.detectAllMarkers(orientedBitmap)
        } else {
            detectResult
        }

        val srcMat = Mat()
        Utils.bitmapToMat(orientedBitmap, srcMat)

        // Thu thập tất cả các điểm từ QR và ArUco
        val allPoints = mutableListOf<Point>()
        val markerIdsUsed = mutableListOf<Int>()

        // Thêm các góc của QR code
        finalDetectResult.qrCorners?.let { qrCorners ->
            allPoints.addAll(qrCorners)
            Log.d("CROP", "Added QR corners: ${qrCorners.size} points")
        }

        // Thêm các góc của ArUco markers
        for (i in finalDetectResult.arUcoIds.indices) {
            val id = finalDetectResult.arUcoIds[i]
            val cornerMat = finalDetectResult.arUcoCorners.getOrNull(i) ?: continue
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
            if (orientedBitmap != bitmap) {
                orientedBitmap.recycle()
            }
            return CropResult(
                null,
                false,
                finalDetectResult.arUcoIds,
                hasQR = finalDetectResult.qrCorners != null,
                qrData = finalDetectResult.qrData,
                totalMarkersDetected = finalDetectResult.totalMarkersFound
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
        if (orientedBitmap != bitmap) {
            orientedBitmap.recycle()
        }

        return CropResult(
            croppedBitmap = croppedBitmap,
            success = true,
            usedMarkerIds = markerIdsUsed,
            corners = allPoints,
            hasQR = finalDetectResult.qrCorners != null,
            qrData = finalDetectResult.qrData,
            totalMarkersDetected = finalDetectResult.totalMarkersFound
        )
    }

    /**
     * Xác định vị trí QR code và xoay ảnh để QR ở góc trên bên trái
     * @return Pair<Bitmap đã xoay, góc xoay (0, 90, 180, 270)>
     */
    private fun orientImageWithQRTopLeft(bitmap: Bitmap, qrCorners: List<Point>?): Pair<Bitmap, Int> {
        if (qrCorners == null || qrCorners.size < 4) {
            Log.d("CROP", "No QR corners, keeping original orientation")
            return Pair(bitmap, 0)
        }

        val width = bitmap.width.toDouble()
        val height = bitmap.height.toDouble()
        val threshold = min(width, height) * 0.3 // 30% của cạnh ngắn hơn

        // Tìm điểm góc trên bên trái của QR (điểm có x + y nhỏ nhất)
        val topLeftQR = qrCorners.minByOrNull { it.x + it.y } ?: return Pair(bitmap, 0)
        
        val qrX = topLeftQR.x
        val qrY = topLeftQR.y
        
        // Xác định góc của ảnh mà QR đang nằm
        val isNearTop = qrY < threshold
        val isNearLeft = qrX < threshold
        val isNearBottom = qrY > (height - threshold)
        val isNearRight = qrX > (width - threshold)
        
        val rotationAngle = when {
            isNearTop && isNearLeft -> {
                Log.d("CROP", "QR already at top-left, no rotation needed")
                0
            }
            isNearTop && isNearRight -> {
                Log.d("CROP", "QR at top-right, rotating 90 degrees clockwise")
                90
            }
            isNearBottom && isNearRight -> {
                Log.d("CROP", "QR at bottom-right, rotating 180 degrees")
                180
            }
            isNearBottom && isNearLeft -> {
                Log.d("CROP", "QR at bottom-left, rotating 270 degrees (or -90)")
                270
            }
            // Nếu không rõ ràng, dùng khoảng cách đến các góc
            else -> {
                val distToTopLeft = kotlin.math.sqrt(qrX * qrX + qrY * qrY)
                val distToTopRight = kotlin.math.sqrt((width - qrX) * (width - qrX) + qrY * qrY)
                val distToBottomLeft = kotlin.math.sqrt(qrX * qrX + (height - qrY) * (height - qrY))
                val distToBottomRight = kotlin.math.sqrt((width - qrX) * (width - qrX) + (height - qrY) * (height - qrY))
                
                when (minOf(distToTopLeft, distToTopRight, distToBottomLeft, distToBottomRight)) {
                    distToTopLeft -> {
                        Log.d("CROP", "QR closest to top-left, no rotation")
                        0
                    }
                    distToTopRight -> {
                        Log.d("CROP", "QR closest to top-right, rotating 90 degrees")
                        90
                    }
                    distToBottomRight -> {
                        Log.d("CROP", "QR closest to bottom-right, rotating 180 degrees")
                        180
                    }
                    distToBottomLeft -> {
                        Log.d("CROP", "QR closest to bottom-left, rotating 270 degrees")
                        270
                    }
                    else -> 0
                }
            }
        }

        if (rotationAngle == 0) {
            return Pair(bitmap, 0)
        }

        // Xoay ảnh
        val matrix = Matrix()
        matrix.postRotate(rotationAngle.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        Log.d("CROP", "Image rotated: $rotationAngle degrees")
        return Pair(rotatedBitmap, rotationAngle)
    }
}