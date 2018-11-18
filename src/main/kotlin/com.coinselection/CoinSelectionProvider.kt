package com.coinselection

import java.math.BigDecimal

// Default sizes in bytes for Segwit legacy-compatible addresses (starting with 3...)
private const val INPUT_SIZE = 91
private const val OUTPUT_SIZE = 32
private const val HEADER_SIZE = 11
private const val MAX_INPUT = 60

interface CoinSelectionProvider {
    fun provide(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, maxNumberOfInputs: Int = MAX_INPUT, numberOfDestinationAddress: Int = 1, inputSize: Int = INPUT_SIZE, outputSize: Int = OUTPUT_SIZE, headerSize: Int = HEADER_SIZE): List<UnspentOutput>
}