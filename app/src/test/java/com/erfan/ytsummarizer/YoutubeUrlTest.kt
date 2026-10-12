package com.erfan.ytsummarizer

import com.erfan.ytsummarizer.data.YoutubeClient
import com.erfan.ytsummarizer.data.YoutubeUrl
import com.erfan.ytsummarizer.data.formatTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeUrlTest {
  @Test fun extractsIdFromCommonLinks() {
    val id = "maabARKJ94o"
    listOf(
      "https://youtu.be/maabARKJ94o?si=fC0ULTKuAv4Ce8WQ",
      "https://www.youtube.com/watch?v=maabARKJ94o&t=30s",
      "https://m.youtube.com/watch?feature=share&v=maabARKJ94o",
      "https://www.youtube.com/shorts/maabARKJ94o",
      "https://www.youtube.com/live/maabARKJ94o",
      "Check this out https://youtu.be/maabARKJ94o",
      "maabARKJ94o",
    ).forEach { assertEquals(it, id, YoutubeUrl.extractId(it)) }
  }

  @Test fun rejectsNonYoutube() {
    assertNull(YoutubeUrl.extractId("https://example.com"))
    assertNull(YoutubeUrl.extractId(""))
  }

  @Test fun parsesJson3Captions() {
    val json = """{"events":[{"tStartMs":0,"segs":[{"utf8":"Hello"},{"utf8":" world"}]},
      {"tStartMs":1500},{"tStartMs":65000,"segs":[{"utf8":"\n"}]},{"tStartMs":70000,"segs":[{"utf8":"Bye"}]}]}"""
    val lines = YoutubeClient.parseJson3(json)
    assertEquals(listOf("Hello world", "Bye"), lines.map { it.text })
    assertEquals(70.0, lines[1].startSec, 0.0)
  }

  @Test fun formatsTime() {
    assertEquals("01:05", formatTime(65.0))
    assertEquals("1:00:01", formatTime(3601.0))
  }
}
