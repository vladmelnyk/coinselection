package com.coinselection

import com.coinselection.model.Algorithm
import com.coinselection.model.Algorithm.LARGEST_FIRST
import com.coinselection.model.Algorithm.RANDOM_IMPROVE

object CoinSelectionFactory {

    fun create(algorithm: Algorithm): CoinSelectionProvider {
        return when (algorithm) {
            RANDOM_IMPROVE -> RandomImproveCoinSelectionProvider
            LARGEST_FIRST -> LargestFirstCoinSelectionProvider
        }
    }
}