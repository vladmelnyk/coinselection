package com.coinselection

import com.coinselection.dto.UnspentOutput
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

private val KB = 1000.toBigDecimal()

class BtcCoinSelectionProviderTest {

//  1. target + fees <= utxoSum <= target * 3 + fees
//  2. fallback scenario ?
//  3. complex scenario: how does the UTXO pool behave over time and what is the average cost per transaction?

    private val coinSelectionProvider: BtcCoinSelectionProvider = BtcCoinSelectionProvider()
    private val smartFeePerByte = BigDecimal.ONE.movePointLeft(8)
    private val random = Random()

    @RepeatedTest(100)
    fun `basic scenario`() {
        val targetValue = BigDecimal(5)
        val rangeMin = 0
        val rangeMax = 1
        val utxoList = (1..1000).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val coinSelectionResult = coinSelectionProvider.provide(utxoList, targetValue, smartFeePerByte)!!
        val sum = coinSelectionResult.selectedUtxos?.sumByBigDecimal { it.amount }
        val count = coinSelectionResult.selectedUtxos?.size
        val feeSimple = calculateTransactionFee(count!!, 2, smartFeePerByte)
        val feeCalculated = coinSelectionResult.totalFee
        Assertions.assertTrue(sum!! > targetValue + feeCalculated)
        Assertions.assertTrue(sum < targetValue * 3.toBigDecimal() + feeCalculated)
        Assertions.assertTrue(feeSimple == feeCalculated)
    }

    @Test
    fun `should trigger improve scenario`() {
        val targetValue = BigDecimal(3)
        val rangeMin = 1.1
        val rangeMax = 1.2
        val maxNumOfInputs = 5
        val utxoList = (1..1000).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val coinSelectionResult = coinSelectionProvider.provide(utxoList, targetValue, smartFeePerByte)!!
        val sum = coinSelectionResult.selectedUtxos?.sumByBigDecimal { it.amount }
        val count = coinSelectionResult.selectedUtxos?.size
        val feeSimple = calculateTransactionFee(count!!, 2, smartFeePerByte)
        val feeCalculated = coinSelectionResult.totalFee
        Assertions.assertTrue(sum!! > targetValue + feeCalculated)
        Assertions.assertTrue(feeSimple == feeCalculated)
        Assertions.assertSame(maxNumOfInputs, coinSelectionResult.selectedUtxos!!.size)
    }

    @Test
    fun `should return null utxoList if total accumulated value is not enough`() {
        val targetValue = BigDecimal(100)
        val rangeMin = 1.1
        val rangeMax = 1.2
        val utxoList = (1..50).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val coinSelectionResult = coinSelectionProvider.provide(utxoList, targetValue, smartFeePerByte)
        Assertions.assertNull(coinSelectionResult)
    }

    @Test
    fun `should include all compulsory utxos`() {
        val targetValue = BigDecimal(5)
        val rangeMin = 1.1
        val rangeMax = 1.2
        val utxoList = (1..50).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val compulsoryUtxoList = (1..20).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val coinSelectionResult = coinSelectionProvider.provide(utxoList, targetValue, smartFeePerByte, compulsoryUtxoList = compulsoryUtxoList)
        Assertions.assertNotNull(coinSelectionResult!!.selectedUtxos)
        Assertions.assertTrue(coinSelectionResult.selectedUtxos!!.containsAll(compulsoryUtxoList))
        val totalFeeExpected = calculateTransactionFee(coinSelectionResult.selectedUtxos!!.size, 2, smartFeePerByte)
        val totalFee = coinSelectionResult.totalFee
        Assertions.assertEquals(totalFeeExpected, totalFee)

    }

    private fun createUnspentOutput(value: Double): UnspentOutput {
        return UnspentOutput(amount = BigDecimal(value))
    }

    private fun <T> Iterable<T>.sumByBigDecimal(transform: (T) -> BigDecimal): BigDecimal {
        return this.fold(BigDecimal.ZERO) { acc, e -> acc + transform.invoke(e) }
    }

    private fun calculateTransactionFee(inputsCount: Int, outputsCount: Int, smartFeePerByte: BigDecimal): BigDecimal {
        val size = inputsCount * 91 + outputsCount * 32 + 11
        return (smartFeePerByte * size.toBigDecimal())
    }

}
