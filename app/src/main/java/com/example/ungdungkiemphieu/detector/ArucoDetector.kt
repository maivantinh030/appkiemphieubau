package com.example.ungdungkiemphieu.detector


import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.objdetect.ArucoDetector
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

class ArUcoDetector {
    // Tạo detector với default parameters
    private val detector: ArucoDetector = ArucoDetector()

    data class DetectionResult(
        val markersCount: Int,
        val markerIds: List<Int>,
        val corners: List<MatOfPoint2f>
    )

    fun detectMarkers(imageProxy: ImageProxy): DetectionResult {
        return try {
            // Chuyển đổi ImageProxy thành OpenCV Mat
            val mat = imageProxyToMat(imageProxy)
            // Chuyển sang grayscale nếu cần
            val grayMat = Mat()
            if (mat.channels() == 3) {
                Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
            } else {
                mat.copyTo(grayMat)
            }
            // Phát hiện markers
            val corners = mutableListOf<Mat>()
            val ids = Mat()

            detector.detectMarkers(grayMat, corners, ids)

            val markerCount = corners.size
            val detectedIds = mutableListOf<Int>()
            val detectedCorners = mutableListOf<MatOfPoint2f>()

            if (markerCount > 0 && !ids.empty()) {
                val idsArray = IntArray(ids.total().toInt())
                ids.get(0, 0, idsArray)

                for (i in idsArray.indices) {
                    detectedIds.add(idsArray[i])
                    // Chuyển đổi corners thành MatOfPoint2f
                    if (i < corners.size) {
                        val cornerMat = corners[i]
                        val points = FloatArray(cornerMat.total().toInt() * cornerMat.channels())
                        cornerMat.get(0, 0, points)

                        val pointList = mutableListOf<Point>()
                        for (j in points.indices step 2) {
                            pointList.add(Point(points[j].toDouble(), points[j + 1].toDouble()))
                        }
                        detectedCorners.add(MatOfPoint2f(*pointList.toTypedArray()))
                    }
                }
            }

            // Cleanup
            grayMat.release()
            mat.release()
            ids.release()
            corners.forEach { it.release() }

            DetectionResult(markerCount, detectedIds, detectedCorners)

        } catch (e: Exception) {
            Log.e("ArUcoDetector", "Error detecting markers: ${e.message}")
            DetectionResult(0, emptyList(), emptyList())
        }
    }

    fun detectMarkersFromBitmap(bitmap: Bitmap): DetectionResult {
        return try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            val cornersList = mutableListOf<Mat>()
            val ids = Mat()

            detector.detectMarkers(grayMat, cornersList, ids)

            val markerCount = cornersList.size
            val detectedIds = mutableListOf<Int>()
            val detectedCorners = mutableListOf<MatOfPoint2f>()

            if (markerCount > 0 && !ids.empty()) {
                val idsArray = IntArray(ids.total().toInt())
                ids.get(0, 0, idsArray)
                detectedIds.addAll(idsArray.toList())

                // QUAN TRỌNG: Thêm phần lấy corners
                for (i in 0 until markerCount) {
                    val cornerMat = cornersList[i]
                    val points = FloatArray(cornerMat.total().toInt() * cornerMat.channels())
                    cornerMat.get(0, 0, points)

                    val pointList = mutableListOf<Point>()
                    for (j in points.indices step 2) {
                        pointList.add(Point(points[j].toDouble(), points[j + 1].toDouble()))
                    }
                    detectedCorners.add(MatOfPoint2f(*pointList.toTypedArray()))
                }
            }

            // Cleanup
            grayMat.release()
            mat.release()
            ids.release()
            cornersList.forEach { it.release() }

            DetectionResult(markerCount, detectedIds, detectedCorners)

        } catch (e: Exception) {
            Log.e("ArUcoDetector", "Error: ${e.message}", e)
            DetectionResult(0, emptyList(), emptyList())
        }
    }

    private fun imageProxyToMat(imageProxy: ImageProxy): Mat {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.capacity())
        buffer.get(data)

        val mat = Mat(imageProxy.height, imageProxy.width, CvType.CV_8UC1)
        mat.put(0, 0, data)

        return mat
    }
}

