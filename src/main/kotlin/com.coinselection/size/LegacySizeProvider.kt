package com.coinselection.size

import com.coinselection.model.TransactionSize

object LegacySizeProvider : TransactionSizeProvider {
    override fun provide(): TransactionSize {
        return TransactionSize(input = 147, output = 34, header = 11)
    }
}