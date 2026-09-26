package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorTest {
    @Test
    fun decimal() {
        assertEquals("0.3", Calculator.calculate("0.1+0.2"))
    }

    @Test
    fun precedence() {
        assertEquals("14", Calculator.calculate("2+3*4"))
    }

    @Test
    fun percent() {
        assertEquals("50", Calculator.calculate("200*25%"))
    }

    @Test
    fun signed() {
        assertEquals("-6", Calculator.calculate("2*-3"))
    }

    @Test(expected = ArithmeticException::class)
    fun zeroDivision() {
        Calculator.calculate("1/0")
    }
}
