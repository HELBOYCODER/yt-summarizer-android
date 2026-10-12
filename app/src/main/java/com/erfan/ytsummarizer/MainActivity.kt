package com.erfan.ytsummarizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.erfan.ytsummarizer.data.YoutubeUrl
import com.erfan.ytsummarizer.ui.AppTheme
import com.erfan.ytsummarizer.ui.MainScreen
import com.erfan.ytsummarizer.ui.MainViewModel

class MainActivity : ComponentActivity() {
  private val vm: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (savedInstanceState == null) handleShare(intent)
    setContent {
      AppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          MainScreen(vm)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleShare(intent)
  }

  /** "Share" from the YouTube app -> this app summarizes the video right away. */
  private fun handleShare(intent: Intent?) {
    if (intent?.action != Intent.ACTION_SEND) return
    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
    if (YoutubeUrl.extractId(text) == null) return
    vm.url = text.trim()
    vm.summarize()
  }
}
