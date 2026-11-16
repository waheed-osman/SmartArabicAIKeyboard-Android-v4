package com.qalamai.keyboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalamai.keyboard.data.SecurePrefsManager
import com.qalamai.keyboard.ime.ActiveToolbarPanel
import com.qalamai.keyboard.ime.OneHandedMode
import com.qalamai.keyboard.ime.SmartKeyboardService
import com.qalamai.keyboard.ui.theme.KeyboardPalette
import kotlin.math.roundToInt

/** معرّفات أزرار الشريط العلوي القابلة للإخفاء. */
object ToolbarIds {
    const val AI = "ai"
    const val CLIPBOARD = "clipboard"
    const val CURSOR = "cursor"
    const val TASHKEEL = "tashkeel"
    const val EMOJI = "emoji"
    const val THEME = "theme"
    const val CALC = "calc"
    const val VOICE = "voice"
    const val PHRASES = "phrases"
    const val TRANSLATE = "translate"

    val all = listOf(
        AI to "الذكاء الاصطناعي",
        CLIPBOARD to "الحافظة",
        CURSOR to "المؤشر / PC",
        TASHKEEL to "التشكيل",
        EMOJI to "الإيموجي",
        THEME to "الثيم",
        CALC to "الحاسبة",
        VOICE to "الإملاء الصوتي",
        PHRASES to "الجمل المستخدمة",
        TRANSLATE to "ترجمة سريعة"
    )
}

private enum class SettingsPage { GRID, SIZE, ONE_HAND, TOOLBAR, SOUND, LIGHT, CORRECTION, FONT, DESIGN, VOICE }

@Composable
fun KeyboardSettingsPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    var page by remember { mutableStateOf(SettingsPage.GRID) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(palette.toolbarSurface)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (page) {
            SettingsPage.GRID -> SettingsGrid(service, palette) { page = it }
            SettingsPage.SIZE -> SizePage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.ONE_HAND -> OneHandPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.TOOLBAR -> ToolbarPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.SOUND -> SoundPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.LIGHT -> LightPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.CORRECTION -> CorrectionPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.FONT -> FontPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.DESIGN -> DesignPage(service, palette) { page = SettingsPage.GRID }
            SettingsPage.VOICE -> VoicePage(service, palette) { page = SettingsPage.GRID }
        }
    }
}

