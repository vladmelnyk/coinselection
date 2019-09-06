package com.coinselection.model

import com.coinselection.dto.UnspentOutput

data class UtxoSumCalculationData(
        val utxoList: List<UnspentOutput>,
        val cumulativeHolder: CumulativeHolder
)