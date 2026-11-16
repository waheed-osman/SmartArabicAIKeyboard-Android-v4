package com.qalamai.keyboard.ai

import android.util.Base64
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.qalamai.keyboard.data.SecurePrefsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** فكرة إعلانية يختارها المستخدم ويعدّلها. */
data class AdIdea(val title: String, val concept: String)

/** صورة إعلانية مولَّدة. */
class GeneratedImage(val bytes: ByteArray, val mimeType: String)

/** نموذج وسيط لتحليل JSON القادم من Gemini (حقول قابلة للفراغ عمداً). */
private data class IdeaDto(val title: String? = null, val concept: String? = null)

/**
 * استوديو الإعلانات: كل دالة هنا تُستدعى فقط بضغطة صريحة من المستخدم في مرحلة محددة،
 * وكل مخرجاتها نصوص قابلة للتعديل قبل اعتمادها والانتقال للمرحلة التالية.
 */
class AdStudioManager(private val prefs: SecurePrefsManager) {

    data class Brief(
        val name: String,
        val notes: String,
        val audience: String,
        val tone: String,
        val analysis: String
    ) {
        fun asText(): String = buildString {
            appendLine("اسم المنتج: $name")
            if (notes.isNotBlank()) appendLine("ملاحظات المعلن: $notes")
            if (audience.isNotBlank()) appendLine("الجمهور المستهدف: $audience")
            if (tone.isNotBlank()) appendLine("نبرة الإعلان المطلوبة: $tone")
            if (analysis.isNotBlank()) {
                appendLine("تحليل المنتج المعتمد:")
                appendLine(analysis)
            }
        }
    }

    companion object {
        private const val MSG_NO_KEY = "يرجى إضافة مفتاح Gemini API أولاً من شاشة الإعدادات."

        private const val HONESTY_RULE =
            "قاعدة صارمة: لا تخترع أسعاراً أو خصومات أو شهادات أو ادعاءات طبية/علمية أو ميزات " +
                "لم يذكرها المعلن أو لا تظهر بوضوح في الصورة. إن احتجت معلومة ناقصة فاكتب [يُستكمل] مكانها."
    }

    private fun withFeedback(base: String, feedback: String): String =
        if (feedback.isBlank()) base
        else "$base\n\nملاحظات المعلن للتعديل (التزم بها حرفياً): ${feedback.trim()}"

    // ------------------------------------------------------------------ المرحلة 2: التحليل

    suspend fun analyzeProduct(
        image: ByteArray,
        mimeType: String,
        brief: Brief,
        feedback: String
    ): Result<String> {
        val prompt = withFeedback(
            "حلّل صورة المنتج المرفقة لصالح معلن عربي. اكتب بالعربية وبإيجاز نقاطاً واضحة تشمل: " +
                "نوع المنتج، أهم ما يظهر من مواصفاته وتصميمه وألوانه، نقاط البيع المحتملة، " +
                "الجمهور المناسب، والمشاعر التي يثيرها الشكل.\n\n$HONESTY_RULE\n\n${brief.asText()}",
            feedback
        )
        val parts = listOf(
            GeminiPart(inlineData = GeminiInlineData(mimeType, Base64.encodeToString(image, Base64.NO_WRAP))),
            GeminiPart(text = prompt)
        )
        return textCall(system = "أنت خبير تسويق وإعلان بصري.", parts = parts, temperature = 0.4)
    }

    // ------------------------------------------------------------------ المرحلة 3: الأفكار

