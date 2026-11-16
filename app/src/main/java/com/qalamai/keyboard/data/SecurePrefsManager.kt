package com.qalamai.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.qalamai.keyboard.ui.theme.KeyboardThemeId

class SecurePrefsManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "qalam_secure_prefs"
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_AUTO_PC_LANDSCAPE = "auto_pc_landscape"
        private const val KEY_CLIPBOARD_HISTORY = "clipboard_history_enabled"
        private const val KEY_HEIGHT_SCALE = "keyboard_height_scale"
        private const val KEY_TEXT_SCALE = "key_text_scale"
        private const val KEY_NUMBER_ROW = "number_row_visible"
        private const val KEY_ONE_HANDED = "one_handed_mode"
        private const val KEY_HIDDEN_TOOLBAR = "hidden_toolbar_items"

        private const val KEY_SOUND_ON = "key_sound_on"
        private const val KEY_SOUND_VOL = "key_sound_volume"
        private const val KEY_GLOW = "key_glow"
        private const val KEY_SPACE_CURSOR = "space_cursor"
        private const val KEY_SHOW_SUGGESTIONS = "show_suggestions"
        private const val KEY_AUTOCORRECT = "autocorrect"
        private const val KEY_AUTOSPACE = "auto_space_after_suggestion"
        private const val KEY_FONT = "font_family"
        private const val KEY_FONT_BOLD = "font_bold"
        private const val KEY_KEY_CORNER = "key_corner"
        private const val KEY_CUSTOM_ACCENT = "custom_accent"
        private const val KEY_CUSTOM_DARK = "custom_dark"
        private const val KEY_VOICE_LANG = "voice_lang"
        private const val KEY_PHRASES = "phrases_json"
        private const val KEY_DICTIONARY = "dictionary_json"

        const val SCALE_MIN = 0.8f
        const val SCALE_MAX = 1.6f
    }

    private val prefs: SharedPreferences = createPrefs()

    private fun createPrefs(): SharedPreferences {
        return try {
            openEncrypted()
        } catch (e: Exception) {
            // يحدث أحياناً بعد استعادة جهاز أو تغيّر مخزن المفاتيح (AEADBadTagException).
            // نحذف الملف التالف ونبدأ من جديد بدلاً من أن تتعطل لوحة المفاتيح.
            context.deleteSharedPreferences(PREFS_FILE)
            openEncrypted()
        }
    }

    private fun openEncrypted(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API, apiKey.trim()).apply()
    }

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API, "") ?: ""

    fun saveSelectedTheme(themeId: KeyboardThemeId) {
        prefs.edit().putString(KEY_THEME, themeId.name).apply()
    }

    fun getSelectedTheme(): KeyboardThemeId {
        val raw = prefs.getString(KEY_THEME, null) ?: return KeyboardThemeId.OBSIDIAN
        return runCatching { KeyboardThemeId.valueOf(raw) }
            .getOrDefault(KeyboardThemeId.OBSIDIAN)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    fun isHapticEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC, true)

    fun setAutoPcInLandscape(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PC_LANDSCAPE, enabled).apply()
    }

    fun isAutoPcInLandscape(): Boolean = prefs.getBoolean(KEY_AUTO_PC_LANDSCAPE, true)

    fun setClipboardHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIPBOARD_HISTORY, enabled).apply()
    }

    fun getHeightScale(): Float = prefs.getFloat(KEY_HEIGHT_SCALE, 1.0f).coerceIn(SCALE_MIN, SCALE_MAX)

    fun setHeightScale(v: Float) {
        prefs.edit().putFloat(KEY_HEIGHT_SCALE, v.coerceIn(SCALE_MIN, SCALE_MAX)).apply()
    }

    fun getTextScale(): Float = prefs.getFloat(KEY_TEXT_SCALE, 1.0f).coerceIn(SCALE_MIN, SCALE_MAX)

    fun setTextScale(v: Float) {
        prefs.edit().putFloat(KEY_TEXT_SCALE, v.coerceIn(SCALE_MIN, SCALE_MAX)).apply()
    }

    fun isNumberRowVisible(): Boolean = prefs.getBoolean(KEY_NUMBER_ROW, true)

    fun setNumberRowVisible(v: Boolean) {
        prefs.edit().putBoolean(KEY_NUMBER_ROW, v).apply()
    }

    /** OFF | LEFT | RIGHT */
    fun getOneHandedMode(): String = prefs.getString(KEY_ONE_HANDED, "OFF") ?: "OFF"

    fun setOneHandedMode(v: String) {
        prefs.edit().putString(KEY_ONE_HANDED, v).apply()
    }

    fun getHiddenToolbarItems(): Set<String> =
        (prefs.getString(KEY_HIDDEN_TOOLBAR, "") ?: "").split(",").filter { it.isNotBlank() }.toSet()

    fun setHiddenToolbarItems(items: Set<String>) {
        prefs.edit().putString(KEY_HIDDEN_TOOLBAR, items.joinToString(",")).apply()
    }

    fun isSoundOn(): Boolean = prefs.getBoolean(KEY_SOUND_ON, false)
    fun setSoundOn(v: Boolean) { prefs.edit().putBoolean(KEY_SOUND_ON, v).apply() }

    fun getSoundVolume(): Float = prefs.getFloat(KEY_SOUND_VOL, 0.5f).coerceIn(0f, 1f)
    fun setSoundVolume(v: Float) { prefs.edit().putFloat(KEY_SOUND_VOL, v.coerceIn(0f, 1f)).apply() }

    /** شدة إضاءة حواف المفاتيح 0..1 (0 = بلا إضاءة). */
    fun getGlow(): Float = prefs.getFloat(KEY_GLOW, 0f).coerceIn(0f, 1f)
    fun setGlow(v: Float) { prefs.edit().putFloat(KEY_GLOW, v.coerceIn(0f, 1f)).apply() }

    fun isSpaceCursorOn(): Boolean = prefs.getBoolean(KEY_SPACE_CURSOR, true)
    fun setSpaceCursorOn(v: Boolean) { prefs.edit().putBoolean(KEY_SPACE_CURSOR, v).apply() }

    fun isShowSuggestions(): Boolean = prefs.getBoolean(KEY_SHOW_SUGGESTIONS, true)
    fun setShowSuggestions(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_SUGGESTIONS, v).apply() }

    fun isAutoCorrect(): Boolean = prefs.getBoolean(KEY_AUTOCORRECT, false)
    fun setAutoCorrect(v: Boolean) { prefs.edit().putBoolean(KEY_AUTOCORRECT, v).apply() }

    fun isAutoSpace(): Boolean = prefs.getBoolean(KEY_AUTOSPACE, true)
    fun setAutoSpace(v: Boolean) { prefs.edit().putBoolean(KEY_AUTOSPACE, v).apply() }

    /** DEFAULT | SERIF | MONO | CURSIVE */
    fun getFont(): String = prefs.getString(KEY_FONT, "DEFAULT") ?: "DEFAULT"
    fun setFont(v: String) { prefs.edit().putString(KEY_FONT, v).apply() }

    fun isFontBold(): Boolean = prefs.getBoolean(KEY_FONT_BOLD, false)
    fun setFontBold(v: Boolean) { prefs.edit().putBoolean(KEY_FONT_BOLD, v).apply() }

    /** نصف قطر زوايا المفاتيح بالـ dp (4 مربع، 7 عادي، 14 دائري). */
    fun getKeyCorner(): Int = prefs.getInt(KEY_KEY_CORNER, 7)
    fun setKeyCorner(v: Int) { prefs.edit().putInt(KEY_KEY_CORNER, v).apply() }

    fun getCustomAccent(): Int = prefs.getInt(KEY_CUSTOM_ACCENT, 0xFF14B8A6.toInt())
    fun setCustomAccent(v: Int) { prefs.edit().putInt(KEY_CUSTOM_ACCENT, v).apply() }

    fun isCustomDark(): Boolean = prefs.getBoolean(KEY_CUSTOM_DARK, true)
    fun setCustomDark(v: Boolean) { prefs.edit().putBoolean(KEY_CUSTOM_DARK, v).apply() }

    fun getVoiceLang(): String = prefs.getString(KEY_VOICE_LANG, "ar-EG") ?: "ar-EG"
    fun setVoiceLang(v: String) { prefs.edit().putString(KEY_VOICE_LANG, v).apply() }

    private val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type

    private fun readList(key: String): List<String> = runCatching {
        com.google.gson.Gson().fromJson<List<String>>(prefs.getString(key, "[]"), listType)
    }.getOrNull().orEmpty()

    private fun writeList(key: String, items: List<String>) {
        prefs.edit().putString(key, com.google.gson.Gson().toJson(items)).apply()
    }

    fun getPhrases(): List<String> = readList(KEY_PHRASES)
    fun setPhrases(items: List<String>) = writeList(KEY_PHRASES, items)

    fun getDictionary(): List<String> = readList(KEY_DICTIONARY)
    fun setDictionary(items: List<String>) = writeList(KEY_DICTIONARY, items)

    fun isClipboardHistoryEnabled(): Boolean = prefs.getBoolean(KEY_CLIPBOARD_HISTORY, true)
}
