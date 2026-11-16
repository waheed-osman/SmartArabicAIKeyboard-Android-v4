package com.qalamai.keyboard.ui.theme

import androidx.compose.ui.graphics.Color

enum class KeyboardThemeId(val titleAr: String) {
    OBSIDIAN("الليلي العصري"),
    ANDALUSIAN("الأندلسي الذهبي"),
    CLOUD_LIGHT("السحابي الفاتح"),
    MECHANICAL_PC("حاسوب ميكانيكي"),
    CUSTOM("تصميم شخصي")
}

data class KeyboardPalette(
    val id: KeyboardThemeId,
    val background: Color,
    val toolbarSurface: Color,
    val keySurface: Color,
    val specialKeySurface: Color,
    val accentColor: Color,
    val keyText: Color,
    val mutedText: Color,
    val borderHairline: Color
)

fun paletteFor(
    id: KeyboardThemeId,
    customAccent: Int = 0xFF14B8A6.toInt(),
    customDark: Boolean = true
): KeyboardPalette = when (id) {
    KeyboardThemeId.CUSTOM -> if (customDark) KeyboardPalette(
        id = id,
        background = Color(0xFF101114),
        toolbarSurface = Color(0xFF1A1C20),
        keySurface = Color(0xFF26292F),
        specialKeySurface = Color(0xFF1C1F24),
        accentColor = Color(customAccent),
        keyText = Color(0xFFF8FAFC),
        mutedText = Color(0xFF9AA3AF),
        borderHairline = Color(0xFF30343B)
    ) else KeyboardPalette(
        id = id,
        background = Color(0xFFEFF3F8),
        toolbarSurface = Color(0xFFFFFFFF),
        keySurface = Color(0xFFFFFFFF),
        specialKeySurface = Color(0xFFDCE3ED),
        accentColor = Color(customAccent),
        keyText = Color(0xFF0F172A),
        mutedText = Color(0xFF475569),
        borderHairline = Color(0xFFCBD5E1)
    )
    KeyboardThemeId.OBSIDIAN -> KeyboardPalette(
        id = id,
        background = Color(0xFF0B0F17),
        toolbarSurface = Color(0xFF111827),
        keySurface = Color(0xFF1E293B),
        specialKeySurface = Color(0xFF151E2E),
        accentColor = Color(0xFF14B8A6),
        keyText = Color(0xFFF8FAFC),
        mutedText = Color(0xFF94A3B8),
        borderHairline = Color(0xFF283548)
    )
    KeyboardThemeId.ANDALUSIAN -> KeyboardPalette(
        id = id,
        background = Color(0xFF14110E),
        toolbarSurface = Color(0xFF1F1A14),
        keySurface = Color(0xFF2B231B),
        specialKeySurface = Color(0xFF211B14),
        accentColor = Color(0xFFD97706),
        keyText = Color(0xFFFEF3C7),
        mutedText = Color(0xFFD6B98D),
        borderHairline = Color(0xFF3E3224)
    )
    KeyboardThemeId.CLOUD_LIGHT -> KeyboardPalette(
        id = id,
        background = Color(0xFFEFF3F8),
        toolbarSurface = Color(0xFFFFFFFF),
        keySurface = Color(0xFFFFFFFF),
        specialKeySurface = Color(0xFFDCE3ED),
        accentColor = Color(0xFF0F766E),
        keyText = Color(0xFF0F172A),
        mutedText = Color(0xFF475569),
        borderHairline = Color(0xFFCBD5E1)
    )
    KeyboardThemeId.MECHANICAL_PC -> KeyboardPalette(
        id = id,
        background = Color(0xFF18181B),
        toolbarSurface = Color(0xFF27272A),
        keySurface = Color(0xFF323238),
        specialKeySurface = Color(0xFF222226),
        accentColor = Color(0xFF38BDF8),
        keyText = Color(0xFFFAFAFA),
        mutedText = Color(0xFFA1A1AA),
        borderHairline = Color(0xFF3F3F46)
    )
}
