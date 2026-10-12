package com.erfan.ytsummarizer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Red = Color(0xFFE5383B)

private val Dark = darkColorScheme(
  primary = Color(0xFFFF6B6B),
  onPrimary = Color(0xFF2B0000),
  primaryContainer = Color(0xFF5A1A1C),
  secondary = Color(0xFFFFB4A9),
  background = Color(0xFF0F0F12),
  surface = Color(0xFF16161A),
  surfaceVariant = Color(0xFF23232A),
)

private val Light = lightColorScheme(
  primary = Red,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFDAD6),
  secondary = Color(0xFF9C4141),
  background = Color(0xFFFFFBFA),
  surface = Color(0xFFFFFFFF),
  surfaceVariant = Color(0xFFF4EDEC),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
