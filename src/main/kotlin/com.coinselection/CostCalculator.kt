package com.coinselection

import com.coinselection.model.TransactionSize
import java.math.BigDecimal

const val OP_RETURN_SIZE: Int = 40

class CostCalculator(
        private val transactionSize: TransactionSize,
        private val feePerByte: BigDecimal,
        private val numberOfOutputs: Int,
        private val hasOpReturnOutput: Boolean = false
) {
    fun getBaseFee(): BigDecimal {
        val baseFee = getCostForHeader() + BigDecimal(1 + numberOfOutputs) * getCostPerOutput()
        return if (hasOpReturnOutput) {
            baseFee + getCostPerOpReturnOutput()
        } else {
            baseFee
        }
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

    private fun getCostPerOpReturnOutput(): BigDecimal {
        return costPerByte(OP_RETURN_SIZE)
    }

    private fun costPerByte(size: Int): BigDecimal {
        return size.toBigDecimal() * feePerByte
    }
}