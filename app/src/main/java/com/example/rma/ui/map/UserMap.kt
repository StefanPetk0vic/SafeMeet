package com.example.rma.ui.map

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.rma.ui.map.components.MapCircle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@Composable
fun UserMap(modifier: Modifier = Modifier,liveMode: Boolean) {

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var firstFocusDone by remember { mutableStateOf(false) }


    val belgrade = LatLng(44.8125, 20.4612)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(belgrade, 15f)
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

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
    ){
        if(liveMode && userLocation != null){
            MapCircle(center = userLocation!!, strokeColor = Color.Blue, fillColor = Color(0x550000FF))
        }

    //TODO: Adding other users to the map

//        otherUsersLocations.forEach { otherLocation ->
//            MapCircle(center = otherLocation, strokeColor = Color.Green, fillColor = Color(0x5500FF00))
//        }
    }
}


