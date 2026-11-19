package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RiderProfile
import com.example.data.RiderRating
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun RatingsScreen(
    rider: RiderProfile,
    ratings: List<RiderRating>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ratings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_ratings),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2D220B), TerminalCard)))
                    .border(1.2.dp, AmberWarning.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OVERALL RIDER SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (rider.ratingCount > 0) "%.1f / 5.0".format(rider.ratingAvg) else "New Rider",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = if (rider.ratingCount > 0) {
                                "Based on ${rider.ratingCount} customer reviews"
                            } else {
                                "Complete deliveries to earn customer stars"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        if (ratings.isNotEmpty()) {
            item {
                val total = ratings.size.coerceAtLeast(1)
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Star Distribution",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        for (star in 5 downTo 1) {
                            val count = ratings.count { it.rating.coerceIn(1, 5) == star }
                            val ratio = count.toFloat() / total.toFloat()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$star★",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.width(26.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = Color(0xFFFBBF24),
                                    trackColor = TerminalBorder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.width(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Customer Reviews",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }

        if (ratings.isEmpty()) {
            item {
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No customer ratings yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(ratings, key = { it.id.ifBlank { "${it.customerName}_${it.createdAt}" } }) { r ->
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                r.customerName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "★".repeat(r.rating.coerceIn(1, 5)),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFFBBF24)
                            )
                        }
                        if (r.comment.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(r.comment, style = MaterialTheme.typography.bodySmall, color = TextDim)
                        }
                    }
                }
            }
        }
    }
}
