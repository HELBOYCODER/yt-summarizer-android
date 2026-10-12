package com.erfan.ytsummarizer.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiException(message: String) : Exception(message)

/** Minimal REST client for the Gemini API (free key from https://aistudio.google.com/apikey). */
class GeminiClient(private val apiKey: String, private val model: String) {
  private val json = "application/json".toMediaType()

  fun generate(parts: JSONArray, jsonOutput: Boolean = false): String {
    if (apiKey.isBlank()) throw GeminiException("NO_KEY")
    val config = JSONObject().put("temperature", 0.3)
    if (jsonOutput) config.put("responseMimeType", "application/json")
    val body = JSONObject()
      .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
      .put("generationConfig", config)
      .toString()
    val req = Request.Builder()
      .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
      .header("x-goog-api-key", apiKey)
      .post(body.toRequestBody(json))
      .build()
    Http.client.newCall(req).execute().use { r ->
      val text = r.body?.string().orEmpty()
      if (!r.isSuccessful) {
        val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }
          .getOrDefault("HTTP ${r.code}")
        throw GeminiException(msg)
      }
      val o = JSONObject(text)
      val candidate = o.optJSONArray("candidates")?.optJSONObject(0)
        ?: throw GeminiException(o.optJSONObject("promptFeedback")?.optString("blockReason") ?: "Empty response")
      val p = candidate.optJSONObject("content")?.optJSONArray("parts")
        ?: throw GeminiException("Empty response (${candidate.optString("finishReason")})")
      return (0 until p.length()).joinToString("") { p.getJSONObject(it).optString("text") }
    }
  }

  companion object {
    fun text(t: String): JSONObject = JSONObject().put("text", t)
    fun youtube(url: String): JSONObject = JSONObject().put("file_data", JSONObject().put("file_uri", url))
  }
}
