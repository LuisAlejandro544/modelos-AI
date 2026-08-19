package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF0A2338),
    primaryContainer = Color(0xFF193753),
    onPrimaryContainer = Color(0xFFCCE4FF),
    secondary = AmberSecondaryDark,
    onSecondary = Color(0xFF451E00),
    secondaryContainer = Color(0xFF4E2A0B),
    onSecondaryContainer = Color(0xFFFFDDB8),
    tertiary = MossTertiaryDark,
    onTertiary = Color(0xFF003827),
    background = WarmSlateDark,
    onBackground = TextPrimaryDark,
    surface = WarmSlateCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = WarmSlateCardVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = WarmSlateBorderDark,
    outlineVariant = Color(0xFF333E4C)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E5F8),
    onPrimaryContainer = Color(0xFF092036),
    secondary = AmberSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECCF),
    onSecondaryContainer = Color(0xFF3B1800),
    tertiary = MossTertiaryLight,
    onTertiary = Color.White,
    background = WarmSlateLight,
    onBackground = TextPrimaryLight,
    surface = WarmSlateCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = WarmSlateCardVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = WarmSlateBorderLight,
    outlineVariant = Color(0xFFE2E8F0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to maintain consistent warm identity requested by the user
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
