package com.coinselection.model

import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

data class CumulativeHolder(
        val accumulatedSum: AtomicReference<BigDecimal>,
        val accumulatedFee: AtomicReference<BigDecimal>
) {
    fun getSum(): BigDecimal {
        return accumulatedSum.get()
    }

    fun getFee(): BigDecimal {
        return accumulatedFee.get()
    }

    fun appendSum(value: BigDecimal) {
        append(accumulatedSum, value)
    }

    fun appendFee(value: BigDecimal) {
        append(accumulatedFee, value)
    }

    fun reset() {
        accumulatedSum.set(BigDecimal.ZERO)
        accumulatedFee.set(BigDecimal.ZERO)
    }

    companion object {
        fun defaultInit(): CumulativeHolder {
            return CumulativeHolder(accumulatedSum = AtomicReference(BigDecimal.ZERO), accumulatedFee = AtomicReference(BigDecimal.ZERO))
        }
    }

    private fun append(atomicReference: AtomicReference<BigDecimal>, with: BigDecimal): BigDecimal {
        return atomicReference.accumulateAndGet(with) { t, u -> t + u }
    }
}