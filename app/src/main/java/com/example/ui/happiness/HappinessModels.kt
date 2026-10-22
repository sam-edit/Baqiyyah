package com.example.ui.happiness

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.R
import java.util.UUID

/**
 * Model representing an Islamic background image template.
 *
 * HOW TO ADD MORE IMAGES IN THE FUTURE:
 * Simply add a new [HappinessTemplate] entry to the list in [HappinessTemplatesProvider]!
 * Images of ANY aspect ratio are supported (1:1 square, 4:5 portrait, 9:16 story, 16:9 landscape, etc.).
 *
 * Example:
 * HappinessTemplate(
 *     id = "my_custom_mosque",
 *     title = "My Mosque View",
 *     urduTitle = "مسجد کا منظر",
 *     drawableResId = R.drawable.my_custom_mosque,
 *     aspectRatio = 1.0f // Or whatever width/height ratio your image has
 * )
 */
data class HappinessTemplate(
    val id: String,
    val title: String,
    val urduTitle: String,
    @DrawableRes val drawableResId: Int,
    val aspectRatio: Float = 1.0f,
    val description: String = ""
)

/**
 * Supported built-in typography fonts for text overlays.
 * Only two authentic built-in fonts:
 * 1. Urdu (Built-in Noto Nastaliq Urdu)
 * 2. Arabic (Built-in Scheherazade New Naskh)
 */
enum class HappinessFontFamily(val displayName: String, val urduName: String) {
    URDU("Urdu (اردو)", "اردو"),
    ARABIC("Arabic (عربی)", "عربی")
}

/**
 * Origin of the text content to govern typography and editing rules
 */
enum class HappinessSourceType(val displayName: String, val urduName: String) {
    CUSTOM("Custom Text", "آزادانہ متن"),
    QURAN("Quran Ayah", "قرآنی آیت"),
    HADITH("Hadith", "حدیث مبارکہ")
}

/**
 * Model representing an editable text overlay on the canvas.
 */
data class HappinessTextItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xPercent: Float = 0.5f, // Normalized center horizontal position (0.0 .. 1.0)
    val yPercent: Float = 0.5f, // Normalized center vertical position (0.0 .. 1.0)
    val fontSizeSp: Float = 22f,
    val color: Color = Color.White,
    val backgroundColor: Color? = null, // Text background color (null = transparent)
    val fontFamilyType: HappinessFontFamily = HappinessFontFamily.URDU,
    val sourceType: HappinessSourceType = HappinessSourceType.CUSTOM,
    val textAlign: TextAlign = TextAlign.Center,
    val isBold: Boolean = true,
    val hasShadow: Boolean = true
)

/**
 * Registry of Islamic background image templates.
 * 10 initial templates of varying aspect ratios (1:1, 4:3, 3:4, 16:9, 4:5, 9:16).
 */
object HappinessTemplatesProvider {

