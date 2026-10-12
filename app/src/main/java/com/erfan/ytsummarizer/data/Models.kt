package com.erfan.ytsummarizer.data

data class VideoMeta(val id: String, val title: String, val author: String)

data class TranscriptLine(val startSec: Double, val text: String)

data class Transcript(val language: String, val lines: List<TranscriptLine>) {
  fun plainText(): String = lines.joinToString(" ") { it.text }
  fun timestampedText(): String =
    lines.joinToString("\n") { "[${formatTime(it.startSec)}] ${it.text}" }
}

data class SummaryResult(
  val videoId: String,
  val title: String,
  val author: String,
  val summary: String,
  val transcript: String,
  /** "captions" when YouTube subtitles were used, "video" when Gemini watched the video itself. */
  val source: String,
  val createdAt: Long = System.currentTimeMillis(),
)

fun formatTime(sec: Double): String {
  val t = sec.toInt()
  val h = t / 3600
  val m = (t % 3600) / 60
  val s = t % 60
  return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
