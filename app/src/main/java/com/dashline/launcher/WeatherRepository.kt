// Copyright (C) 2026 Metehan Kaygısız & Dashline Auto
// SPDX-License-Identifier: GPL-3.0-only

package com.dashline.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** One upcoming day in the forecast. dateIso is "yyyy-MM-dd" (formatted by the UI). */
data class DailyForecast(val dateIso: String, val iconRes: Int, val high: String, val low: String)

/** Current real weather plus a short multi-day forecast and automotive telemetry badges. */
data class Weather(
    val iconRes: Int,
    val description: String,
    val temp: String,
    val forecast: List<DailyForecast>,
    val city: String = "",
    val humidity: String = "",
    val wind: String = "",
    val pressure: String = ""
)

/**
 * Fetches REAL current weather from Open-Meteo (https://open-meteo.com) — a free keyless API.
 * Uses real GPS location or user configured city, parses real conditions, wind, humidity, pressure,
 * and caches real data in SharedPreferences to avoid fake placeholder values.
 */
object WeatherRepository {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var inMemoryCache: Weather? = null

    fun getCached(context: Context, prefs: Prefs): Weather? {
        if (inMemoryCache != null) return inMemoryCache
        val json = prefs.cachedWeatherJson
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val parsed = parse(context, root, prefs.weatherCity)
            inMemoryCache = parsed
            parsed
        } catch (e: Exception) {
            null
        }
    }

    /** Loads real weather on a background thread; delivers result (or null) on the UI thread. */
    fun load(context: Context, useFahrenheit: Boolean, customCity: String? = null, callback: (Weather?) -> Unit) {
        val prefs = Prefs(context)
        val unit = if (useFahrenheit) "fahrenheit" else "celsius"
        val windUnit = if (useFahrenheit) "mph" else "ms"

        Thread {
            var lat: Double? = null
            var lon: Double? = null
            var resolvedCity = customCity?.trim() ?: ""

            // 1. Try user-specified city via Open-Meteo Geocoding
            if (resolvedCity.isNotBlank()) {
                try {
                    val geoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
                        "?name=${java.net.URLEncoder.encode(resolvedCity, "UTF-8")}&count=1&language=ru"
                    val geoJson = (URL(geoUrl).openConnection() as HttpURLConnection).run {
                        connectTimeout = 4000
                        readTimeout = 4000
                        inputStream.bufferedReader().use { it.readText() }
                    }
                    val geoRoot = JSONObject(geoJson)
                    val results = geoRoot.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val first = results.getJSONObject(0)
                        lat = first.optDouble("latitude")
                        lon = first.optDouble("longitude")
                        resolvedCity = first.optString("name", resolvedCity)
                    }
                } catch (e: Exception) {
                    // fall through to GPS
                }
            }

            // 2. If no city coordinates, try real device GPS
            if (lat == null || lon == null) {
                val loc = lastKnownLocation(context)
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                    if (resolvedCity.isBlank()) {
                        resolvedCity = prefs.weatherCity.ifBlank { "GPS" }
                    }
                }
            }

            if (lat == null || lon == null) {
                mainHandler.post { callback(getCached(context, prefs)) }
                return@Thread
            }

            val forecastUrl = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,surface_pressure,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&forecast_days=4&temperature_unit=$unit&wind_speed_unit=$windUnit&timezone=auto"

            val weather = try {
                val json = (URL(forecastUrl).openConnection() as HttpURLConnection).run {
                    connectTimeout = 5000
                    readTimeout = 5000
                    inputStream.bufferedReader().use { it.readText() }
                }
                val root = JSONObject(json)
                val parsed = parse(context, root, resolvedCity)
                if (parsed != null) {
                    prefs.cachedWeatherJson = json
                    inMemoryCache = parsed
                }
                parsed
            } catch (e: Exception) {
                getCached(context, prefs)
            }

            mainHandler.post { callback(weather ?: getCached(context, prefs)) }
        }.start()
    }

    private fun parse(context: Context, root: JSONObject, resolvedCity: String): Weather? {
        val current = root.optJSONObject("current") ?: return null
        val temp = current.optDouble("temperature_2m", Double.NaN)
        if (temp.isNaN()) return null
        val code = current.optInt("weather_code", -1)
        val (iconRes, descRes) = describe(code)

        val humidityVal = current.optDouble("relative_humidity_2m", Double.NaN)
        val humidityStr = if (!humidityVal.isNaN()) "${Math.round(humidityVal)}%" else "—"

        val windVal = current.optDouble("wind_speed_10m", Double.NaN)
        val windStr = if (!windVal.isNaN()) String.format(Locale.US, "%.1f м/с", windVal) else "—"

        val pressHpa = current.optDouble("surface_pressure", Double.NaN)
        val pressMm = if (!pressHpa.isNaN()) "${Math.round(pressHpa * 0.750062)} мм" else "—"

        return Weather(
            iconRes = iconRes,
            description = context.getString(descRes),
            temp = "${Math.round(temp)}°",
            forecast = parseDaily(root.optJSONObject("daily")),
            city = resolvedCity,
            humidity = humidityStr,
            wind = windStr,
            pressure = pressMm
        )
    }

    /** Upcoming days only (skip index 0 = today). Up to 3 days. */
    private fun parseDaily(daily: JSONObject?): List<DailyForecast> {
        daily ?: return emptyList()
        val times = daily.optJSONArray("time") ?: return emptyList()
        val codes = daily.optJSONArray("weather_code") ?: return emptyList()
        val highs = daily.optJSONArray("temperature_2m_max") ?: return emptyList()
        val lows = daily.optJSONArray("temperature_2m_min") ?: return emptyList()
        val result = mutableListOf<DailyForecast>()
        var i = 1
        while (i < times.length() && result.size < 3) {
            val (icon, _) = describe(codes.optInt(i, -1))
            result.add(
                DailyForecast(
                    dateIso = times.optString(i),
                    iconRes = icon,
                    high = "${Math.round(highs.optDouble(i))}°",
                    low = "${Math.round(lows.optDouble(i))}°"
                )
            )
            i++
        }
        return result
    }

    /** Map a WMO weather code to an icon + description. */
    private fun describe(code: Int): Pair<Int, Int> = when (code) {
        0 -> R.drawable.ic_wx_clear to R.string.wx_clear
        1 -> R.drawable.ic_wx_clear to R.string.wx_mainly_clear
        2 -> R.drawable.ic_wx_partly to R.string.wx_partly_cloudy
        3 -> R.drawable.ic_wx_cloudy to R.string.wx_overcast
        45, 48 -> R.drawable.ic_wx_fog to R.string.wx_fog
        51, 53, 55, 56, 57 -> R.drawable.ic_wx_rain to R.string.wx_drizzle
        61, 63, 65, 66, 67 -> R.drawable.ic_wx_rain to R.string.wx_rain
        80, 81, 82 -> R.drawable.ic_wx_rain to R.string.wx_showers
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_wx_snow to R.string.wx_snow
        95, 96, 99 -> R.drawable.ic_wx_thunder to R.string.wx_thunderstorm
        else -> R.drawable.ic_wx_cloudy to R.string.wx_overcast
    }

    private fun lastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        return try {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).asSequence()
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
    }
}
