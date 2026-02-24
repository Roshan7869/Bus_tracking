package com.example.bustrackingapp.core.util

import com.example.bustrackingapp.BuildConfig
import com.example.bustrackingapp.R

object Constants {
    const val apiBaseUrl = BuildConfig.API_BASE_URL
    const val socketBaseUrl = BuildConfig.SOCKET_BASE_URL

    const val userPrefs = "user_prefs"

    val busColor = mapOf(
        "red" to R.color.red,
        "green" to R.color.green,
        "blue" to R.color.blue,
        "orange" to R.color.orange,
        "other" to R.color.black,
    )
    val busIcon = mapOf(
        "red" to R.drawable.bus_location_red,
        "green" to R.drawable.bus_location_green,
        "blue" to R.drawable.bus_location_blue,
        "orange" to R.drawable.bus_location_orange,
        "other" to R.drawable.locate_bus,
    )

    val busStatus = mapOf(
        "na" to "NA",
        "in_route" to "IN ROUTE",
        "delayed" to "DELAYED",
        "cancelled" to "CANCELLED",
        "break_down" to "BREAK DOWN",
    )

    object UserType {
        const val passenger = "passenger"
        const val driver = "driver"
    }

    val days = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
}
