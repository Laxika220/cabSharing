package com.example.cabsharing.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cabsharing.model.Match
import com.example.cabsharing.model.RideCard
import com.example.cabsharing.repository.FirebaseRepository
import com.example.cabsharing.ui.theme.AquaAccent
import com.example.cabsharing.ui.theme.IndigoPrimary
import com.example.cabsharing.ui.theme.LavenderSurface
import com.example.cabsharing.ui.theme.MidnightNavy
import com.example.cabsharing.ui.theme.SlateGrey
import com.example.cabsharing.ui.theme.CabSharingTheme
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
    var userRides by remember { mutableStateOf<List<RideCard>>(emptyList()) }

    LaunchedEffect(Unit) {
        // First, get the user's own rides to find matches
        repository.getUserRides(userId).onSuccess { myRides ->
            userRides = myRides
            if (myRides.isNotEmpty()) {
                // For simplicity, match based on the most recent ride created by the user
                repository.getMatchingRides(userId, myRides.first()).onSuccess { matchedRides ->
                    rides = matchedRides
                    isLoading = false
                }
            } else {
                // If no user rides, show all available rides as a fallback
                repository.getRides(userId).onSuccess { allRides ->
                    rides = allRides
                    isLoading = false
                }
            }
        }.onFailure {
            // Fallback to all rides on error
            repository.getRides(userId).onSuccess { allRides ->
                rides = allRides
                isLoading = false
            }
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        listOf(LavenderSurface, Color.White)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (rides.isEmpty() || currentIndex >= rides.size) {
            EmptyState(onCreateRide)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SwipeScreenTopBar(onCreateRide = onCreateRide)

                if (userRides.isNotEmpty() && rides.isNotEmpty()) {
                    Text(
                        text = "Matches for your ride to ${userRides.first().to}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

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

                SwipeActions(
                    onNope = {
                        currentIndex++
                    },
                    onLike = {
                        scope.launch {
                            val ride = rides[currentIndex]
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Show next card behind current card
        if (currentIndex + 1 < rides.size) {
            RideCardUI(
                rides[currentIndex + 1],
                Modifier
                    .graphicsLayer(scaleX = 0.92f, scaleY = 0.92f)
                    .offset(y = 16.dp)
                    .alpha(0.7f)
            )
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
    Card(modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                CardHeader(ride)

                RideTimeline(ride)

                DetailPills(ride)

                if (ride.genderPreference != "Any") {
                    PreferenceBadge(ride.genderPreference)
                }
            }
            RideFooter(ride)
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

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            onClick = onCreateRide,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Ride")
        }
    }
}

@Composable
private fun SwipeScreenTopBar(onCreateRide: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Find your next cab buddy",
                style = MaterialTheme.typography.titleMedium,
                color = MidnightNavy
            )
            Text(
                text = "Swipe right on a ride to send a request",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ExtendedFloatingActionButton(
            onClick = onCreateRide,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("List ride") },
            containerColor = IndigoPrimary,
            contentColor = Color.White
        )
    }
}

@Composable
private fun CardHeader(ride: RideCard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = ride.userName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Verified student",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
}

@Composable
private fun RideTimeline(ride: RideCard) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TimelineRow(label = "From", value = ride.from)
        TimelineRow(label = "To", value = ride.to)
        TimelineRow(label = "Date", value = ride.departureDate)
        TimelineRow(label = "Time", value = ride.departureTime)
        TimelineRow(label = "Meeting point", value = ride.venue)
    }
}

@Composable
private fun TimelineRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (label != "Time") {
            Icon(
                imageVector = Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DetailPills(ride: RideCard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Pill(icon = Icons.Outlined.CheckCircle, text = "On-time driver")
        Pill(icon = Icons.Outlined.Shield, text = "Safety first")
    }
}

@Composable
private fun Pill(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PreferenceBadge(preference: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "Prefers $preference riders",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RideFooter(ride: RideCard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Estimated split fare", color = SlateGrey, fontSize = 13.sp)
            Text(
                text = "₹${(250 / maxOf(ride.seatsAvailable, 1))} approx",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.padding(horizontal = 16.dp))


        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = AquaAccent.copy(alpha = 0.12f)
        ) {
            Row(
                modifier =  Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = AquaAccent
                )
                Text("Average wait 4 min", color = AquaAccent, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SwipeActions(onNope: () -> Unit, onLike: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwipeActionButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Skip",
            containerColor = Color(0xFFFFEFF1),
            iconTint = Color(0xFFDC3C4D),
            onClick = onNope
        )
        SwipeActionButton(
            icon = Icons.Rounded.Favorite,
            contentDescription = "Match",
            containerColor = Color(0xFFE9FFF4),
            iconTint = Color(0xFF17C964),
            onClick = onLike
        )
    }
}

@Composable
private fun SwipeActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 10.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

private val previewRide = RideCard(
    id = "ride_1",
    userId = "priya",
    userName = "Priya Sharma",
    from = "Christ University, Lavasa",
    to = "Pune Railway Station",
    departureTime = "9:30 AM",
    venue = "Main gate",
    seatsAvailable = 3,
    genderPreference = "Any"
)

@Preview(showBackground = true)
@Composable
private fun RideCardPreview() {
    CabSharingTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            RideCardUI(ride = previewRide, modifier = Modifier.fillMaxSize())
        }
    }
}