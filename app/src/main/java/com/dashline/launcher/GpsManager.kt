// Copyright (C) 2026 Dashline Auto
// SPDX-License-Identifier: GPL-3.0-only

package com.dashline.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.Locale

/**
 * Real-time GPS Telemetry snapshot.
 */
data class TelemetryData(
    val speedKmh: Int,
    val headingDegrees: Float,
    val headingCardinal: String,
    val currentStreet: String,
    val currentCity: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Int,
    val hasGpsFix: Boolean,
    val tripDistanceKm: Float,
    val totalOdometerKm: Float,
    val tripElapsedSeconds: Long,
    val tripAvgSpeedKmh: Int,
    val tripMaxSpeedKmh: Int,
    val tripFuelUsedLitres: Float
)

/**
 * Manages real hardware GPS sensors, tracks real speed, odometer, and geocodes street names.
 */
class GpsManager(private val context: Context, private val prefs: Prefs) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<(TelemetryData) -> Unit>()

    private var lastLocation: Location? = null
    private var lastFixTime: Long = 0L
    private var lastGeocodeTime: Long = 0L
    private var cachedStreet: String = ""
    private var cachedCity: String = ""

    private var currentSpeedKmh: Int = 0
    private var currentHeading: Float = 0f
    private var maxSpeedKmh: Int = 0
    private var tripDistanceKm: Float = 0f
    private var tripStartTimeMs: Long = SystemClock.elapsedRealtime()

    private var isListening = false

    init {
        tripDistanceKm = prefs.carGpsTripKm
    }

    fun addListener(listener: (TelemetryData) -> Unit) {
        listeners.add(listener)
        listener(getSnapshot())
    }

    fun removeListener(listener: (TelemetryData) -> Unit) {
        listeners.remove(listener)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isListening || !Permissions.hasLocation(context)) return
        val lm = locationManager ?: return

        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, this)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5.0f, this)
            }
            // Seed with last known location immediately
            val seed = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            seed?.let { onLocationChanged(it) }

            isListening = true
        } catch (e: Exception) {
            // Permission or security exception
        }
    }

    fun stop() {
        if (!isListening) return
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            // ignore
        }
        isListening = false
    }

    fun resetTrip() {
        tripDistanceKm = 0f
        maxSpeedKmh = 0
        tripStartTimeMs = SystemClock.elapsedRealtime()
        prefs.carGpsTripKm = 0f
        notifyListeners()
    }

    override fun onLocationChanged(location: Location) {
        lastFixTime = SystemClock.elapsedRealtime()

        // 1. Calculate Real Speed (m/s -> km/h)
        val speedKmhRaw = if (location.hasSpeed()) (location.speed * 3.6f) else 0f
        currentSpeedKmh = Math.round(speedKmhRaw).coerceAtLeast(0)
        if (currentSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = currentSpeedKmh
        }

        // 2. Heading / Compass Bearing
        if (location.hasBearing() && currentSpeedKmh > 2) {
            currentHeading = location.bearing
        }

        // 3. Accumulate real distance
        val prev = lastLocation
        if (prev != null && location.hasAccuracy() && location.accuracy < 50f) {
            val distMeters = prev.distanceTo(location)
            // Filter noise: only accumulate if speed is plausible or distance is reasonable
            if (distMeters in 2.0f..300.0f) {
                tripDistanceKm += (distMeters / 1000f)
                prefs.carGpsTripKm = tripDistanceKm
            }
        }
        lastLocation = location

        // 4. Reverse Geocode for real street / location
        maybeGeocode(location)

        notifyListeners()
    }

    private fun maybeGeocode(location: Location) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastGeocodeTime < 25000L && cachedStreet.isNotEmpty()) return
        lastGeocodeTime = now

        Thread {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val thoroughfare = addr.thoroughfare
                        val subThoroughfare = addr.subThoroughfare
                        val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea

                        val street = when {
                            !thoroughfare.isNullOrEmpty() && !subThoroughfare.isNullOrEmpty() ->
                                "$thoroughfare, $subThoroughfare"
                            !thoroughfare.isNullOrEmpty() -> thoroughfare
                            !addr.featureName.isNullOrEmpty() -> addr.featureName
                            else -> locality ?: ""
                        }

                        mainHandler.post {
                            if (!street.isNullOrEmpty()) cachedStreet = street
                            if (!locality.isNullOrEmpty()) cachedCity = locality
                            notifyListeners()
                        }
                    }
                }
            } catch (e: Exception) {
                // Network or geocoder service failure
            }
        }.start()
    }

    fun getSnapshot(): TelemetryData {
        val now = SystemClock.elapsedRealtime()
        val hasFix = (now - lastFixTime) < 15000L && lastLocation != null
        val loc = lastLocation

        val elapsedSec = ((now - tripStartTimeMs) / 1000L).coerceAtLeast(0L)
        val elapsedHours = elapsedSec / 3600f
        val avgSpeed = if (elapsedHours > 0.005f) {
            Math.round(tripDistanceKm / elapsedHours).coerceAtLeast(0)
        } else {
            0
        }

        val fuelRate = prefs.carConsumptionRate
        val fuelUsed = tripDistanceKm * (fuelRate / 100f)
        val totalOdo = prefs.carOdometerBaseline + tripDistanceKm

        val street = when {
            cachedStreet.isNotEmpty() -> cachedStreet
            hasFix -> String.format(Locale.US, "GPS: %.4f° N, %.4f° E", loc?.latitude ?: 0.0, loc?.longitude ?: 0.0)
            else -> context.getString(R.string.gps_searching)
        }

        val city = cachedCity.ifEmpty { prefs.weatherCity.ifEmpty { "GPS" } }

        return TelemetryData(
            speedKmh = if (hasFix) currentSpeedKmh else 0,
            headingDegrees = currentHeading,
            headingCardinal = degreesToCardinal(currentHeading),
            currentStreet = street,
            currentCity = city,
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            altitudeMeters = if (loc?.hasAltitude() == true) loc.altitude.toInt() else 0,
            hasGpsFix = hasFix,
            tripDistanceKm = tripDistanceKm,
            totalOdometerKm = totalOdo,
            tripElapsedSeconds = elapsedSec,
            tripAvgSpeedKmh = avgSpeed,
            tripMaxSpeedKmh = maxSpeedKmh,
            tripFuelUsedLitres = fuelUsed
        )
    }

    private fun notifyListeners() {
        val snapshot = getSnapshot()
        listeners.forEach { it(snapshot) }
    }

    private fun degreesToCardinal(deg: Float): String {
        val d = (deg % 360 + 360) % 360
        return when {
            d >= 337.5 || d < 22.5 -> "С (Север)"
            d < 67.5 -> "СВ (Северо-Восток)"
            d < 112.5 -> "В (Восток)"
            d < 157.5 -> "ЮВ (Юго-Восток)"
            d < 202.5 -> "Ю (Юг)"
            d < 247.5 -> "ЮЗ (Юго-Запад)"
            d < 292.5 -> "З (Запад)"
            else -> "СЗ (Северо-Запад)"
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