@Composable
private fun SettingsGrid(
    service: SmartKeyboardService,
    palette: KeyboardPalette,
    go: (SettingsPage) -> Unit
) {
    var hapticOn by remember { mutableStateOf(service.isHapticOn()) }

    val tiles: List<Triple<String, ImageVector, Pair<Boolean, () -> Unit>>> = listOf(
        Triple("حاسبة", Icons.Default.Calculate, false to { service.activePanel = ActiveToolbarPanel.CALC }),
        Triple("كيبورد متحرك", Icons.Default.PictureInPicture, service.floating to { service.toggleFloating() }),
        Triple("اللغات ولوحة المفاتيح", Icons.Default.Language, false to { service.showInputMethodPicker() }),
        Triple("صوت لوحة المفاتيح", Icons.Default.MusicNote, service.soundOn to { go(SettingsPage.SOUND) }),
        Triple("إضاءة لوحة المفاتيح", Icons.Default.Lightbulb, (service.glow > 0f) to { go(SettingsPage.LIGHT) }),
        Triple("حجم لوحة المفاتيح", Icons.Default.OpenInFull, false to { go(SettingsPage.SIZE) }),
        Triple("صف الأرقام العلوي", Icons.Default.Dialpad, service.showNumberRow to {
            service.setNumberRow(!service.showNumberRow)
        }),
        Triple("مؤشر الكتابة (سحب المسافة)", Icons.Default.SpaceBar, service.spaceCursor to {
            service.updateSpaceCursor(!service.spaceCursor)
        }),
        Triple("تخصيص شريط الأدوات", Icons.Default.Tune, false to { go(SettingsPage.TOOLBAR) }),
        Triple("طريقة اليد الواحدة", Icons.Default.TouchApp, (service.oneHandedMode != OneHandedMode.OFF) to {
            go(SettingsPage.ONE_HAND)
        }),
        Triple("التصحيح والترشيح", Icons.Default.Spellcheck, service.autoCorrect to { go(SettingsPage.CORRECTION) }),
        Triple("إعدادات الصوت (إملاء)", Icons.Default.Mic, false to { go(SettingsPage.VOICE) }),
        Triple("تصميم شخصي", Icons.Default.Brush, (service.currentTheme == com.qalamai.keyboard.ui.theme.KeyboardThemeId.CUSTOM) to {
            go(SettingsPage.DESIGN)
        }),
        Triple("الخط", Icons.Default.FormatSize, false to { go(SettingsPage.FONT) }),
        Triple("الجمل المستخدمة", Icons.Default.ChatBubble, false to { service.activePanel = ActiveToolbarPanel.PHRASES }),
        Triple("قاموسي", Icons.Default.MenuBook, false to { service.activePanel = ActiveToolbarPanel.DICTIONARY }),
        Triple("انسخ للترجمة", Icons.Default.Translate, false to { service.translateLastCopied() }),
        Triple("الأشكال (الثيمات)", Icons.Default.Palette, false to { service.activePanel = ActiveToolbarPanel.THEMES }),
        Triple("الملصقات والإيموجي", Icons.Default.Mood, false to { service.activePanel = ActiveToolbarPanel.EMOJI }),
        Triple("وضع الحاسوب الكامل", Icons.Default.Keyboard,
            (service.layoutMode == com.qalamai.keyboard.ime.KeyboardLayoutMode.FULL_PC) to {
                service.togglePcMode()
            }),
        Triple("الاهتزاز اللمسي", Icons.Default.Vibration, hapticOn to {
            hapticOn = service.toggleHaptic()
        }),
        Triple("استوديو الإعلانات", Icons.Default.AutoAwesome, false to { service.openAdStudio() }),
        Triple("تقرير الأخطاء", Icons.Default.BugReport, false to { service.sendBugReport() }),
        Triple("الأسئلة الشائعة", Icons.Default.Help, false to { service.openSettingsScreen() }),
        Triple("كل الإعدادات", Icons.Default.Settings, false to { service.openSettingsScreen() })
    )

    tiles.chunked(4).forEach { rowTiles ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowTiles.forEach { (label, icon, state) ->
                SettingsTile(label, icon, state.first, palette, Modifier.weight(1f), state.second)
            }
            repeat(4 - rowTiles.size) { Box(modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SettingsTile(
    label: String,
    icon: ImageVector,
    active: Boolean,
    palette: KeyboardPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) palette.accentColor.copy(alpha = 0.25f) else palette.keySurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) palette.accentColor else palette.keyText,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            color = palette.keyText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun PageHeader(title: String, palette: KeyboardPalette, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = palette.keyText,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            color = palette.keyText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun SizePage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("حجم اللوحة والأحرف", palette, onBack)

    ScaleSlider("ارتفاع اللوحة (المفاتيح)", service.heightScale, palette,
        onChange = { service.previewScales(it, service.textScale) },
        onFinished = { service.persistScales() })

    ScaleSlider("حجم الأحرف", service.textScale, palette,
        onChange = { service.previewScales(service.heightScale, it) },
        onFinished = { service.persistScales() })

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "صغير" to (0.9f to 0.9f),
            "عادي" to (1.0f to 1.0f),
            "كبير" to (1.2f to 1.25f),
            "كبير جداً" to (1.4f to 1.5f)
        ).forEach { (name, v) ->
            PresetChip(name, palette, Modifier.weight(1f)) { service.applyScalePreset(v.first, v.second) }
        }
    }
}

@Composable
private fun PresetChip(label: String, palette: KeyboardPalette, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.keySurface)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = palette.keyText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScaleSlider(
    label: String,
    value: Float,
    palette: KeyboardPalette,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.keyText, fontSize = 12.sp)
            Text("${(value * 100).roundToInt()}%", color = palette.accentColor, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            valueRange = SecurePrefsManager.SCALE_MIN..SecurePrefsManager.SCALE_MAX,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentColor,
                activeTrackColor = palette.accentColor,
                inactiveTrackColor = palette.borderHairline
            )
        )
    }
}

@Composable
private fun OneHandPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("طريقة اليد الواحدة", palette, onBack)
    Text(
        "تُصغَّر اللوحة عرضاً وتُلصق بجهة اليد التي تكتب بها.",
        color = palette.mutedText,
        fontSize = 11.sp
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            OneHandedMode.OFF to "إيقاف",
            OneHandedMode.RIGHT to "اليد اليمنى",
            OneHandedMode.LEFT to "اليد اليسرى"
        ).forEach { (mode, name) ->
            FilterChip(
                selected = service.oneHandedMode == mode,
                onClick = { service.setOneHanded(mode) },
                label = { Text(name, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun ToolbarPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("تخصيص شريط الأدوات", palette, onBack)
    ToolbarIds.all.forEach { (id, name) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = palette.keyText, fontSize = 13.sp)
            Switch(
                checked = id !in service.hiddenToolbar,
                onCheckedChange = { service.toggleToolbarItem(id) }
            )
        }
    }
}

@Composable
private fun SoundPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("صوت لوحة المفاتيح", palette, onBack)
    SwitchLine("صوت عند الضغط على المفاتيح", service.soundOn, palette) { service.updateSound(it) }
    ScaleLine("مستوى الصوت", service.soundVolume, palette, 0f..1f,
        onChange = { service.previewSoundVolume(it) }, onFinished = { service.persistSoundVolume() })
    Text(
        "ملاحظة: يتبع صوت اللمس في النظام، فتأكد من تفعيل «أصوات اللمس» في إعدادات الصوت بهاتفك.",
        color = palette.mutedText, fontSize = 11.sp
    )
}

