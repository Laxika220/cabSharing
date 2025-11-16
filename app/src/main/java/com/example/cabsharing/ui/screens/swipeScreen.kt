package com.example.cabsharing.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cabsharing.model.Match
import com.example.cabsharing.model.RideCard
import com.example.cabsharing.repository.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeScreen(userId: String, onCreateRide: () -> Unit) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    var rides by remember { mutableStateOf<List<RideCard>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showMatchAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.getRides(userId).onSuccess {
            rides = it
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (rides.isEmpty() || currentIndex >= rides.size) {
            EmptyState(onCreateRide)
        } else {
            SwipeableCards(
                rides = rides,
                currentIndex = currentIndex,
                onSwipeLeft = {
                    currentIndex++
                },
                onSwipeRight = { ride ->
                    scope.launch {
                        val match = Match(
                            rideId = ride.id,
                            userId = userId,
                            ownerId = ride.userId
                        )
                        repository.createMatch(match, ride.id)
                        showMatchAnimation = true
                        delay(1500)
                        showMatchAnimation = false
                        currentIndex++
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = onCreateRide,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, "Create Ride")
        }

        AnimatedVisibility(
            visible = showMatchAnimation,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            MatchOverlay()
        }
    }
}

@Composable
fun SwipeableCards(
    rides: List<RideCard>,
    currentIndex: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: (RideCard) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Show next card behind current card
        if (currentIndex + 1 < rides.size) {
            RideCardUI(rides[currentIndex + 1], Modifier.graphicsLayer(scaleX = 0.9f, scaleY = 0.9f))
        }

        // Show current card
        if (currentIndex < rides.size) {
            DraggableCard(
                ride = rides[currentIndex],
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = { onSwipeRight(rides[currentIndex]) }
            )
        }
    }
}

@Composable
fun DraggableCard(
    ride: RideCard,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.7f)
            .offset { IntOffset(animatedOffsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer(
                rotationZ = animatedOffsetX / 20f,
                alpha = 1f - (abs(offsetX) / 1000f)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > 300 -> {
                                offsetX = 2000f
                                scope.launch {
                                    delay(200)
                                    onSwipeRight()
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                            offsetX < -300 -> {
                                offsetX = -2000f
                                scope.launch {
                                    delay(200)
                                    onSwipeLeft()
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                            else -> {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        RideCardUI(ride, Modifier)

        // Show stamps while dragging
        if (offsetX > 50) {
            LikeStamp(Modifier.align(Alignment.TopStart))
        } else if (offsetX < -50) {
            NopeStamp(Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun RideCardUI(ride: RideCard, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ride.userName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "${ride.seatsAvailable} seat${if (ride.seatsAvailable > 1) "s" else ""}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                InfoRow("From", ride.from)
                InfoRow("To", ride.to)
                InfoRow("Time", ride.departureTime)
                InfoRow("Venue", ride.venue)

                if (ride.genderPreference != "Any") {
                    InfoRow("Preference", ride.genderPreference)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFEBEE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Nope",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color(0xFFE8F5E9)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFF43A047),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LikeStamp(modifier: Modifier) {
    Surface(
        modifier = modifier
            .padding(32.dp)
            .rotate(-20f),
        color = Color(0xFF43A047),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "LIKE",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NopeStamp(modifier: Modifier) {
    Surface(
        modifier = modifier
            .padding(32.dp)
            .rotate(20f),
        color = Color(0xFFE53935),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "NOPE",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MatchOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "It's a Match! 🎉",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun EmptyState(onCreateRide: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😴",
            fontSize = 72.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No more rides",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Create a ride to get started",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateRide,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Ride")
        }
    }
}