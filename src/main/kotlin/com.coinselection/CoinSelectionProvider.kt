package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import java.math.BigDecimal

interface CoinSelectionProvider {

    fun provide(utxoList: List<UnspentOutput>,
                targetValue: BigDecimal,
                feeRatePerByte: BigDecimal,
                numberOfOutputs: Int = 1,
                compulsoryUtxoList: List<UnspentOutput>? = null,
                hasOpReturnOutput: Boolean = false
    ): CoinSelectionResult?
}