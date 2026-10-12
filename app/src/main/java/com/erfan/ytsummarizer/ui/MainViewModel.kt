package com.erfan.ytsummarizer.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfan.ytsummarizer.data.GeminiClient
import com.erfan.ytsummarizer.data.GeminiException
import com.erfan.ytsummarizer.data.Stage
import com.erfan.ytsummarizer.data.Store
import com.erfan.ytsummarizer.data.SummaryResult
import com.erfan.ytsummarizer.data.Summarizer
import com.erfan.ytsummarizer.data.YoutubeClient
import com.erfan.ytsummarizer.data.YoutubeUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainViewModel(app: Application) : AndroidViewModel(app) {
  val store = Store(app)

  var url by mutableStateOf("")
  var stage by mutableStateOf<Stage?>(null)
    private set
  var result by mutableStateOf<SummaryResult?>(null)
    private set
  var error by mutableStateOf<String?>(null)
    private set
  var history by mutableStateOf(store.history())
    private set
  var showSettings by mutableStateOf(store.apiKey.isBlank())
  var showHistory by mutableStateOf(false)

  private var job: Job? = null
  val busy get() = stage != null

  fun summarize() {
    val id = YoutubeUrl.extractId(url)
    if (id == null) {
      error = "لینک یوتیوب معتبر نیست."
      return
    }
    if (store.apiKey.isBlank()) {
      showSettings = true
      return
    }
    error = null
    result = null
    job?.cancel()
    job = viewModelScope.launch {
      try {
        val summarizer = Summarizer(YoutubeClient(), GeminiClient(store.apiKey, store.model), store.language)
        val r = withContext(Dispatchers.IO) {
          summarizer.run(id) { s -> stage = s } // snapshot state is safe to write from any thread
        }
        result = r
        store.add(r)
        history = store.history()
      } catch (e: GeminiException) {
        error = when {
          e.message == "NO_KEY" -> "کلید Gemini تنظیم نشده."
          e.message.orEmpty().contains("API key", true) -> "کلید Gemini نامعتبر است: ${e.message}"
          e.message.orEmpty().contains("quota", true) || e.message.orEmpty().contains("exhausted", true) ->
            "سهمیه رایگان Gemini موقتاً تمام شده؛ چند دقیقه بعد دوباره امتحان کن.\n${e.message}"
          else -> "خطای Gemini: ${e.message}"
        }
      } catch (e: IOException) {
        error = "خطای اتصال اینترنت: ${e.message}"
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        error = "خطا: ${e.message}"
      } finally {
        stage = null
      }
    }
  }

  fun cancel() {
    job?.cancel()
    stage = null
  }

  fun open(r: SummaryResult) {
    result = r
    url = YoutubeUrl.watchUrl(r.videoId)
    error = null
    showHistory = false
  }

  fun delete(r: SummaryResult) {
    store.remove(r)
    history = store.history()
    if (result?.videoId == r.videoId) result = null
  }

  fun saveSettings(key: String, model: String, language: String) {
    store.apiKey = key
    store.model = model.ifBlank { Store.DEFAULT_MODEL }
    store.language = language
    showSettings = false
  }
}
