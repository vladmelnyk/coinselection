package com.coinselection

import com.coinselection.model.Algorithm
import com.coinselection.model.Algorithm.LARGEST_FIRST
import com.coinselection.model.Algorithm.RANDOM_IMPROVE
import com.coinselection.model.TransactionSize
import com.coinselection.size.SegwitLegacyCompatibleSizeProvider

object CoinSelectionFactory {

    fun create(
            algorithm: Algorithm,
            maxNumberOfInputs: Int = MAX_INPUT,
            transactionSize: TransactionSize = SegwitLegacyCompatibleSizeProvider.provide()
    ): CoinSelectionProvider {
        return when (algorithm) {
            RANDOM_IMPROVE -> RandomImproveCoinSelectionProvider(maxNumberOfInputs, transactionSize)
            LARGEST_FIRST -> LargestFirstCoinSelectionProvider(maxNumberOfInputs, transactionSize)
        }
    }
}