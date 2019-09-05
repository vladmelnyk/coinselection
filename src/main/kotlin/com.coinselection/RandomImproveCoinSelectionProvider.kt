package com.coinselection

import com.coinselection.DefaultCoinSelectionProvider.UtxoSumCalculationData
import com.coinselection.DefaultCoinSelectionProvider.appendSumAndFee
import com.coinselection.DefaultCoinSelectionProvider.maxNumberOfInputs
import com.coinselection.DefaultCoinSelectionProvider.selectUntilSumIsLessThanTarget
import com.coinselection.DefaultCoinSelectionProvider.sumIsLessThanTarget
import com.coinselection.DefaultCoinSelectionProvider.transactionSize
import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.CumulativeHolder
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

internal object RandomImproveCoinSelectionProvider : CoinSelectionProvider by DefaultCoinSelectionProvider {

    override fun provide(utxoList: List<UnspentOutput>,
                         targetValue: BigDecimal,
                         feeRatePerByte: BigDecimal,
                         numberOfOutputs: Int,
                         compulsoryUtxoList: List<UnspentOutput>?,
                         hasOpReturnOutput: Boolean
    ): CoinSelectionResult? {

        val costCalculator = CostCalculator(
                transactionSize,
                feeRatePerByte,
                numberOfOutputs,
                hasOpReturnOutput
        )

        val utxoListShuffled = utxoList.shuffled()
        val dataPair =
                selectUntilSumIsLessThanTarget(
                        utxoListShuffled,
                        targetValue,
                        costCalculator,
                        compulsoryUtxoList
                ) ?: fallbackLargestFirstSelection(
                        utxoList = utxoList,
                        costCalculator = costCalculator,
                        targetValue = targetValue,
                        compulsoryUtxoList = compulsoryUtxoList
                ) ?: return null

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

    private fun fallbackLargestFirstSelection(utxoList: List<UnspentOutput>,
                                              costCalculator: CostCalculator,
                                              targetValue: BigDecimal,
                                              compulsoryUtxoList: List<UnspentOutput>?): UtxoSumCalculationData? {
        return LargestFirstCoinSelectionProvider.largestFirstSelection(
                utxoList = utxoList,
                costCalculator = costCalculator,
                targetValue = targetValue,
                compulsoryUtxoList = compulsoryUtxoList
        )
    }

}