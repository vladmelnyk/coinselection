package com.coinselection.dto

import java.math.BigDecimal

data class CoinSelectionResult(
        val selectedUtxos: List<UnspentOutput>,
        val totalFee: BigDecimal
)