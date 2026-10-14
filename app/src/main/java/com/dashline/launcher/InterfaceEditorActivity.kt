// Copyright (C) 2026 Dashline Auto
// SPDX-License-Identifier: GPL-3.0-only

package com.dashline.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.dashline.launcher.databinding.ActivityInterfaceEditorBinding

/**
 * Interface and Layout Editor for Dashline Launcher.
 * Allows adding/removing elements, widgets, buttons, changing sizes, positions,
 * fonts, font sizes, vehicle telemetry parameters, and app shortcuts.
 */
class InterfaceEditorActivity : BaseActivity() {

    private lateinit var binding: ActivityInterfaceEditorBinding
    private lateinit var prefs: Prefs

    private companion object {
        const val REQ_PICK_NAV = 101
        const val REQ_PICK_MUSIC = 102
        const val REQ_PICK_PHONE = 103
        const val REQ_PICK_DOCK_1 = 111
        const val REQ_PICK_DOCK_2 = 112
        const val REQ_PICK_DOCK_3 = 113
        const val REQ_PICK_DOCK_4 = 114
        const val REQ_PICK_WIDGET = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterfaceEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.header.pageTitle.text = "Редактор интерфейса"
        bindPageNav(binding.header.navBack, binding.header.navHome)

        setupTabs()
        loadPreferencesIntoUi()
        setupClickListeners()
    }

    private fun setupTabs() {
        val tabs = listOf(
            binding.tabElements to binding.panelElements,
            binding.tabSizes to binding.panelSizes,
            binding.tabPosition to binding.panelPosition,
            binding.tabFonts to binding.panelFonts,
            binding.tabButtons to binding.panelButtons,
            binding.tabSensors to binding.panelSensors
        )

        fun selectTab(activeTab: Button, activePanel: View) {
            tabs.forEach { (tab, panel) ->
                if (tab == activeTab) {
                    tab.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                    tab.setBackgroundResource(R.drawable.bg_pill_status)
                    panel.visibility = View.VISIBLE
                } else {
                    tab.setTextColor(getColor(R.color.text_secondary))
                    tab.setBackgroundResource(android.R.color.transparent)
                    panel.visibility = View.GONE
                }
            }
        }

        tabs.forEach { (tab, panel) ->
            tab.setOnClickListener { selectTab(tab, panel) }
        }
    }

    private fun loadPreferencesIntoUi() {
        // Section 1: Elements
        binding.chkNavCard.isChecked = prefs.showNavCard
        binding.chkMediaCard.isChecked = prefs.showMediaCard
        binding.chkWeatherCard.isChecked = prefs.showWeatherCard
        binding.chkTripCard.isChecked = prefs.showTripCard
        binding.chkDock.isChecked = prefs.showDock
        binding.chkSidebar.isChecked = prefs.showSidebar
        binding.chkTopBar.isChecked = prefs.showTopBar

        when (prefs.navWidgetType) {
            Prefs.WIDGET_TYPE_SYSTEM_WIDGET -> binding.rbNavSystemWidget.isChecked = true
            Prefs.WIDGET_TYPE_GPS_HUD -> binding.rbNavHud.isChecked = true
            else -> binding.rbNavLiveGps.isChecked = true
        }

        // Section 2: Sizes
        when (prefs.columnSplit) {
            "50_50" -> binding.rbSplit5050.isChecked = true
            "70_30" -> binding.rbSplit7030.isChecked = true
            "40_60" -> binding.rbSplit4060.isChecked = true
            else -> binding.rbSplit6040.isChecked = true
        }

        when (prefs.buttonSizeDp) {
            40 -> binding.rbBtn40.isChecked = true
            56 -> binding.rbBtn56.isChecked = true
            64 -> binding.rbBtn64.isChecked = true
            else -> binding.rbBtn48.isChecked = true
        }

        when (prefs.dockAppCount) {
            3 -> binding.rbDock3.isChecked = true
            5 -> binding.rbDock5.isChecked = true
            6 -> binding.rbDock6.isChecked = true
            else -> binding.rbDock4.isChecked = true
        }

        // Section 3: Position
        if (prefs.sidebarPosition == "right") {
            binding.rbSidebarRight.isChecked = true
        } else {
            binding.rbSidebarLeft.isChecked = true
        }

        when (prefs.dashboardLayout) {
            Prefs.LAYOUT_COMPACT -> binding.rbLayoutInverted.isChecked = true
            Prefs.LAYOUT_SPORT -> binding.rbLayoutSport.isChecked = true
            else -> binding.rbLayoutClassic.isChecked = true
        }

        if (prefs.cardsStackOrder == "weather_first") {
            binding.rbStackWeatherFirst.isChecked = true
        } else {
            binding.rbStackMediaFirst.isChecked = true
        }

        // Section 4: Fonts
        when (prefs.fontStyle) {
            "mono" -> binding.rbFontMono.isChecked = true
            "condensed" -> binding.rbFontCondensed.isChecked = true
            "serif" -> binding.rbFontSerif.isChecked = true
            else -> binding.rbFontSans.isChecked = true
        }

        when (prefs.fontScale) {
            0.85f -> binding.rbScale85.isChecked = true
            1.15f -> binding.rbScale115.isChecked = true
            1.30f -> binding.rbScale130.isChecked = true
            else -> binding.rbScale100.isChecked = true
        }

        binding.chkFontBold.isChecked = prefs.fontBold

        // Section 5: Buttons
        updateAppButtonLabels()

        // Section 6: Sensors & Car
        binding.etCarModel.setText(prefs.carModel)
        binding.etCarOdometer.setText(String.format("%.0f", prefs.carOdometerBaseline))
        binding.etCarFuelRate.setText(String.format("%.1f", prefs.carConsumptionRate))
        binding.etWeatherCity.setText(prefs.weatherCity)
    }

