package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.CumulativeHolder
import com.coinselection.model.TransactionSize
import com.coinselection.size.SegwitLegacyCompatibleSizeProvider
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.util.concurrent.atomic.AtomicReference

const val MAX_INPUT = 60

class BtcCoinSelectionProvider(
        private val maxNumberOfInputs: Int = MAX_INPUT,
        private val transactionSize: TransactionSize = SegwitLegacyCompatibleSizeProvider.provide()
) {

    fun provide(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, numberOfOutputs: Int = 1): CoinSelectionResult? {
        val cumulativeHolder = CumulativeHolder(accumulatedSum = AtomicReference(ZERO), accumulatedFee = AtomicReference(ZERO))
        val costCalculator = CostCalculator(transactionSize = transactionSize, feePerByte = feeRatePerByte, numberOfOutputs = numberOfOutputs)
        val selectedUtxoList = select(utxoList, targetValue, costCalculator, cumulativeHolder)
                ?: return null
        val improvedUtxoList = improve(getRemainingUtxoList(selectedUtxoList = selectedUtxoList, originalUtxoList = utxoList), targetValue = targetValue, costCalculator = costCalculator, cumulativeHolder = cumulativeHolder)
        return CoinSelectionResult(selectedUtxos = mergeUtxoLists(selectedUtxoList = selectedUtxoList, improvedUtxoList = improvedUtxoList), totalFee = cumulativeHolder.accumulatedFee.get())
    }

    private fun select(utxoList: List<UnspentOutput>, targetValue: BigDecimal, costCalculator: CostCalculator, cumulativeHolder: CumulativeHolder): List<UnspentOutput>? {
        cumulativeHolder.appendFee(costCalculator.getBaseFee())
        var selectedUtxoList = utxoList
                .shuffled()
                .asSequence()
                .takeWhile { cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee() }
                .take(maxNumberOfInputs)
                .onEach { appendCumulativeHolder(cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, sum = it.amount) }
                .toList()
        if (cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee()) {
//            fallback to largest-first algorithm
            cumulativeHolder.reset()
            cumulativeHolder.appendFee(costCalculator.getBaseFee())
            selectedUtxoList = utxoList
                    .sortedByDescending { it.amount }
                    .asSequence()
                    .takeWhile { cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee() }
                    .take(maxNumberOfInputs)
                    .onEach { appendCumulativeHolder(cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, sum = it.amount) }
                    .toList()
//            Return null utxo list if total amount is still not enough
            if (cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee()) {
                return null
            }
        }
        return selectedUtxoList

    }

    private fun improve(remainingUtxoList: List<UnspentOutput>, targetValue: BigDecimal, costCalculator: CostCalculator, cumulativeHolder: CumulativeHolder): List<UnspentOutput> {
        val maxTargetValue = BigDecimal(3) * targetValue
        val optimalTargetValue = BigDecimal(2) * targetValue
        val delta = AtomicReference(BigDecimal.valueOf(Long.MAX_VALUE))
        return remainingUtxoList
                .shuffled()
                .asSequence()
                .take(maxNumberOfInputs)
                .takeWhile { cumulativeHolder.getSum() < maxTargetValue + cumulativeHolder.getFee() }
                .onEach { delta.getAndSet((cumulativeHolder.accumulatedSum.get() - (optimalTargetValue + cumulativeHolder.getFee())).abs()) }
                .takeWhile { (cumulativeHolder.getSum() + it.amount - (optimalTargetValue + cumulativeHolder.getFee() + costCalculator.getCostPerInput())).abs() < delta.get() }
                .onEach { appendCumulativeHolder(cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, sum = it.amount) }
                .toList()
    }

    private fun appendCumulativeHolder(cumulativeHolder: CumulativeHolder, costCalculator: CostCalculator, sum: BigDecimal) {
        cumulativeHolder.appendSum(sum)
        cumulativeHolder.appendFee(costCalculator.getCostPerInput())
    }

    private fun getRemainingUtxoList(selectedUtxoList: List<UnspentOutput>, originalUtxoList: List<UnspentOutput>): List<UnspentOutput> {
        return originalUtxoList.subtract(selectedUtxoList).toList()
    }

    private fun mergeUtxoLists(selectedUtxoList: List<UnspentOutput>, improvedUtxoList: List<UnspentOutput>): List<UnspentOutput> {
        return selectedUtxoList.union(improvedUtxoList).toList()
    }
}