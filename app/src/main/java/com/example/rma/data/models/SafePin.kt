package com.example.rma.data.models

data class SafePin(
    val id: String = "",
    val userId: String = "",

    val username: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,

    val description: String = "",
    val imageUrl: String = "",

    val timestamp: Long = System.currentTimeMillis(),

    val reviews: List<Int> = emptyList()
)
