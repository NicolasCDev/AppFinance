package com.example.appfinancetest

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Formats a double amount as a monetary string with a thousands separator.
 * Format: "1 234,56 €"
 */
fun formatCurrency(amount: Double?): String {
    return String.format(Locale.getDefault(), "%,.2f €", amount ?: 0.0)
}
/**
 * Format a double as a percentage with a single digit after the decimal point.
 * Format: "12.3%"
 */
fun formatPercentage(amount: Double?): String {
    return String.format(Locale.getDefault(), "%.1f%%", amount ?: 0.0)
}

/**
 * Retrieves a color from the attributes of the current theme.
 */
private fun getThemeColor(context: Context, @AttrRes attrRes: Int): Color {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attrRes, typedValue, true)
    return Color(typedValue.data)
}

/**
 * Stylized Text component for monetary amounts.
 * Dynamically uses the colors defined in the theme (themes.xml via attrs.xml).
 */
@Composable
fun CurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    isNegative: Boolean? = null,
    isVisibilityOff: Boolean = false,
    showSign: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val context = LocalContext.current
    val finalIsNegative = isNegative ?: (amount < 0)
    
    // Dynamic theme color retrieval
    val colorPositive = getThemeColor(context, R.attr.colorAmountPositive)
    val colorNegative = getThemeColor(context, R.attr.colorAmountNegative)
    
    val color = if (finalIsNegative) colorNegative else colorPositive

    val displayValue = if (isVisibilityOff) {
        if (showSign) {
            val sign = if (finalIsNegative) "-" else "+"
            "$sign *** €"
        } else {
            "**** €"
        }
    } else {
        val sign = if (showSign) (if (finalIsNegative) "-" else "+") else ""
        val valueToFormat = if (showSign) abs(amount) else amount
        "$sign${formatCurrency(valueToFormat)}"
    }

    Text(
        text = displayValue,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = style
    )
}

/**
 * Text component for monetary amounts using the colorOnPrimary color from the theme.
 * Keep the formatting with two decimal places, thousands separator, and € symbol.
 */
@Composable
fun CurrencyTextOnPrimary(
    amount: Double,
    modifier: Modifier = Modifier,
    isVisibilityOff: Boolean = false,
    showSign: Boolean = false,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val color = getThemeColor(LocalContext.current, R.attr.colorOnPrimary)

    val finalIsNegative = amount < 0
    
    val displayValue = if (isVisibilityOff) {
        if (showSign) {
            val sign = if (finalIsNegative) "-" else "+"
            "$sign *** €"
        } else {
            "**** €"
        }
    } else {
        val sign = if (showSign) (if (finalIsNegative) "-" else "+") else ""
        val valueToFormat = if (showSign) abs(amount) else amount
        "$sign${formatCurrency(valueToFormat)}"
    }

    Text(
        text = displayValue,
        modifier = modifier,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        textAlign = textAlign,
        style = style
    )
}
/**
 * Text component for percentages.
 * Must display the (+/-) sign, one decimal place, and use
 * the colors colorAmountPositive/colorAmountNegative.
 */
@Composable
fun PercentageText(
    amount: Double,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.bodySmall
) {
    val context = LocalContext.current
    val isNegative = amount < 0

    // Recovering theme colors
    val colorPositive = getThemeColor(context, R.attr.colorAmountPositive)
    val colorNegative = getThemeColor(context, R.attr.colorAmountNegative)
    val color = if (isNegative) colorNegative else colorPositive

    // String construction: Sign + Formatted absolute value
    // String.format “%.1f%%” guarantees a single decimal place and the % symbol is appended.
    val sign = if (isNegative) "-" else "+"
    val displayValue = "$sign${formatPercentage(abs(amount))}"

    Text(
        text = displayValue,
        modifier = modifier,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        textAlign = textAlign,
        style = style
    )
}

/**
 * Text component for percentages using the colorOnPrimary color from the theme.
 * Displays the (+/-) sign and one decimal place.
 */
@Composable
fun PercentageTextOnPrimary(
    amount: Double,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val color = getThemeColor(LocalContext.current, R.attr.colorOnPrimary)

    val displayValue = formatPercentage(abs(amount))

    Text(
        text = displayValue,
        modifier = modifier,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        textAlign = textAlign,
        style = style
    )
}
