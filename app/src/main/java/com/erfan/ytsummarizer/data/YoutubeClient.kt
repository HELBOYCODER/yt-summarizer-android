package com.erfan.ytsummarizer.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Reads public video info and caption tracks straight from YouTube.
 * No account and no third-party summarizer service involved.
 */
class YoutubeClient {
  private val json = "application/json".toMediaType()

  fun fetchMeta(id: String): VideoMeta {
    val url = "https://www.youtube.com/oembed?format=json&url=" +
      URLEncoder.encode(YoutubeUrl.watchUrl(id), "UTF-8")
    return try {
      Http.client.newCall(Request.Builder().url(url).build()).execute().use { r ->
        if (!r.isSuccessful) return VideoMeta(id, "YouTube video", "")
        val o = JSONObject(r.body!!.string())
        VideoMeta(id, o.optString("title", "YouTube video"), o.optString("author_name", ""))
      }
    } catch (e: Exception) {
      VideoMeta(id, "YouTube video", "")
    }
  }

  /**
   * Tries to download the video's subtitles (manual first, then auto-generated).
   * Returns null when the video has no captions or YouTube refuses the request;
   * the caller then falls back to letting Gemini watch the video.
   */
  fun fetchTranscript(id: String, preferredLang: String?): Transcript? {
    for (client in CLIENTS) {
      val tracks = try {
        captionTracks(id, client)
      } catch (e: Exception) {
        null
      } ?: continue
      if (tracks.isEmpty()) continue
      val track = pickTrack(tracks, preferredLang)
      val lines = try {
        downloadTrack(track.baseUrl, client.userAgent)
      } catch (e: Exception) {
        emptyList()
      }
      if (lines.isNotEmpty()) return Transcript(track.lang, lines)
    }
    return null
  }

  private data class Track(val baseUrl: String, val lang: String, val auto: Boolean)

  private data class Client(val name: String, val version: String, val userAgent: String, val extra: Map<String, Any> = emptyMap())

  private fun captionTracks(id: String, c: Client): List<Track>? {
    val clientObj = JSONObject().put("clientName", c.name).put("clientVersion", c.version).put("hl", "en")
    c.extra.forEach { (k, v) -> clientObj.put(k, v) }
    val body = JSONObject()
      .put("context", JSONObject().put("client", clientObj))
      .put("videoId", id)
      .toString()
    val req = Request.Builder()
      .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
      .header("User-Agent", c.userAgent)
      .post(body.toRequestBody(json))
      .build()
    Http.client.newCall(req).execute().use { r ->
      if (!r.isSuccessful) return null
      val o = JSONObject(r.body!!.string())
      val arr = o.optJSONObject("captions")
        ?.optJSONObject("playerCaptionsTracklistRenderer")
        ?.optJSONArray("captionTracks") ?: return emptyList()
      return (0 until arr.length()).map {
        val t = arr.getJSONObject(it)
        Track(t.getString("baseUrl"), t.optString("languageCode"), t.optString("kind") == "asr")
      }
    }
  }

  private fun pickTrack(tracks: List<Track>, lang: String?): Track {
    val manual = tracks.filter { !it.auto }
    if (lang != null) {
      manual.firstOrNull { it.lang.startsWith(lang) }?.let { return it }
      tracks.firstOrNull { it.lang.startsWith(lang) }?.let { return it }
    }
    return manual.firstOrNull() ?: tracks.first()
  }

  private fun downloadTrack(baseUrl: String, ua: String): List<TranscriptLine> {
    val url = baseUrl.replace(Regex("&fmt=[^&]*"), "") + "&fmt=json3"
    Http.client.newCall(Request.Builder().url(url).header("User-Agent", ua).build()).execute().use { r ->
      if (!r.isSuccessful) return emptyList()
      val text = r.body!!.string()
      if (text.isBlank()) return emptyList()
      return parseJson3(text)
    }
  }

  companion object {
    private val CLIENTS = listOf(
      Client(
        "ANDROID", "20.10.38",
        "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip",
        mapOf("androidSdkVersion" to 34),
      ),
      Client(
        "WEB", "2.20250101.00.00",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36",
      ),
    )

    /** Parses YouTube's "json3" timed-text format into lines. */
    fun parseJson3(text: String): List<TranscriptLine> {
      val events = JSONObject(text).optJSONArray("events") ?: return emptyList()
      val out = ArrayList<TranscriptLine>()
      for (i in 0 until events.length()) {
        val e = events.getJSONObject(i)
        val segs = e.optJSONArray("segs") ?: continue
        val sb = StringBuilder()
        for (j in 0 until segs.length()) sb.append(segs.getJSONObject(j).optString("utf8"))
        val line = sb.toString().replace('\n', ' ').trim()
        if (line.isNotEmpty()) out += TranscriptLine(e.optLong("tStartMs") / 1000.0, line)
      }
      return out
    }
  }
}
