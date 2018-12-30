package com.coinselection.size

import com.coinselection.model.TransactionComponentsSize

object LegacySizeProvider : TransactionSizeProvider {
    override fun provide(): TransactionComponentsSize {
        return TransactionComponentsSize(input = 147, output = 34, header = 11)
    }
}