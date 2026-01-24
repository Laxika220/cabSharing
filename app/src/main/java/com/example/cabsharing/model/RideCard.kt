package com.example.cabsharing.model


/**
 * Data model for a ride card
 * Represents a cab sharing opportunity created by a user
 */
data class RideCard(
    val id: String = "",                    // Firebase document ID
    val userId: String = "",                // Creator's user ID
    val userName: String = "",              // Creator's name
    val from: String = "",                  // Starting location
    val to: String = "",                    // Destination
    val departureDate: String = "",         // Date of departure (e.g., "2026-01-25")
    val departureTime: String = "",         // Time of departure (e.g., "9:00 AM")
    val venue: String = "",                 // Meeting point
    val seatsAvailable: Int = 1,            // Number of available seats
    val genderPreference: String = "Any",   // "Male", "Female", or "Any"
    val timestamp: Long = System.currentTimeMillis()  // Creation timestamp
)