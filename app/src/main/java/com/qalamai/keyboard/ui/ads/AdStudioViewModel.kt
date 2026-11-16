package com.qalamai.keyboard.ui.ads

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qalamai.keyboard.ai.AdIdea
import com.qalamai.keyboard.ai.AdStudioManager
import com.qalamai.keyboard.ai.GeneratedImage
import com.qalamai.keyboard.data.SecurePrefsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AdStep(val titleAr: String) {
    PRODUCT("المنتج"),
    ANALYSIS("التحليل"),
    IDEAS("الفكرة"),
    COPY("النص"),
    VISUAL("التصميم"),
    PACK("المحتوى")
}

/**
 * حالة استوديو الإعلانات. لا تُرسل أي بيانات إلى Gemini إلا من دوال التوليد أدناه،
 * وكلها تُستدعى بضغطة زر صريحة، ولا ينتقل المستخدم لمرحلة تالية إلا بزر «اعتمد».
 */
class AdStudioViewModel(app: Application) : AndroidViewModel(app) {

    private val manager = AdStudioManager(SecurePrefsManager(app))
    private var job: Job? = null

    var step by mutableStateOf(AdStep.PRODUCT)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // المرحلة 1: المنتج
    var productName by mutableStateOf("")
    var productNotes by mutableStateOf("")
    var audience by mutableStateOf("")
    var tone by mutableStateOf("")
    var productBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    private var productJpeg: ByteArray? = null

    // مراحل التوليد (كلها نصوص قابلة للتعديل قبل الاعتماد)
    var analysis by mutableStateOf("")
    val ideas = mutableStateListOf<AdIdea>()
    var selectedIdea by mutableIntStateOf(-1)
        private set
    var ideaTitle by mutableStateOf("")
    var ideaConcept by mutableStateOf("")
    var copyText by mutableStateOf("")
    var visualPrompt by mutableStateOf("")
    var adBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    var adImage by mutableStateOf<GeneratedImage?>(null)
        private set
    var packText by mutableStateOf("")

    /** ملاحظاتك لإعادة التوليد (تُمسح عند تغيّر المرحلة). */
    var feedback by mutableStateOf("")

    var finalApproved by mutableStateOf(false)
        private set

    // ------------------------------------------------------------------ التنقل والاعتماد

    fun canApprove(): Boolean = when (step) {
        AdStep.PRODUCT -> productJpeg != null && productName.isNotBlank()
        AdStep.ANALYSIS -> analysis.isNotBlank()
        AdStep.IDEAS -> ideaConcept.isNotBlank()
        AdStep.COPY -> copyText.isNotBlank()
        AdStep.VISUAL -> adImage != null
        AdStep.PACK -> false
    }

    fun approveAndNext() {
        if (loading || !canApprove()) return
        val next = AdStep.entries.getOrNull(step.ordinal + 1) ?: return
        goTo(next)
    }

    fun back() {
        if (loading) return
        val prev = AdStep.entries.getOrNull(step.ordinal - 1) ?: return
        goTo(prev)
    }

    fun jumpBackTo(target: AdStep) {
        if (loading || target.ordinal >= step.ordinal) return
        goTo(target)
    }

    private fun goTo(target: AdStep) {
        step = target
        feedback = ""
        error = null
        finalApproved = false
    }

    fun approveFinal() {
        if (packText.isNotBlank() && adImage != null && copyText.isNotBlank()) finalApproved = true
    }

    fun cancel() {
        job?.cancel()
    }

    fun dismissError() {
        error = null
    }

    // ------------------------------------------------------------------ اختيار الصورة

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val bmp = withContext(Dispatchers.IO) { ImageUtils.loadScaled(ctx, uri) }
            if (bmp == null) {
                error = "تعذر قراءة الصورة. جرّب صورة أخرى."
                return@launch
            }
            productJpeg = withContext(Dispatchers.IO) { ImageUtils.toJpegBytes(bmp) }
            productBitmap = bmp.asImageBitmap()
        }
    }

    // ------------------------------------------------------------------ التوليد (بضغطة صريحة فقط)

    private fun brief() = AdStudioManager.Brief(
        name = productName.trim(),
        notes = productNotes.trim(),
        audience = audience.trim(),
        tone = tone.trim(),
        analysis = analysis.trim()
    )

    private fun currentIdea() = AdIdea(ideaTitle.trim(), ideaConcept.trim())

    private fun run(block: suspend () -> Unit) {
        if (loading) return
        loading = true
        error = null
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } finally {
                loading = false
            }
        }
    }

    fun analyze() {
        val img = productJpeg ?: return
        val fb = feedback
        run {
            manager.analyzeProduct(img, "image/jpeg", brief(), fb)
                .onSuccess { analysis = it; feedback = "" }
                .onFailure { error = it.message }
        }
    }

    fun makeIdeas() {
        val fb = feedback
        run {
            manager.generateIdeas(brief(), fb)
                .onSuccess { list ->
                    ideas.clear()
                    ideas.addAll(list)
                    selectedIdea = -1
                    ideaTitle = ""
                    ideaConcept = ""
                    feedback = ""
                }
                .onFailure { error = it.message }
        }
    }

    fun selectIdea(index: Int) {
        val idea = ideas.getOrNull(index) ?: return
        selectedIdea = index
        ideaTitle = idea.title
        ideaConcept = idea.concept
    }

    fun makeCopy() {
        val fb = feedback
        run {
            manager.writeCopy(brief(), currentIdea(), fb)
                .onSuccess { copyText = it; feedback = "" }
                .onFailure { error = it.message }
        }
    }

    fun makeVisualPrompt() {
        val fb = feedback
        run {
            manager.draftVisualPrompt(brief(), currentIdea(), copyText, fb)
                .onSuccess { visualPrompt = it; feedback = "" }
                .onFailure { error = it.message }
        }
    }

    fun makeImage() {
        val img = productJpeg ?: return
        if (visualPrompt.isBlank()) {
            error = "اكتب أو ولّد وصف التصميم أولاً."
            return
        }
        val fb = feedback
        run {
            manager.generateImage(img, "image/jpeg", visualPrompt, fb)
                .onSuccess { gen ->
                    val bmp = withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(gen.bytes, 0, gen.bytes.size)
                    }
                    if (bmp == null) {
                        error = "تعذر عرض الصورة المولّدة، أعد المحاولة."
                    } else {
                        adImage = gen
                        adBitmap = bmp.asImageBitmap()
                        feedback = ""
                    }
                }
                .onFailure { error = it.message }
        }
    }

    fun makePack() {
        val fb = feedback
        run {
            manager.buildContentPack(brief(), currentIdea(), copyText, fb)
                .onSuccess { packText = it; feedback = ""; finalApproved = false }
                .onFailure { error = it.message }
        }
    }

    /** النص الكامل الجاهز للنسخ/المشاركة. */
    fun fullAdText(): String = buildString {
        appendLine(copyText.trim())
        if (packText.isNotBlank()) {
            appendLine()
            appendLine("————————")
            appendLine(packText.trim())
        }
    }.trim()
}
