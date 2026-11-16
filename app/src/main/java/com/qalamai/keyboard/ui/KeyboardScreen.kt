package com.qalamai.keyboard.ui

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalamai.keyboard.ai.TonePreset
import com.qalamai.keyboard.data.ClipboardCategory
import com.qalamai.keyboard.ime.ActiveToolbarPanel
import com.qalamai.keyboard.ime.KeyboardLayoutMode
import com.qalamai.keyboard.ime.OneHandedMode
import com.qalamai.keyboard.ime.SmartKeyboardService
import com.qalamai.keyboard.ui.theme.KeyboardPalette
import com.qalamai.keyboard.ui.theme.KeyboardThemeId
import com.qalamai.keyboard.ui.theme.paletteFor

@Composable
fun KeyboardScreen(service: SmartKeyboardService) {
    val palette = paletteFor(service.currentTheme, service.customAccent, service.customDark)

    // ترتيب المفاتيح ثابت (يسار→يمين) مثل لوحات المفاتيح الفعلية بغض النظر عن لغة النظام
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalKeyScale provides KeyScale(
            height = service.heightScale,
            text = service.textScale,
            glow = service.glow,
            corner = service.keyCorner,
            fontFamily = when (service.fontName) {
                "SERIF" -> FontFamily.Serif
                "MONO" -> FontFamily.Monospace
                "CURSIVE" -> FontFamily.Cursive
                else -> null
            },
            bold = service.fontBold
        )
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
            .padding(bottom = 6.dp)
    ) {
        // 1. شريط الأدوات العلوي (Toolbar)
        TopToolbar(service = service, palette = palette)

        // 2. شريط التنبؤ بالكلمات / حالة الذكاء الاصطناعي
        SuggestionsStrip(service = service, palette = palette)

        // 3. اللوحة المنبثقة النشطة (AI / الحافظة / المؤشر / التشكيل / الثيمات)
        when (service.activePanel) {
            ActiveToolbarPanel.AI_TOOLS -> AiToolsPanel(service, palette)
            ActiveToolbarPanel.CLIPBOARD -> ClipboardPanel(service, palette)
            ActiveToolbarPanel.CURSOR_PC -> CursorAndPcShortcutsPanel(service, palette)
            ActiveToolbarPanel.TASHKEEL -> TashkeelPanel(service, palette)
            ActiveToolbarPanel.THEMES -> ThemePickerPanel(service, palette)
            ActiveToolbarPanel.EMOJI -> EmojiStickersPanel(service, palette)
            ActiveToolbarPanel.CALC -> CalculatorPanel(service, palette)
            ActiveToolbarPanel.PHRASES -> ListManagerPanel(
                title = "الجمل المستخدمة",
                emptyHint = "اكتب جملة في الحقل ثم اضغط «إضافة من الحقل» لحفظها هنا.",
                addLabel = "إضافة من الحقل",
                items = service.phrases,
                palette = palette,
                onAdd = { service.addPhraseFromField() },
                onDelete = { service.deletePhrase(it) },
                onPick = { service.insertPhrase(it) }
            )
            ActiveToolbarPanel.DICTIONARY -> ListManagerPanel(
                title = "قاموسي الشخصي",
                emptyHint = "اكتب كلمة ثم اضغط «إضافة آخر كلمة» لتظهر في الاقتراحات.",
                addLabel = "إضافة آخر كلمة",
                items = service.dictionary,
                palette = palette,
                onAdd = { service.addLastWordToDictionary() },
                onDelete = { service.deleteDictionaryWord(it) },
                onPick = { service.insertPhrase(it) }
            )
            ActiveToolbarPanel.SETTINGS -> KeyboardSettingsPanel(service, palette)
            ActiveToolbarPanel.NONE -> Unit
        }

        // 4 + 5. صف الأرقام وتخطيط المفاتيح (يدعم اليد الواحدة واللوحة المتحركة)
        val oneHanded = service.oneHandedMode != OneHandedMode.OFF
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxPx = constraints.maxWidth.toFloat()
            val fraction = when {
                service.floating -> 0.72f
                oneHanded -> 0.82f
                else -> 1f
            }
            val freePx = maxPx * (1f - fraction)
            val alignment = when {
                service.floating -> Alignment.TopStart
                service.oneHandedMode == OneHandedMode.LEFT -> Alignment.TopStart
                service.oneHandedMode == OneHandedMode.RIGHT -> Alignment.TopEnd
                else -> Alignment.TopCenter
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .align(alignment)
                    .then(
                        if (service.floating) Modifier.offset {
                            IntOffset(service.floatOffsetPx.coerceIn(0f, freePx).roundToInt(), 0)
                        } else Modifier
                    )
            ) {
                if (service.floating) {
                    // مقبض السحب: حرّك اللوحة يميناً ويساراً
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .pointerInput(freePx) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    service.floatOffsetPx =
                                        (service.floatOffsetPx + drag.x).coerceIn(0f, freePx)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(palette.mutedText)
                        )
                    }
                }
                if (service.showNumberRow) {
                    PersistentNumberRow(service = service, palette = palette)
                }
                when (service.layoutMode) {
                    KeyboardLayoutMode.ARABIC -> ArabicKeyboardGrid(service, palette)
                    KeyboardLayoutMode.ENGLISH -> EnglishKeyboardGrid(service, palette)
                    KeyboardLayoutMode.SYMBOLS -> SymbolsKeyboardGrid(service, palette)
                    KeyboardLayoutMode.FULL_PC -> FullPcKeyboardGrid(service, palette)
                }
            }
        }
    }
    }
}

