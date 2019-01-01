package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.CumulativeHolder
import com.coinselection.model.TransactionSize
import com.coinselection.size.SegwitLegacyCompatibleSizeProvider
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

const val MAX_INPUT = 60

class BtcCoinSelectionProvider(
        private val maxNumberOfInputs: Int = MAX_INPUT,
        private val transactionSize: TransactionSize = SegwitLegacyCompatibleSizeProvider.provide()
) {

    fun provide(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, numberOfOutputs: Int = 1): CoinSelectionResult? {
        val cumulativeHolder = CumulativeHolder.defaultInit()
        val costCalculator = CostCalculator(transactionSize = transactionSize, feePerByte = feeRatePerByte, numberOfOutputs = numberOfOutputs)
        val selectedUtxoList = select(utxoList, targetValue, costCalculator, cumulativeHolder)
                ?: return null
        val improvedUtxoList = improve(getRemainingUtxoList(selectedUtxoList = selectedUtxoList, originalUtxoList = utxoList), targetValue = targetValue, costCalculator = costCalculator, cumulativeHolder = cumulativeHolder)
        return CoinSelectionResult(selectedUtxos = mergeUtxoLists(selectedUtxoList = selectedUtxoList, improvedUtxoList = improvedUtxoList), totalFee = cumulativeHolder.getFee())
    }

    private fun select(utxoList: List<UnspentOutput>, targetValue: BigDecimal, costCalculator: CostCalculator, cumulativeHolder: CumulativeHolder): List<UnspentOutput>? {
        return selectUntilSumIsLessThanTarget(utxoListRearanged = utxoList.shuffled(), cumulativeHolder = cumulativeHolder, targetValue = targetValue, costCalculator = costCalculator)
                ?: fallbackLargestFirstSelection(utxoListRearanged = utxoList.sortedByDescending { it.amount }, cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, targetValue = targetValue)
    }

    private fun improve(remainingUtxoList: List<UnspentOutput>, targetValue: BigDecimal, costCalculator: CostCalculator, cumulativeHolder: CumulativeHolder): List<UnspentOutput> {
        val maxTargetValue = BigDecimal(3) * targetValue
        val optimalTargetValue = BigDecimal(2) * targetValue
        val delta = AtomicReference(BigDecimal.valueOf(Long.MAX_VALUE))
        return remainingUtxoList
                .shuffled()
                .asSequence()
                .take(maxNumberOfInputs)
                .takeWhile { sumIsLessThanTarget(cumulativeHolder = cumulativeHolder, targetValue = maxTargetValue) }
                .onEach { delta.getAndSet((cumulativeHolder.getSum() - (optimalTargetValue + cumulativeHolder.getFee())).abs()) }
                .takeWhile { (cumulativeHolder.getSum() + it.amount - (optimalTargetValue + cumulativeHolder.getFee() + costCalculator.getCostPerInput())).abs() < delta.get() }
                .onEach { appendSumAndFee(cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, sum = it.amount) }
                .toList()
    }

    private fun sumIsLessThanTarget(cumulativeHolder: CumulativeHolder, targetValue: BigDecimal): Boolean {
        return cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee()
    }

    private fun selectUntilSumIsLessThanTarget(utxoListRearanged: List<UnspentOutput>, cumulativeHolder: CumulativeHolder, targetValue: BigDecimal, costCalculator: CostCalculator): List<UnspentOutput>? {
        cumulativeHolder.appendFee(costCalculator.getBaseFee())
        val selectedUtxoList = utxoListRearanged
                .asSequence()
                .takeWhile { sumIsLessThanTarget(cumulativeHolder = cumulativeHolder, targetValue = targetValue) }
                .take(maxNumberOfInputs)
                .onEach { appendSumAndFee(cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, sum = it.amount) }
                .toList()
        if (sumIsLessThanTarget(cumulativeHolder = cumulativeHolder, targetValue = targetValue)) {
            return null
        }
        return selectedUtxoList
    }

    private fun fallbackLargestFirstSelection(utxoListRearanged: List<UnspentOutput>, cumulativeHolder: CumulativeHolder, costCalculator: CostCalculator, targetValue: BigDecimal): List<UnspentOutput>? {
        cumulativeHolder.reset()
        cumulativeHolder.appendFee(costCalculator.getBaseFee())
        val selectedUtxoList = selectUntilSumIsLessThanTarget(utxoListRearanged = utxoListRearanged,
                cumulativeHolder = cumulativeHolder, costCalculator = costCalculator, targetValue = targetValue)
//            Return null utxo list if total amount is still not enough
        if (sumIsLessThanTarget(cumulativeHolder = cumulativeHolder, targetValue = targetValue)) {
            return null
        }
        return selectedUtxoList
    }

    private fun appendSumAndFee(cumulativeHolder: CumulativeHolder, costCalculator: CostCalculator, sum: BigDecimal) {
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