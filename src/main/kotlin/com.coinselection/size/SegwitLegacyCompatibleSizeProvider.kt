package com.coinselection.size

import com.coinselection.model.TransactionComponentsSize

object SegwitLegacyCompatibleSizeProvider : TransactionSizeProvider {
    override fun provide(): TransactionComponentsSize {
        return TransactionComponentsSize(input = 91, output = 32, header = 11)
    }
}