    suspend fun generateIdeas(brief: Brief, feedback: String): Result<List<AdIdea>> {
        val prompt = withFeedback(
            "اقترح 3 أفكار إعلانية مختلفة جذرياً (زاوية مختلفة لكل فكرة) للمنتج التالي.\n" +
                "أعد مصفوفة JSON فقط من 3 كائنات، لكل كائن الحقلان title (عنوان قصير) و concept " +
                "(شرح الفكرة والمشهد والشعور في 2-4 جمل).\n$HONESTY_RULE\n\n${brief.asText()}",
            feedback
        )
        return textCall(
            system = "أنت مدير إبداعي في وكالة إعلانات عربية.",
            parts = listOf(GeminiPart(text = prompt)),
            temperature = 0.9,
            json = true
        ).mapCatching { raw ->
            val cleaned = stripFences(raw)
            val type = object : TypeToken<List<IdeaDto>>() {}.type
            val parsed: List<IdeaDto> = GeminiClient.gson.fromJson(cleaned, type) ?: emptyList()
            val ideas = parsed
                .map { AdIdea(it.title.orEmpty().trim(), it.concept.orEmpty().trim()) }
                .filter { it.concept.isNotEmpty() }
                .take(3)
            if (ideas.isEmpty()) throw IllegalStateException("لم أستطع توليد أفكار صالحة، حاول مرة أخرى.")
            ideas
        }.recoverCatching { err ->
            if (err is CancellationException) throw err
            throw IllegalStateException(
                if (err is JsonParseException) "تعذر فهم رد الذكاء الاصطناعي، أعد المحاولة."
                else err.message ?: "تعذر توليد الأفكار."
            )
        }
    }

    // ------------------------------------------------------------------ المرحلة 4: النص

    suspend fun writeCopy(brief: Brief, idea: AdIdea, feedback: String): Result<String> {
        val prompt = withFeedback(
            "اكتب نص الإعلان النهائي بالعربية وفق الفكرة المعتمدة. الصيغة (بدون مقدمات):\n" +
                "العنوان الرئيسي:\nعنوان بديل:\nالنص الأساسي: (3-5 أسطر)\nنقاط مختصرة: (3 نقاط)\n" +
                "الدعوة لاتخاذ إجراء:\n\n$HONESTY_RULE\n\n${brief.asText()}\n" +
                "الفكرة المعتمدة: ${idea.title}\n${idea.concept}",
            feedback
        )
        return textCall(
            system = "أنت كاتب إعلانات عربي محترف ودقيق اللغة.",
            parts = listOf(GeminiPart(text = prompt)),
            temperature = 0.7
        )
    }

    // ------------------------------------------------------------------ المرحلة 5: التصميم

    suspend fun draftVisualPrompt(
        brief: Brief,
        idea: AdIdea,
        copy: String,
        feedback: String
    ): Result<String> {
        val prompt = withFeedback(
            "اكتب وصفاً بالإنجليزية (من 4 إلى 7 جمل) لصورة إعلانية فوتوغرافية احترافية لنموذج توليد صور، " +
                "تعتمد على المنتج في الصورة المرجعية وعلى الفكرة التالية. حدّد: المشهد والخلفية، الإضاءة، " +
                "زاوية الكاميرا، الألوان، والمزاج. شروط: حافظ على شكل المنتج وألوانه وشعاره كما هي دون تغيير، " +
                "ولا تضع أي نص أو حروف داخل الصورة (النص يُضاف لاحقاً). أعد الوصف فقط.\n\n" +
                "${brief.asText()}\nالفكرة: ${idea.title}\n${idea.concept}\nنص الإعلان:\n$copy",
            feedback
        )
        return textCall(
            system = "أنت مخرج فني ومصوّر إعلانات.",
            parts = listOf(GeminiPart(text = prompt)),
            temperature = 0.7
        )
    }

