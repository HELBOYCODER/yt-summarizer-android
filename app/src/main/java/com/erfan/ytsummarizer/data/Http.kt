package com.erfan.ytsummarizer.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object Http {
  val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.MINUTES) // video analysis on Gemini can take a while
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
}
