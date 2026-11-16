package com.qalamai.keyboard.ui.ads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.qalamai.keyboard.ai.GeneratedImage
import java.io.File

class AdStudioActivity : ComponentActivity() {

    private val vm: AdStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AdStudioScreen(
                        vm = vm,
                        onClose = { finish() },
                        onCopy = { copyToClipboard(it) },
                        onShare = { text, image -> share(text, image) }
                    )
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ad", text))
        Toast.makeText(this, "تم نسخ النص", Toast.LENGTH_SHORT).show()
    }

    private fun share(text: String, image: GeneratedImage?) {
        val send = Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, text) }
        if (image != null) {
            val dir = File(cacheDir, "ads").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // لا نُبقي نسخاً قديمة في الكاش
            val ext = if (image.mimeType.contains("jpeg")) "jpg" else "png"
            val file = File(dir, "ad_${System.currentTimeMillis()}.$ext")
            file.writeBytes(image.bytes)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            send.type = image.mimeType
            send.putExtra(Intent.EXTRA_STREAM, uri)
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            send.type = "text/plain"
        }
        startActivity(Intent.createChooser(send, "مشاركة الإعلان"))
    }
}
