package com.coinselection.size

import com.coinselection.model.TransactionSize

interface TransactionSizeProvider {
    fun provide(): TransactionSize
}

