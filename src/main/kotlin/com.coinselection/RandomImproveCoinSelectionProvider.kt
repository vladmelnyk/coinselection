package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.Algorithm.LARGEST_FIRST
import com.coinselection.model.CumulativeHolder
import com.coinselection.model.TransactionSize
import com.coinselection.model.UtxoSumCalculationData
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

internal class RandomImproveCoinSelectionProvider(maxNumberOfInputs: Int, transactionSize: TransactionSize)
    : DefaultCoinSelectionProvider(maxNumberOfInputs, transactionSize) {

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

        val dataPair =
                selectUntilSumIsLessThanTarget(
                        utxoList.shuffled(),
                        targetValue,
                        costCalculator,
                        compulsoryUtxoList
                ) ?: fallbackLargestFirstSelection(
                        utxoList = utxoList,
                        targetValue = targetValue,
                        compulsoryUtxoList = compulsoryUtxoList,
                        feeRatePerByte = feeRatePerByte,
                        numberOfOutputs = numberOfOutputs,
                        hasOpReturnOutput = hasOpReturnOutput
                ) ?: return null

        val remainingUtxoList = utxoList.subtract(dataPair.utxoList).toList()

        val remainingTotalAmount = remainingUtxoList.sumByBigDecimal { it.amount }

        val canPerformImproveStep = remainingTotalAmount > targetValue
        val improvedUtxoList =
                if (canPerformImproveStep) {
                    improve(remainingUtxoList, targetValue, costCalculator, dataPair.cumulativeHolder)
                } else {
                    listOf()
                }

        val utxoResult = dataPair.utxoList.union(improvedUtxoList).toList()

        return CoinSelectionResult(
                selectedUtxos = utxoResult,
                totalFee = dataPair.cumulativeHolder.getFee())
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
                                              targetValue: BigDecimal,
                                              feeRatePerByte: BigDecimal,
                                              numberOfOutputs: Int,
                                              compulsoryUtxoList: List<UnspentOutput>?,
                                              hasOpReturnOutput: Boolean): UtxoSumCalculationData? {

        val coinSelectionResult = CoinSelectionFactory.create(
                algorithm = LARGEST_FIRST,
                maxNumberOfInputs = maxNumberOfInputs,
                transactionSize = transactionSize)
                .provide(
                        utxoList = utxoList,
                        targetValue = targetValue,
                        compulsoryUtxoList = compulsoryUtxoList,
                        feeRatePerByte = feeRatePerByte,
                        numberOfOutputs = numberOfOutputs,
                        hasOpReturnOutput = hasOpReturnOutput
                )

        return UtxoSumCalculationData(
                utxoList = coinSelectionResult?.selectedUtxos ?: return null,
                cumulativeHolder = CumulativeHolder(
                        accumulatedSum = AtomicReference(utxoList.sumByBigDecimal { it.amount }),
                        accumulatedFee = AtomicReference(coinSelectionResult.totalFee))
        )
    }

}