package com.example.cabsharing.model

/**
 * Data model for a match
 * Represents a successful connection between a user and a ride
 */
data class Match(
    val id: String = "",                    // Firebase document ID
    val rideId: String = "",                // ID of the matched ride
    val userId: String = "",                // ID of user who swiped right
    val ownerId: String = "",               // ID of ride creator
    val timestamp: Long = System.currentTimeMillis()  // Match creation timestamp
)