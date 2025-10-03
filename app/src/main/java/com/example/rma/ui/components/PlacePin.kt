package com.example.rma.ui.components

import AuthRepository
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.rma.data.models.SafePin
import com.example.rma.data.repository.MapRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun PlacePinDialog(
    userLocation: LatLng,
    mapRepository: MapRepository,
    authRepository: AuthRepository,
    currentUsername: String,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description of location:") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { cameraLauncher.launch(cameraImageUri) }) {
                    Text("Take Photo")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        uploading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                var imageUrl = photoUri?.let { authRepository.CloudinarySavePicture(it) } ?: ""
                                imageUrl = imageUrl.replace("http://", "https://")

                                val pin = SafePin(
                                    userId = authRepository.firebaseAuth.currentUser?.uid ?: "",
                                    lat = userLocation.latitude,
                                    lon = userLocation.longitude,
                                    description = description,
                                    imageUrl = imageUrl,
                                    username = currentUsername
                                )
                                //Pin to FireBase
                                mapRepository.savePin(pin)

                                withContext(Dispatchers.Main) {
                                    uploading = false
                                    onDismiss()
                                }
                            } catch (e: Exception) {
                                Log.e("PlacePinDialog", "Error saving pin: ${e.message}")
                                withContext(Dispatchers.Main) { uploading = false }
                            }
                        }
                    },
                    enabled = description.isNotEmpty() && photoUri != null && !uploading
                ) {
                    Text(if (uploading) "Uploading..." else "Save Pin")
                }
            }
        }
    }
}

