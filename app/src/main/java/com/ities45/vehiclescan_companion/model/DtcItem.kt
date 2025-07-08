package com.ities45.vehiclescan_companion.model

data class DtcItem(
    val code: String,
    val module: String,
    val type: String,
    val description: String,
    val iconResId: Int,
    val name: String
)