@Composable
private fun TopToolbar(service: SmartKeyboardService, palette: KeyboardPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(palette.toolbarSurface)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ToolbarIds.AI !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "الذكاء AI",
                    active = service.activePanel == ActiveToolbarPanel.AI_TOOLS,
                    palette = palette,
                    icon = Icons.Default.AutoAwesome
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.AI_TOOLS)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.AI_TOOLS
                }
            }

            if (ToolbarIds.CLIPBOARD !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "الحافظة",
                    active = service.activePanel == ActiveToolbarPanel.CLIPBOARD,
                    palette = palette,
                    icon = Icons.Default.ContentPaste
                ) {
                    service.refreshClipboardList()
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.CLIPBOARD)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.CLIPBOARD
                }
            }

            if (ToolbarIds.CURSOR !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "المؤشر PC",
                    active = service.activePanel == ActiveToolbarPanel.CURSOR_PC,
                    palette = palette,
                    icon = Icons.Default.OpenWith
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.CURSOR_PC)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.CURSOR_PC
                }
            }

            if (ToolbarIds.TASHKEEL !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "تشكيل",
                    active = service.activePanel == ActiveToolbarPanel.TASHKEEL,
                    palette = palette,
                    icon = Icons.Default.Spellcheck
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.TASHKEEL)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.TASHKEEL
                }
            }

            if (ToolbarIds.EMOJI !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "إيموجي",
                    active = service.activePanel == ActiveToolbarPanel.EMOJI,
                    palette = palette,
                    icon = Icons.Default.Mood
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.EMOJI)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.EMOJI
                }
            }

            if (ToolbarIds.THEME !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "ثيم",
                    active = service.activePanel == ActiveToolbarPanel.THEMES,
                    palette = palette,
                    icon = Icons.Default.Palette
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.THEMES)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.THEMES
                }
            }

            if (ToolbarIds.CALC !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "حاسبة",
                    active = service.activePanel == ActiveToolbarPanel.CALC,
                    palette = palette,
                    icon = Icons.Default.Calculate
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.CALC)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.CALC
                }
            }

            if (ToolbarIds.PHRASES !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "جمل",
                    active = service.activePanel == ActiveToolbarPanel.PHRASES,
                    palette = palette,
                    icon = Icons.Default.ChatBubble
                ) {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.PHRASES)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.PHRASES
                }
            }

            if (ToolbarIds.VOICE !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "إملاء",
                    active = false,
                    palette = palette,
                    icon = Icons.Default.Mic
                ) { service.startVoiceInput() }
            }

            if (ToolbarIds.TRANSLATE !in service.hiddenToolbar) {
                ToolbarIconButton(
                    label = "ترجمة",
                    active = false,
                    palette = palette,
                    icon = Icons.Default.Translate
                ) { service.quickTranslate() }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { service.toggleLanguage() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (service.layoutMode == KeyboardLayoutMode.ARABIC) "En" else "ع",
                    color = palette.keyText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = {
                    service.layoutMode = if (service.layoutMode == KeyboardLayoutMode.FULL_PC) {
                        KeyboardLayoutMode.ARABIC
                    } else {
                        KeyboardLayoutMode.FULL_PC
                    }
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "تبديل وضع الحاسوب الكامل",
                    tint = if (service.layoutMode == KeyboardLayoutMode.FULL_PC) palette.accentColor else palette.mutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = {
                    service.activePanel = if (service.activePanel == ActiveToolbarPanel.SETTINGS)
                        ActiveToolbarPanel.NONE else ActiveToolbarPanel.SETTINGS
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = if (service.activePanel == ActiveToolbarPanel.SETTINGS) palette.accentColor else palette.mutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    label: String,
    active: Boolean,
    palette: KeyboardPalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) palette.accentColor.copy(alpha = 0.18f) else Color.Transparent,
        border = if (active) BorderStroke(1.dp, palette.accentColor) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) palette.accentColor else palette.keyText,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = if (active) palette.accentColor else palette.keyText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SuggestionsStrip(service: SmartKeyboardService, palette: KeyboardPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(kh(34))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (service.aiLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = palette.accentColor
            )
            Text(
                text = "جاري المعالجة عبر Gemini AI...",
                color = palette.accentColor,
                fontSize = 12.sp
            )
        } else if (!service.aiStatusMessage.isNullOrEmpty()) {
            Text(
                text = service.aiStatusMessage.orEmpty(),
                color = palette.mutedText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                service.suggestions.forEach { word ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.specialKeySurface)
                            .clickable { service.applySuggestion(word) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = word, color = palette.keyText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiToolsPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .border(0.5.dp, palette.borderHairline)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "مساعد الذكاء الاصطناعي (يعمل فقط عند ضغطك)",
            color = palette.mutedText,
            fontSize = 11.sp
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AiActionButton("تدقيق إملائي ونحوي", palette) { service.runAiProofread() }
            AiActionButton("ترجمة للإنجليزية", palette) { service.runAiTranslate("English") }
            AiActionButton("ترجمة للعربية", palette) { service.runAiTranslate("Arabic") }
            AiActionButton("إكمال الجملة AI", palette) { service.runAiAutocomplete() }
            AiActionButton("تشكيل تلقائي", palette) { service.runAiTashkeel() }
            AiActionButton("استوديو الإعلانات", palette) { service.openAdStudio() }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TonePreset.entries.forEach { preset ->
                AiActionButton(preset.labelAr, palette) { service.runAiToneChange(preset) }
            }
        }
    }
}

@Composable
private fun AiActionButton(text: String, palette: KeyboardPalette, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = palette.keySurface,
        border = BorderStroke(1.dp, palette.borderHairline)
    ) {
        Text(
            text = text,
            color = palette.keyText,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun CursorAndPcShortcutsPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // صف اختصارات الحاسوب الحقيقية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "Ctrl+A" to { service.selectAll() },
                "Ctrl+C" to { service.copySelection() },
                "Ctrl+V" to { service.pasteFromClipboard() },
                "Ctrl+Z" to { service.undo() },
                "Ctrl+Y" to { service.redo() },
                "Tab" to { service.sendTab() }
            ).forEach { (label, action) ->
                Surface(
                    onClick = action,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = palette.keySurface
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = palette.keyText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // أسهم تحريك المؤشر + وضع التحديد
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { service.isSelectionMode = !service.isSelectionMode },
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(8.dp),
                color = if (service.isSelectionMode) palette.accentColor else palette.keySurface
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (service.isSelectionMode) "التحديد: مفعل" else "تحديد النص",
                        color = if (service.isSelectionMode) Color.Black else palette.keyText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            ArrowBtn("◄", palette, Modifier.weight(1f)) { service.moveCursor(KeyEvent.KEYCODE_DPAD_LEFT) }
            ArrowBtn("▲", palette, Modifier.weight(1f)) { service.moveCursor(KeyEvent.KEYCODE_DPAD_UP) }
            ArrowBtn("▼", palette, Modifier.weight(1f)) { service.moveCursor(KeyEvent.KEYCODE_DPAD_DOWN) }
            ArrowBtn("►", palette, Modifier.weight(1f)) { service.moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT) }
        }
    }
}

@Composable
private fun ArrowBtn(symbol: String, palette: KeyboardPalette, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = palette.keySurface
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text = symbol, color = palette.keyText, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ClipboardPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    var selectedCategory by remember { mutableStateOf(ClipboardCategory.ALL) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .background(palette.toolbarSurface)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ClipboardCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = cat
                        service.refreshClipboardList(cat)
                    },
                    label = { Text(cat.labelAr, fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(service.clipboardEntries, key = { it.id }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.keySurface)
                        .clickable { service.pasteSpecificClip(entry.text) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.text,
                        color = palette.keyText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = { service.togglePinClip(entry.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "تثبيت",
                                tint = if (entry.isPinned) palette.accentColor else palette.mutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = { service.deleteClip(entry.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = palette.mutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TashkeelPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    val marks = listOf("َ", "ً", "ُ", "ٌ", "ِ", "ٍ", "ْ", "ّ", "ـ", "ء", "أ", "إ", "آ", "ؤ", "ئ", "ة", "ى", "ﷺ", "﷽")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        marks.forEach { mark ->
            Surface(
                onClick = { service.commitText(mark) },
                shape = RoundedCornerShape(6.dp),
                color = palette.keySurface
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mark, color = palette.keyText, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ThemePickerPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KeyboardThemeId.entries.forEach { id ->
            val selected = service.currentTheme == id
            Surface(
                onClick = { service.selectTheme(id) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) palette.accentColor.copy(alpha = 0.2f) else palette.keySurface,
                border = BorderStroke(1.dp, if (selected) palette.accentColor else palette.borderHairline)
            ) {
                Text(
                    text = id.titleAr,
                    color = palette.keyText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PersistentNumberRow(service: SmartKeyboardService, palette: KeyboardPalette) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        numbers.forEach { num ->
            KeyButton(
                label = num,
                palette = palette,
                modifier = Modifier.weight(1f).height(kh(34)),
                isSpecial = true
            ) {
                service.commitText(num)
            }
        }
    }
}

@Composable
private fun ArabicKeyboardGrid(service: SmartKeyboardService, palette: KeyboardPalette) {
    val row1 = if (service.isShifted) listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
               else listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
    val row2 = if (service.isShifted) listOf("ش", "س", "ي", "ب", "ل", "أ", "إ", "آ", "ت", "ن", "م", "ك", "ط")
               else listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
    val row3 = if (service.isShifted) listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ")
               else listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ", "ذ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        KeyRow(row1, service, palette)
        KeyRow(row2, service, palette)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SpecialIconKey(
                icon = Icons.Default.KeyboardCapslock,
                palette = palette,
                active = service.isShifted,
                modifier = Modifier.weight(1.3f)
            ) {
                service.isShifted = !service.isShifted
            }
            row3.forEach { key ->
                KeyButton(label = key, palette = palette, modifier = Modifier.weight(1f).height(kh(44))) {
                    service.commitText(key)
                }
            }
            SpecialIconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                palette = palette,
                modifier = Modifier.weight(1.4f)
            ) {
                service.deleteBackward()
            }
        }

        BottomControlRow(service = service, palette = palette, languageLabel = "EN") {
            service.layoutMode = KeyboardLayoutMode.ENGLISH
        }
    }
}

@Composable
private fun EnglishKeyboardGrid(service: SmartKeyboardService, palette: KeyboardPalette) {
    val r1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val r2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val r3 = listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        KeyRow(r1.map { if (service.isShifted) it.uppercase() else it }, service, palette)
        KeyRow(r2.map { if (service.isShifted) it.uppercase() else it }, service, palette)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SpecialIconKey(
                icon = Icons.Default.KeyboardCapslock,
                palette = palette,
                active = service.isShifted,
                modifier = Modifier.weight(1.4f)
            ) {
                service.isShifted = !service.isShifted
            }
            r3.forEach { k ->
                val ch = if (service.isShifted) k.uppercase() else k
                KeyButton(label = ch, palette = palette, modifier = Modifier.weight(1f).height(kh(44))) {
                    service.commitText(ch)
                }
            }
            SpecialIconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                palette = palette,
                modifier = Modifier.weight(1.4f)
            ) {
                service.deleteBackward()
            }
        }

        BottomControlRow(service = service, palette = palette, languageLabel = "عربي") {
            service.layoutMode = KeyboardLayoutMode.ARABIC
        }
    }
}

@Composable
private fun SymbolsKeyboardGrid(service: SmartKeyboardService, palette: KeyboardPalette) {
    val s1 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    val s2 = listOf("*", "\"", "'", ":", ";", "!", "؟", "،", ".", "_")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        KeyRow(s1, service, palette)
        KeyRow(s2, service, palette)
        BottomControlRow(service = service, palette = palette, languageLabel = "عربي") {
            service.layoutMode = KeyboardLayoutMode.ARABIC
        }
    }
}

@Composable
private fun FullPcKeyboardGrid(service: SmartKeyboardService, palette: KeyboardPalette) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // صف اختصارات PC الكاملة
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton("Tab", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true) { service.sendTab() }
            KeyButton("Ctrl", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true, isAccent = service.isCtrlPressed) {
                service.isCtrlPressed = !service.isCtrlPressed
            }
            KeyButton("Ctrl+A", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true) { service.selectAll() }
            KeyButton("Ctrl+C", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true) { service.copySelection() }
            KeyButton("Ctrl+V", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true) { service.pasteFromClipboard() }
            KeyButton("Ctrl+Z", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true) { service.undo() }
            KeyButton("Ctrl+Y", palette, Modifier.weight(1.2f).height(kh(36)), isSpecial = true) { service.redo() }
        }

        ArabicKeyboardGrid(service = service, palette = palette)
    }
}

@Composable
private fun KeyRow(keys: List<String>, service: SmartKeyboardService, palette: KeyboardPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            KeyButton(
                label = key,
                palette = palette,
                modifier = Modifier.weight(1f).height(kh(44))
            ) {
                service.commitText(key)
            }
        }
    }
}

