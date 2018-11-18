package com.coinselection

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

private const val KB = 1024L

class BtcCoinSelectionProviderTest {

//  1. target + fees <= utxoSum <= target * 3 + fees
//  2. fallback scenario ?
//  3. complex scenario: how does the UTXO pool behave over time and what is the average cost per transaction?

    private val coinSelectionProvider: CoinSelectionProvider = BtcCoinSelectionProvider()
    private val smartFee = BigDecimal(0.00000005)
    private val random = Random()

    @RepeatedTest(100)
    fun `basic scenario`() {
        val targetValue = BigDecimal(5)
        val rangeMin = 0
        val rangeMax = 1
        val utxoList = (1..1000).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val selectedUtxos = coinSelectionProvider.provide(utxoList, targetValue, smartFee)
        println(selectedUtxos.map { it.amount })
        val sum = selectedUtxos.sumByBigDecimal { it.amount }
        val count = selectedUtxos.size
        val fee = calculateTransactionFee(count, 2, smartFee)
        Assertions.assertTrue(sum > targetValue + BigDecimal(fee))
    }

    @Test
    fun `should fallback to largest first`() {
        val targetValue = BigDecimal(5)
        val rangeMin = 0
        val rangeMax = 1
        val maxNumOfInputs = 3
        val utxoList = (1..1000).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val selectedUtxos = coinSelectionProvider.provide(utxoList, targetValue, smartFee, maxNumberOfInputs = maxNumOfInputs)
        println(selectedUtxos.map { it.amount })
        val sum = selectedUtxos.sumByBigDecimal { it.amount }
        val count = selectedUtxos.size
        val fee = calculateTransactionFee(count, 2, smartFee)
        Assertions.assertTrue(sum > targetValue + BigDecimal(fee))
        Assertions.assertTrue(selectedUtxos.contains(utxoList.asSequence().sortedByDescending { it.amount }.first()))
    }

    @Test
    fun `should trigger improve scenario`() {
        val targetValue = BigDecimal(3)
        val rangeMin = 1.1
        val rangeMax = 1.2
        val maxNumOfInputs = 3
        val utxoList = (1..1000).map { rangeMin + (rangeMax - rangeMin) * random.nextDouble() }.map { createUnspentOutput(it) }
        val selectedUtxos = coinSelectionProvider.provide(utxoList, targetValue, smartFee, maxNumOfInputs)
        println(selectedUtxos.map { it.amount })
        val sum = selectedUtxos.sumByBigDecimal { it.amount }
        val count = selectedUtxos.size
        val fee = calculateTransactionFee(count, 2, smartFee)
        Assertions.assertTrue(sum > targetValue + BigDecimal(fee))
        Assertions.assertSame(maxNumOfInputs * 2 - 1, selectedUtxos.size)
    }

    private fun createUnspentOutput(value: Double): UnspentOutput {
        return UnspentOutput(amount = BigDecimal(value))
    }

    private fun <T> Iterable<T>.sumByBigDecimal(transform: (T) -> BigDecimal): BigDecimal {
        return this.fold(BigDecimal.ZERO) { acc, e -> acc + transform.invoke(e) }
    }

    private fun calculateTransactionFee(inputsCount: Int, outputsCount: Int, smartFeePerKB: BigDecimal): Long {
        val size = inputsCount * 91 + outputsCount * 32 + 11
        val smartFeePerByte = smartFeePerKB.div(BigDecimal(KB))
        return (smartFeePerByte * BigDecimal(100000000)).toLong() * size
    }

}
