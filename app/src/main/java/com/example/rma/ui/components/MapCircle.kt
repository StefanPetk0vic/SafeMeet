
package com.example.rma.ui.map.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.maps.android.compose.Circle
import com.google.android.gms.maps.model.LatLng

@Composable
fun MapCircle(
    center: LatLng,
    radiusMeters: Double = 5.0,
    strokeColor: Color = Color.Black,
    fillColor: Color = Color(0x550000FF)
) {
    Circle(
        center = center,
        radius = radiusMeters,
        strokeColor = strokeColor,
        fillColor = fillColor
    )
}
