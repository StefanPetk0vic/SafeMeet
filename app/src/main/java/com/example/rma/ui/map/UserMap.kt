package com.example.rma.ui.map

import AuthRepository
import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.rma.data.models.SafePin
import com.example.rma.data.repository.MapRepository
import com.example.rma.ui.map.components.MapCircle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

import androidx.compose.material.icons.outlined.StarBorder
import com.example.rma.data.models.Friend


@SuppressLint("MissingPermission")
@Composable
fun UserMap(modifier: Modifier = Modifier,liveMode: Boolean, authRepository: AuthRepository,onLocationUpdate: (LatLng) -> Unit) {

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var firstFocusDone by remember { mutableStateOf(false) }

    val mapRepository = remember { MapRepository() }
    var pins by remember { mutableStateOf<List<SafePin>>(emptyList()) }

    var selectedPin by remember { mutableStateOf<SafePin?>(null) }


    LaunchedEffect(Unit) {
        val firebaseAuth = FirebaseAuth.getInstance()

        val db = Firebase.firestore

        db.collection("marks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserMap", "Error listening for pins: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let {
                    pins = it.documents.mapNotNull { doc -> doc.toObject(SafePin::class.java) }
                }
            }
    }


    val belgrade = LatLng(44.8125, 20.4612)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(belgrade, 15f)
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    val friendColors = listOf(Color.Red, Color.Green, Color.Magenta, Color.Cyan, Color.Yellow)

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("users")
            .document(userId)
            .collection("friends")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    friends = snapshot.documents.mapNotNull { it.toObject(Friend::class.java) }
                }
            }
    }

    LaunchedEffect(liveMode) {
        if (liveMode) firstFocusDone = false
        while(liveMode) {
            val location: Location? = try {
                Log.e("MAP-FUN","EVOOO MEEEE")
                fusedLocationClient.lastLocation.await()
            } catch (e: Exception) {
                Log.e("MAP-FUN","ERROR: ${e}")
                null
            }
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                userLocation = latLng
                onLocationUpdate(latLng)

                try {
                    authRepository.updateUserLocation(it.latitude, it.longitude)
                } catch (e: Exception) {
                    Log.e("MAP-FUN", "Failed to update Firestore: $e")
                }

                if (!firstFocusDone) {
                    cameraPositionState.position = CameraPosition
                        .fromLatLngZoom(latLng,cameraPositionState.position.zoom)
                    firstFocusDone = true
                }
            }
            delay(5000L)
        }
    }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        if (liveMode && userLocation != null) {
            MapCircle(center = userLocation!!, strokeColor = Color.Blue, fillColor = Color(0x550000FF))
        }
        friends.forEachIndexed { index, friend ->
            val color = friendColors[index % friendColors.size] // cycle through colors
            if (friend.lat != null && friend.lon != null) {
                MapCircle(
                    center = LatLng(friend.lat, friend.lon),
                    strokeColor = color,
                    fillColor = color.copy(alpha = 0.3f)
                )
            }
        }


        pins.forEach { pin ->
            Marker(
                state = MarkerState(position = LatLng(pin.lat, pin.lon)),
                title = pin.description,
                onClick = {
                    selectedPin = pin
                    true
                }
            )
        }
    }
    selectedPin?.let { pin ->
        Dialog(onDismissRequest = { selectedPin = null }) {
            Card(modifier = Modifier.fillMaxWidth(0.9f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = pin.description, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    AsyncImage(
                        model = pin.imageUrl,
                        contentDescription = "Pin photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        onError = { error ->
                            Log.e("UserMap", "Failed to load image: ${error.result.throwable}")
                        },
                        onSuccess = { success ->
                            Log.d("UserMap", "Image loaded successfully")
                        }
                    )


                    Spacer(modifier = Modifier.height(8.dp))

                    var rating by remember { mutableStateOf(0) }
                    val db = Firebase.firestore
                    Row {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        rating = i
                                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                                        if (userId != null) {
                                            val ratingData = hashMapOf(
                                                "userId" to userId,
                                                "value" to i
                                            )
                                            db.collection("marks")
                                                .document(pin.id)   // <-- Make sure SafePin has an "id"
                                                .collection("reviews")
                                                .document(userId)
                                                .set(ratingData)
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    var reviews by remember { mutableStateOf<List<Int>>(emptyList()) }
                    LaunchedEffect(pin.id) {
                        db.collection("marks")
                            .document(pin.id)
                            .collection("reviews")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) return@addSnapshotListener
                                reviews = snapshot?.documents?.mapNotNull { it.getLong("value")?.toInt() } ?: emptyList()
                            }
                    }

                    val averageRating = if (reviews.isNotEmpty()) {
                        reviews.sum().toFloat() / reviews.size
                    } else 0f
                    Text(text = "Average rating: %.1f (%d reviews)".format(averageRating, reviews.size))

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { selectedPin = null }) {
                        Text("Close")
                    }
                }
            }
        }
    }

}


