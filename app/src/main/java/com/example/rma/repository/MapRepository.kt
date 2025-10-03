package com.example.rma.data.repository

import com.example.rma.data.models.Friend
import com.example.rma.data.models.SafePin
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class MapRepository {

    private val db = Firebase.firestore

    fun addFriendByUsername(
        currentUserId: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onError(Exception("User not found"))
                } else {
                    val friendDoc = result.documents.first()
                    val friendId = friendDoc.id
                    val friendName = friendDoc.getString("username") ?: ""

                    val friend = mapOf(
                        "friendId" to friendId,
                        "username" to friendName
                    )

                    db.collection("users")
                        .document(currentUserId)
                        .collection("friends")
                        .document(friendId)
                        .set(friend)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e) }
                }
            }
            .addOnFailureListener { e -> onError(e) }
        }

    fun getFriends(currentUserId: String, onUpdate: (List<Friend>) -> Unit) {
        db.collection("users")
            .document(currentUserId)
            .collection("friends")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val friends = snapshot.documents.mapNotNull {
                        val id = it.getString("friendId")
                        val uname = it.getString("username")
                        if (id != null && uname != null) Friend(id, uname) else null
                    }
                    onUpdate(friends)
                }
            }
    }
        suspend fun savePin(pin: SafePin) {
        val newDoc = db.collection("marks").document()
        val pinWithId = pin.copy(id = newDoc.id)
        newDoc.set(pinWithId).await()
    }

    suspend fun getAllPins(): List<SafePin> {
        return try {
            val snapshot = db.collection("marks").get().await()
            snapshot.documents.mapNotNull { it.toObject<SafePin>() }
        } catch (e: Exception) {
            emptyList()
        }

    }
}