@Composable
private fun BottomControlRow(
    service: SmartKeyboardService,
    palette: KeyboardPalette,
    languageLabel: String,
    onToggleLang: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        KeyButton(
            label = if (service.layoutMode == KeyboardLayoutMode.SYMBOLS) "ABC" else "?123",
            palette = palette,
            modifier = Modifier.weight(1.3f).height(kh(44)),
            isSpecial = true
        ) {
            service.layoutMode = if (service.layoutMode == KeyboardLayoutMode.SYMBOLS)
                KeyboardLayoutMode.ARABIC else KeyboardLayoutMode.SYMBOLS
        }

        KeyButton(
            label = languageLabel,
            palette = palette,
            modifier = Modifier.weight(1.2f).height(kh(44)),
            isSpecial = true,
            onClick = onToggleLang
        )

        val stepPx = with(LocalDensity.current) { 14.dp.toPx() }
        KeyButton(
            label = "مسافة",
            palette = palette,
            modifier = Modifier
                .weight(4.0f)
                .height(kh(44))
                .then(
                    if (service.spaceCursor) Modifier.pointerInput(stepPx) {
                        var acc = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { acc = 0f },
                            onDragCancel = { acc = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            acc += dragAmount
                            val steps = (acc / stepPx).toInt()
                            if (steps != 0) {
                                service.moveCursorSteps(steps)
                                acc -= steps * stepPx
                            }
                        }
                    } else Modifier
                )
        ) {
            service.commitText(" ")
        }

        KeyButton(
            label = "ـ",
            palette = palette,
            modifier = Modifier.weight(0.9f).height(kh(44)),
            isSpecial = true
        ) {
            service.commitText("ـ")
        }

        SpecialIconKey(
            icon = Icons.AutoMirrored.Filled.KeyboardReturn,
            palette = palette,
            isAccent = true,
            modifier = Modifier.weight(1.5f)
        ) {
            service.sendEnterOrAction()
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(LocalKeyScale.current.corner.dp),
        color = when {
            isAccent -> palette.accentColor
            isSpecial -> palette.specialKeySurface
            else -> palette.keySurface
        },
        border = keyBorder(palette)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isAccent) Color.Black else palette.keyText,
                fontSize = if (label.length > 2) ts(12) else ts(17),
                fontWeight = keyFontWeight(),
                fontFamily = LocalKeyScale.current.fontFamily,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpecialIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(kh(44)),
        shape = RoundedCornerShape(LocalKeyScale.current.corner.dp),
        color = when {
            isAccent -> palette.accentColor
            active -> palette.accentColor.copy(alpha = 0.25f)
            else -> palette.specialKeySurface
        },
        border = keyBorder(palette)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAccent) Color.Black else palette.keyText,
                modifier = Modifier.size(kh(20))
            )
        }
    }
}

/** حافة المفتاح: خط رفيع عادي أو حافة مضيئة بلون التمييز حسب إعداد الإضاءة. */
@Composable
private fun keyBorder(palette: KeyboardPalette): BorderStroke {
    val glow = LocalKeyScale.current.glow
    return if (glow > 0.01f) BorderStroke(1.dp + (1.5f * glow).dp, palette.accentColor.copy(alpha = 0.25f + 0.75f * glow))
    else BorderStroke(0.5.dp, palette.borderHairline)
}
