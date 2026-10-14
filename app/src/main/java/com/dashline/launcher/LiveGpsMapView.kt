// Copyright (C) 2026 Dashline Auto
// SPDX-License-Identifier: GPL-3.0-only

package com.dashline.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.SystemClock
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time Automotive GPS Navigation and Radar Map View.
 * Displays actual GPS coordinates, vehicle bearing heading, trail of driven path,
 * real GPS speed, and allows launching real navigation apps.
 */
class LiveGpsMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F141C")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C2534")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#223046")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val routeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val vehiclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }

    private val vehicleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E9DAE")
        textSize = 24f
    }

    private val accentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        textSize = 28f
        isFakeBoldText = true
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC121824")
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    // Telemetry state
    private var telemetry: TelemetryData? = null
    private val breadcrumbs = mutableListOf<Pair<Double, Double>>()
    private val maxBreadcrumbs = 150

    // Zoom scale (meters per radius unit)
    private var zoomIndex = 1
    private val zoomScales = floatArrayOf(250f, 500f, 1000f, 2500f, 5000f) // radius in meters
    private val zoomLabels = arrayOf("250 м", "500 м", "1 км", "2.5 км", "5 км")

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            openRealNavigation()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomIn()
            return true
        }
    })

    fun updateTelemetry(data: TelemetryData) {
        this.telemetry = data

        if (data.hasGpsFix && data.latitude != 0.0 && data.longitude != 0.0) {
            val last = breadcrumbs.lastOrNull()
            if (last == null || (Math.abs(last.first - data.latitude) > 0.00005 || Math.abs(last.second - data.longitude) > 0.00005)) {
                breadcrumbs.add(Pair(data.latitude, data.longitude))
                if (breadcrumbs.size > maxBreadcrumbs) {
                    breadcrumbs.removeAt(0)
                }
            }
        }

        invalidate()
    }

    fun zoomIn() {
        if (zoomIndex > 0) {
            zoomIndex--
            invalidate()
            Toast.makeText(context, "Масштаб: ${zoomLabels[zoomIndex]}", Toast.LENGTH_SHORT).show()
        }
    }

    fun zoomOut() {
        if (zoomIndex < zoomScales.size - 1) {
            zoomIndex++
            invalidate()
            Toast.makeText(context, "Масштаб: ${zoomLabels[zoomIndex]}", Toast.LENGTH_SHORT).show()
        }
    }

    fun recenter() {
        invalidate()
        Toast.makeText(context, "Центр на автомобиле", Toast.LENGTH_SHORT).show()
    }

    fun openRealNavigation() {
        val t = telemetry
        val lat = t?.latitude ?: 0.0
        val lon = t?.longitude ?: 0.0

        val uri = if (lat != 0.0 && lon != 0.0) {
            Uri.parse("geo:$lat,$lon?q=$lat,$lon(Мой+автомобиль)&z=16")
        } else {
            Uri.parse("geo:0,0?q=navigation")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        val prefs = Prefs(context)
        val preferredNav = prefs.navApp

        if (!preferredNav.isNullOrEmpty() && preferredNav != Prefs.DEFAULT_NAV) {
            mapIntent.setPackage(preferredNav)
        }

        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Fallback: try opening google maps or any map
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(genericIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Навигационное приложение не найдено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val cx = w * 0.5f
        val cy = h * 0.52f
        val maxR = Math.min(w, h) * 0.44f

        // 2. Coordinate Grid
        val step = 40f
        var x = step
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += step
        }
        var y = step
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += step
        }

        // 3. Concentric Radar Rings & Crosshairs
        val r1 = maxR * 0.35f
        val r2 = maxR * 0.70f
        val r3 = maxR

        canvas.drawCircle(cx, cy, r1, ringPaint)
        canvas.drawCircle(cx, cy, r2, ringPaint)
        canvas.drawCircle(cx, cy, r3, ringPaint)

        // Crosshairs
        canvas.drawLine(cx - maxR, cy, cx + maxR, cy, ringPaint)
        canvas.drawLine(cx, cy - maxR, cx, cy + maxR, ringPaint)

        // Cardinal directions
        textPaint.textSize = 18f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("С (N)", cx, cy - maxR - 8f, textPaint)
        canvas.drawText("Ю (S)", cx, cy + maxR + 22f, textPaint)
        canvas.drawText("З (W)", cx - maxR - 26f, cy + 6f, textPaint)
        canvas.drawText("В (E)", cx + maxR + 26f, cy + 6f, textPaint)

        // 4. Breadcrumb trail / Path
        val t = telemetry
        val currentLat = t?.latitude ?: 0.0
        val currentLon = t?.longitude ?: 0.0
        val hasFix = t?.hasGpsFix == true && currentLat != 0.0 && currentLon != 0.0

        if (hasFix && breadcrumbs.size > 1) {
            val scaleMeters = zoomScales[zoomIndex]
            val metersPerPixel = scaleMeters / maxR
            // approximate: 1 degree lat = 111,139 meters; lon = 111,139 * cos(lat)
            val cosLat = Math.cos(Math.toRadians(currentLat)).toFloat()
            val mPerDegLat = 111139f
            val mPerDegLon = 111139f * cosLat

            val path = Path()
            var started = false

            for (pt in breadcrumbs) {
                val dLat = (pt.first - currentLat).toFloat()
                val dLon = (pt.second - currentLon).toFloat()

                val dyMeters = -dLat * mPerDegLat
                val dxMeters = dLon * mPerDegLon

                val px = cx + (dxMeters / metersPerPixel)
                val py = cy + (dyMeters / metersPerPixel)

                if (px in -50f..(w + 50f) && py in -50f..(h + 50f)) {
                    if (!started) {
                        path.moveTo(px, py)
                        started = true
                    } else {
                        path.lineTo(px, py)
                    }
                }
            }

            if (started) {
                canvas.drawPath(path, routeGlowPaint)
                canvas.drawPath(path, routePaint)
            }
        }

        // 5. Vehicle Cursor (Arrow pointing in direction of heading)
        val heading = t?.headingDegrees ?: 0f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(heading)

        val arrowPath = Path().apply {
            moveTo(0f, -22f) // tip
            lineTo(14f, 16f) // bottom right
            lineTo(0f, 10f)  // notch
            lineTo(-14f, 16f) // bottom left
            close()
        }

        // Vehicle glow circle
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4000E5FF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(0f, 0f, 26f, glowPaint)

        canvas.drawPath(arrowPath, vehiclePaint)
        canvas.drawPath(arrowPath, vehicleStrokePaint)
        canvas.restore()

        // 6. Draw HUD Telemetry Badges
        drawHudOverlay(canvas, w, h, t, hasFix)
    }

    private fun drawHudOverlay(canvas: Canvas, w: Float, h: Float, t: TelemetryData?, hasFix: Boolean) {
        val pad = 12f

        // --- Bottom-Right: GPS Info Badge ---
        val speedText = if (hasFix) "${t?.speedKmh ?: 0} км/ч" else "0 км/ч"
        val altText = if (hasFix) "${t?.altitudeMeters ?: 0} м" else "—"
        val headingText = if (hasFix) "${t?.headingDegrees?.toInt() ?: 0}° ${t?.headingCardinal?.take(2) ?: ""}" else "—"
        val scaleText = zoomLabels[zoomIndex]

        val badgeW = 210f
        val badgeH = 74f
        val badgeX = w - badgeW - pad
        val badgeY = h - badgeH - pad

        val rect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)
        canvas.drawRoundRect(rect, 10f, 10f, badgeBgPaint)
        canvas.drawRoundRect(rect, 10f, 10f, badgeBorderPaint)

        textPaint.textSize = 16f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#8E9DAE")

        canvas.drawText("СКОРОСТЬ GPS:", badgeX + 10f, badgeY + 22f, textPaint)
        accentTextPaint.textSize = 22f
        accentTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(speedText, badgeX + badgeW - 10f, badgeY + 22f, accentTextPaint)

        canvas.drawText("ВЫСОТА / КУРС:", badgeX + 10f, badgeY + 44f, textPaint)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$altText • $headingText", badgeX + badgeW - 10f, badgeY + 44f, textPaint)

        textPaint.color = Color.parseColor("#8E9DAE")
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("МАСШТАБ:", badgeX + 10f, badgeY + 64f, textPaint)
        textPaint.color = Color.parseColor("#00E5FF")
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(scaleText, badgeX + badgeW - 10f, badgeY + 64f, textPaint)

        // --- Bottom-Left: Satellite Fix Badge ---
        val fixW = 160f
        val fixH = 34f
        val fixRect = RectF(pad, h - fixH - pad, pad + fixW, h - pad)
        canvas.drawRoundRect(fixRect, 8f, 8f, badgeBgPaint)
        canvas.drawRoundRect(fixRect, 8f, 8f, badgeBorderPaint)

        val statusDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (hasFix) Color.parseColor("#00E676") else Color.parseColor("#FFD600")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pad + 16f, h - fixH * 0.5f - pad, 6f, statusDotPaint)

        textPaint.textSize = 15f
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.LEFT
        val fixLabel = if (hasFix) "GPS: 3D Фикс" else "GPS: Поиск датчика…"
        canvas.drawText(fixLabel, pad + 30f, h - fixH * 0.5f - pad + 5f, textPaint)
    }
}
