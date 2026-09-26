package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Calculator
import com.example.Item

@Composable
fun CalculatorScreen(
    history: List<Item>,
    onSaveCalculation: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var currentResult by remember { mutableStateOf("") }

    fun append(char: String) {
        if (expression.length < 500) {
            expression += char
            currentResult = try {
                Calculator.calculate(expression)
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun evaluate() {
        if (expression.isBlank()) return
        try {
            val answer = Calculator.calculate(expression)
            val original = expression
            expression = answer
            currentResult = ""
            onSaveCalculation(original, answer)
        } catch (e: Exception) {
            onError(e.message ?: "Invalid calculation")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calculator_screen")
    ) {
        // Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (expression.isEmpty()) "0" else expression,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier.testTag("calc_display")
                )
                if (currentResult.isNotBlank() && currentResult != expression) {
                    Text(
                        text = "= $currentResult",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Keypad buttons
        val rows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in listOf("÷", "×", "−", "+", "=")
                        val isAction = key in listOf("C", "⌫", "%")
                        val weight = if (key == "0") 2f else 1f

                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> {
                                        expression = ""
                                        currentResult = ""
                                    }
                                    "⌫" -> {
                                        if (expression.isNotEmpty()) {
                                            expression = expression.dropLast(1)
                                            currentResult = try {
                                                Calculator.calculate(expression)
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        }
                                    }
                                    "=" -> evaluate()
                                    else -> append(key)
                                }
                            },
                            modifier = Modifier
                                .weight(weight)
                                .height(52.dp)
                                .testTag("btn_$key"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    key == "=" -> MaterialTheme.colorScheme.primary
                                    isOperator -> MaterialTheme.colorScheme.primaryContainer
                                    isAction -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = when {
                                    key == "=" -> MaterialTheme.colorScheme.onPrimary
                                    isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isAction -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        ) {
                            if (key == "⌫") {
                                Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History
        Text(
            text = "Calculation History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No calculations in history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("calc_history_list"),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = item.data.optString("name"),
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
