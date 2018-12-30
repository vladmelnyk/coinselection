package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.TransactionSize
import com.coinselection.size.SegwitLegacyCompatibleSizeProvider
import java.math.BigDecimal

private const val MAX_INPUT = 60

interface CoinSelectionProvider {
    fun provide(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, maxNumberOfInputs: Int = MAX_INPUT, numberOfDestinationAddress: Int = 1, transactionSize: TransactionSize = SegwitLegacyCompatibleSizeProvider.provide()): CoinSelectionResult
}