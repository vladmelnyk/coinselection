package com.coinselection.size

import com.coinselection.model.TransactionSize

object SegwitLegacyCompatibleSizeProvider : TransactionSizeProvider {
    override fun provide(): TransactionSize {
        return TransactionSize(input = 91, output = 32, header = 11)
    }
}