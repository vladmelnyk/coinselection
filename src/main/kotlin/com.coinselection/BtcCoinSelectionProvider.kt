package com.coinselection

import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

class BtcCoinSelectionProvider : CoinSelectionProvider {

    override fun provide(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, maxNumberOfInputs: Int, numberOfDestinationAddress: Int, inputSize: Int, outputSize: Int, headerSize: Int): List<UnspentOutput> {
        val selectedUtxoListSumAndFee = select(utxoList, targetValue, feeRatePerByte, maxNumberOfInputs, numberOfDestinationAddress, inputSize, outputSize, headerSize)
        val selectedUtxoList = selectedUtxoListSumAndFee.first
        val cumulativeSum = selectedUtxoListSumAndFee.second
        val cumulativeFee = selectedUtxoListSumAndFee.third
        val improvedUtxoList = improve(utxoList.subtract(selectedUtxoList).toList(), cumulativeSum, cumulativeFee, targetValue, feeRatePerByte, maxNumberOfInputs, inputSize)
        return selectedUtxoList.union(improvedUtxoList).toList()
    }

    private fun select(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, maxNumberOfInputs: Int, numberOfDestinationAddress: Int, inputSize: Int, outputSize: Int, headerSize: Int): Triple<List<UnspentOutput>, AtomicReference<BigDecimal>, AtomicReference<BigDecimal>> {
        val cumulativeSum = AtomicReference<BigDecimal>(BigDecimal.ZERO)
        val costPerInput = inputSize.toBigDecimal() * feeRatePerByte
        val costPerOutput = outputSize.toBigDecimal() * feeRatePerByte
        val basicFee = headerSize.toBigDecimal() * feeRatePerByte + BigDecimal(1 + numberOfDestinationAddress) * costPerOutput
        val cumulativeFee = AtomicReference<BigDecimal>(basicFee)

        var selectedUtxoList = utxoList
                .shuffled()
                .asSequence()
                .takeWhile { cumulativeSum.get() < targetValue + cumulativeFee.get() }
                .take(maxNumberOfInputs)
                .onEach { append(atomicReference = cumulativeSum, with = it.amount) }
                .onEach { append(atomicReference = cumulativeFee, with = costPerInput) }
                .toList()
        if (cumulativeSum.get() < targetValue + cumulativeFee.get()) {
//            fallback to largest-first algorithm
            cumulativeSum.set(BigDecimal.ZERO)
            cumulativeFee.set(basicFee)
            selectedUtxoList = utxoList
                    .asSequence()
                    .sortedByDescending { it.amount }
                    .takeWhile { cumulativeSum.get() < targetValue + cumulativeFee.get() }
                    .onEach { append(atomicReference = cumulativeSum, with = it.amount) }
                    .onEach { append(atomicReference = cumulativeFee, with = costPerInput) }
                    .toList()
        }
        return Triple(selectedUtxoList, cumulativeSum, cumulativeFee)

    }

    private fun improve(remainingUtxoList: List<UnspentOutput>, currentCumulativeSum: AtomicReference<BigDecimal>, currentCumulativeFee: AtomicReference<BigDecimal>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, maxNumOfInputs: Int, inputSize: Int): List<UnspentOutput> {
        val costPerInput = inputSize.toBigDecimal() * feeRatePerByte
        val maxTargetValue = BigDecimal(3) * targetValue
        val optimalTargetValue = BigDecimal(2) * targetValue
        val delta = AtomicReference((currentCumulativeSum.get() - (optimalTargetValue + currentCumulativeFee.get())).abs())
        val improvedUtxoList = remainingUtxoList
                .shuffled()
                .asSequence()
                .takeWhile { currentCumulativeSum.get() < maxTargetValue + currentCumulativeFee.get() }
                .take(maxNumOfInputs)
                .onEach { append(atomicReference = currentCumulativeSum, with = it.amount) }
                .onEach { append(atomicReference = currentCumulativeFee, with = costPerInput) }
                .takeWhile { (currentCumulativeSum.get() - (optimalTargetValue + currentCumulativeFee.get())).abs() < delta.get() }
                .onEach { delta.getAndSet((currentCumulativeSum.get() - (optimalTargetValue + currentCumulativeFee.get())).abs()) }
                .toList()
        return improvedUtxoList
    }

    private fun append(atomicReference: AtomicReference<BigDecimal>, with: BigDecimal?): BigDecimal {
        return atomicReference.accumulateAndGet(with) { t, u -> t + u }
    }

}