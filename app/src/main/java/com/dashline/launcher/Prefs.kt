// Copyright (C) 2026 Metehan Kaygısız
// SPDX-License-Identifier: GPL-3.0-only

package com.dashline.launcher

import android.content.Context

/**
 * Thin wrapper over SharedPreferences for launcher settings the user configures
 * (chosen phone / nav / radio / media apps) plus the first-run flag.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("radio_launcher", Context.MODE_PRIVATE)

    init {
        migrateLegacyCard()
    }

    var setupDone: Boolean
        get() = sp.getBoolean(KEY_SETUP_DONE, true)
        set(v) = sp.edit().putBoolean(KEY_SETUP_DONE, v).apply()

    /** Package name of the user's chosen phone app, or null if not set yet. */
    var phoneApp: String?
        get() = sp.getString(KEY_PHONE, null)
        set(v) = sp.edit().putString(KEY_PHONE, v).apply()

    /** Navigation app. Defaults to Google Maps per product decision. */
    var navApp: String
        get() = sp.getString(KEY_NAV, DEFAULT_NAV) ?: DEFAULT_NAV
        set(v) = sp.edit().putString(KEY_NAV, v).apply()

    var radioApp: String?
        get() = sp.getString(KEY_RADIO, null)
        set(v) = sp.edit().putString(KEY_RADIO, v).apply()

    /** App to launch from the media widget when nothing is playing. */
    var mediaApp: String?
        get() = sp.getString(KEY_MEDIA, null)
        set(v) = sp.edit().putString(KEY_MEDIA, v).apply()

    /** Phone-projection apps behind the Android Auto / CarPlay shortcuts. */
    var androidAutoApp: String?
        get() = sp.getString(KEY_AUTO, null)
        set(v) = sp.edit().putString(KEY_AUTO, v).apply()

    var carPlayApp: String?
        get() = sp.getString(KEY_CARPLAY, null)
        set(v) = sp.edit().putString(KEY_CARPLAY, v).apply()

    /** true = Fahrenheit, false = Celsius (default). */
    var useFahrenheit: Boolean
        get() = sp.getBoolean(KEY_FAHRENHEIT, false)
        set(v) = sp.edit().putBoolean(KEY_FAHRENHEIT, v).apply()

    /**
     * Whether we've already shown the location prompt once. Setup asks first; the
     * dashboard only asks if setup never got the chance (e.g. upgraded installs),
     * so the user is never nagged on every launch.
     */
    var askedLocation: Boolean
        get() = sp.getBoolean(KEY_ASKED_LOCATION, false)
        set(v) = sp.edit().putBoolean(KEY_ASKED_LOCATION, v).apply()

    /** Language override as an ISO code (e.g. "tr"), or "" to follow the system. */
    var language: String
        get() = sp.getString(KEY_LANGUAGE, "") ?: ""
        set(v) = sp.edit().putString(KEY_LANGUAGE, v).apply()

    /**
     * Theme mode: "auto" (by time), "light", "dark", or "system".
     * Defaults to dark — it's the right look for a dash at night and matches the
     * SYNC-style design; users can switch to auto/light in Settings.
     */
    var themeMode: String
        get() = sp.getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
        set(v) = sp.edit().putString(KEY_THEME, v).apply()

    // ---- Automotive dashboard preferences ----
    var dashboardLayout: String
        get() = sp.getString(KEY_DASHBOARD_LAYOUT, LAYOUT_PREMIUM) ?: LAYOUT_PREMIUM
        set(v) = sp.edit().putString(KEY_DASHBOARD_LAYOUT, v).apply()

    var carModel: String
        get() = sp.getString(KEY_CAR_MODEL, "Mazda CX-5") ?: "Mazda CX-5"
        set(v) = sp.edit().putString(KEY_CAR_MODEL, v).apply()

    var carOdometerBaseline: Float
        get() = sp.getFloat(KEY_CAR_ODOMETER_BASE, 124500f)
        set(v) = sp.edit().putFloat(KEY_CAR_ODOMETER_BASE, v).apply()

    var carGpsTripKm: Float
        get() = sp.getFloat(KEY_CAR_GPS_TRIP, 0f)
        set(v) = sp.edit().putFloat(KEY_CAR_GPS_TRIP, v).apply()

    var carConsumptionRate: Float
        get() = sp.getFloat(KEY_CAR_CONSUMPTION_RATE, 8.9f)
        set(v) = sp.edit().putFloat(KEY_CAR_CONSUMPTION_RATE, v).apply()

    var showNavCard: Boolean
        get() = sp.getBoolean(KEY_SHOW_NAV, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_NAV, v).apply()

    var showMediaCard: Boolean
        get() = sp.getBoolean(KEY_SHOW_MEDIA, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_MEDIA, v).apply()

    var showWeatherCard: Boolean
        get() = sp.getBoolean(KEY_SHOW_WEATHER, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_WEATHER, v).apply()

    var showTripCard: Boolean
        get() = sp.getBoolean(KEY_SHOW_TRIP, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_TRIP, v).apply()

    var showDock: Boolean
        get() = sp.getBoolean(KEY_SHOW_DOCK, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_DOCK, v).apply()

    var showSidebar: Boolean
        get() = sp.getBoolean(KEY_SHOW_SIDEBAR, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_SIDEBAR, v).apply()

    var showTopBar: Boolean
        get() = sp.getBoolean(KEY_SHOW_TOPBAR, true)
        set(v) = sp.edit().putBoolean(KEY_SHOW_TOPBAR, v).apply()

    var navWidgetType: String
        get() = sp.getString(KEY_NAV_WIDGET_TYPE, WIDGET_TYPE_GPS_HUD) ?: WIDGET_TYPE_GPS_HUD
        set(v) = sp.edit().putString(KEY_NAV_WIDGET_TYPE, v).apply()

    var mediaWidgetType: String
        get() = sp.getString(KEY_MEDIA_WIDGET_TYPE, WIDGET_TYPE_PLAYER) ?: WIDGET_TYPE_PLAYER
        set(v) = sp.edit().putString(KEY_MEDIA_WIDGET_TYPE, v).apply()

    var fontScale: Float
        get() = sp.getFloat(KEY_FONT_SCALE, 1.0f)
        set(v) = sp.edit().putFloat(KEY_FONT_SCALE, v).apply()

    var fontStyle: String
        get() = sp.getString(KEY_FONT_STYLE, "sans") ?: "sans"
        set(v) = sp.edit().putString(KEY_FONT_STYLE, v).apply()

    var iconScale: Float
        get() = sp.getFloat(KEY_ICON_SCALE, 1.0f)
        set(v) = sp.edit().putFloat(KEY_ICON_SCALE, v).apply()

    var weatherCity: String
        get() = sp.getString(KEY_WEATHER_CITY, "") ?: ""
        set(v) = sp.edit().putString(KEY_WEATHER_CITY, v).apply()

    var cachedWeatherJson: String?
        get() = sp.getString(KEY_CACHED_WEATHER, null)
        set(v) = sp.edit().putString(KEY_CACHED_WEATHER, v).apply()

    var sidebarPosition: String
        get() = sp.getString(KEY_SIDEBAR_POS, "left") ?: "left"
        set(v) = sp.edit().putString(KEY_SIDEBAR_POS, v).apply()

    var columnSplit: String
        get() = sp.getString(KEY_COLUMN_SPLIT, "60_40") ?: "60_40"
        set(v) = sp.edit().putString(KEY_COLUMN_SPLIT, v).apply()

    var fontBold: Boolean
        get() = sp.getBoolean(KEY_FONT_BOLD, false)
        set(v) = sp.edit().putBoolean(KEY_FONT_BOLD, v).apply()

    var buttonSizeDp: Int
        get() = sp.getInt(KEY_BUTTON_SIZE_DP, 48)
        set(v) = sp.edit().putInt(KEY_BUTTON_SIZE_DP, v).apply()

    var dockAppCount: Int
        get() = sp.getInt(KEY_DOCK_APP_COUNT, 4).coerceIn(3, 6)
        set(v) = sp.edit().putInt(KEY_DOCK_APP_COUNT, v.coerceIn(3, 6)).apply()

    var clockDigitSize: String
        get() = sp.getString(KEY_CLOCK_DIGIT_SIZE, "normal") ?: "normal"
        set(v) = sp.edit().putString(KEY_CLOCK_DIGIT_SIZE, v).apply()

    var speedUnit: String
        get() = sp.getString(KEY_SPEED_UNIT, "kmh") ?: "kmh"
        set(v) = sp.edit().putString(KEY_SPEED_UNIT, v).apply()

    var cardsStackOrder: String
        get() = sp.getString(KEY_CARDS_STACK_ORDER, "media_first") ?: "media_first"
        set(v) = sp.edit().putString(KEY_CARDS_STACK_ORDER, v).apply()

    fun dockAppPackage(slotIndex: Int): String? =
        sp.getString("dock_app_slot_$slotIndex", null)

    fun setDockAppPackage(slotIndex: Int, pkg: String) =
        sp.edit().putString("dock_app_slot_$slotIndex", pkg).apply()

    fun sidebarAppPackage(slotIndex: Int): String? =
        sp.getString("sidebar_app_slot_$slotIndex", null)

    fun setSidebarAppPackage(slotIndex: Int, pkg: String) =
        sp.edit().putString("sidebar_app_slot_$slotIndex", pkg).apply()

    // ---- home screen panels -------------------------------------------------

    /** PANEL_MEDIA_FIRST (default) or PANEL_PHONE_FIRST — which card sits on top. */
    var panelOrder: String
        get() = sp.getString(KEY_PANEL_ORDER, PANEL_MEDIA_FIRST) ?: PANEL_MEDIA_FIRST
        set(v) = sp.edit().putString(KEY_PANEL_ORDER, v).apply()

    /**
     * Share of the right column given to the media card, 0..1. Set by dragging
     * the divider in the Customize screen, so it's continuous rather than a
     * handful of presets. Clamped so neither card can collapse to nothing.
     */
    var mediaFraction: Float
        get() = sp.getFloat(KEY_MEDIA_FRACTION, 0.72f).coerceIn(MIN_FRACTION, MAX_FRACTION)
        set(v) = sp.edit().putFloat(KEY_MEDIA_FRACTION, v.coerceIn(MIN_FRACTION, MAX_FRACTION)).apply()

    // ---- dashboard cards ----------------------------------------------------

    /**
     * Both cards share one model: a slot with a content mode, its own shortcut
     * slots and its own list of hosted widgets. The media card simply defaults
     * to the player and the lower card to the phone shortcut.
     */
    fun cardMode(slot: String): String =
        sp.getString(modeKey(slot), defaultMode(slot)) ?: defaultMode(slot)

    fun setCardMode(slot: String, mode: String) =
        sp.edit().putString(modeKey(slot), mode).apply()

    /** What a slot shows before the user changes anything. */
    fun defaultCardMode(slot: String): String = when (slot) {
        SLOT_MEDIA -> CARD_MEDIA
        SLOT_CLOCK -> CARD_CLOCK
        SLOT_WEATHER -> CARD_WEATHER
        else -> CARD_PHONE
    }

    private fun defaultMode(slot: String) = defaultCardMode(slot)

    /** Hosted widget ids for a slot, in display order. */
    fun cardWidgets(slot: String): List<Int> =
        (sp.getString(widgetsKey(slot), "") ?: "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it != INVALID_WIDGET }

    fun setCardWidgets(slot: String, ids: List<Int>) =
        // Capped here as well as in [addCardWidget]: replace and repair paths
        // write straight through, and a card with more than this is unreadable.
        sp.edit()
            .putString(widgetsKey(slot), ids.distinct().take(MAX_CARD_WIDGETS).joinToString(","))
            .apply()

    /** Every widget id the dashboard refers to, across all four slots. */
    fun widgetIdsInUse(): Set<Int> =
        listOf(SLOT_MEDIA, SLOT_SECOND, SLOT_CLOCK, SLOT_WEATHER)
            .flatMap { cardWidgets(it) }
            .toSet()

    /** Whether any card is set to show widgets at all. */
    fun anyCardUsesWidgets(): Boolean =
        listOf(SLOT_MEDIA, SLOT_SECOND, SLOT_CLOCK, SLOT_WEATHER)
            .any { cardMode(it) == CARD_WIDGET }

    /** Returns false when the card is already full. */
    fun addCardWidget(slot: String, id: Int): Boolean {
        val ids = cardWidgets(slot)
        if (id == INVALID_WIDGET || id in ids || ids.size >= MAX_CARD_WIDGETS) return false
        setCardWidgets(slot, ids + id)
        return true
    }

    fun removeCardWidget(slot: String, id: Int) =
        setCardWidgets(slot, cardWidgets(slot) - id)

    /** Swap one widget for another in place, keeping its position on the card. */
    fun replaceCardWidget(slot: String, oldId: Int, newId: Int) =
        setCardWidgets(slot, cardWidgets(slot).map { if (it == oldId) newId else it })

    fun cardShortcut(slot: String, index: Int): String? =
        sp.getString(shortcutKey(slot, index), null)

    fun setCardShortcut(slot: String, index: Int, pkg: String) =
        sp.edit().putString(shortcutKey(slot, index), pkg).apply()

    fun clearCardShortcut(slot: String, index: Int) =
        sp.edit().remove(shortcutKey(slot, index)).apply()

    private fun modeKey(slot: String) = "card_${slot}_mode"
    private fun widgetsKey(slot: String) = "card_${slot}_widgets"
    private fun shortcutKey(slot: String, index: Int) = "card_${slot}_shortcut_$index"

    /**
     * Earlier versions only had a configurable lower card, with a single widget.
     * Carry those settings into the per-slot keys once, so an upgrade doesn't
     * silently reset someone's dashboard.
     */
    private fun migrateLegacyCard() {
        if (sp.getBoolean(KEY_CARDS_MIGRATED, false)) return

        // The legacy mode values match the new ones, so they carry over as-is.
        sp.getString(KEY_SECOND_MODE, null)?.let { setCardMode(SLOT_SECOND, it) }
        val legacyWidget = sp.getInt(KEY_WIDGET_ID, INVALID_WIDGET)
        if (legacyWidget != INVALID_WIDGET) setCardWidgets(SLOT_SECOND, listOf(legacyWidget))
        for (index in 0 until PANEL_SHORTCUT_COUNT) {
            sp.getString("panel_shortcut_$index", null)
                ?.let { setCardShortcut(SLOT_SECOND, index, it) }
        }
        sp.edit().putBoolean(KEY_CARDS_MIGRATED, true).apply()
    }

    // ---- update check (sideloaded builds only) ------------------------------

    /** When the GitHub release check last ran, so it runs at most once a day. */
    var lastUpdateCheck: Long
        get() = sp.getLong(KEY_UPDATE_CHECKED, 0L)
        set(v) = sp.edit().putLong(KEY_UPDATE_CHECKED, v).apply()

    /**
     * A version the user chose not to install. Stored per version rather than as
     * an on/off switch, so declining one update doesn't hide every later one.
     */
    var skippedVersion: String
        get() = sp.getString(KEY_UPDATE_SKIPPED, "") ?: ""
        set(v) = sp.edit().putString(KEY_UPDATE_SKIPPED, v).apply()

    /**
     * The Android Auto / CarPlay tiles under the clock panel. Plenty of units
     * have no projection app at all, and the row is dead space there.
     */
    var projectionVisible: Boolean
        get() = sp.getBoolean(KEY_PROJECTION_ON, true)
        set(v) = sp.edit().putBoolean(KEY_PROJECTION_ON, v).apply()

    // ---- clock ------------------------------------------------------------

    /** CLOCK_DIGITAL (default), CLOCK_ANALOG or CLOCK_MINIMAL. */
    var clockStyle: String
        get() = sp.getString(KEY_CLOCK_STYLE, CLOCK_DIGITAL) ?: CLOCK_DIGITAL
        set(v) = sp.edit().putString(KEY_CLOCK_STYLE, v).apply()

    /** Stopwatch on the dashboard. Off by default — it's a niche extra. */
    var chronoEnabled: Boolean
        get() = sp.getBoolean(KEY_CHRONO_ON, false)
        set(v) = sp.edit().putBoolean(KEY_CHRONO_ON, v).apply()

    /** elapsedRealtime() when the stopwatch was last started, or 0 if paused. */
    var chronoStartedAt: Long
        get() = sp.getLong(KEY_CHRONO_START, 0L)
        set(v) = sp.edit().putLong(KEY_CHRONO_START, v).apply()

    /** Milliseconds banked from previous runs, so pause/resume accumulates. */
    var chronoAccumulated: Long
        get() = sp.getLong(KEY_CHRONO_ACC, 0L)
        set(v) = sp.edit().putLong(KEY_CHRONO_ACC, v).apply()

    val chronoRunning: Boolean get() = chronoStartedAt != 0L

    // ---- bottom tab bar ----------------------------------------------------

    /**
     * Tab order, including hidden ones. Stored as a CSV of TabAction ids so new
     * tabs added in a future version simply appear at the end rather than
     * invalidating a saved order.
     */
    var tabOrder: List<TabAction>
        get() {
            val saved = sp.getString(KEY_TAB_ORDER, null)
                ?.split(",")
                ?.mapNotNull { TabAction.byId(it.trim()) }
                ?: return TabAction.DEFAULT_ORDER
            // Append any tab the saved list doesn't know about yet.
            return saved + TabAction.DEFAULT_ORDER.filterNot { it in saved }
        }
        set(v) = sp.edit().putString(KEY_TAB_ORDER, v.joinToString(",") { it.id }).apply()

    var hiddenTabs: Set<String>
        get() = HashSet(sp.getStringSet(KEY_TABS_HIDDEN, emptySet()) ?: emptySet())
        set(v) = sp.edit().putStringSet(KEY_TABS_HIDDEN, v).apply()

    fun isTabVisible(tab: TabAction): Boolean = !tab.canHide || tab.id !in hiddenTabs

    fun setTabVisible(tab: TabAction, visible: Boolean) {
        if (!tab.canHide) return
        hiddenTabs = if (visible) hiddenTabs - tab.id else hiddenTabs + tab.id
    }

    /** Tabs actually drawn, in order. */
    fun visibleTabs(): List<TabAction> = tabOrder.filter { isTabVisible(it) }

    /** Id of the selected UI colour gradient (see GradientThemes.PRESETS). */
    var gradient: String
        get() = sp.getString(KEY_GRADIENT, "midnight") ?: "midnight"
        set(v) = sp.edit().putString(KEY_GRADIENT, v).apply()

    /** Keep the screen on while the dashboard is showing. */
    var keepScreenOn: Boolean
        get() = sp.getBoolean(KEY_KEEP_ON, true)
        set(v) = sp.edit().putBoolean(KEY_KEEP_ON, v).apply()

    // ---- favorite-apps dock ------------------------------------------------

    /**
     * Whether the top-bar dock is shown at all. Off is a real setup, not a
     * degenerate one: with favourites pinned to a card the dock is a second copy
     * of the same apps, and the bar looks better without it.
     */
    var favoritesEnabled: Boolean
        get() = sp.getBoolean(KEY_FAV_ON, true)
        set(v) = sp.edit().putBoolean(KEY_FAV_ON, v).apply()

    /** How many quick-launch slots the top bar shows. */
    var favoriteCount: Int
        get() = sp.getInt(KEY_FAV_COUNT, FAVORITE_COUNT)
            .coerceIn(FavoriteDock.MIN_SLOTS, FavoriteDock.MAX_SLOTS)
        set(v) = sp.edit()
            .putInt(KEY_FAV_COUNT, v.coerceIn(FavoriteDock.MIN_SLOTS, FavoriteDock.MAX_SLOTS))
            .apply()

    /**
     * Preferred icon size. A ceiling rather than a demand — the dock drops to a
     * smaller one when the chosen count wouldn't fit; see [FavoriteDock].
     */
    var favoriteSize: String
        get() = sp.getString(KEY_FAV_SIZE, FavoriteDock.SIZE_MEDIUM) ?: FavoriteDock.SIZE_MEDIUM
        set(v) = sp.edit().putString(KEY_FAV_SIZE, v).apply()

    fun getFavorite(index: Int): String? = sp.getString(favKey(index), null)

    fun setFavorite(index: Int, pkg: String) =
        sp.edit().putString(favKey(index), pkg).apply()

    fun clearFavorite(index: Int) = sp.edit().remove(favKey(index)).apply()

    private fun favKey(index: Int) = "fav_$index"

    // ---- hidden apps (from the drawer) -------------------------------------

    /** Defensive copy — the Set returned by SharedPreferences must not be mutated. */
    val hiddenApps: Set<String>
        get() = HashSet(sp.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet())

    fun hideApp(pkg: String) =
        sp.edit().putStringSet(KEY_HIDDEN, hiddenApps + pkg).apply()

    fun clearHiddenApps() = sp.edit().remove(KEY_HIDDEN).apply()

    /** Resolve the effective candidate list for a role (user choice first). */
    fun candidatesFor(role: String): List<String> = when (role) {
        ROLE_PHONE -> listOfNotNull(phoneApp) + Defaults.PHONE
        ROLE_NAV -> listOf(navApp) + Defaults.NAV
        ROLE_RADIO -> listOfNotNull(radioApp) + Defaults.RADIO
        ROLE_MEDIA -> listOfNotNull(mediaApp) + Defaults.MEDIA
        ROLE_ANDROID_AUTO -> listOfNotNull(androidAutoApp) + Defaults.ANDROID_AUTO
        ROLE_CARPLAY -> listOfNotNull(carPlayApp) + Defaults.CARPLAY
        else -> emptyList()
    }

    fun setForRole(role: String, pkg: String) {
        when (role) {
            ROLE_PHONE -> phoneApp = pkg
            ROLE_NAV -> navApp = pkg
            ROLE_RADIO -> radioApp = pkg
            ROLE_MEDIA -> mediaApp = pkg
            ROLE_ANDROID_AUTO -> androidAutoApp = pkg
            ROLE_CARPLAY -> carPlayApp = pkg
        }
    }

    companion object {
        const val DEFAULT_NAV = "com.google.android.apps.maps"

        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_PHONE = "phone_app"
        private const val KEY_NAV = "nav_app"
        private const val KEY_RADIO = "radio_app"
        private const val KEY_MEDIA = "media_app"
        private const val KEY_AUTO = "android_auto_app"
        private const val KEY_CARPLAY = "carplay_app"
        private const val KEY_FAHRENHEIT = "use_fahrenheit"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_KEEP_ON = "keep_screen_on"
        private const val KEY_HIDDEN = "hidden_apps"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_ASKED_LOCATION = "asked_location"
        private const val KEY_GRADIENT = "gradient"
        private const val KEY_PANEL_ORDER = "panel_order"
        private const val KEY_MEDIA_FRACTION = "media_fraction"
        private const val KEY_WIDGET_ID = "panel_widget_id"

        /** Neither card may collapse; keeps both usable at any drag position. */
        const val MIN_FRACTION = 0.25f
        const val MAX_FRACTION = 0.80f
        private const val KEY_SECOND_MODE = "second_card_mode"

        const val PANEL_MEDIA_FIRST = "media_first"
        const val PANEL_PHONE_FIRST = "phone_first"

        /** The configurable cards in the right-hand column. */
        const val SLOT_MEDIA = "media"
        const val SLOT_SECOND = "second"

        /**
         * The clock panel's two halves. Portrait only: landscape needs them both
         * to fill the left column, and there's no drawer below to take up slack.
         */
        const val SLOT_CLOCK = "clock"
        const val SLOT_WEATHER = "weather"

        /**
         * What a card shows. CARD_MEDIA is only meaningful for the media slot and
         * CARD_PHONE for the lower one; the other two apply to either.
         */
        const val CARD_MEDIA = "media"
        const val CARD_PHONE = "phone"
        const val CARD_CLOCK = "clock"
        const val CARD_WEATHER = "weather"
        const val CARD_SHORTCUTS = "shortcuts"
        const val CARD_WIDGET = "widget"

        /**
         * The slot is hidden entirely. Its neighbours take the space back, so a
         * dashboard with fewer sections still fills the screen rather than
         * leaving a gap where the card used to be.
         */
        const val CARD_NONE = "none"

        /** Widgets get unreadably narrow past this, even on a 10" unit. */
        const val MAX_CARD_WIDGETS = 3
        private const val INVALID_WIDGET = -1
        private const val KEY_CARDS_MIGRATED = "cards_migrated"

        /** Shortcut slots in a card when it's in shortcuts mode. */
        const val PANEL_SHORTCUT_COUNT = 4

        private const val KEY_UPDATE_CHECKED = "update_last_checked"
        private const val KEY_UPDATE_SKIPPED = "update_skipped_version"

        private const val KEY_CLOCK_STYLE = "clock_style"
        private const val KEY_CHRONO_ON = "chrono_enabled"
        private const val KEY_CHRONO_START = "chrono_started_at"
        private const val KEY_CHRONO_ACC = "chrono_accumulated"

        const val CLOCK_DIGITAL = "digital"
        const val CLOCK_ANALOG = "analog"
        const val CLOCK_MINIMAL = "minimal"

        private const val KEY_TAB_ORDER = "tab_order"
        private const val KEY_TABS_HIDDEN = "tabs_hidden"

        const val THEME_AUTO = "auto"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        const val ROLE_PHONE = "phone"
        const val ROLE_NAV = "nav"
        const val ROLE_RADIO = "radio"
        const val ROLE_MEDIA = "media"
        const val ROLE_ANDROID_AUTO = "android_auto"
        const val ROLE_CARPLAY = "carplay"
        const val ROLE_FAVORITE = "favorite"

        const val LAYOUT_PREMIUM = "premium"
        const val LAYOUT_COMPACT = "compact"
        const val LAYOUT_SPORT = "sport"

        const val WIDGET_TYPE_GPS_HUD = "gps_hud"
        const val WIDGET_TYPE_SYSTEM_WIDGET = "system_widget"
        const val WIDGET_TYPE_QUICK_ACTIONS = "quick_actions"
        const val WIDGET_TYPE_PLAYER = "player"

        private const val KEY_DASHBOARD_LAYOUT = "dashboard_layout"
        private const val KEY_CAR_MODEL = "car_model"
        private const val KEY_CAR_ODOMETER_BASE = "car_odo_base"
        private const val KEY_CAR_GPS_TRIP = "car_gps_trip"
        private const val KEY_CAR_CONSUMPTION_RATE = "car_consumption_rate"
        private const val KEY_SHOW_NAV = "show_nav"
        private const val KEY_SHOW_MEDIA = "show_media"
        private const val KEY_SHOW_WEATHER = "show_weather"
        private const val KEY_SHOW_TRIP = "show_trip"
        private const val KEY_SHOW_DOCK = "show_dock"
        private const val KEY_SHOW_SIDEBAR = "show_sidebar"
        private const val KEY_SHOW_TOPBAR = "show_topbar"
        private const val KEY_NAV_WIDGET_TYPE = "nav_widget_type"
        private const val KEY_MEDIA_WIDGET_TYPE = "media_widget_type"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_FONT_STYLE = "font_style"
        private const val KEY_ICON_SCALE = "icon_scale"
        private const val KEY_WEATHER_CITY = "weather_city"
        private const val KEY_CACHED_WEATHER = "cached_weather_json"
        private const val KEY_SIDEBAR_POS = "sidebar_position"
        private const val KEY_COLUMN_SPLIT = "column_split"
        private const val KEY_FONT_BOLD = "font_bold"
        private const val KEY_BUTTON_SIZE_DP = "button_size_dp"
        private const val KEY_DOCK_APP_COUNT = "dock_app_count"
        private const val KEY_CLOCK_DIGIT_SIZE = "clock_digit_size"
        private const val KEY_SPEED_UNIT = "speed_unit"
        private const val KEY_CARDS_STACK_ORDER = "cards_stack_order"

        /** Default number of quick-launch slots in the top-bar dock. */
        const val FAVORITE_COUNT = 5
        private const val KEY_FAV_COUNT = "favorite_count"
        private const val KEY_FAV_ON = "favorites_enabled"
        private const val KEY_PROJECTION_ON = "projection_row"
        private const val KEY_FAV_SIZE = "favorite_size"
    }
}