    private fun updateAppButtonLabels() {
        binding.tvNavAppVal.text = getAppName(prefs.navApp)
        binding.tvMusicAppVal.text = getAppName(prefs.mediaApp)
        binding.tvPhoneAppVal.text = getAppName(prefs.phoneApp)

        binding.tvDockSlot1Val.text = getAppName(prefs.dockAppPackage(0) ?: prefs.navApp)
        binding.tvDockSlot2Val.text = getAppName(prefs.dockAppPackage(1) ?: "com.spotify.music")
        binding.tvDockSlot3Val.text = getAppName(prefs.dockAppPackage(2) ?: "ru.yandex.music")
        binding.tvDockSlot4Val.text = getAppName(prefs.dockAppPackage(3) ?: "com.google.android.apps.youtube.music")
    }

    private fun getAppName(packageName: String?): String {
        if (packageName.isNullOrBlank()) return "По умолчанию"
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun setupClickListeners() {
        // App pickers
        binding.rowNavApp.setOnClickListener {
            AppPickerActivity.start(this, Prefs.ROLE_NAV, REQ_PICK_NAV)
        }
        binding.rowMusicApp.setOnClickListener {
            AppPickerActivity.start(this, Prefs.ROLE_MEDIA, REQ_PICK_MUSIC)
        }
        binding.rowPhoneApp.setOnClickListener {
            AppPickerActivity.start(this, Prefs.ROLE_PHONE, REQ_PICK_PHONE)
        }

        binding.rowDockSlot1.setOnClickListener {
            AppPickerActivity.start(this, "dock_0", REQ_PICK_DOCK_1)
        }
        binding.rowDockSlot2.setOnClickListener {
            AppPickerActivity.start(this, "dock_1", REQ_PICK_DOCK_2)
        }
        binding.rowDockSlot3.setOnClickListener {
            AppPickerActivity.start(this, "dock_2", REQ_PICK_DOCK_3)
        }
        binding.rowDockSlot4.setOnClickListener {
            AppPickerActivity.start(this, "dock_3", REQ_PICK_DOCK_4)
        }

        // System Widgets
        binding.btnAddSystemWidget.setOnClickListener {
            val id = WidgetHost.allocateId(this)
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            startActivityForResult(pickIntent, REQ_PICK_WIDGET)
        }

        binding.btnClearWidgets.setOnClickListener {
            prefs.setCardWidgets(Prefs.SLOT_MEDIA, emptyList())
            prefs.setCardWidgets(Prefs.SLOT_SECOND, emptyList())
            Toast.makeText(this, "Виджеты очищены", Toast.LENGTH_SHORT).show()
        }

        // Reset trip odo
        binding.btnResetTripOdo.setOnClickListener {
            prefs.carGpsTripKm = 0f
            Toast.makeText(this, "Суточный пробег сброшен на 0 км", Toast.LENGTH_SHORT).show()
        }

        // Save & Apply
        binding.btnSaveAndApply.setOnClickListener {
            saveAll()
            Toast.makeText(this, "Настройки интерфейса сохранены!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Reset Defaults
        binding.btnResetDefaults.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Сбросить интерфейс?")
                .setMessage("Вернуть все элементы, размеры и шрифты к исходным настройкам по умолчанию?")
                .setPositiveButton("Сбросить") { _, _ ->
                    resetToDefaults()
                    loadPreferencesIntoUi()
                    Toast.makeText(this, "Настройки сброшены", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        val pkg = data?.getStringExtra(AppPickerActivity.EXTRA_PACKAGE)
        when (requestCode) {
            REQ_PICK_NAV -> pkg?.let { prefs.navApp = it }
            REQ_PICK_MUSIC -> pkg?.let { prefs.mediaApp = it }
            REQ_PICK_PHONE -> pkg?.let { prefs.phoneApp = it }
            REQ_PICK_DOCK_1 -> pkg?.let { prefs.setDockAppPackage(0, it) }
            REQ_PICK_DOCK_2 -> pkg?.let { prefs.setDockAppPackage(1, it) }
            REQ_PICK_DOCK_3 -> pkg?.let { prefs.setDockAppPackage(2, it) }
            REQ_PICK_DOCK_4 -> pkg?.let { prefs.setDockAppPackage(3, it) }
            REQ_PICK_WIDGET -> {
                val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WidgetHost.INVALID_ID)
                    ?: WidgetHost.INVALID_ID
                if (widgetId != WidgetHost.INVALID_ID) {
                    prefs.addCardWidget(Prefs.SLOT_MEDIA, widgetId)
                    prefs.navWidgetType = Prefs.WIDGET_TYPE_SYSTEM_WIDGET
                    binding.rbNavSystemWidget.isChecked = true
                    Toast.makeText(this, "Виджет добавлен!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        updateAppButtonLabels()
    }

    private fun saveAll() {
        // Section 1: Elements
        prefs.showNavCard = binding.chkNavCard.isChecked
        prefs.showMediaCard = binding.chkMediaCard.isChecked
        prefs.showWeatherCard = binding.chkWeatherCard.isChecked
        prefs.showTripCard = binding.chkTripCard.isChecked
        prefs.showDock = binding.chkDock.isChecked
        prefs.showSidebar = binding.chkSidebar.isChecked
        prefs.showTopBar = binding.chkTopBar.isChecked

        prefs.navWidgetType = when {
            binding.rbNavSystemWidget.isChecked -> Prefs.WIDGET_TYPE_SYSTEM_WIDGET
            binding.rbNavHud.isChecked -> Prefs.WIDGET_TYPE_GPS_HUD
            else -> "live_gps"
        }

        // Section 2: Sizes
        prefs.columnSplit = when {
            binding.rbSplit5050.isChecked -> "50_50"
            binding.rbSplit7030.isChecked -> "70_30"
            binding.rbSplit4060.isChecked -> "40_60"
            else -> "60_40"
        }

        prefs.buttonSizeDp = when {
            binding.rbBtn40.isChecked -> 40
            binding.rbBtn56.isChecked -> 56
            binding.rbBtn64.isChecked -> 64
            else -> 48
        }

        prefs.dockAppCount = when {
            binding.rbDock3.isChecked -> 3
            binding.rbDock5.isChecked -> 5
            binding.rbDock6.isChecked -> 6
            else -> 4
        }

        // Section 3: Position
        prefs.sidebarPosition = if (binding.rbSidebarRight.isChecked) "right" else "left"

        prefs.dashboardLayout = when {
            binding.rbLayoutInverted.isChecked -> Prefs.LAYOUT_COMPACT
            binding.rbLayoutSport.isChecked -> Prefs.LAYOUT_SPORT
            else -> Prefs.LAYOUT_PREMIUM
        }

        prefs.cardsStackOrder = if (binding.rbStackWeatherFirst.isChecked) "weather_first" else "media_first"

        // Section 4: Fonts
        prefs.fontStyle = when {
            binding.rbFontMono.isChecked -> "mono"
            binding.rbFontCondensed.isChecked -> "condensed"
            binding.rbFontSerif.isChecked -> "serif"
            else -> "sans"
        }

        prefs.fontScale = when {
            binding.rbScale85.isChecked -> 0.85f
            binding.rbScale115.isChecked -> 1.15f
            binding.rbScale130.isChecked -> 1.30f
            else -> 1.0f
        }

        prefs.fontBold = binding.chkFontBold.isChecked

        // Section 6: Sensors & Car
        val carModelInput = binding.etCarModel.text.toString().trim()
        if (carModelInput.isNotEmpty()) prefs.carModel = carModelInput

        binding.etCarOdometer.text.toString().trim().toFloatOrNull()?.let {
            prefs.carOdometerBaseline = it
        }

        binding.etCarFuelRate.text.toString().trim().replace(',', '.').toFloatOrNull()?.let {
            prefs.carConsumptionRate = it
        }

        prefs.weatherCity = binding.etWeatherCity.text.toString().trim()
    }

    private fun resetToDefaults() {
        prefs.showNavCard = true
        prefs.showMediaCard = true
        prefs.showWeatherCard = true
        prefs.showTripCard = true
        prefs.showDock = true
        prefs.showSidebar = true
        prefs.showTopBar = true
        prefs.navWidgetType = "live_gps"
        prefs.columnSplit = "60_40"
        prefs.buttonSizeDp = 48
        prefs.dockAppCount = 4
        prefs.sidebarPosition = "left"
        prefs.dashboardLayout = Prefs.LAYOUT_PREMIUM
        prefs.cardsStackOrder = "media_first"
        prefs.fontStyle = "sans"
        prefs.fontScale = 1.0f
        prefs.fontBold = false
        prefs.carModel = "Mazda CX-5"
        prefs.carConsumptionRate = 8.9f
    }
}
