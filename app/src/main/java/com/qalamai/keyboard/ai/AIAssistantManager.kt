package com.qalamai.keyboard.ai

import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.qalamai.keyboard.data.SecurePrefsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class TonePreset(val labelAr: String, val directive: String) {
    FORMAL_LEGAL(
        "رسمي / قانوني",
        "أعد صياغة النص التالي بصيغة رسمية قانونية رصينة ومهنية خالية من العامية."
    ),
    FRIENDLY(
        "ودود ولطيف",
        "أعد صياغة النص التالي بأسلوب ودود دافئ وقريب من القارئ."
    ),
    SHORTEN(
        "اختصار النص",
        "اختصر النص التالي إلى أقل عدد ممكن من الكلمات مع الحفاظ التام على المعنى الأساسي."
    ),
    EXPAND(
        "توسيع وإثراء",
        "وسّع النص التالي وأضف صياغة احترافية غنية بالتفاصيل المنطقية المترابطة."
    )
}

/**
 * يدير الاتصال بـ Gemini API باستخدام Retrofit.
 * قاعدة الخصوصية: لا يتم استدعاء أي دالة هنا تلقائياً أثناء الكتابة؛
 * يتم الإرسال فقط عند ضغط المستخدم يدوياً على زر الذكاء الاصطناعي.
 */
class AIAssistantManager(
    private val prefsManager: SecurePrefsManager
) {
    companion object {
        private const val DEFAULT_MODEL = GeminiClient.TEXT_MODEL

        /** حد أقصى لطول النص المرسل لحماية الاستهلاك والخصوصية. */
        const val MAX_INPUT_CHARS = 6000

        private const val MSG_NO_KEY = "يرجى إضافة مفتاح Gemini API أولاً من شاشة الإعدادات."
    }

    private val gson = GeminiClient.gson

    suspend fun proofread(text: String): Result<String> = executePrompt(
        systemPrompt = "أنت مدقق لغوي وإملائي ونحوي خبير باللغتين العربية والإنجليزية. " +
            "صحح الأخطاء الإملائية والنحوية وعلامات الترقيم بدقة مع الحفاظ على معنى النص. " +
            "أعد النص المصحح فقط دون أي شرح أو علامات اقتباس.",
        userText = text
    )

    suspend fun rewriteTone(text: String, preset: TonePreset): Result<String> = executePrompt(
        systemPrompt = "${preset.directive} حافظ على لغة النص الأصلية وأعد النص الجديد فقط دون مقدمات.",
        userText = text
    )

    suspend fun translate(text: String, targetLanguage: String): Result<String> = executePrompt(
        systemPrompt = "You are a real-time keyboard translator. Translate the text into $targetLanguage " +
            "naturally and accurately. Return ONLY the translated text.",
        userText = text
    )

    suspend fun addTashkeel(text: String): Result<String> = executePrompt(
        systemPrompt = "اضبط النص العربي التالي بالتشكيل الصحيح الكامل (الحركات والشدة والتنوين) " +
            "وفق قواعد النحو والصرف. أعد النص المشكول فقط.",
        userText = text
    )

    suspend fun autocomplete(contextText: String): Result<List<String>> {
        val raw = executePrompt(
            systemPrompt = "أنت محرك إكمال جمل ذكي. أرجع مصفوفة JSON تحتوي على 3 اقتراحات مختصرة " +
                "تكمل النص المرسل مباشرة دون تكرار النص الأصلي. " +
                "الصيغة: [\"اقتراح 1\", \"اقتراح 2\", \"اقتراح 3\"]",
            userText = contextText,
            temperature = 0.4,
            json = true
        )
        return raw.mapCatching { parseSuggestions(it) }
            .recoverCatching { err ->
                if (err is CancellationException) throw err
                throw IllegalStateException(
                    if (err is JsonParseException) "تعذر فهم اقتراحات الإكمال، حاول مرة أخرى."
                    else err.message ?: "تعذر الحصول على اقتراحات."
                )
            }
    }

    private fun parseSuggestions(raw: String): List<String> {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val listType = object : TypeToken<List<String>>() {}.type
        val parsed: List<String> = gson.fromJson(cleaned, listType) ?: emptyList()
        return parsed.map { it.trim() }.filter { it.isNotEmpty() }.take(3)
    }

    private suspend fun executePrompt(
        systemPrompt: String,
        userText: String,
        temperature: Double = 0.3,
        json: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(MSG_NO_KEY))
        }
        if (userText.length > MAX_INPUT_CHARS) {
            return@withContext Result.failure(
                IllegalStateException("النص طويل جداً (الحد الأقصى $MAX_INPUT_CHARS حرف). حدّد جزءاً أقصر.")
            )
        }

        try {
            val request = GeminiRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(userText)))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = temperature,
                    responseMimeType = if (json) "application/json" else null
                )
            )
            val response = GeminiClient.api.generateContent(DEFAULT_MODEL, apiKey, request)
            val output = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.trim()

            if (output.isNullOrEmpty()) {
                Result.failure(IllegalStateException("لم يُرجع Gemini استجابة صالحة (قد يكون النص محجوباً)."))
            } else {
                Result.success(output)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(GeminiClient.friendlyError(e)))
        }
    }
}
