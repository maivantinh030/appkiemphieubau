package com.example.ungdungkiemphieu.detector


import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.QRCodeDetector

class QRAndArUcoDetector {

    data class DetectionResult(
        val qrCorners: List<Point>?,
        val qrData: String?,
        val arUcoIds: List<Int>,
        val arUcoCorners: List<MatOfPoint2f>,
        val totalMarkersFound: Int
    )

    private val qrDetector = QRCodeDetector()
    private val arUcoDetector = ArUcoDetector()

    suspend fun detectAllMarkers(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.Default) {
        // 1. Phát hiện QR Code - thử multiple approaches
        var qrCornersList: List<Point>? = null
        var qrData: String? = null

        // Approach 1: ML Kit với original bitmap
        val mlKitResult = tryMLKitDetection(bitmap)
        if (mlKitResult.first != null) {
            qrData = mlKitResult.first
            qrCornersList = mlKitResult.second
            Log.d("QRDetector", "ML Kit QR detected: $qrData")
        }

        // Approach 2: Nếu ML Kit fail, thử với enhanced bitmap
        if (qrData.isNullOrEmpty()) {
            val enhancedBitmap = createEnhancedBitmap(bitmap)
            if (enhancedBitmap != null) {
                val enhancedResult = tryMLKitDetection(enhancedBitmap)
                if (enhancedResult.first != null) {
                    qrData = enhancedResult.first
                    qrCornersList = enhancedResult.second
                    Log.d("QRDetector", "ML Kit enhanced QR detected: $qrData")
                }
                // Clean up enhanced bitmap
                if (enhancedBitmap != bitmap) {
                    enhancedBitmap.recycle()
                }
            }
        }

        // 2. Phát hiện ArUco markers
        val arUcoResult = arUcoDetector.detectMarkersFromBitmap(bitmap)
        Log.d("QRDetector", "ArUco detected: ${arUcoResult.markersCount} markers, IDs: ${arUcoResult.markerIds}")

        val totalFound = (if (qrCornersList != null) 1 else 0) + arUcoResult.markersCount

        DetectionResult(
            qrCorners = qrCornersList,
            qrData = qrData?.takeIf { it.isNotEmpty() },
            arUcoIds = arUcoResult.markerIds,
            arUcoCorners = arUcoResult.corners,
            totalMarkersFound = totalFound
        )
    }

    private suspend fun tryMLKitDetection(bitmap: Bitmap): Pair<String?, List<Point>?> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            // Thêm timeout để tránh hang
            val barcodes = withTimeout(2_000) { scanner.process(image).await() }

            val qr = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
            if (qr != null) {
                val qrData = qr.rawValue
                val cornerPoints = qr.cornerPoints?.map {
                    Point(it.x.toDouble(), it.y.toDouble())
                }
                Pair(qrData, cornerPoints)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Log.w("QRDetector", "ML Kit detection failed: ${e.message}")
            Pair(null, null)
        }
    }

    private fun createEnhancedBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            val grayMat = Mat()
            val enhancedMat = Mat()

            // Convert to grayscale
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // Apply adaptive threshold để cải thiện contrast
            Imgproc.adaptiveThreshold(
                grayMat, enhancedMat, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                11, 2.0
            )

            // Convert back to bitmap
            val enhancedBitmap = Bitmap.createBitmap(
                bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(enhancedMat, enhancedBitmap)

            // Cleanup
            mat.release()
            grayMat.release()
            enhancedMat.release()

            enhancedBitmap
        } catch (e: Exception) {
            Log.w("QRDetector", "Failed to create enhanced bitmap: ${e.message}")
            null
        }
    }
}