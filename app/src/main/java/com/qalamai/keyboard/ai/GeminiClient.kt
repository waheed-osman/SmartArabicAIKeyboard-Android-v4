package com.qalamai.keyboard.ai

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** عميل Gemini المشترك بين أدوات الكتابة واستوديو الإعلانات. */
object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    const val TEXT_MODEL = "gemini-2.5-flash"

    /** نموذج توليد/تعديل الصور. غيّره هنا إن تغيّر اسم النموذج المتاح لحسابك. */
    const val IMAGE_MODEL = "gemini-2.5-flash-image"

    val gson: Gson = Gson()

    private val okHttpClient: OkHttpClient by lazy {
        // لا يوجد Logging Interceptor عمداً: لا نسجّل نصوص المستخدم ولا صوره ولا المفتاح.
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // توليد الصور أبطأ من النص
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: GeminiRetrofitApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GeminiRetrofitApi::class.java)
    }

    fun friendlyError(e: Throwable): String = when (e) {
        is HttpException -> httpMessage(e.code())
        is IOException -> "تعذر الاتصال بالإنترنت. تحقق من الشبكة وحاول مجدداً."
        else -> "حدث خطأ غير متوقع أثناء المعالجة."
    }

    private fun httpMessage(code: Int): String = when (code) {
        400, 401, 403 -> "مفتاح Gemini API غير صالح أو غير مصرّح له بهذه الميزة. راجع الإعدادات."
        404 -> "النموذج غير متاح حالياً لحسابك."
        429 -> "تم تجاوز حد الاستخدام (توليد الصور قد يتطلب خطة مدفوعة). انتظر قليلاً أو راجع حصتك."
        in 500..599 -> "خدمة Gemini مشغولة حالياً. حاول بعد قليل."
        else -> "فشل الطلب (رمز $code)."
    }
}
