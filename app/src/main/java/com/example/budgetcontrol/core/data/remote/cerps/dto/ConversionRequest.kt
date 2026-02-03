package com.example.budgetcontrol.core.data.remote.cerps.dto

import java.math.BigDecimal

data class ConversionRequest(
    val amount: BigDecimal,
    val from: String,
    val to: String
) {
    constructor(amount: Double, from: String, to: String) : this(
        amount = BigDecimal.valueOf(amount),
        from = from,
        to = to
    )
}