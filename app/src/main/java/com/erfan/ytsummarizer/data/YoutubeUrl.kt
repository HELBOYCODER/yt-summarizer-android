package com.erfan.ytsummarizer.data

object YoutubeUrl {
  private val ID = Regex("^[A-Za-z0-9_-]{11}$")
  private val PATTERNS = listOf(
    Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
    Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
    Regex("""youtube\.com/(?:shorts|embed|live|v)/([A-Za-z0-9_-]{11})"""),
  )

  /** Returns the 11-char video id from any common YouTube link (or a bare id), or null. */
  fun extractId(input: String): String? {
    val text = input.trim()
    if (ID.matches(text)) return text
    for (p in PATTERNS) p.find(text)?.let { return it.groupValues[1] }
    return null
  }

  fun watchUrl(id: String) = "https://www.youtube.com/watch?v=$id"
  fun thumbnailUrl(id: String) = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}
