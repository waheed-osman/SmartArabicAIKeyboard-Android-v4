package com.qalamai.keyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** إعدادات مظهر المفاتيح: ارتفاع، حجم أحرف، إضاءة الحواف، تدوير الزوايا، الخط. */
data class KeyScale(
    val height: Float = 1f,
    val text: Float = 1f,
    val glow: Float = 0f,
    val corner: Int = 7,
    val fontFamily: FontFamily? = null,
    val bold: Boolean = false
)

val LocalKeyScale = compositionLocalOf { KeyScale() }

/** ارتفاع مفتاح مقاساً حسب إعداد المستخدم. */
@Composable
fun kh(base: Int): Dp = (base * LocalKeyScale.current.height).dp

/** حجم خط مقاساً حسب إعداد المستخدم. */
@Composable
fun ts(base: Int): TextUnit = (base * LocalKeyScale.current.text).sp

@Composable
fun keyFontWeight(default: FontWeight = FontWeight.Medium): FontWeight =
    if (LocalKeyScale.current.bold) FontWeight.Bold else default