    suspend fun generateImage(
        productImage: ByteArray,
        mimeType: String,
        visualPrompt: String,
        feedback: String
    ): Result<GeneratedImage> {
        val prompt = withFeedback(
            "Create a professional advertising photo using the attached product photo as the exact reference. " +
                "Keep the product's shape, colors, branding and details unchanged. " +
                "Do not render any text, letters or watermarks in the image.\n\n$visualPrompt",
            feedback
        )
        val parts = listOf(
            GeminiPart(inlineData = GeminiInlineData(mimeType, Base64.encodeToString(productImage, Base64.NO_WRAP))),
            GeminiPart(text = prompt)
        )
        // نموذج الصور لا يدعم دائماً system instruction، لذا كل التعليمات داخل نص الطلب.
        val response = call(
            model = GeminiClient.IMAGE_MODEL,
            system = null,
            parts = parts,
            temperature = 0.8,
            modalities = listOf("TEXT", "IMAGE")
        )
        return response.mapCatching { resp ->
            val responseParts = resp.candidates?.firstOrNull()?.content?.parts.orEmpty()
            val img = responseParts.firstOrNull { it.inlineData != null }?.inlineData
            if (img == null) {
                val why = responseParts.mapNotNull { it.text }.joinToString(" ").trim()
                throw IllegalStateException(
                    if (why.isNotEmpty()) "لم يُنتج النموذج صورة: ${why.take(160)}"
                    else "لم يُنتج النموذج صورة (قد يكون الطلب محجوباً). عدّل الوصف وأعد المحاولة."
                )
            }
            GeneratedImage(Base64.decode(img.data, Base64.DEFAULT), img.mimeType)
        }
    }

    // ------------------------------------------------------------------ المرحلة 6: المحتوى

    suspend fun buildContentPack(
        brief: Brief,
        idea: AdIdea,
        copy: String,
        feedback: String
    ): Result<String> {
        val prompt = withFeedback(
            "جهّز حزمة محتوى للنشر بالعربية اعتماداً على الإعلان المعتمد، بالأقسام التالية (بدون مقدمات):\n" +
                "1) منشور إنستغرام/فيسبوك (مع إيموجي معتدلة)\n2) منشور مختصر لـ X/تويتر (أقل من 250 حرفاً)\n" +
                "3) سيناريو فيديو قصير 15-20 ثانية (مشاهد مرقمة: ما يظهر + الجملة المنطوقة/المكتوبة)\n" +
                "4) 8 هاشتاغات مناسبة\n5) أفضل وقت مقترح للنشر مع تبرير بسيط\n\n$HONESTY_RULE\n\n" +
                "${brief.asText()}\nالفكرة: ${idea.title}\n${idea.concept}\nنص الإعلان المعتمد:\n$copy",
            feedback
        )
        return textCall(
            system = "أنت مدير محتوى سوشيال ميديا عربي.",
            parts = listOf(GeminiPart(text = prompt)),
            temperature = 0.7
        )
    }

    // ------------------------------------------------------------------ داخلي

    private fun stripFences(raw: String): String =
        raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private suspend fun textCall(
        system: String?,
        parts: List<GeminiPart>,
        temperature: Double,
        json: Boolean = false
    ): Result<String> =
        call(GeminiClient.TEXT_MODEL, system, parts, temperature, json, null).mapCatching { resp ->
            val text = resp.candidates?.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text }?.joinToString("")?.trim()
            if (text.isNullOrEmpty()) {
                throw IllegalStateException("لم يُرجع Gemini استجابة صالحة (قد يكون الطلب محجوباً).")
            }
            text
        }

    private suspend fun call(
        model: String,
        system: String?,
        parts: List<GeminiPart>,
        temperature: Double,
        json: Boolean = false,
        modalities: List<String>? = null
    ): Result<GeminiResponse> = withContext(Dispatchers.IO) {
        val apiKey = prefs.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException(MSG_NO_KEY))
        }
        try {
            val request = GeminiRequest(
                systemInstruction = system?.let { GeminiContent(parts = listOf(GeminiPart(text = it))) },
                contents = listOf(GeminiContent(role = "user", parts = parts)),
                generationConfig = GeminiGenerationConfig(
                    temperature = temperature,
                    responseMimeType = if (json) "application/json" else null,
                    responseModalities = modalities
                )
            )
            Result.success(GeminiClient.api.generateContent(model, apiKey, request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(GeminiClient.friendlyError(e)))
        }
    }
}
