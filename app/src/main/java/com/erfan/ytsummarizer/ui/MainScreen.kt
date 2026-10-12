package com.erfan.ytsummarizer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erfan.ytsummarizer.data.Stage
import com.erfan.ytsummarizer.data.Summarizer
import com.erfan.ytsummarizer.data.SummaryResult
import com.erfan.ytsummarizer.data.YoutubeUrl
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
  val snackbar = remember { SnackbarHostState() }
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(if (vm.showHistory) "تاریخچه" else "خلاصه‌ساز یوتیوب", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          if (vm.showHistory) {
            IconButton({ vm.showHistory = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
          } else {
            IconButton({ vm.showHistory = true }) { Icon(Icons.Default.History, "تاریخچه") }
          }
        },
        actions = { IconButton({ vm.showSettings = true }) { Icon(Icons.Default.Settings, "تنظیمات") } },
      )
    },
    snackbarHost = { SnackbarHost(snackbar) },
  ) { pad ->
    Box(Modifier.padding(pad).fillMaxSize()) {
      if (vm.showHistory) HistoryList(vm) else Home(vm, snackbar)
    }
  }
  if (vm.showSettings) SettingsDialog(vm)
}

@Composable
private fun Home(vm: MainViewModel, snackbar: SnackbarHostState) {
  val clipboard = LocalClipboardManager.current
  Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      "لینک ویدیو رو بذار؛ زیرنویس گرفته میشه و با هوش مصنوعی خلاصه میشه. اگه ویدیو زیرنویس نداشته باشه، خود ویدیو تحلیل میشه.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
      value = vm.url,
      onValueChange = { vm.url = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("لینک یوتیوب") },
      placeholder = { Text("https://youtu.be/...") },
      singleLine = true,
      enabled = !vm.busy,
      textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
      keyboardActions = KeyboardActions(onGo = { vm.summarize() }),
      trailingIcon = {
        if (vm.url.isEmpty()) {
          IconButton({ clipboard.getText()?.text?.let { vm.url = it.trim() } }) {
            Icon(Icons.Default.ContentPaste, "چسباندن")
          }
        } else if (!vm.busy) {
          IconButton({ vm.url = "" }) { Icon(Icons.Default.Close, "پاک کردن") }
        }
      },
      shape = RoundedCornerShape(14.dp),
    )
    if (vm.busy) {
      Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
          Spacer(Modifier.width(14.dp))
          Text(stageLabel(vm.stage), Modifier.weight(1f))
          TextButton({ vm.cancel() }) { Text("لغو") }
        }
      }
    } else {
      Button(
        onClick = { vm.summarize() },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        enabled = vm.url.isNotBlank(),
      ) {
        Icon(Icons.Default.AutoAwesome, null)
        Spacer(Modifier.width(8.dp))
        Text("خلاصه کن", fontWeight = FontWeight.Bold)
      }
    }
    vm.error?.let {
      Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
      ) { Text(it, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
    }
    AnimatedVisibility(vm.result != null) {
      vm.result?.let { ResultCard(it, snackbar) }
    }
  }
}

private fun stageLabel(s: Stage?) = when (s) {
  Stage.META -> "در حال گرفتن اطلاعات ویدیو…"
  Stage.CAPTIONS -> "در حال گرفتن زیرنویس…"
  Stage.WATCHING -> "زیرنویس نداشت؛ هوش مصنوعی داره خود ویدیو رو تحلیل می‌کنه (ممکنه یکی دو دقیقه طول بکشه)…"
  Stage.SUMMARIZING -> "در حال خلاصه‌سازی…"
  null -> ""
}

@Composable
private fun ResultCard(r: SummaryResult, snackbar: SnackbarHostState) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  var tab by rememberSaveable(r.videoId, r.createdAt) { mutableIntStateOf(0) }
  val content = if (tab == 0) r.summary else r.transcript

  Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(16.dp)) {
      Text(
        r.title,
        style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
        fontWeight = FontWeight.Bold,
      )
      if (r.author.isNotBlank()) {
        Text(r.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(
        if (r.source == "captions") "منبع: زیرنویس ویدیو" else "منبع: تحلیل مستقیم ویدیو",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
      )
      Row(Modifier.padding(top = 8.dp)) {
        IconButton({
          clipboard.setText(AnnotatedString(content))
          scope.launch { snackbar.showSnackbar("کپی شد") }
        }) { Icon(Icons.Default.ContentCopy, "کپی") }
        IconButton({
          val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "${r.title}\n${YoutubeUrl.watchUrl(r.videoId)}\n\n$content")
          context.startActivity(Intent.createChooser(send, null))
        }) { Icon(Icons.Default.Share, "اشتراک") }
        IconButton({
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrl.watchUrl(r.videoId))))
        }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "باز کردن در یوتیوب") }
      }
      TabRow(selectedTabIndex = tab) {
        Tab(tab == 0, { tab = 0 }, text = { Text("خلاصه") })
        Tab(tab == 1, { tab = 1 }, text = { Text("متن کامل") })
      }
      Spacer(Modifier.height(12.dp))
      SelectionContainer {
        if (tab == 0) {
          MarkdownText(r.summary)
        } else {
          Text(
            r.transcript.ifBlank { "متنی موجود نیست." },
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
          )
        }
      }
    }
  }
}

@Composable
private fun HistoryList(vm: MainViewModel) {
  if (vm.history.isEmpty()) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("هنوز چیزی خلاصه نکردی.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
  }
  val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
  LazyColumn(Modifier.fillMaxSize()) {
    items(vm.history, key = { it.videoId }) { r ->
      Row(
        Modifier.fillMaxWidth().clickable { vm.open(r) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            r.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
          )
          Text(
            fmt.format(Date(r.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton({ vm.delete(r) }) { Icon(Icons.Default.Delete, "حذف") }
      }
      HorizontalDivider()
    }
  }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SettingsDialog(vm: MainViewModel) {
  val context = LocalContext.current
  var key by remember { mutableStateOf(vm.store.apiKey) }
  var model by remember { mutableStateOf(vm.store.model) }
  var lang by remember { mutableStateOf(vm.store.language) }
  AlertDialog(
    onDismissRequest = { vm.showSettings = false },
    title = { Text("تنظیمات") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "برای خلاصه‌سازی یک کلید رایگان Gemini لازمه (یک بار، بدون پرداخت). از Google AI Studio بگیر و اینجا بچسبون.",
          style = MaterialTheme.typography.bodySmall,
        )
        TextButton({
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
        }) { Text("گرفتن کلید رایگان ←") }
        OutlinedTextField(
          key, { key = it },
          label = { Text("Gemini API Key") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
        )
        OutlinedTextField(
          model, { model = it },
          label = { Text("مدل") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
        )
        Text("زبان خلاصه", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Summarizer.LANGUAGES.forEach { l ->
            FilterChip(selected = lang == l, onClick = { lang = l }, label = { Text(l, textAlign = TextAlign.Center) })
          }
        }
      }
    },
    confirmButton = { Button({ vm.saveSettings(key, model, lang) }) { Text("ذخیره") } },
    dismissButton = { TextButton({ vm.showSettings = false }) { Text("بستن") } },
  )
}