/** Common package names to try when the user hasn't picked one for a role. */
object Defaults {
    val PHONE = listOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer"
    )
    val NAV = listOf(
        "ru.yandex.yandexnavi",
        "ru.yandex.yandexmaps",
        "ru.dublgis.dgismobile",
        "com.google.android.apps.maps",
        "com.waze",
        "com.sygic.aura"
    )
    val RADIO = listOf(
        "com.mtk.fm",
        "com.microntek.radio",
        "com.android.fmradio",
        "com.rtk.fmradio"
    )
    val MEDIA = listOf(
        "ru.yandex.music",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.google.android.music",
        "com.maxmpz.audioplayer",
        "com.android.music"
    )

    /**
     * Phone projection. On head units this is usually a third-party "link" app
     * (ZLink, EasyConnected, AutoKit, Carbit) that provides both Android Auto and
     * CarPlay, rather than Google's own Android Auto — which most units can't run.
     * These are best-guess defaults; the user can pick their own in Settings.
     */
    val ANDROID_AUTO = listOf(
        "com.google.android.projection.gearhead",
        "com.zjinnova.zlink",
        "net.easyconn",
        "com.autokit.carlink",
        "com.carbit.link"
    )
    val CARPLAY = listOf(
        "com.zjinnova.zlink",
        "net.easyconn",
        "com.autokit.carlink",
        "com.carbit.link",
        "com.hzbhd.carplay"
    )
}
