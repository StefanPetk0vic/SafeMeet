package com.example.rma.ui.login

import AuthRepository
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.rma.data.models.Friend
import com.example.rma.data.repository.MapRepository
import com.example.rma.ui.components.PlacePinDialog
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.example.rma.ui.map.UserMap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomePage(
    profileUrl: String?,
    userFullName: String,
    userEmail: String,
    userPhone: String,
    authRepository: AuthRepository,
    navController: NavHostController,
    onPinClick: () -> Unit,
    onFriendsClick: () -> Unit,
) {
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    var showFriendsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    var showProfile by remember { mutableStateOf(false) }
    var liveMode by remember { mutableStateOf(false) }

    var showPlacePinDialog by remember { mutableStateOf(false) } // controls PlacePinDialog
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) } // tracks user location
    val mapRepository = remember { MapRepository() } // reused repository instance


    Scaffold(
        topBar = {
            TopBar(
                profilePictureUrl = profileUrl,
                onProfileClick = { showProfile = true }
            )
        },
        bottomBar = {
            BottomNavBar(
                onLiveClick = {
                    if (locationPermissionState.status.isGranted) {
                        liveMode = !liveMode
                    } else {
                        locationPermissionState.launchPermissionRequest()
                    }
                },
                onPinClick = {
                    if(currentUserLocation != null){
                        showPlacePinDialog =true
                    }
                },
                onFriendsClick = { showFriendsDialog = true },
                liveMode = liveMode
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(800.dp)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                if (locationPermissionState.status.isGranted) {
                    UserMap(liveMode = liveMode,authRepository = authRepository, onLocationUpdate = { latLng -> currentUserLocation = latLng })
                } else {
                    Text("Location permission required for LIVE mode")
                }
            }
        }
    }

    if (showProfile) {
        ProfilePopup(
            profilePictureUrl = profileUrl,
            fullName = userFullName,
            email = userEmail,
            phone = userPhone,
            onDismiss = { showProfile = false },
            onLogout = {
                authRepository.logout()
                navController.navigate("login") { popUpTo("home") { inclusive = true } }
            }
        )
    }
    if (showPlacePinDialog && currentUserLocation != null) {
        PlacePinDialog(
            userLocation = currentUserLocation!!,
            mapRepository = mapRepository,
            authRepository = authRepository,
            currentUsername = userFullName,
            onDismiss = { showPlacePinDialog = false }
        )
    }
    if (showFriendsDialog) {
        FriendsDialog(
            currentUserId = authRepository.firebaseAuth.currentUser?.uid ?: "",
            onDismiss = { showFriendsDialog = false },
            mapRepository = mapRepository,
            authRepository = authRepository
        )
    }

}


@Composable
fun BottomNavBar(
    onLiveClick: () -> Unit,
    onPinClick: () -> Unit,
    onFriendsClick: () -> Unit,
    liveMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onFriendsClick) { Text("Friends") }
        Button(
            onClick = onLiveClick,
            shape = CircleShape,
            modifier = Modifier.size(90.dp),
            colors = if (liveMode) ButtonDefaults.buttonColors(containerColor = Color.Red)
            else ButtonDefaults.buttonColors()
        ) { Text("LIVE") }
        Button(
            onClick = onPinClick,
            enabled = liveMode
        ) { Text("Place Pins") }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopBar(profilePictureUrl: String?, onProfileClick: () -> Unit){
    TopAppBar(
        title = {        Text(
            "SafeMeet",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )},
        actions = {
            val painter = if (profilePictureUrl.isNullOrEmpty()) {
                rememberVectorPainter(Icons.Default.Person)
            } else {
                rememberAsyncImagePainter(profilePictureUrl,
                    onError = {error-> Log.e("Coil","FAILED to load pfp")})
            }
            Image(
                painter = painter,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp).padding(8.dp).clip(CircleShape)
                    .clickable{onProfileClick()},
                contentScale = ContentScale.Crop
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun ProfilePopup(
    profilePictureUrl: String?,
    fullName: String,
    email: String,
    phone: String,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val painter = if (profilePictureUrl.isNullOrEmpty()) {
                    rememberVectorPainter(Icons.Default.Person)
                } else {
                    rememberAsyncImagePainter(profilePictureUrl,
                        onError = {error-> Log.e("Coil","FAILED to load pfp")})
                }
                Image(
                    painter = painter,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start) {
                    Row {
                        Text("Full Name:", color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(fullName,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Row {
                        Text("Email:", color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(email,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Row {
                        Text("Phone:", color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(phone,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}

@Composable
fun FriendsDialog(
    currentUserId: String,
    onDismiss: () -> Unit,
    mapRepository: MapRepository,
    authRepository: AuthRepository
) {
    var username by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf(listOf<Friend>()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Listen to friends in real-time
    DisposableEffect(currentUserId) {
        val registration = mapRepository.getFriendsListener(currentUserId) { newFriends ->
            friends = newFriends // directly update state
        }
        onDispose { registration.remove() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Friends") },
        text = {
            Column {
                // Add friend input
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Enter friend's username") }
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (username.isNotBlank()) {
                        mapRepository.addFriendByUsername(
                            currentUserId,
                            username,
                            onSuccess = { username = ""; error = null },
                            onError = { e -> error = e.message }
                        )
                    }
                }) {
                    Text("Add Friend")
                }

                error?.let { Text("Error: $it", color = Color.Red) }
                Spacer(Modifier.height(12.dp))

                Text("Your Friends:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                // LazyColumn for friends
                LazyColumn {
                    items(friends, key = { it.friendId }) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = friend.username,
                                fontSize = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    mapRepository.removeFriend(currentUserId, friend.friendId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Remove", color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
