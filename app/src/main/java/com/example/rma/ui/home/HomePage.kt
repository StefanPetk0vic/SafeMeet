package com.example.rma.ui.login

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter

@Composable
fun HomePage(
    profilePictureUrl: String?,
    onProfileClick: () -> Unit,
    onLiveClick: () -> Unit,
    onPinClick: () -> Unit,
    onFriendsClick: () -> Unit
) {
    Scaffold(
        topBar = { TopBar(profilePictureUrl, onProfileClick) },
        bottomBar = { BottomNavBar(onLiveClick, onPinClick, onFriendsClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(800.dp)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text("Map Placeholder", color = Color.White)
            }
        }
    }
}


@Composable
fun BottomNavBar(
    onLiveClick: () -> Unit,
    onPinClick: () -> Unit,
    onFriendsClick: () -> Unit
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
            modifier = Modifier.size(90.dp)
        ) { Text("LIVE") }
        Button(onClick = onPinClick) { Text("Place Pin") }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopBar(profilePictureUrl: String?, onProfileClick: () -> Unit){
    TopAppBar(
        title = {Text("SafeMeet")},
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Profile picture
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
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info
                Text("Full Name: $fullName")
                Text("Email: $email")
                Text("Phone: $phone")

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
