package com.example.ungdungkiemphieu

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.ungdungkiemphieu.data.network.AuthManager
import kotlinx.coroutines.launch
import com.example.ungdungkiemphieu.ui.theme.ỨngDụngKiểmPhiếuBầuTheme

import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat

class MainActivity : FragmentActivity() {
    
    private lateinit var authManager: AuthManager
    
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = AuthManager(this)

        // ✅ CHECK VÀ REFRESH TOKEN KHI MỞ APP
        lifecycleScope.launch {
            val tokenValid = authManager.checkAndRefreshTokenIfNeeded()
            if (!tokenValid && authManager.isLoggedIn()) {
                Log.w("MainActivity", "Token refresh failed, user may need to re-login")
            }
        }

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!")
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully")
            if (testOpenCV()) {
                Log.d("OpenCV", "OpenCV is working properly!")
            }
        }
        enableEdgeToEdge()
        if(!hasRequiredPermissions()){
            ActivityCompat.requestPermissions(
                this,
                CAMERAX_PERMISSIONS,
                0
            )
        }
        setContent {
            ỨngDụngKiểmPhiếuBầuTheme() {
                AppNavGraph()
            }
        }
    }
    override fun onPause() {
        super.onPause()
        // Đánh dấu app vào background khi user gạt app
        authManager.markAppInBackground()
        Log.d("MainActivity", "App paused - session marked as inactive")
    }
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "App resumed")
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    companion object{
        private val CAMERAX_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }
}

fun testOpenCV(): Boolean {
    return try {
        val testMat = Mat.zeros(100, 100, CvType.CV_8UC1)
        Log.d("OpenCV", "Test mat created: ${testMat.rows()}x${testMat.cols()}")
        testMat.release()
        true
    } catch (e: Exception) {
        Log.e("OpenCV", "OpenCV test failed: ${e.message}")
        false
    }
}