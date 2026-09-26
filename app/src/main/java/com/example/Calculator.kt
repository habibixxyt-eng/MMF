package com.example

import java.math.BigDecimal
import java.math.MathContext

object Calculator {
    fun calculate(text: String): String {
        val s = text.replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
        require(s.length <= 500) { "Expression too long" }
        require(s.isNotEmpty()) { "Enter an expression" }

        var p = 0
        val mc = MathContext.DECIMAL128

        fun number(): BigDecimal {
            var negative = false
            while (p < s.length && s[p] in "+-") {
                if (s[p++] == '-') negative = !negative
            }
            val start = p
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++
            require(p > start) { "Enter a number" }
            var n = s.substring(start, p).toBigDecimal()
            if (negative) n = n.negate()
            while (p < s.length && s[p] == '%') {
                p++
                n = n.divide(BigDecimal(100), mc)
            }
            return n
        }

        fun term(): BigDecimal {
            var n = number()
            while (p < s.length && s[p] in "*/") {
                val op = s[p++]
                val b = number()
                n = if (op == '*') n.multiply(b, mc) else n.divide(b, mc)
            }
            return n
        }

        var answer = term()
        while (p < s.length && s[p] in "+-") {
            val op = s[p++]
            val b = term()
            answer = if (op == '+') answer.add(b, mc) else answer.subtract(b, mc)
        }
        require(p == s.length) { "Invalid expression" }
        return answer.stripTrailingZeros().toPlainString()
    }
}
