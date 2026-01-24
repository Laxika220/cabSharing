// kotlin
package com.example.cabsharing.repository

import com.example.cabsharing.model.Match
import com.example.cabsharing.model.RideCard
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val ridesCollection = db.collection("rides")
    private val matchesCollection = db.collection("matches")

    suspend fun createRide(ride: RideCard): Result<String> {
        return try {
            // specify the awaited Task result type explicitly
            val docRef: DocumentReference = ridesCollection.add(ride).await<DocumentReference>()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRides(currentUserId: String): Result<List<RideCard>> {
        return try {
            val snapshot: QuerySnapshot = ridesCollection
                .whereGreaterThan("seatsAvailable", 0)
                .orderBy("seatsAvailable", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await<QuerySnapshot>()

            val rides = snapshot.documents.mapNotNull { doc: DocumentSnapshot ->
                doc.toObject(RideCard::class.java)?.copy(id = doc.id)
            }.filter { it.userId != currentUserId }

            Result.success(rides)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRides(userId: String): Result<List<RideCard>> {
        return try {
            val snapshot: QuerySnapshot = ridesCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await<QuerySnapshot>()

            val rides = snapshot.documents.mapNotNull { doc: DocumentSnapshot ->
                doc.toObject(RideCard::class.java)?.copy(id = doc.id)
            }
            Result.success(rides)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchingRides(currentUserId: String, userRide: RideCard): Result<List<RideCard>> {
        return try {
            // Match by date and destination
            val snapshot: QuerySnapshot = ridesCollection
                .whereEqualTo("departureDate", userRide.departureDate)
                .whereEqualTo("to", userRide.to)
                .whereGreaterThan("seatsAvailable", 0)
                .get()
                .await<QuerySnapshot>()

            val rides = snapshot.documents.mapNotNull { doc: DocumentSnapshot ->
                doc.toObject(RideCard::class.java)?.copy(id = doc.id)
            }.filter { it.userId != currentUserId && it.from == userRide.from }

            Result.success(rides)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMatch(match: Match, rideId: String): Result<Unit> {
        return try {
            // return a concrete Boolean so await() can infer the Task type
            db.runTransaction { transaction: Transaction ->
                val rideRef = ridesCollection.document(rideId)
                val rideSnapshot = transaction.get(rideRef)
                val currentSeats = rideSnapshot.getLong("seatsAvailable")?.toInt() ?: 0

                if (currentSeats > 0) {
                    transaction.update(rideRef, "seatsAvailable", currentSeats - 1)
                    transaction.set(matchesCollection.document(), match)
                }
                true
            }.await<Boolean>()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyMatches(userId: String): Result<List<Match>> {
        return try {
            val snapshot: QuerySnapshot = matchesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await<QuerySnapshot>()

            val matches = snapshot.documents.mapNotNull { doc: DocumentSnapshot ->
                doc.toObject(Match::class.java)?.copy(id = doc.id)
            }

            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRideById(rideId: String): Result<RideCard?> {
        return try {
            val snapshot: DocumentSnapshot = ridesCollection.document(rideId).get().await<DocumentSnapshot>()
            val ride = snapshot.toObject(RideCard::class.java)?.copy(id = snapshot.id)
            Result.success(ride)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRide(rideId: String): Result<Unit> {
        return try {
            ridesCollection.document(rideId).delete().await<Void?>()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRide(rideId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            ridesCollection.document(rideId).update(updates).await<Void?>()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
