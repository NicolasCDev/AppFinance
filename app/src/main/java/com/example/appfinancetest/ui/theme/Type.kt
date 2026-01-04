package com.example.appfinancetest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// style = MaterialTheme.typography.titleSmall
// Set of Material typography styles to start with
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp, // 24.sp
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center,
        color = white
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp, // 30.sp
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp,
        color = white
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp, // 20.sp
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = white
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // 16.sp
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center,
        color = white
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp, // 12.sp
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp,
        color = white
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,// 10.sp
        lineHeight = 10.sp,
        letterSpacing = 0.5.sp,
        color = gray
    )
)