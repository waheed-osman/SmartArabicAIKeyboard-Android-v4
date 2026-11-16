package com.qalamai.keyboard.ai

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiRetrofitApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(
    @SerializedName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    @SerializedName("contents")
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("parts")
    val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("inlineData")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mimeType")
    val mimeType: String,
    /** بيانات الصورة بصيغة Base64 (بدون أسطر). */
    @SerializedName("data")
    val data: String
)

data class GeminiGenerationConfig(
    @SerializedName("temperature")
    val temperature: Double? = null,
    @SerializedName("responseMimeType")
    val responseMimeType: String? = null,
    @SerializedName("responseModalities")
    val responseModalities: List<String>? = null
)

data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @SerializedName("content")
    val content: GeminiContent?
)
