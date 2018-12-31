package com.coinselection

import com.coinselection.model.TransactionSize
import java.math.BigDecimal

class CostCalculator(
        private val transactionSize: TransactionSize,
        private val feePerByte: BigDecimal,
        private val numberOfOutputs: Int
) {
    fun getBaseFee(): BigDecimal {
        return getCostForHeader() + BigDecimal(1 + numberOfOutputs) * getCostPerOutput()
    }

    fun getCostPerInput(): BigDecimal {
        return costPerByte(transactionSize.input)
    }

    private fun getCostForHeader(): BigDecimal {
        return costPerByte(transactionSize.header)
    }

    private fun getCostPerOutput(): BigDecimal {
        return costPerByte(transactionSize.output)
    }

    private fun costPerByte(size: Int): BigDecimal {
        return size.toBigDecimal() * feePerByte
    }
}