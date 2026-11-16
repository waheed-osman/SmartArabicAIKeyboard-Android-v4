package com.qalamai.keyboard.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** جسر بسيط (داخل نفس العملية) لتسليم نص الإملاء الصوتي إلى لوحة المفاتيح. */
object VoiceBridge {
    @Volatile var listener: ((String) -> Unit)? = null
    @Volatile private var pending: String? = null

    fun deliver(text: String) {
        pending = text
        listener?.invoke(text)
    }

    fun takePending(): String? {
        val p = pending
        pending = null
        return p
    }
}

/**
 * نافذة شفافة تشغّل واجهة التعرف على الكلام الخاصة بالنظام (Google).
 * لا يحتاج تطبيقنا صلاحية الميكروفون لأن التسجيل يتم داخل خدمة التعرف نفسها.
 */
class VoiceInputActivity : ComponentActivity() {

    companion object {
        const val EXTRA_LANG = "lang"
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim().orEmpty()
            if (text.isNotEmpty()) VoiceBridge.deliver(text)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return // أُعيد إنشاء النافذة: النتيجة قيد المعالجة
        val lang = intent.getStringExtra(EXTRA_LANG) ?: "ar-EG"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تكلّم الآن")
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "لا توجد خدمة تعرّف على الكلام على هذا الجهاز.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
