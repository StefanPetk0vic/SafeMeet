        package com.example.rma.data.models

        data class Friend(
            val friendId: String = "",
            val username: String = "",
            val lat: Double? = null,
            val lon: Double? = null,
            var isLive: Boolean = false,
            val profilePictureUrl: String? = null,
            val lastUpdated: Long? = null,
            val phone: String? = null,
            val fullName: String? = null,
            val email: String? = null
        )

