package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = StudyBluePrimaryDark,
    onPrimary = StudyBlueOnPrimaryDark,
    primaryContainer = StudyBlueContainerDark,
    onPrimaryContainer = StudyBlueOnContainerDark,
    secondary = StudyPurpleSecondaryDark,
    onSecondary = StudyPurpleOnSecondaryDark,
    secondaryContainer = StudyPurpleContainerDark,
    onSecondaryContainer = StudyPurpleOnContainerDark,
    tertiary = StudyTealTertiaryDark,
    onTertiary = StudyTealOnTertiaryDark,
    tertiaryContainer = StudyTealContainerDark,
    onTertiaryContainer = StudyTealOnContainerDark,
    background = StudyBackgroundDark,
    onBackground = StudyOnSurfaceDark,
    surface = StudySurfaceDark,
    onSurface = StudyOnSurfaceDark,
    surfaceVariant = StudySurfaceVariantDark,
    onSurfaceVariant = StudyOnSurfaceVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StudyBluePrimaryLight,
    onPrimary = StudyBlueOnPrimaryLight,
    primaryContainer = StudyBlueContainerLight,
    onPrimaryContainer = StudyBlueOnContainerLight,
    secondary = StudyPurpleSecondaryLight,
    onSecondary = StudyPurpleOnSecondaryLight,
    secondaryContainer = StudyPurpleContainerLight,
    onSecondaryContainer = StudyPurpleOnContainerLight,
    tertiary = StudyTealTertiaryLight,
    onTertiary = StudyTealOnTertiaryLight,
    tertiaryContainer = StudyTealContainerLight,
    onTertiaryContainer = StudyTealOnContainerLight,
    background = StudyBackgroundLight,
    onBackground = StudyOnSurfaceLight,
    surface = StudySurfaceLight,
    onSurface = StudyOnSurfaceLight,
    surfaceVariant = StudySurfaceVariantLight,
    onSurfaceVariant = StudyOnSurfaceVariantLight,
  )

val StudyBuddyShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun StudyBuddyTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = StudyBuddyShapes,
    content = content,
  )
}
