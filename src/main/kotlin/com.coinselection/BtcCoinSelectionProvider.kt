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

    private class UtxoSumCalculationData(
            val utxoList: List<UnspentOutput>,
            val cumulativeHolder: CumulativeHolder
    )

    fun provide(utxoList: List<UnspentOutput>,
                targetValue: BigDecimal,
                feeRatePerByte: BigDecimal,
                numberOfOutputs: Int = 1,
                compulsoryUtxoList: List<UnspentOutput>? = null,
                hasOpReturnOutput: Boolean = false
    ): CoinSelectionResult? {

        val costCalculator = CostCalculator(
                transactionSize,
                feeRatePerByte,
                numberOfOutputs,
                hasOpReturnOutput
        )

        val dataPair = selectUntilSumIsLessThanTarget(
                utxoList.shuffled(), targetValue, costCalculator, compulsoryUtxoList)
                ?: fallbackLargestFirstSelection(
                        utxoListRearanged = utxoList.sortedByDescending { it.amount },
                        costCalculator = costCalculator,
                        targetValue = targetValue,
                        compulsoryUtxoList = compulsoryUtxoList)
                ?: return null

        val selectedUtxoList = dataPair.utxoList
        val cumulativeHolder = dataPair.cumulativeHolder

        val remainingUtxoList = utxoList.subtract(selectedUtxoList).toList()
        val improvedUtxoList = improve(remainingUtxoList, targetValue, costCalculator, cumulativeHolder)

        val utxoResult = selectedUtxoList.union(improvedUtxoList).toList()

        return CoinSelectionResult(
                selectedUtxos = utxoResult,
                totalFee = cumulativeHolder.getFee())
    }

    private fun improve(remainingUtxoList: List<UnspentOutput>, targetValue: BigDecimal, costCalculator: CostCalculator, cumulativeHolder: CumulativeHolder): List<UnspentOutput> {
        val maxTargetValue = BigDecimal(3) * targetValue
        val optimalTargetValue = BigDecimal(2) * targetValue
        val delta = AtomicReference(BigDecimal.valueOf(Long.MAX_VALUE))
        return remainingUtxoList
                .shuffled()
                .asSequence()
                .take(maxNumberOfInputs)
                .filter { it.amount >= costCalculator.getCostPerInput() }
                .takeWhile { sumIsLessThanTarget(cumulativeHolder, targetValue = maxTargetValue) }
                .onEach { delta.getAndSet((cumulativeHolder.getSum() - (optimalTargetValue + cumulativeHolder.getFee())).abs()) }
                .takeWhile { (cumulativeHolder.getSum() + it.amount - (optimalTargetValue + cumulativeHolder.getFee() + costCalculator.getCostPerInput())).abs() < delta.get() }
                .onEach { appendSumAndFee(cumulativeHolder, costCalculator, sum = it.amount) }
                .toList()
    }

    private fun sumIsLessThanTarget(cumulativeHolder: CumulativeHolder, targetValue: BigDecimal): Boolean {
        return cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee()
    }

    private fun selectUntilSumIsLessThanTarget(utxoListRearanged: List<UnspentOutput>,
                                               targetValue: BigDecimal,
                                               costCalculator: CostCalculator,
                                               compulsoryUtxoList: List<UnspentOutput>?): UtxoSumCalculationData? {
        val cumulativeHolder = CumulativeHolder.defaultInit()
        cumulativeHolder.appendFee(costCalculator.getBaseFee())
        val selectedCompulsoryUtxoList = compulsoryUtxoList?.asSequence()
                ?.take(maxNumberOfInputs)
                ?.onEach { appendSumAndFee(cumulativeHolder, costCalculator, sum = it.amount) }
                ?.toList() ?: listOf()
        val selectedUtxoList = utxoListRearanged
                .asSequence()
                .filter { it.amount >= costCalculator.getCostPerInput() }
                .takeWhile { sumIsLessThanTarget(cumulativeHolder, targetValue) }
                .take(maxNumberOfInputs)
                .onEach { appendSumAndFee(cumulativeHolder, costCalculator, sum = it.amount) }
                .toList()
        if (sumIsLessThanTarget(cumulativeHolder, targetValue)) {
            return null
        }
        return UtxoSumCalculationData(selectedCompulsoryUtxoList + selectedUtxoList, cumulativeHolder)
    }

    private fun fallbackLargestFirstSelection(utxoListRearanged: List<UnspentOutput>,
                                              costCalculator: CostCalculator,
                                              targetValue: BigDecimal,
                                              compulsoryUtxoList: List<UnspentOutput>?): UtxoSumCalculationData? {
        val dataPair = selectUntilSumIsLessThanTarget(utxoListRearanged, targetValue, costCalculator, compulsoryUtxoList)
        return if (dataPair == null || sumIsLessThanTarget(dataPair.cumulativeHolder, targetValue)) {
            null
        } else {
            val cumulativeHolder = dataPair.cumulativeHolder
            cumulativeHolder.appendFee(costCalculator.getBaseFee())
            UtxoSumCalculationData(dataPair.utxoList, cumulativeHolder)
        }
    }

    private fun appendSumAndFee(cumulativeHolder: CumulativeHolder, costCalculator: CostCalculator, sum: BigDecimal) {
        cumulativeHolder.appendSum(sum)
        cumulativeHolder.appendFee(costCalculator.getCostPerInput())
    }

}