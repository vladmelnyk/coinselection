package com.coinselection.dto

import java.math.BigDecimal

data class UnspentOutput(
        val txid: String? = null,
        val vout: Int? = null,
        val label: String? = null,
        val address: String? = null,
        val scriptPubKey: String? = null,
        val amount: BigDecimal,
        val confirmations: Int? = null,
        val redeemScript: String? = null,
        val spendable: Boolean? = null,
        val solvable: Boolean? = null,
        val safe: Boolean? = null
)