package com.example.ungdungkiemphieu.ui.screen

import com.example.ungdungkiemphieu.ui.theme.AppColors

import android.util.Log
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ungdungkiemphieu.repository.PollRepository
import com.example.ungdungkiemphieu.viewmodel.*
import kotlin.collections.map
import kotlin.compareTo

@Composable
fun PollResultScreen(
    navController: NavController,
    pollId: Int?
) {
    val viewModel = remember { PollViewModel(PollRepository()) }
    val stats by viewModel.candidateStats.collectAsState()
    val totalVotes by viewModel.totalBallots.collectAsState()

    Log.d("PollResultScreen", "pollId: $pollId")
    Log.d("PollResultScreen", "stats: $stats")
    Log.d("PollResultScreen", "totalVotes: $totalVotes")

    // Trigger load on enter
    LaunchedEffect(pollId) {
        if (pollId != null) {
            Log.d("PollResultScreen", "Loading results for poll: $pollId")
            viewModel.getResultPollById(pollId)
        } else {
            Log.e("PollResultScreen", "pollId is NULL!")
        }
    }

    // Map backend stats to UI CandidateResult
    val candidates = remember(stats, totalVotes) {
        stats.map { candidatestats ->
            CandidateResult(candidatestats.name, candidatestats.count)
        }.sortedByDescending { it.votes }
    }

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
        // Background decorative elements
        BackgroundDecorations()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            PollResultTopBar(
                onBackClick = { navController.popBackStack() }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Results title
                item {
                    Text(
                        text = "Kết quả chi tiết",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Candidate results
                if (candidates.isEmpty()) {
                    item {
                        Text(
                            text = "Đang tải dữ liệu...",
                            fontSize = 16.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    itemsIndexed(candidates) { index, candidate ->
                        CandidateResultItem(
                            candidate = candidate,
                            totalVotes = totalVotes,
                            position = index + 1,
                            animationDelay = index * 200 // Staggered animation
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Data model
data class CandidateResult(
    val name: String,
    val votes: Int
)

@Composable
fun PollResultTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = AppColors.CardBackground,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.CardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Chi tiết kiểm phiếu",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
    }
}


//
@Composable
fun WinnerSection(winner: CandidateResult?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.CardBackground,
    ) {
        Box {
            // Gradient overlay for winner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.1f), // Gold
                                Color(0xFFFFA500).copy(alpha = 0.05f) // Orange
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700), // Gold
                                    Color(0xFFFFA500)  // Orange
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Người trúng cử",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = winner?.name ?: "Chưa có kết quả",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                if (winner != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${winner.votes} phiếu bầu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFD700) // Gold color
                    )
                }
            }
        }
    }
}

@Composable
fun CandidateResultItem(
    candidate: CandidateResult,
    totalVotes: Int,
    position: Int,
    animationDelay: Int = 0
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val percentage = if (totalVotes > 0) (candidate.votes.toFloat() / totalVotes) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) percentage else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = animationDelay,
            easing = EaseOutBounce
        ),
        label = "progress_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Candidate info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position and name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Position badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when (position) {
                                    1 -> Color(0xFFFFD700) // Gold
                                    2 -> Color(0xFFC0C0C0) // Silver
                                    3 -> Color(0xFFCD7F32) // Bronze
                                    else -> AppColors.TextSecondary.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = position.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (position <= 3) Color.White else AppColors.TextSecondary
                        )
                    }

                    // Name
                    Text(
                        text = candidate.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }

                // Vote count
                Text(
                    text = "${candidate.votes} phiếu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OrangeGradientStart
                    )
                    Text(
                        text = "$totalVotes tổng phiếu",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = AppColors.TextSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        AppColors.OrangeGradientStart,
                                        AppColors.OrangeGradientEnd
                                    )
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
        }
    }
}
