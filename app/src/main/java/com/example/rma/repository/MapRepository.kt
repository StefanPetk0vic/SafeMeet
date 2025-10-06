package com.example.rma.data.repository

import android.util.Log
import com.example.rma.data.models.Friend
import com.example.rma.data.models.SafePin
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class MapRepository {

    private val db = Firebase.firestore

    // Add a friend by username
    fun addFriendByUsername(
        currentUserId: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val friendDoc = snapshot.documents.firstOrNull()
                if (friendDoc != null) {
                    val friendId = friendDoc.id
                    val data = mapOf(
                        "friendId" to friendId,
                        "username" to friendDoc.getString("username"),
                        "lat" to friendDoc.getDouble("lat"),
                        "lon" to friendDoc.getDouble("lon"),
                        "isLive" to (friendDoc.getBoolean("isLive") ?: false)
                    )
                    db.collection("users")
                        .document(currentUserId)
                        .collection("follows")
                        .document(friendId)
                        .set(data)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it) }
                } else {
                    onError(Exception("User not found"))
                }
            }
            .addOnFailureListener { onError(it) }
    }


    // Listen to the "follows" subcollection in real-time
    fun getFriendsListener(userId: String, onUpdate: (List<Friend>) -> Unit): ListenerRegistration {
        val db = Firebase.firestore
        return db.collection("users")
            .document(userId)
            .collection("follows")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Repo", "Error listening for friends: $error")
                    return@addSnapshotListener
                }
                val friends = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Friend::class.java)
                } ?: emptyList()
                onUpdate(friends)
            }
    }


    fun getFriendLiveLocation(friendId: String, onUpdate: (Friend) -> Unit): ListenerRegistration {
        return db.collection("users")
            .document(friendId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val friend = snapshot.toObject(Friend::class.java)
                    if (friend != null) {
                        onUpdate(friend.copy(friendId = friendId))
                    }
                }
            }
    }


    // Remove friend from follows
    fun removeFriend(currentUserId: String, friendId: String) {
        val db = Firebase.firestore
        db.collection("users")
            .document(currentUserId)
            .collection("follows")
            .document(friendId)
            .delete()
    }


    fun setLiveMode(userId: String, isLive: Boolean, lat: Double? = null, lon: Double? = null) {
        val updates = mutableMapOf<String, Any>(
            "isLive" to isLive
        )
        lat?.let { updates["lat"] = it }
        lon?.let { updates["lon"] = it }

        db.collection("users").document(userId)
            .set(updates, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("MapRepository", "Failed to set live mode: $e")
            }
    }
    fun getFriendsDetails(friendIds: List<String>, onResult: (List<Friend>) -> Unit) {
        val db = Firebase.firestore
        if (friendIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        db.collection("users")
            .whereIn("id", friendIds) // assumes user docs have a field "id" matching UID
            .get()
            .addOnSuccessListener { snapshot ->
                val friends = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val username = doc.getString("username") ?: return@mapNotNull null
                    Friend(friendId = id, username = username)
                }
                onResult(friends)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    suspend fun savePin(pin: SafePin) {
        val newDoc = db.collection("marks").document()
        val pinWithId = pin.copy(id = newDoc.id)
        newDoc.set(pinWithId).await()
    }
}
