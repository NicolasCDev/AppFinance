package com.example.appfinancetest.classes

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "slider_prefs")

class DataStorage(private val context: Context) {
    companion object {
        // Dashboard Filter keys
        val DASHBOARD_DATE_MIN_FILTER_KEY = stringPreferencesKey("dashboard_date_min_filter")
        val DASHBOARD_DATE_MAX_FILTER_KEY = stringPreferencesKey("dashboard_date_max_filter")
        val DASHBOARD_CATEGORY_FILTER_KEY = stringPreferencesKey("dashboard_category_filter")
        val DASHBOARD_ITEM_FILTER_KEY = stringPreferencesKey("dashboard_item_filter")
        val DASHBOARD_LABEL_FILTER_KEY = stringPreferencesKey("dashboard_label_filter")
        val DASHBOARD_AMOUNT_MIN_FILTER_KEY = stringPreferencesKey("dashboard_amount_min_filter")
        val DASHBOARD_AMOUNT_MAX_FILTER_KEY = stringPreferencesKey("dashboard_amount_max_filter")

        // Patrimonial keys
        val PATRIMONIAL_START_DATE_KEY = floatPreferencesKey("patrimonial_start_date")
        val PATRIMONIAL_END_DATE_KEY = floatPreferencesKey("patrimonial_end_date")
        val PATRIMONIAL_OPTION_KEY = stringPreferencesKey("patrimonial_selected_option")

        // Visibility key
        val IS_VISIBILITY_OFF_KEY = booleanPreferencesKey("is_visibility_off")

        // Home key
        val HOME_TIME_RANGE_KEY = stringPreferencesKey("home_time_range")
        
        // Theme key
        val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")

        // Investment Second Screen keys
        val INVESTMENT_SECOND_TAB_INDEX_KEY = intPreferencesKey("investment_second_tab_index")
        val INVESTMENT_SECOND_CATEGORY_KEY = stringPreferencesKey("investment_second_category")
        val INVESTMENT_SECOND_VIEW_MODE_KEY = stringPreferencesKey("investment_second_view_mode")

        // Budget Screen keys
        val BUDGET_CATEGORY_KEY = stringPreferencesKey("budget_category")
        val BUDGET_PERIOD_MODE_KEY = stringPreferencesKey("budget_period_mode")
        val BUDGET_SELECTED_YEAR_KEY = intPreferencesKey("budget_selected_year")
        val BUDGET_SELECTED_MONTH_KEY = intPreferencesKey("budget_selected_month")
    }

