package com.qalamai.keyboard.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

enum class ClipboardCategory(val labelAr: String) {
    ALL("الكل"),
    PINNED("المثبتة"),
    LINKS("روابط"),
    SNIPPETS("أكواد وعبارات"),
    GENERAL("عام")
}

data class ClipboardEntry(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isPinned: Boolean = false,
    val category: ClipboardCategory = ClipboardCategory.GENERAL,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * سجل الحافظة محفوظ مشفراً (AES-256-GCM عبر EncryptedFile) داخل مساحة التطبيق الخاصة.
 */
class ClipboardRepository(private val context: Context) {

    companion object {
        private const val FILE_NAME = "clipboard_history.json"
        private const val MAX_UNPINNED_ITEMS = 50
        private const val MAX_CLIP_LENGTH = 2000
    }

    private val gson = Gson()
    private val storageFile = File(context.filesDir, FILE_NAME)
    private val entries = mutableListOf<ClipboardEntry>()

    init {
        loadFromDisk()
    }

    @Synchronized
    fun addClip(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || text.length > MAX_CLIP_LENGTH) return

        val existingIndex = entries.indexOfFirst { it.text == text }
        if (existingIndex >= 0) {
            val existing = entries.removeAt(existingIndex)
            entries.add(0, existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            val category = detectCategory(text)
            entries.add(0, ClipboardEntry(text = text, category = category))
        }

        trimUnpinnedLimit()
        saveToDisk()
    }

    @Synchronized
    fun togglePin(id: String) {
        val idx = entries.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val current = entries[idx]
            entries[idx] = current.copy(isPinned = !current.isPinned)
            saveToDisk()
        }
    }

    @Synchronized
    fun deleteClip(id: String) {
        entries.removeAll { it.id == id }
        saveToDisk()
    }

    @Synchronized
    fun getClips(category: ClipboardCategory = ClipboardCategory.ALL): List<ClipboardEntry> {
        val sorted = entries.sortedWith(
            compareByDescending<ClipboardEntry> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
        return when (category) {
            ClipboardCategory.ALL -> sorted
            ClipboardCategory.PINNED -> sorted.filter { it.isPinned }
            else -> sorted.filter { it.category == category }
        }
    }

    private fun detectCategory(text: String): ClipboardCategory {
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> ClipboardCategory.LINKS
            text.contains("{") || text.contains("fun ") || text.contains("@") || text.length > 80 ->
                ClipboardCategory.SNIPPETS
            else -> ClipboardCategory.GENERAL
        }
    }

    private fun trimUnpinnedLimit() {
        val pinned = entries.filter { it.isPinned }
        val unpinned = entries.filter { !it.isPinned }.take(MAX_UNPINNED_ITEMS)
        entries.clear()
        entries.addAll(pinned)
        entries.addAll(unpinned)
    }

    /** مسح كل السجل بما فيه العناصر المثبتة. */
    @Synchronized
    fun clearAll() {
        entries.clear()
        storageFile.delete()
    }

    /** إعادة القراءة من القرص (مثلاً بعد مسح السجل من شاشة الإعدادات). */
    @Synchronized
    fun reload() {
        entries.clear()
        loadFromDisk()
    }

    private fun encryptedFile(): EncryptedFile {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedFile.Builder(
            context,
            storageFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    private fun loadFromDisk() {
        if (!storageFile.exists()) return
        try {
            val json = encryptedFile().openFileInput().use { it.readBytes().toString(Charsets.UTF_8) }
            val listType = object : TypeToken<List<ClipboardEntry>>() {}.type
            val loaded: List<ClipboardEntry>? = gson.fromJson(json, listType)
            if (loaded != null) {
                entries.clear()
                entries.addAll(loaded)
            }
        } catch (e: Exception) {
            // ملف تالف أو غير قابل لفك التشفير: نتجاهله ونبدأ بسجل فارغ.
            storageFile.delete()
        }
    }

    private fun saveToDisk() {
        try {
            // EncryptedFile لا يكتب فوق ملف موجود، لذا نحذفه أولاً.
            if (storageFile.exists()) storageFile.delete()
            encryptedFile().openFileOutput().use { out ->
                out.write(gson.toJson(entries).toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            // فشل الحفظ لا يجب أن يوقف الكتابة.
        }
    }
}
