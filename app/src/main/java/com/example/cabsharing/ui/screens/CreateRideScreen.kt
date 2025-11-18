package com.example.cabsharing.ui.screens



import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cabsharing.model.RideCard
import com.example.cabsharing.repository.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRideScreen(userId: String, onRideCreated: () -> Unit) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var departureTime by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("1") }
    var genderPreference by remember { mutableStateOf("Any") }
    var isCreating by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val genderOptions = listOf("Any", "Male", "Female")
    var expandedGender by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create a Ride",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Share your cab and split costs",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = from,
            onValueChange = { from = it },
            label = { Text("From") },
            placeholder = { Text("Christ University Lavasa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = to,
            onValueChange = { to = it },
            label = { Text("To") },
            placeholder = { Text("Pune Station") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = departureTime,
            onValueChange = { departureTime = it },
            label = { Text("Departure Time") },
            placeholder = { Text("9:00 AM") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = venue,
            onValueChange = { venue = it },
            label = { Text("Meeting Venue") },
            placeholder = { Text("Main Gate") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = seats,
            onValueChange = { if (it.all { char -> char.isDigit() }) seats = it },
            label = { Text("Seats Available") },
            placeholder = { Text("1") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expandedGender,
            onExpandedChange = { expandedGender = !expandedGender }
        ) {
            OutlinedTextField(
                value = genderPreference,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender Preference (Optional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp )
            )

            ExposedDropdownMenu(
                expanded = expandedGender,
                onDismissRequest = { expandedGender = false }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            genderPreference = option
                            expandedGender = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    isCreating = true
                    val ride = RideCard(
                        userId = userId,
                        userName = userId,
                        from = from,
                        to = to,
                        departureTime = departureTime,
                        venue = venue,
                        seatsAvailable = seats.toIntOrNull() ?: 1,
                        genderPreference = genderPreference
                    )

                    repository.createRide(ride).onSuccess {
                        showSuccess = true
                        delay(1500)
                        onRideCreated()
                    }
                    isCreating = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = from.isNotBlank() && to.isNotBlank() &&
                    departureTime.isNotBlank() && venue.isNotBlank() && !isCreating,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Ride", fontSize = 18.sp)
            }
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "✅ Ride created successfully!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}