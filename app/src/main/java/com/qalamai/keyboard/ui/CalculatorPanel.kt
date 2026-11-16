package com.qalamai.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalamai.keyboard.ime.SmartKeyboardService
import com.qalamai.keyboard.ui.theme.KeyboardPalette
import java.math.BigDecimal
import java.math.RoundingMode

/** محرك حساب صغير: + − × ÷ % وأقواس وأرقام عشرية. */
object CalcEngine {

    fun evaluate(expr: String): String? {
        val parser = Parser(expr.replace(" ", ""))
        val value = runCatching { parser.parseAll() }.getOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return format(value)
    }

    private fun format(v: Double): String =
        BigDecimal(v).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private class Parser(private val s: String) {
        private var i = 0

        fun parseAll(): Double {
            val v = expr()
            if (i != s.length) throw IllegalArgumentException()
            return v
        }

        private fun peek(): Char? = s.getOrNull(i)

        private fun expr(): Double {
            var v = term()
            while (true) {
                when (peek()) {
                    '+' -> { i++; v += term() }
                    '−', '-' -> { i++; v -= term() }
                    else -> return v
                }
            }
        }

        private fun term(): Double {
            var v = factor()
            while (true) {
                when (peek()) {
                    '×', '*' -> { i++; v *= factor() }
                    '÷', '/' -> {
                        i++
                        val d = factor()
                        if (d == 0.0) throw ArithmeticException()
                        v /= d
                    }
                    else -> return v
                }
            }
        }

        private fun factor(): Double {
            var neg = false
            while (peek() == '−' || peek() == '-') { neg = !neg; i++ }
            var v: Double
            if (peek() == '(') {
                i++
                v = expr()
                if (peek() != ')') throw IllegalArgumentException()
                i++
            } else {
                val start = i
                while (peek()?.let { it.isDigit() || it == '.' } == true) i++
                if (start == i) throw IllegalArgumentException()
                v = s.substring(start, i).toDouble()
            }
            while (peek() == '%') { i++; v /= 100.0 }
            return if (neg) -v else v
        }
    }
}

@Composable
fun CalculatorPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    var expr by remember { mutableStateOf("") }
    val result = if (expr.isBlank()) null else CalcEngine.evaluate(expr)

    fun press(k: String) {
        when (k) {
            "C" -> expr = ""
            "⌫" -> expr = expr.dropLast(1)
            "=" -> CalcEngine.evaluate(expr)?.let { expr = it }
            else -> expr += k
        }
    }

    val rows = listOf(
        listOf("C", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "%", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (expr.isEmpty()) "0" else expr,
                    color = palette.keyText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = if (result != null && result != expr) "= $result" else " ",
                    color = palette.accentColor,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            CalcKey("⌫", palette, Modifier.weight(0.5f)) { press("⌫") }
        }
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                r.forEach { k ->
                    CalcKey(k, palette, Modifier.weight(1f), accent = k in listOf("÷", "×", "−", "+", "=")) {
                        press(k)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            CalcKey("إدراج النتيجة في الحقل", palette, Modifier.weight(1f), accent = true) {
                val out = result ?: expr
                if (out.isNotBlank()) service.insertPhrase(out)
            }
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    palette: KeyboardPalette,
    modifier: Modifier,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (accent) palette.accentColor.copy(alpha = 0.22f) else palette.keySurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (accent) palette.accentColor else palette.keyText,
            fontSize = if (label.length > 2) 12.sp else 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
