package com.erfan.ytsummarizer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Tiny Markdown renderer: headings, bullets, numbered lists and **bold**. */
@Composable
fun MarkdownText(md: String, modifier: Modifier = Modifier) {
  val body = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
  Column(modifier) {
    md.lines().forEach { raw ->
      val line = raw.trimEnd()
      val trimmed = line.trimStart()
      when {
        trimmed.isEmpty() -> Spacer(Modifier.height(6.dp))
        trimmed.startsWith("#") -> {
          val text = trimmed.trimStart('#').trim()
          Spacer(Modifier.height(8.dp))
          Text(
            inline(text),
            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          Spacer(Modifier.height(4.dp))
        }
        trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
          val indent = (line.length - trimmed.length) / 2
          Row(Modifier.padding(start = (indent * 12).dp, top = 2.dp, bottom = 2.dp)) {
            Text("•", style = body, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(inline(trimmed.substring(2).trim()), style = body)
          }
        }
        Regex("""^\d+[.)]\s""").containsMatchIn(trimmed) -> {
          val num = trimmed.substringBefore(' ')
          Row(Modifier.padding(vertical = 2.dp)) {
            Text(num, style = body, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(inline(trimmed.substringAfter(' ').trim()), style = body)
          }
        }
        else -> Text(inline(trimmed), style = body, modifier = Modifier.padding(vertical = 2.dp))
      }
    }
  }
}

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
  val parts = text.split("**")
  parts.forEachIndexed { i, part ->
    val clean = part.replace("`", "")
    if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(clean) } else append(clean)
  }
}
