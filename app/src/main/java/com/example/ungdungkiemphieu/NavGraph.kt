package com.example.ungdungkiemphieu


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.data.network.AuthManager
import com.example.ungdungkiemphieu.data.network.RetrofitClient
import com.example.ungdungkiemphieu.data.network.SettingsManager
import com.example.ungdungkiemphieu.detector.CameraScreen
import com.example.ungdungkiemphieu.ui.screen.*

import kotlin.text.toIntOrNull

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(){
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var selectedPoll by remember { mutableStateOf<Poll?>(null) }
    
    // Determine start destination based on login state
    val startDestination = if (authManager.isLoggedIn()) "home" else "login"
    
    // Kiểm tra session khi app resume
    LaunchedEffect(Unit) {
        // Check nếu cần re-auth khi mở app
        if (authManager.needsReauthentication()) {
            authManager.clearSession()
            // Navigate về login
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else if (authManager.isAccessTokenExpiring()) {
            try {
                val refreshToken = authManager.getRefreshToken()
                if (refreshToken != null) {
                    val request = RefreshTokenRequest(refreshToken)
                    val response = RetrofitClient.apiService.refreshToken(request)
                    
                    if (response.success && response.accessToken != null) {
                        authManager.updateAccessToken(
                            response.accessToken,
                            response.expiresIn ?: 3600
                        )
                        Log.d("NavGraph", "Token refreshed on app resume")
                    }
                } else {
                    // Không có refresh token, về login
                    authManager.clearSession()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Log.e("NavGraph", "Failed to refresh token on resume: ${e.message}")
            }
        }
    }
    
    // Handle logout callback from RetrofitClient (when token expires)
    LaunchedEffect(Unit) {
        RetrofitClient.initialize(authManager, settingsManager) {
            // Navigate to login when token expires
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }


    Scaffold() {innerpadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerpadding)
        ){
            NavHost(
                navController = navController,
                startDestination = startDestination
            ){
                composable("login"){
                    LoginScreen(navController)
                }
                composable("signup"){
                    SignupScreen(navController)
                }
                composable(
                    route = "camera/{pollId}",
                    arguments = listOf(
                        navArgument("pollId") {
                            type = NavType.IntType
                        }
                    )){
                        navBackStackEntry->
                        val pollId = navBackStackEntry.arguments?.getInt("pollId")

                        CameraScreen(
                            navController = navController,
                            modifier = Modifier.fillMaxSize(),
                            pollId = pollId
                        )
                }
                composable("home"){
                    MeetingAttendanceScreen(navController = navController)
//                    HomeScreen(
//                        navController = navController
//                    )

                }
                composable("join_poll"){
                    JoinPollScreen(
                        navController = navController
                    )
                }
                composable("my_poll"){
                    MyPoll(navController = navController)
                }
                composable(
                    route = "poll_detail/{pollId}",
                    arguments = listOf(
                        navArgument("pollId") {
                            type = NavType.IntType
                        }
                    )
                ){
                    navBackStackEntry->
                    val pollId = navBackStackEntry.arguments?.getInt("pollId")
                    PollDetailScreen(
                        pollId = pollId,
                        navController = navController
                    )
                }
                composable("poll_result/{pollId}",
                    arguments = listOf(
                        navArgument("pollId") {
                            type = NavType.IntType
                        }
                    )
                    ){
                    navBackStackEntry->
                    val poll_id = navBackStackEntry.arguments?.getInt("pollId")
                    PollResultScreen(
                        navController = navController,
                        pollId = poll_id

                    )
                }
                composable("pollSummary/{pollId}",
                    arguments = listOf(
                        navArgument("pollId") {
                            type = NavType.IntType
                        }
                    )
                ){
                    navBackStackEntry->
                    val poll_id = navBackStackEntry.arguments?.getInt("pollId")
                    Log.d("PollSummaryScreen", "Poll ID: $poll_id")
                    PollSummaryScreen(
                        pollId = poll_id,
                        navController = navController
                    )
                }
                composable("voter_list/{pollId}") { backStackEntry ->
                    val pollId = backStackEntry.arguments?.getString("pollId")?.toIntOrNull()
                    VoterListScreen(pollId = pollId, navController = navController)
                }

            }
        }
    }
}