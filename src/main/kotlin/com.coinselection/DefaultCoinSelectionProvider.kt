package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.CumulativeHolder
import com.coinselection.model.TransactionSize
import com.coinselection.size.SegwitLegacyCompatibleSizeProvider
import java.math.BigDecimal

internal object DefaultCoinSelectionProvider : CoinSelectionProvider {
    internal const val maxNumberOfInputs: Int = MAX_INPUT
    internal val transactionSize: TransactionSize = SegwitLegacyCompatibleSizeProvider.provide()

    override fun provide(utxoList: List<UnspentOutput>, targetValue: BigDecimal, feeRatePerByte: BigDecimal, numberOfOutputs: Int, compulsoryUtxoList: List<UnspentOutput>?, hasOpReturnOutput: Boolean): CoinSelectionResult? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class UtxoSumCalculationData(
            val utxoList: List<UnspentOutput>,
            val cumulativeHolder: CumulativeHolder
    )


    fun appendSumAndFee(cumulativeHolder: CumulativeHolder, costCalculator: CostCalculator, sum: BigDecimal) {
        cumulativeHolder.appendSum(sum)
        cumulativeHolder.appendFee(costCalculator.getCostPerInput())
    }


    fun sumIsLessThanTarget(cumulativeHolder: CumulativeHolder, targetValue: BigDecimal): Boolean {
        return cumulativeHolder.getSum() < targetValue + cumulativeHolder.getFee()
    }

    fun selectUntilSumIsLessThanTarget(utxoList: List<UnspentOutput>,
                                       targetValue: BigDecimal,
                                       costCalculator: CostCalculator,
                                       compulsoryUtxoList: List<UnspentOutput>?): UtxoSumCalculationData? {
        val cumulativeHolder = CumulativeHolder.defaultInit()
        cumulativeHolder.appendFee(costCalculator.getBaseFee())
        val selectedCompulsoryUtxoList = compulsoryUtxoList?.asSequence()
                ?.take(maxNumberOfInputs)
                ?.onEach { appendSumAndFee(cumulativeHolder, costCalculator, sum = it.amount) }
                ?.toList() ?: listOf()
        val selectedUtxoList = utxoList
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
}