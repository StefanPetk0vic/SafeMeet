package com.example.rma.ui.map

import AuthRepository
import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import android.os.Looper
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.rma.data.models.Friend
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch




@SuppressLint("MissingPermission")
@Composable
fun UserMap(
    modifier: Modifier = Modifier,
    liveMode: Boolean,
    authRepository: AuthRepository,
    onLocationUpdate: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var pins by remember { mutableStateOf<List<SafePin>>(emptyList()) }
    var selectedPin by remember { mutableStateOf<SafePin?>(null) }

    var firstFocusDone by remember { mutableStateOf(false) }

    val radiusMeters = 300f

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val belgrade = LatLng(44.8125, 20.4612)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(belgrade, 15f)
    }

    // --- FRIENDS STATE & MAP REPOSITORY ---
    val mapRepository = remember { MapRepository() }
    val liveFriendsMap = remember { mutableStateMapOf<String, Friend>() } // friendId -> Friend
    val friendColors = listOf(Color.Red, Color.Green, Color.Magenta, Color.Cyan, Color.Yellow)

    // --- FETCH PINS ---
    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("marks").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserMap", "Error listening for pins: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                pins = it.documents.mapNotNull { doc -> doc.toObject(SafePin::class.java) }
            }
        }
    }

    // --- FETCH FRIENDS ONCE ---
    LaunchedEffect(Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val db = Firebase.firestore

        // Coroutine for periodic updates every 5 seconds
        launch {
            while (true) {
                db.collection("users")
                    .document(currentUserId)
                    .collection("follows")
                    .get()
                    .addOnSuccessListener { followsSnapshot ->
                        followsSnapshot.documents.forEach { followDoc ->
                            val friendId = followDoc.id
                            db.collection("users")
                                .document(friendId)
                                .get()
                                .addOnSuccessListener { friendDoc ->
                                    val friend = friendDoc.toObject(Friend::class.java)
                                    if (friend != null && friend.lat != null && friend.lon != null) {
                                        liveFriendsMap[friend.friendId] = friend
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MAP", "Failed to fetch friend $friendId: $e")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MAP", "Failed to fetch follows: $e")
                    }

                delay(5000L) // Wait 5 seconds before next update
            }
        }
    }


    // --- USER LOCATION UPDATES ---
    val currentLiveMode by rememberUpdatedState(liveMode)

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!currentLiveMode) return
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    onLocationUpdate(latLng)

                    if (!firstFocusDone) {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                        firstFocusDone = true
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        authRepository.updateUserLocation(
                            location.latitude,
                            location.longitude,
                            currentLiveMode
                        )
                    }
                }
            }
        }
    }


    LaunchedEffect(liveMode) {
        if (liveMode) {
            firstFocusDone = false
            Log.e("GOD BLESS","${liveMode} ==TRUE")
            val locationRequest = LocationRequest.create().apply {
                interval = 5000L
                fastestInterval = 2000L
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {

            Log.e("GOD BLESS","${liveMode} == FALSE")
            fusedLocationClient.removeLocationUpdates(locationCallback)
            userLocation?.let { lastLoc ->
                CoroutineScope(Dispatchers.IO).launch {
                    authRepository.updateUserLocation(lastLoc.latitude, lastLoc.longitude, false)
                }
            }
        }
    }

    // --- GOOGLE MAP ---
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Draw my own location
        userLocation?.let { myLoc ->
            if (liveMode) {
                MapCircle(
                    center = myLoc,
                    strokeColor = Color.Blue,
                    fillColor = Color(0x550000FF) // semi-transparent blue
                )
            }
        }

        // Draw friends WITHIN 300 M RADIUS !!!!!!!!
        userLocation?.let { myLoc ->
            liveFriendsMap.values.forEachIndexed { index, friend ->
                if (friend.lat != null && friend.lon != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        myLoc.latitude, myLoc.longitude,
                        friend.lat, friend.lon,
                        results
                    )
                    val distance = results[0]
                    if (distance <= radiusMeters) {
                        val color = friendColors[index % friendColors.size]
                        MapCircle(
                            center = LatLng(friend.lat, friend.lon),
                            strokeColor = color,
                            fillColor = color.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Draw pins
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

    // --- PIN DIALOG ---
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
                        onSuccess = { Log.d("UserMap", "Image loaded successfully") }
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
                                                .document(pin.id)
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