    // Dashboard Filter Flows
    val dashboardDateMinFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_DATE_MIN_FILTER_KEY] }
    val dashboardDateMaxFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_DATE_MAX_FILTER_KEY] }
    val dashboardCategoryFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_CATEGORY_FILTER_KEY] }
    val dashboardItemFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_ITEM_FILTER_KEY] }
    val dashboardLabelFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_LABEL_FILTER_KEY] }
    val dashboardAmountMinFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_AMOUNT_MIN_FILTER_KEY] }
    val dashboardAmountMaxFilterFlow: Flow<String?> = context.dataStore.data.map { it[DASHBOARD_AMOUNT_MAX_FILTER_KEY] }

    // Patrimonial Flows
    val patrimonialStartDateFlow: Flow<Float?> = context.dataStore.data.map { it[PATRIMONIAL_START_DATE_KEY] }
    val patrimonialEndDateFlow: Flow<Float?> = context.dataStore.data.map { it[PATRIMONIAL_END_DATE_KEY] }
    val patrimonialOptionFlow: Flow<String?> = context.dataStore.data.map { it[PATRIMONIAL_OPTION_KEY] }

    // Visibility Flow - Set default to true to hide values on first open
    val isVisibilityOffFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_VISIBILITY_OFF_KEY] ?: true }

    // Theme Flow - null means use system default
    val isDarkThemeFlow: Flow<Boolean?> = context.dataStore.data.map { it[IS_DARK_THEME_KEY] }

    // Save methods for Dashboard Filters
    suspend fun saveDashboardDateMinFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_DATE_MIN_FILTER_KEY] = filter }
    }
    suspend fun saveDashboardDateMaxFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_DATE_MAX_FILTER_KEY] = filter }
    }
    suspend fun saveDashboardCategoryFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_CATEGORY_FILTER_KEY] = filter }
    }
    suspend fun saveDashboardItemFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_ITEM_FILTER_KEY] = filter }
    }
    suspend fun saveDashboardLabelFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_LABEL_FILTER_KEY] = filter }
    }
    suspend fun saveDashboardAmountMinFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_AMOUNT_MIN_FILTER_KEY] = filter }
    }
    suspend fun saveDashboardAmountMaxFilter(filter: String) {
        context.dataStore.edit { prefs -> prefs[DASHBOARD_AMOUNT_MAX_FILTER_KEY] = filter }
    }

    // Save methods for Patrimonial
    suspend fun savePatrimonialStartDate(date: Float) {
        context.dataStore.edit { prefs -> prefs[PATRIMONIAL_START_DATE_KEY] = date }
    }
    suspend fun savePatrimonialEndDate(date: Float) {
        context.dataStore.edit { prefs -> prefs[PATRIMONIAL_END_DATE_KEY] = date }
    }
    suspend fun savePatrimonialOption(option: String) {
        context.dataStore.edit { prefs -> prefs[PATRIMONIAL_OPTION_KEY] = option }
    }

    // Save method for Visibility
    suspend fun saveVisibilityState(isOff: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_VISIBILITY_OFF_KEY] = isOff }
    }

    // Home Time Range Flow & Save
    val homeTimeRangeFlow: Flow<String?> = context.dataStore.data.map { it[HOME_TIME_RANGE_KEY] }

    suspend fun saveHomeTimeRange(rangeName: String) {
        context.dataStore.edit { prefs -> prefs[HOME_TIME_RANGE_KEY] = rangeName }
    }

    // Save method for Theme
    suspend fun saveDarkThemeState(isDark: Boolean?) {
        context.dataStore.edit { prefs ->
            if (isDark == null) {
                prefs.remove(IS_DARK_THEME_KEY)
            } else {
                prefs[IS_DARK_THEME_KEY] = isDark
            }
        }
    }

    // Investment Second Screen Flows & Save methods
    val investmentSecondTabIndexFlow: Flow<Int?> = context.dataStore.data.map { it[INVESTMENT_SECOND_TAB_INDEX_KEY] }
    val investmentSecondCategoryFlow: Flow<String?> = context.dataStore.data.map { it[INVESTMENT_SECOND_CATEGORY_KEY] }
    val investmentSecondViewModeFlow: Flow<String?> = context.dataStore.data.map { it[INVESTMENT_SECOND_VIEW_MODE_KEY] }

    suspend fun saveInvestmentSecondTabIndex(index: Int) {
        context.dataStore.edit { prefs -> prefs[INVESTMENT_SECOND_TAB_INDEX_KEY] = index }
    }
    suspend fun saveInvestmentSecondCategory(category: String) {
        context.dataStore.edit { prefs -> prefs[INVESTMENT_SECOND_CATEGORY_KEY] = category }
    }
    suspend fun saveInvestmentSecondViewMode(viewMode: String) {
        context.dataStore.edit { prefs -> prefs[INVESTMENT_SECOND_VIEW_MODE_KEY] = viewMode }
    }

    // Budget Screen Flows & Save methods
    val budgetCategoryFlow: Flow<String?> = context.dataStore.data.map { it[BUDGET_CATEGORY_KEY] }
    val budgetPeriodModeFlow: Flow<String?> = context.dataStore.data.map { it[BUDGET_PERIOD_MODE_KEY] }
    val budgetSelectedYearFlow: Flow<Int?> = context.dataStore.data.map { it[BUDGET_SELECTED_YEAR_KEY] }
    val budgetSelectedMonthFlow: Flow<Int?> = context.dataStore.data.map { it[BUDGET_SELECTED_MONTH_KEY] }

    suspend fun saveBudgetCategory(category: String) {
        context.dataStore.edit { prefs -> prefs[BUDGET_CATEGORY_KEY] = category }
    }
    suspend fun saveBudgetPeriodMode(periodMode: String) {
        context.dataStore.edit { prefs -> prefs[BUDGET_PERIOD_MODE_KEY] = periodMode }
    }
    suspend fun saveBudgetSelectedYear(year: Int) {
        context.dataStore.edit { prefs -> prefs[BUDGET_SELECTED_YEAR_KEY] = year }
    }
    suspend fun saveBudgetSelectedMonth(month: Int) {
        context.dataStore.edit { prefs -> prefs[BUDGET_SELECTED_MONTH_KEY] = month }
    }
}
