package com.example.budgetcontrol.core.util

const val RATES_STALE_MS = 8 * 60 * 60 * 1000L
const val HEALTH_CHECK_CACHE_MS = 30_000L
const val CERPS_TIMEOUT_MS = 3_000L
const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
const val LTTB_THRESHOLD = 100
// Pin Y-axis when observed range is below 0.1% of base rate
const val FLAT_REL_THRESHOLD = 0.001f

val PERIODS = listOf("1D", "7D", "30D", "90D", "180D")