    /**
     * Complete list of templates available in the app.
     * To add more images in the future, simply append your new template to this list!
     */
    val templates: List<HappinessTemplate> = listOf(
        HappinessTemplate(
            id = "1",
            title = "1",
            urduTitle = "1",
            drawableResId = R.drawable.happiness_image1,
            aspectRatio = 1.0f, // 1:1 Square
            description = "Image 1"
        ),
        HappinessTemplate(
            id = "2",
            title = "2",
            urduTitle = "2",
            drawableResId = R.drawable.happiness_image2,
            aspectRatio = 1.333f, // 4:3 Landscape
            description = "Image 2"
        ),
        HappinessTemplate(
            id = "3",
            title = "3",
            urduTitle = "3",
            drawableResId = R.drawable.happiness_image3,
            aspectRatio = 1.0f, // 1:1 Square
            description = "Image 3"
        ),
        HappinessTemplate(
            id = "4",
            title = "4",
            urduTitle = "4",
            drawableResId = R.drawable.happiness_image4,
            aspectRatio = 0.75f, // 3:4 Portrait
            description = "Image 4"
        ),
        HappinessTemplate(
            id = "5",
            title = "5",
            urduTitle = "5",
            drawableResId = R.drawable.happiness_image1,
            aspectRatio = 1.0f, // 1:1 Square
            description = "Image 5"
        ),
        HappinessTemplate(
            id = "6",
            title = "6",
            urduTitle = "6",
            drawableResId = R.drawable.happiness_image6,
            aspectRatio = 0.8f, // 4:5 Portrait
            description = "Image 6"
        ),
        HappinessTemplate(
            id = "7",
            title = "7",
            urduTitle = "7",
            drawableResId = R.drawable.happiness_image7,
            aspectRatio = 0.5625f, // 9:16 Story / Tall Portrait
            description = "Image 7"
        ),
        HappinessTemplate(
            id = "8",
            title = "8",
            urduTitle = "8",
            drawableResId = R.drawable.happiness_image8,
            aspectRatio = 0.75f, // 3:4 Portrait
            description = "Image 8"
        ),
        HappinessTemplate(
            id = "9",
            title = "9",
            urduTitle = "9",
            drawableResId = R.drawable.happiness_image9,
            aspectRatio = 1.777f, // 16:9 Banner / Wide Landscape
            description = "Image 9"
        ),
        HappinessTemplate(
            id = "10",
            title = "10",
            urduTitle = "10",
            drawableResId = R.drawable.happiness_image10,
            aspectRatio = 1.0f, // 1:1 Square
            description = "Image 10"
        ),
        HappinessTemplate(
            id = "11",
            title = "11",
            urduTitle = "11",
            drawableResId = R.drawable.happiness_image11,
            aspectRatio = 0.75f, // 16:9 Banner / Wide Landscape
            description = "Image 11"
        ),
        HappinessTemplate(
            id = "12",
            title = "12",
            urduTitle = "12",
            drawableResId = R.drawable.happiness_image12,
            aspectRatio = 2.0f, // 16:9 Banner / Wide Landscape
            description = "Image 12"
        ),
        HappinessTemplate(
            id = "13",
            title = "13",
            urduTitle = "13",
            drawableResId = R.drawable.happiness_image13,
            aspectRatio = 0.8f, // 16:9 Banner / Wide Landscape
            description = "Image 13"
        ),
        HappinessTemplate(
            id = "14",
            title = "14",
            urduTitle = "14",
            drawableResId = R.drawable.happiness_image14,
            aspectRatio = 0.666f, // 16:9 Banner / Wide Landscape
            description = "Image 14"
        ),
        HappinessTemplate(
            id = "15",
            title = "15",
            urduTitle = "15",
            drawableResId = R.drawable.happiness_image15,
            aspectRatio = 0.5625f, // 16:9 Banner / Wide Landscape
            description = "Image 15"
        ),
        HappinessTemplate(
            id = "16",
            title = "16",
            urduTitle = "16",
            drawableResId = R.drawable.happiness_image16,
            aspectRatio = 0.5625f, // 16:9 Banner / Wide Landscape
            description = "Image 16"
        ),
        HappinessTemplate(
            id = "17",
            title = "17",
            urduTitle = "17",
            drawableResId = R.drawable.happiness_image17,
            aspectRatio = 0.666f, // 16:9 Banner / Wide Landscape
            description = "Image 17"
        ),
        HappinessTemplate(
            id = "18",
            title = "18",
            urduTitle = "18",
            drawableResId = R.drawable.happiness_image18,
            aspectRatio = 1.333f, // 16:9 Banner / Wide Landscape
            description = "Image 18"
        ),
        HappinessTemplate(
            id = "19",
            title = "19",
            urduTitle = "19",
            drawableResId = R.drawable.happiness_image19,
            aspectRatio = 0.666f, // 16:9 Banner / Wide Landscape
            description = "Image 19"
        ),
        HappinessTemplate(
            id = "20",
            title = "20",
            urduTitle = "20",
            drawableResId = R.drawable.happiness_image20,
            aspectRatio = 0.5625f, // 16:9 Banner / Wide Landscape
            description = "Image 20"
        )
    )

    fun getDefault(): HappinessTemplate = templates.first()
}
