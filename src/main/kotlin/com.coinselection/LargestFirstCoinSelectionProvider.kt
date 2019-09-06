package com.coinselection

import com.coinselection.dto.CoinSelectionResult
import com.coinselection.dto.UnspentOutput
import com.coinselection.model.TransactionSize
import com.coinselection.model.UtxoSumCalculationData
import java.math.BigDecimal

internal class LargestFirstCoinSelectionProvider(maxNumberOfInputs: Int, transactionSize: TransactionSize)
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

    private fun largestFirstSelection(utxoList: List<UnspentOutput>,
                                      costCalculator: CostCalculator,
                                      targetValue: BigDecimal,
                                      compulsoryUtxoList: List<UnspentOutput>?): UtxoSumCalculationData? {
        val dataPair = selectUntilSumIsLessThanTarget(
                utxoList.sortedByDescending { it.amount },
                targetValue,
                costCalculator,
                compulsoryUtxoList
        )
        return if (dataPair == null || sumIsLessThanTarget(dataPair.cumulativeHolder, targetValue)) {
            null
        } else {
            UtxoSumCalculationData(
                    utxoList = dataPair.utxoList,
                    cumulativeHolder = dataPair.cumulativeHolder)
        }
    }


}