package com.ities45.vehiclescan_companion.util

import com.ities45.vehiclescan_companion.R
import com.ities45.vehiclescan_companion.model.DtcItem

object DtcMapper {
    fun getDtcItem(code: String): DtcItem? = when (code) {
        "P0100" -> DtcItem(
            code = code,
            module = "Engine Control Module (Stored)",
            type = "Powertrain",
            name = "Air Flow Sensor Issue",
            description = "The sensor that measures air going into the engine may not be working properly. This can affect fuel usage and performance.",
            iconResId = R.drawable.air_flow
        )
        "P0200" -> DtcItem(
            code = code,
            module = "Engine Control Module (Stored)",
            type = "Powertrain",
            name = "Fuel Injector Problem",
            description = "A problem was found in the fuel injector system, which can cause rough running or poor fuel efficiency.",
            iconResId = R.drawable.fuel_injector
        )
        "P0107" -> DtcItem(
            code = code,
            module = "Engine Control Module (Pending)",
            type = "Powertrain",
            name = "Air Pressure Sensor Issue",
            description = "The sensor that reads outside air pressure may be sending low signals. This might affect how the engine runs.",
            iconResId = R.drawable.barometric_pressure
        )
        "P0207" -> DtcItem(
            code = code,
            module = "Engine Control Module (Pending)",
            type = "Powertrain",
            name = "Throttle Response Timeout",
            description = "The engine's throttle did not respond in time. This might cause poor acceleration or delayed engine reaction.",
            iconResId = R.drawable.throttle
        )
        "P1234" -> DtcItem(
            code = code,
            module = "Engine Control Module (Permanent)",
            type = "Powertrain (Security)",
            name = "Ignition Security Lockout",
            description = "The car's security system has stopped the engine from starting. This could be due to unauthorized access or a system fault.",
            iconResId = R.drawable.ignition
        )
        "P0102" -> DtcItem(
            code = code,
            module = "Transmission Control Module (Pending)",
            type = "Powertrain",
            name = "Low Air Flow Signal",
            description = "Low air flow into the engine detected. This may cause poor performance or stalling.",
            iconResId = R.drawable.air_flow
        )
        "U1600" -> DtcItem(
            code = code,
            module = "Transmission Control Module (Pending)",
            type = "Network",
            name = "Immobilizer System Error",
            description = "The car's anti-theft system isn't communicating properly. This might stop the engine from starting.",
            iconResId = R.drawable.immoblizer
        )
        "B2245" -> DtcItem(
            code = code,
            module = "ABS Control Module (Pending)",
            type = "Chassis",
            name = "ABS System Fault",
            description = "The anti-lock brake system has reported a problem. Braking may still work, but without ABS assistance.",
            iconResId = R.drawable.abs
        )
        else -> null
    }
}
