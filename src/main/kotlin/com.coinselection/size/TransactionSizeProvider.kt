package com.coinselection.size

import com.coinselection.model.TransactionComponentsSize

interface TransactionSizeProvider {
    fun provide(): TransactionComponentsSize
}

