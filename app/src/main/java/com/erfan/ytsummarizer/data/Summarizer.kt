package com.erfan.ytsummarizer.data

import org.json.JSONArray
import org.json.JSONObject

enum class Stage { META, CAPTIONS, WATCHING, SUMMARIZING }

/**
 * Pipeline: video link -> subtitles (or Gemini watches the video if there are none) -> summary.
 * Every request is independent; nothing depends on an account or a usage quota of a third-party site.
 */
class Summarizer(
  private val youtube: YoutubeClient,
  private val gemini: GeminiClient,
  private val outputLanguage: String,
) {
  fun run(videoId: String, onStage: (Stage) -> Unit): SummaryResult {
    onStage(Stage.META)
    val meta = youtube.fetchMeta(videoId)

    onStage(Stage.CAPTIONS)
    val transcript = youtube.fetchTranscript(videoId, langCode(outputLanguage))

    return if (transcript != null) {
      onStage(Stage.SUMMARIZING)
      val prompt = summaryPrompt(meta) + "\n\nTRANSCRIPT (with timestamps):\n" + transcript.timestampedText()
      val summary = gemini.generate(JSONArray().put(GeminiClient.text(prompt)))
      SummaryResult(videoId, meta.title, meta.author, summary.trim(), transcript.timestampedText(), "captions")
    } else {
      // No subtitles available: Gemini watches the public YouTube video directly.
      onStage(Stage.WATCHING)
      val prompt = summaryPrompt(meta) + """

Also produce a full transcript of the spoken words in the video's original language, one line per sentence, each line prefixed with a [mm:ss] timestamp.
Return ONLY JSON: {"summary": "<markdown summary>", "transcript": "<transcript>"}"""
      val raw = gemini.generate(
        JSONArray().put(GeminiClient.youtube(YoutubeUrl.watchUrl(videoId))).put(GeminiClient.text(prompt)),
        jsonOutput = true,
      )
      val o = parseLooseJson(raw)
      SummaryResult(
        videoId, meta.title, meta.author,
        o?.optString("summary")?.trim().orEmpty().ifEmpty { raw.trim() },
        o?.optString("transcript")?.trim().orEmpty(),
        "video",
      )
    }
  }

  private fun summaryPrompt(meta: VideoMeta) = """
You are an expert video summarizer. Summarize the YouTube video "${meta.title}"${if (meta.author.isNotBlank()) " by ${meta.author}" else ""}.
Write the summary in $outputLanguage, using Markdown with exactly these sections:

## Summary
A concise paragraph (3-5 sentences) capturing the main idea.

## Highlights
5-10 bullet points with the key takeaways. Start each bullet with a relevant emoji.

## Timeline
Bullets of the form "**[mm:ss]** what happens" covering the main sections of the video.

## Conclusion
One or two sentences.
""".trim()

  private fun parseLooseJson(raw: String): JSONObject? {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
  }

  companion object {
    val LANGUAGES = listOf("Persian (فارسی)", "English", "Same as the video")

    fun langCode(lang: String): String? = when {
      lang.startsWith("Persian") -> "fa"
      lang.startsWith("English") -> "en"
      else -> null
    }
  }
}