@Composable
private fun LightPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("إضاءة لوحة المفاتيح", palette, onBack)
    ScaleLine("شدة إضاءة حواف المفاتيح", service.glow, palette, 0f..1f,
        onChange = { service.previewGlow(it) }, onFinished = { service.persistGlow() })
    Text("اللون يتبع لون الثيم المحدد. 0% = بدون إضاءة.", color = palette.mutedText, fontSize = 11.sp)
}

@Composable
private fun CorrectionPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("التصحيح والترشيح", palette, onBack)
    SwitchLine("إظهار شريط الاقتراحات", service.showSuggestions, palette) { service.updateShowSuggestions(it) }
    SwitchLine("إضافة مسافة بعد اختيار اقتراح", service.autoSpace, palette) { service.updateAutoSpace(it) }
    SwitchLine("تصحيح تلقائي للأخطاء الشائعة (شكرا ← شكراً، الى ← إلى…)", service.autoCorrect, palette) {
        service.updateAutoCorrect(it)
    }
    Text("الاقتراحات تأتي من قاموسك الشخصي وعبارات شائعة، محلياً دون اتصال.", color = palette.mutedText, fontSize = 11.sp)
}

@Composable
private fun FontPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("الخط", palette, onBack)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("DEFAULT" to "الافتراضي", "SERIF" to "Serif", "MONO" to "Mono", "CURSIVE" to "Cursive").forEach { (id, name) ->
            FilterChip(
                selected = service.fontName == id,
                onClick = { service.updateFont(id) },
                label = { Text(name, fontSize = 12.sp) }
            )
        }
    }
    SwitchLine("خط عريض", service.fontBold, palette) { service.updateFontBold(it) }
    Text("شكل الحروف العربية يعتمد على الخطوط المثبتة في هاتفك.", color = palette.mutedText, fontSize = 11.sp)
}

private val SWATCHES = listOf(
    0xFF14B8A6, 0xFF38BDF8, 0xFF6366F1, 0xFFA855F7, 0xFFEC4899,
    0xFFEF4444, 0xFFF97316, 0xFFD97706, 0xFF22C55E, 0xFF94A3B8
)

@Composable
private fun DesignPage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("تصميم شخصي", palette, onBack)
    Text("لون التمييز (يُفعّل الثيم الشخصي)", color = palette.keyText, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SWATCHES.forEach { c ->
            val selected = service.customAccent == c.toInt()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color(c))
                    .then(
                        if (selected) Modifier.border(2.dp, palette.keyText, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { service.updateCustomAccent(c.toInt()) }
            )
        }
    }
    SwitchLine("الوضع الليلي", service.customDark, palette) { service.updateCustomDark(it) }
    Text("شكل المفاتيح", color = palette.keyText, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(4 to "مربع", 7 to "عادي", 14 to "دائري").forEach { (r, name) ->
            FilterChip(
                selected = service.keyCorner == r,
                onClick = { service.updateKeyCorner(r) },
                label = { Text(name, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun VoicePage(service: SmartKeyboardService, palette: KeyboardPalette, onBack: () -> Unit) {
    PageHeader("إعدادات الصوت (الإملاء)", palette, onBack)
    Text("لغة الإملاء الصوتي (زر 🎤 في الشريط):", color = palette.keyText, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("ar-EG" to "عربي (مصر)", "ar-SA" to "عربي (السعودية)", "en-US" to "English").forEach { (code, name) ->
            FilterChip(
                selected = service.voiceLang == code,
                onClick = { service.updateVoiceLang(code) },
                label = { Text(name, fontSize = 12.sp) }
            )
        }
    }
    Text(
        "يستخدم خدمة التعرف على الكلام في هاتفك (Google)، وقد يُرسَل الصوت إليها وفق إعداداتها. معطّل في حقول كلمات المرور.",
        color = palette.mutedText, fontSize = 11.sp
    )
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, palette: KeyboardPalette, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.keyText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ScaleLine(
    label: String,
    value: Float,
    palette: KeyboardPalette,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.keyText, fontSize = 12.sp)
            Text("${(value * 100).roundToInt()}%", color = palette.accentColor, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentColor,
                activeTrackColor = palette.accentColor,
                inactiveTrackColor = palette.borderHairline
            )
        )
    }
}
