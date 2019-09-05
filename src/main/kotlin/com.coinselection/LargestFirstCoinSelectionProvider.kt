package com.coinselection

import com.coinselection.DefaultCoinSelectionProvider.UtxoSumCalculationData
import com.coinselection.DefaultCoinSelectionProvider.selectUntilSumIsLessThanTarget
import com.coinselection.DefaultCoinSelectionProvider.sumIsLessThanTarget
import com.coinselection.DefaultCoinSelectionProvider.transactionSize
import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import java.math.BigDecimal

internal object LargestFirstCoinSelectionProvider
    : CoinSelectionProvider by DefaultCoinSelectionProvider {

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

        val dataPair = largestFirstSelection(
                utxoList = utxoList,
                costCalculator = costCalculator,
                targetValue = targetValue,
                compulsoryUtxoList = compulsoryUtxoList
        ) ?: return null

        val selectedUtxoList = dataPair.utxoList
        val cumulativeHolder = dataPair.cumulativeHolder


        return CoinSelectionResult(
                selectedUtxos = selectedUtxoList,
                totalFee = cumulativeHolder.getFee())
    }

    internal fun largestFirstSelection(utxoList: List<UnspentOutput>,
                                       costCalculator: CostCalculator,
                                       targetValue: BigDecimal,
                                       compulsoryUtxoList: List<UnspentOutput>?): UtxoSumCalculationData? {
        val utxoListSorted = utxoList.sortedByDescending { it.amount }
        val dataPair = selectUntilSumIsLessThanTarget(utxoListSorted, targetValue, costCalculator, compulsoryUtxoList)
        return if (dataPair == null || sumIsLessThanTarget(dataPair.cumulativeHolder, targetValue)) {
            null
        } else {
            val cumulativeHolder = dataPair.cumulativeHolder
            UtxoSumCalculationData(dataPair.utxoList, cumulativeHolder)
        }
    }


}