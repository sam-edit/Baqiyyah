package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AlDeenTokens {
    // Spacing & Padding
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 12.dp
    val SpacingLarge = 16.dp
    val SpacingXLarge = 20.dp
    val SpacingXXLarge = 24.dp
    val SpacingHuge = 32.dp

    // Corner Radii (Geometric Balance: structured, proportional curves)
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp
    val CornerRadiusXLarge = 22.dp
    val CornerRadiusPill = 50.dp

    // Shapes
    val ShapeCard = RoundedCornerShape(CornerRadiusLarge)
    val ShapeSmallCard = RoundedCornerShape(CornerRadiusMedium)
    val ShapePill = RoundedCornerShape(CornerRadiusPill)
    val ShapeSheet = RoundedCornerShape(topStart = CornerRadiusXLarge, topEnd = CornerRadiusXLarge)

    // Elevation & Borders
    val CardBorderWidth = 1.dp
    val ElevationLow = 2.dp
    val ElevationMedium = 4.dp
    val ElevationHigh = 8.dp

    // Icon Sizes
    val IconSizeSmall = 18.dp
    val IconSizeMedium = 24.dp
    val IconSizeLarge = 28.dp
    val IconSizeXLarge = 36.dp
    val TouchTargetMin = 48.dp

    // Animation Durations
    const val AnimationDurationShort = 200
    const val AnimationDurationMedium = 350
    const val AnimationDurationLong = 500
}
