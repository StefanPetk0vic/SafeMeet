package com.example.rma.ui.login

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import coil.compose.rememberAsyncImagePainter

@Composable
fun LoginScreen(
    onLoginClick:(String,String)-> Unit,
    onRegisterClick:()-> Unit
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
        ) {
        TextField(
            value = email,
            onValueChange = { email = it},
            label = {Text("Email")},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = password,
            onValueChange = { password = it},
            label = {Text("Password")},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {onLoginClick(email,password)},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        TextButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
    }
}
}

@Composable
fun RegisterScreen(
    onRegisterClick:(String,String, String, String, String, Uri?)-> Unit,
    onBackToLoginClick:()-> Unit
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUri = uri
    }

    val context = LocalContext.current
    val cameraImageUri = remember {
        val file = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = cameraImageUri
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                if(photoUri != null){
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else{
                    Box( modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Photo")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Choose Photo")
                }
                Button(onClick = { cameraLauncher.launch(cameraImageUri) }) {
                    Text("Take Photo")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = fullName,
                onValueChange = { fullName = it},
                label = {Text("Full Name")},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = username,
                onValueChange = { username = it},
                label = {Text("username")},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = email,
                onValueChange = { email = it},
                label = {Text("Email")},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = password,
                onValueChange = { password = it},
                label = {Text("Password")},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = phone,
                onValueChange = { phone = it},
                label = {Text("Phone number")},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {onRegisterClick(email,password, fullName, username, phone, photoUri)},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create account")
            }
            TextButton(
                onClick = onBackToLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I have an account, login")
            }
        }
    }
}