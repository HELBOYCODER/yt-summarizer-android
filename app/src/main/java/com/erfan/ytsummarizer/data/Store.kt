package com.erfan.ytsummarizer.data

import android.content.Context
import com.erfan.ytsummarizer.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

/** Local-only settings and history (SharedPreferences). */
class Store(context: Context) {
  private val prefs = context.getSharedPreferences("yt_summarizer", Context.MODE_PRIVATE)

  var apiKey: String
    get() = prefs.getString("api_key", null) ?: BuildConfig.DEFAULT_GEMINI_KEY
    set(v) = prefs.edit().putString("api_key", v.trim()).apply()

  var model: String
    get() = prefs.getString("model", null) ?: DEFAULT_MODEL
    set(v) = prefs.edit().putString("model", v.trim()).apply()

  var language: String
    get() = prefs.getString("language", null) ?: Summarizer.LANGUAGES.first()
    set(v) = prefs.edit().putString("language", v).apply()

  fun history(): List<SummaryResult> {
    val arr = JSONArray(prefs.getString("history", "[]"))
    return (0 until arr.length()).map {
      val o = arr.getJSONObject(it)
      SummaryResult(
        o.getString("id"), o.optString("title"), o.optString("author"),
        o.optString("summary"), o.optString("transcript"), o.optString("source"),
        o.optLong("createdAt"),
      )
    }
  }

  fun add(r: SummaryResult) {
    val items = listOf(r) + history().filterNot { it.videoId == r.videoId }
    save(items.take(MAX_HISTORY))
  }

  fun remove(r: SummaryResult) = save(history().filterNot { it.videoId == r.videoId })

  private fun save(items: List<SummaryResult>) {
    val arr = JSONArray()
    items.forEach {
      arr.put(
        JSONObject().put("id", it.videoId).put("title", it.title).put("author", it.author)
          .put("summary", it.summary).put("transcript", it.transcript).put("source", it.source)
          .put("createdAt", it.createdAt),
      )
    }
    prefs.edit().putString("history", arr.toString()).apply()
  }

  companion object {
    const val DEFAULT_MODEL = "gemini-2.5-flash"
    const val MAX_HISTORY = 50
  }
}
