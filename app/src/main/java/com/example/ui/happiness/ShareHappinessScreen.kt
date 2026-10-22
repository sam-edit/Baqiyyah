package com.example.ui.happiness

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SkyBlueAccent = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareHappinessScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 10 Background Templates
    val templates = remember { HappinessTemplatesProvider.templates }
    var selectedTemplate by remember { mutableStateOf(HappinessTemplatesProvider.getDefault()) }

    // Text Boxes over the image
    var textItems by remember {
        mutableStateOf(
            listOf(
                HappinessTextItem(
                    text = "السلام علیکم",
                    xPercent = 0.5f,
                    yPercent = 0.45f,
                    fontSizeSp = 24f,
                    color = Color.White,
                    backgroundColor = null,
                    fontFamilyType = HappinessFontFamily.URDU,
                    sourceType = HappinessSourceType.CUSTOM
                )
            )
        )
    }

    var selectedTextId by remember { mutableStateOf<String?>(textItems.firstOrNull()?.id) }
    val activeItem = textItems.firstOrNull { it.id == selectedTextId }

    // Dialog states
    var showInsertDialog by remember { mutableStateOf(false) }
    var dialogInitialTab by remember { mutableStateOf(InsertContentType.MANUAL) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var showColorPaletteDialog by remember { mutableStateOf(false) }
    var isPickingBgColor by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Canvas size in pixels for accurate 1:1 hand drag
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Share Happiness",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 17.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AlDeenText(
                            text = "(خوشیاں بانٹیں)",
                            style = AlDeenTypography.UrduBody.copy(
                                color = EmeraldPrimary,
                                fontSize = 13.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("happiness_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldPrimary
                        )
                    }
                },
                actions = {
                    // Main Prominent Top Share Button
                    Button(
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                coroutineScope.launch {
                                    HappinessImageExporter.exportAndShare(
                                        context = context,
                                        template = selectedTemplate,
                                        textItems = textItems
                                    )
                                    isExporting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("happiness_top_share_button")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("share_happiness_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // =========================================================================
            // UPPER AREA: The Card Canvas (100% Uninterrupted for 1-Finger Drag)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .aspectRatio(selectedTemplate.aspectRatio, matchHeightConstraintsFirst = true)
                        .fillMaxHeight()
                        .shadow(10.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .onGloballyPositioned { coordinates ->
                            canvasSize = coordinates.size
                        }
                        .testTag("happiness_canvas_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    selectedTextId = null
                                }
                            }
                    ) {
                        // Background Islamic Image
                        Image(
                            painter = painterResource(id = selectedTemplate.drawableResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )

                        // Render Text Boxes
                        textItems.forEach { item ->
                            val isSelected = (item.id == selectedTextId)
                            var itemWidthPx by remember { mutableIntStateOf(0) }
                            var itemHeightPx by remember { mutableIntStateOf(0) }
                            val paraLayouts = remember(item.id) { mutableStateMapOf<Int, TextLayoutResult>() }
                            val paraYs = remember(item.id) { mutableStateMapOf<Int, Float>() }

                            Box(
                                modifier = Modifier
                                    .offset {
                                        val left = (canvasSize.width * item.xPercent - itemWidthPx / 2f).roundToInt()
                                        val top = (canvasSize.height * item.yPercent - itemHeightPx / 2f).roundToInt()
                                        IntOffset(
                                            x = left.coerceIn(0, (canvasSize.width - itemWidthPx).coerceAtLeast(0)),
                                            y = top.coerceIn(0, (canvasSize.height - itemHeightPx).coerceAtLeast(0))
                                        )
                                    }
                                    .onSizeChanged { size ->
                                        itemWidthPx = size.width
                                        itemHeightPx = size.height
                                    }
                                    .widthIn(max = 300.dp)
                            ) {
                                // Inner Text Box (1-Finger Move Drag)
                                Box(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth()
                                        .pointerInput(item.id) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                selectedTextId = item.id
                                                var drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                                    change.consume()
                                                }
                                                if (drag != null) {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                        if (!change.pressed) break
                                                        val dragAmount = change.position - change.previousPosition
                                                        change.consume()
                                                        val cw = canvasSize.width.coerceAtLeast(1)
                                                        val ch = canvasSize.height.coerceAtLeast(1)
                                                        val dx = dragAmount.x / cw
                                                        val dy = dragAmount.y / ch
                                                        textItems = textItems.map { t ->
                                                            if (t.id == item.id) {
                                                                t.copy(
                                                                    xPercent = (t.xPercent + dx).coerceIn(0.05f, 0.95f),
                                                                    yPercent = (t.yPercent + dy).coerceIn(0.05f, 0.95f)
                                                                )
                                                            } else t
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .background(
                                            if (isSelected && item.backgroundColor == null) Color.Black.copy(alpha = 0.35f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) SkyBlueAccent else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .drawBehind {
                                            val bg = item.backgroundColor ?: return@drawBehind
                                            val expected = if (item.sourceType == HappinessSourceType.CUSTOM) 1
                                            else item.text.replace(Regex("\n{3,}"), "\n\n").split("\n\n").count { it.isNotBlank() }
                                            val lines = ArrayList<FloatArray>()
                                            for (pi in 0 until expected) {
                                                val lay = paraLayouts[pi] ?: continue
                                                val y0 = paraYs[pi] ?: 0f
                                                for (i in 0 until lay.lineCount) {
                                                    val l = lay.getLineLeft(i)
                                                    val r = lay.getLineRight(i)
                                                    if (r - l <= 0.5f) continue
                                                    lines.add(floatArrayOf(l, r, y0 + lay.getLineTop(i), y0 + lay.getLineBottom(i)))
                                                }
                                            }
                                            val corners = buildHighlightCorners(lines, 8.dp.toPx(), 2.dp.toPx(), 10.dp.toPx())
                                            if (corners.size >= 3) {
                                                val path = Path()
                                                val last = corners.last()
                                                path.moveTo(last[4], last[5])
                                                for (c in corners) {
                                                    path.lineTo(c[0], c[1])
                                                    path.quadraticBezierTo(c[2], c[3], c[4], c[5])
                                                }
                                                path.close()
                                                drawPath(path, bg)
                                            }
                                        }
                                ) {
                                    when (item.sourceType) {
                                        HappinessSourceType.CUSTOM -> {
                                            val chosenFamily = when (item.fontFamilyType) {
                                                HappinessFontFamily.URDU -> AlDeenFontRegistry.UrduFontFamily
                                                HappinessFontFamily.ARABIC -> AlDeenFontRegistry.ArabicFontFamily
                                            }
                                            TrackedText(
                                                index = 0,
                                                layouts = paraLayouts,
                                                ys = paraYs,
                                                text = item.text,
                                                color = item.color,
                                                fontSize = item.fontSizeSp.sp,
                                                lineHeight = (item.fontSizeSp * 1.35f).sp,
                                                fontFamily = chosenFamily,
                                                textAlign = item.textAlign,
                                                style = LocalTextStyle.current.copy(
                                                    shadow = if (item.hasShadow && item.backgroundColor == null) Shadow(
                                                        color = Color.Black.copy(alpha = 0.9f),
                                                        offset = Offset(2f, 3f),
                                                        blurRadius = 6f
                                                    ) else null
                                                )
                                            )
                                        }
                                        HappinessSourceType.QURAN, HappinessSourceType.HADITH -> {
                                            val cleanParagraphs = item.text.replace(Regex("\n{3,}"), "\n\n").split("\n\n")
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy((item.fontSizeSp * 0.25f).dp)
                                            ) {
                                                cleanParagraphs.filter { it.isNotBlank() }.forEachIndexed { pIndex, para ->
                                                    if (para.isNotBlank()) {
                                                        val script = detectScriptLanguage(para)
                                                        val isArabic = (script == ScriptLanguage.ARABIC)
                                                        TrackedText(
                                                            index = pIndex,
                                                            layouts = paraLayouts,
                                                            ys = paraYs,
                                                            text = para,
                                                            color = item.color,
                                                            fontSize = if (isArabic) item.fontSizeSp.sp else (item.fontSizeSp * 0.82f).sp,
                                                            lineHeight = if (isArabic) (item.fontSizeSp * 1.5f).sp else (item.fontSizeSp * 1.35f).sp,
                                                            fontFamily = if (isArabic) AlDeenFontRegistry.ArabicFontFamily else AlDeenFontRegistry.UrduFontFamily,
                                                            textAlign = item.textAlign,
                                                            style = LocalTextStyle.current.copy(
                                                                shadow = if (item.hasShadow && item.backgroundColor == null) Shadow(
                                                                    color = Color.Black.copy(alpha = 0.9f),
                                                                    offset = Offset(2f, 3f),
                                                                    blurRadius = 6f
                                                                ) else null
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Corner Badges on selected text block:
                                if (isSelected) {
                                    // 1. Left side top: Cross (X) delete button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .size(28.dp)
                                            .shadow(4.dp, CircleShape)
                                            .background(Color(0xFFE53935), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .clickable {
                                                textItems = textItems.filterNot { it.id == item.id }
                                                if (selectedTextId == item.id) {
                                                    selectedTextId = textItems.lastOrNull()?.id
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete box",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // 2. Left side bottom corner: Edit button (tap to edit text and change font)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .size(28.dp)
                                            .shadow(4.dp, CircleShape)
                                            .background(Color(0xFF0284C7), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .clickable {
                                                editingTextId = item.id
                                                dialogInitialTab = InsertContentType.MANUAL
                                                showInsertDialog = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit text and font",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // 3. Right side bottom corner: Resizer (diagonal arrow handle)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(28.dp)
                                            .shadow(4.dp, CircleShape)
                                            .background(EmeraldPrimary, CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .pointerInput(item.id + "_resize") {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val delta = (dragAmount.x + dragAmount.y) * 0.12f
                                                    textItems = textItems.map { t ->
                                                        if (t.id == item.id) {
                                                            t.copy(fontSizeSp = (t.fontSizeSp + delta).coerceIn(12f, 54f))
                                                        } else t
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_diagonal_resize),
                                            contentDescription = "Resize text",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // LOWER AREA: Reordered Controls (Colors on top, Images at bottom)
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, MintBorder.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // -------------------------------------------------------------
                    // 1. Action Row: Quick Add Buttons (+ Text, + Ayah, + Hadith)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Custom Text Quick Add
                        Button(
                            onClick = {
                                editingTextId = null
                                dialogInitialTab = InsertContentType.MANUAL
                                showInsertDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_custom_text_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Text (متن)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Quran Ayah Quick Add
                        Button(
                            onClick = {
                                editingTextId = null
                                dialogInitialTab = InsertContentType.QURAN
                                showInsertDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_ayah_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldContainer, contentColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ayah (آیت)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Hadith Quick Add
                        Button(
                            onClick = {
                                editingTextId = null
                                dialogInitialTab = InsertContentType.HADITH
                                showInsertDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_hadith_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldContainer, contentColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hadith (حدیث)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // -------------------------------------------------------------
                    // 3. Text Color Row
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            modifier = Modifier.width(48.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            items(PredefinedHappinessColors) { color ->
                                val isCurrent = (activeItem?.color == color)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isCurrent) 2.5.dp else 1.dp,
                                            color = if (isCurrent) EmeraldPrimary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (activeItem != null) {
                                                textItems = textItems.map { t ->
                                                    if (t.id == activeItem.id) t.copy(color = color) else t
                                                }
                                            } else {
                                                textItems = textItems.map { t -> t.copy(color = color) }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (color == Color.White || color == Color(0xFFFFF3CD)) Color.Black else Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Circular Color Palette Button
                        IconButton(
                            onClick = {
                                isPickingBgColor = false
                                showColorPaletteDialog = true
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Palette", tint = EmeraldPrimary, modifier = Modifier.size(19.dp))
                        }
                    }

                    // -------------------------------------------------------------
                    // 4. Text Background Color Row (Requirement)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Text Bg:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            modifier = Modifier.width(48.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // "None / Transparent" option
                            item {
                                val isNone = (activeItem?.backgroundColor == null)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .border(
                                            width = if (isNone) 2.5.dp else 1.dp,
                                            color = if (isNone) EmeraldPrimary else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (activeItem != null) {
                                                textItems = textItems.map { t ->
                                                    if (t.id == activeItem.id) t.copy(backgroundColor = null) else t
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Ø", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }

                            // Preset Background Colors
                            items(PredefinedBackgroundColors) { color ->
                                val isCurrent = (activeItem?.backgroundColor == color)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isCurrent) 2.5.dp else 1.dp,
                                            color = if (isCurrent) EmeraldPrimary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (activeItem != null) {
                                                textItems = textItems.map { t ->
                                                    if (t.id == activeItem.id) t.copy(backgroundColor = color) else t
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Background Circular Palette Button
                        IconButton(
                            onClick = {
                                isPickingBgColor = true
                                showColorPaletteDialog = true
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Bg Palette", tint = EmeraldPrimary, modifier = Modifier.size(19.dp))
                        }
                    }

                    // -------------------------------------------------------------
                    // 5. Clean Images Selector at the very bottom (Clean thumbnails only, NO title/description text!)
                    // -------------------------------------------------------------
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp)
                    ) {
                        items(templates) { template ->
                            val isSelected = (template.id == selectedTemplate.id)

                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) EmeraldPrimary else MintBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTemplate = template }
                                    .testTag("template_${template.id}")
                            ) {
                                Image(
                                    painter = painterResource(id = template.drawableResId),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Number label badge: 1, 2, 3...
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(3.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = template.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(14.dp)
                                            .background(EmeraldPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Insert Content Dialog
    if (showInsertDialog) {
        val editingItem = textItems.firstOrNull { it.id == editingTextId }
        InsertContentDialog(
            initialText = editingItem?.text.orEmpty(),
            initialFont = editingItem?.fontFamilyType ?: HappinessFontFamily.URDU,
            initialTab = dialogInitialTab,
            onConfirm = { newText, newFont, newSourceType ->
                if (editingItem != null) {
                    textItems = textItems.map { t ->
                        if (t.id == editingItem.id) {
                            t.copy(text = newText, fontFamilyType = newFont, sourceType = newSourceType)
                        } else t
                    }
                } else {
                    val newItem = HappinessTextItem(
                        text = newText,
                        xPercent = 0.5f,
                        yPercent = 0.45f,
                        fontSizeSp = 22f,
                        color = Color.White,
                        fontFamilyType = newFont,
                        sourceType = newSourceType
                    )
                    textItems = textItems + newItem
                    selectedTextId = newItem.id
                }
            },
            onDismiss = {
                showInsertDialog = false
                editingTextId = null
            }
        )
    }



    // Circular Color Palette Dialog (for both text color and text background)
    if (showColorPaletteDialog) {
        ColorPaletteDialog(
            initialColor = if (isPickingBgColor) (activeItem?.backgroundColor ?: Color(0x99000000)) else (activeItem?.color ?: Color.White),
            title = if (isPickingBgColor) "Text Background Color (پس منظر کا رنگ)" else "Text Color (متن کا رنگ)",
            allowTransparent = isPickingBgColor,
            onColorSelected = { pickedColor ->
                if (isPickingBgColor) {
                    if (activeItem != null) {
                        textItems = textItems.map { t ->
                            if (t.id == activeItem.id) t.copy(backgroundColor = pickedColor) else t
                        }
                    }
                } else {
                    val chosen = pickedColor ?: Color.White
                    if (activeItem != null) {
                        textItems = textItems.map { t ->
                            if (t.id == activeItem.id) t.copy(color = chosen) else t
                        }
                    } else {
                        textItems = textItems.map { t -> t.copy(color = chosen) }
                    }
                }
            },
            onDismiss = { showColorPaletteDialog = false }
        )
    }
}

/**
 * Plain text that reports its layout and position, so the parent can draw
 * one connected background shape behind all lines/paragraphs.
 */
@Composable
private fun TrackedText(
    index: Int,
    layouts: MutableMap<Int, TextLayoutResult>,
    ys: MutableMap<Int, Float>,
    text: String,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        style = style,
        onTextLayout = { layouts[index] = it },
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { ys[index] = it.positionInParent().y }
    )
}
