package com.qalamai.keyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Build

object BugReport {
    /** يفتح قائمة المشاركة بتقرير جاهز (معلومات الجهاز فقط، لا نصوص ولا مفاتيح). */
    fun share(context: Context) {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
        val body = buildString {
            appendLine("تقرير خطأ - لوحة القلم الذكية")
            appendLine("الإصدار: $version")
            appendLine("أندرويد: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("وصف المشكلة:")
            appendLine("الخطوات لإعادة الحدوث:")
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "تقرير خطأ - لوحة القلم الذكية")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(
            Intent.createChooser(send, "إرسال تقرير الأخطاء").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
