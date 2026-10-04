package com.example.appfinancetest.classes

import com.example.appfinancetest.R

enum class HomeTimeRange(val labelResId: Int, val periodNameResId: Int, val days: Double) {
    ONE_MONTH(R.string.range_1m, R.string.period_1_month, 30.0),
    SIX_MONTHS(R.string.range_6m, R.string.period_6_months, 182.0),
    ONE_YEAR(R.string.range_1y, R.string.period_1_year, 365.0),
    FIVE_YEARS(R.string.range_5y, R.string.period_5_years, 1825.0),
    ALL(R.string.range_all, R.string.period_all, 36500.0)
}
