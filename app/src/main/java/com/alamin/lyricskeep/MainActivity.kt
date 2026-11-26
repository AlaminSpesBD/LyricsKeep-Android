package com.alamin.lyricskeep

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.Icons
import android.media.MediaPlayer
import android.graphics.BitmapFactory
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private data class Lyric(val id: Long, val title: String, val body: String, val folder: String, val coverPath: String = "", val audioPath: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LyricsKeepApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsKeepApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lyrics_keep", 0) }
    fun loadLyrics(): List<Lyric> = runCatching {
        val array = JSONArray(prefs.getString("lyrics", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Lyric(o.optLong("id"), o.optString("title"), o.optString("body"), o.optString("folder"), o.optString("coverPath"), o.optString("audioPath"))
        }
    }.getOrDefault(emptyList())

    var lyrics by remember { mutableStateOf(loadLyrics()) }
    var query by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var showEditor by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Lyric?>(null) }
    var newFolder by remember { mutableStateOf("") }
    var parentFolder by remember { mutableStateOf("Root") }
    var folders by remember { mutableStateOf((runCatching { val a = JSONArray(prefs.getString("folders", "[]")); (0 until a.length()).map { a.getString(it) } }.getOrDefault(emptyList()) + lyrics.map { it.folder } + "General").distinct().sorted()) }
    var editing by remember { mutableStateOf<Lyric?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("General") }
    var coverPath by remember { mutableStateOf("") }
    var audioPath by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var currentMs by remember { mutableIntStateOf(0) }
    var currentTrackId by remember { mutableLongStateOf(-1L) }
    var showFolders by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var actionLyric by remember { mutableStateOf<Lyric?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var compactCards by remember { mutableStateOf(prefs.getBoolean("compact_cards", false)) }
    var showFolderPath by remember { mutableStateOf(prefs.getBoolean("show_folder_path", true)) }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
    var autoBackup by remember { mutableStateOf(prefs.getBoolean("auto_backup", false)) }
    var fontScale by remember { mutableFloatStateOf(prefs.getFloat("font_scale", 1f)) }
    var nameFont by remember { mutableStateOf(prefs.getString("name_font", "Sans Serif") ?: "Sans Serif") }
    var lyricsFont by remember { mutableStateOf(prefs.getString("lyrics_font", "Serif") ?: "Serif") }
    var folderFont by remember { mutableStateOf(prefs.getString("folder_font", "Sans Serif") ?: "Sans Serif") }
    var defaultSort by remember { mutableStateOf(prefs.getString("sort_order", "Recently added") ?: "Recently added") }
    var defaultFolder by remember { mutableStateOf(prefs.getString("default_folder", "General") ?: "General") }

    // One consistent back action for both Android system back and the in-app back button.
    val handleBack = {
        when {
            showFolders -> showFolders = false
            showEditor -> showEditor = false
            preview != null -> { preview = null }
            activeTab != 0 -> activeTab = 0
            selectedFolder != "All" -> {
                val parent = selectedFolder.substringBeforeLast("/", "All")
                selectedFolder = if (parent.isBlank()) "All" else parent
            }
        }
    }
    BackHandler(enabled = showFolders || showEditor || preview != null || activeTab != 0 || selectedFolder != "All") {
        handleBack()
    }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) runCatching {
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val dest = File(dir, "cover_" + System.currentTimeMillis() + ".img")
            context.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            coverPath = dest.absolutePath
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) runCatching {
            val dir = File(context.filesDir, "audios").apply { mkdirs() }
            val dest = File(dir, "audio_" + System.currentTimeMillis() + ".mp3")
            context.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            audioPath = dest.absolutePath
        }
    }

    var pendingTrack by remember { mutableStateOf<Lyric?>(null) }
    fun startTrackNow(item: Lyric) {
        // Keep playback strictly inside the selected lyric's folder.
        val queue = lyrics.filter { it.folder == item.folder && it.audioPath.isNotBlank() }
        val index = queue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        val intent = Intent(context, LyricsPlaybackService::class.java).apply {
            action = LyricsPlaybackService.ACTION_PLAY
            putStringArrayListExtra(LyricsPlaybackService.EXTRA_PATHS, ArrayList(queue.map { it.audioPath }))
            putStringArrayListExtra(LyricsPlaybackService.EXTRA_TITLES, ArrayList(queue.map { it.title }))
            putExtra(LyricsPlaybackService.EXTRA_IDS, queue.map { it.id }.toLongArray())
            putExtra(LyricsPlaybackService.EXTRA_INDEX, index)
        }
        ContextCompat.startForegroundService(context, intent)
        preview = item
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingTrack?.let { startTrackNow(it) }
        pendingTrack = null
    }
    fun startTrack(item: Lyric) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingTrack = item
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startTrackNow(item)
    }

    DisposableEffect(context, lyrics) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != LyricsPlaybackService.ACTION_STATE) return
                isPlaying = intent.getBooleanExtra(LyricsPlaybackService.EXTRA_PLAYING, false)
                durationMs = intent.getIntExtra(LyricsPlaybackService.EXTRA_DURATION, 0)
                currentMs = intent.getIntExtra(LyricsPlaybackService.EXTRA_POSITION, 0)
                val id = intent.getLongExtra(LyricsPlaybackService.EXTRA_ID, -1L)
                currentTrackId = id
                if (preview != null) lyrics.firstOrNull { it.id == id }?.let { preview = it }
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(LyricsPlaybackService.ACTION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun persistFolders(items: List<String>) {
        val a = JSONArray(); items.distinct().sorted().forEach { a.put(it) }
        prefs.edit().putString("folders", a.toString()).apply(); folders = items.distinct().sorted()
    }

    fun save(items: List<Lyric>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title)
                .put("body", item.body).put("folder", item.folder).put("coverPath", item.coverPath).put("audioPath", item.audioPath))
        }
        prefs.edit().putString("lyrics", array.toString()).apply()
        lyrics = items
        val paths = items.flatMap { l -> l.folder.split("/").filter { it.isNotBlank() }.indices.map { i -> l.folder.split("/").take(i + 1).joinToString("/") } }
        persistFolders(folders + paths)
    }


    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) runCatching {
            val json = JSONArray()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output).use { zip ->
                    lyrics.forEach { item ->
                        val coverEntry = if (item.coverPath.isNotBlank() && File(item.coverPath).exists()) "covers/" + File(item.coverPath).name else ""
                        val audioEntry = if (item.audioPath.isNotBlank() && File(item.audioPath).exists()) "audios/" + File(item.audioPath).name else ""
                        json.put(JSONObject().put("id", item.id).put("title", item.title).put("body", item.body)
                            .put("folder", item.folder).put("coverEntry", coverEntry).put("audioEntry", audioEntry)
                            .put("coverPath", item.coverPath).put("audioPath", item.audioPath))
                        listOf(Pair(coverEntry, item.coverPath), Pair(audioEntry, item.audioPath)).forEach { pair ->
                            if (pair.first.isNotBlank()) {
                                val file = File(pair.second)
                                if (file.exists()) {
                                    zip.putNextEntry(ZipEntry(pair.first))
                                    file.inputStream().use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                            }
                        }
                    }
                    zip.putNextEntry(ZipEntry("lyrics.json"))
                    zip.write(json.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
                    val foldersJson = JSONArray().apply { folders.forEach { put(it) } }
                    zip.putNextEntry(ZipEntry("folders.json"))
                    zip.write(foldersJson.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            var lyricsJson = "[]"
            var foldersJson = "[]"
            val restoredFiles = mutableMapOf<String, String>()
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        when (entry.name) {
                            "lyrics.json" -> lyricsJson = zip.readBytes().toString(Charsets.UTF_8)
                            "folders.json" -> foldersJson = zip.readBytes().toString(Charsets.UTF_8)
                            else -> {
                                val dest = File(context.filesDir, "restored_media/" + File(entry.name).name)
                                dest.parentFile?.mkdirs()
                                dest.outputStream().use { output -> zip.copyTo(output) }
                                restoredFiles[entry.name] = dest.absolutePath
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            val array = JSONArray(lyricsJson)
            val restored = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val coverEntry = o.optString("coverEntry")
                val audioEntry = o.optString("audioEntry")
                Lyric(o.optLong("id"), o.optString("title"), o.optString("body"), o.optString("folder", "General"),
                    restoredFiles[coverEntry] ?: o.optString("coverPath"), restoredFiles[audioEntry] ?: o.optString("audioPath"))
            }
            val fa = JSONArray(foldersJson)
            val restoredFolders = (0 until fa.length()).map { fa.getString(it) }
            save(restored)
            persistFolders(restoredFolders + restored.map { it.folder } + "General")
            selectedFolder = "All"
        }
    }

    val folderChoices = listOf("All") + folders
    val childFolders = folders.filter { path ->
        if (selectedFolder == "All") !path.contains("/")
        else path.substringBeforeLast("/", "") == selectedFolder
    }.sorted()
    val visible = lyrics.filter {
        (if (selectedFolder == "All") !it.folder.contains("/") else it.folder == selectedFolder) &&
            (it.title.contains(query, true) || it.body.contains(query, true))
    }
    val pageBg = if (darkMode) Color(0xFF100A0E) else Color(0xFFFFF4F8)
    val pageSurface = if (darkMode) Color(0xFF21151C) else Color.White
    val pageText = if (darkMode) Color.White else Color(0xFF171116)
    val pageMuted = if (darkMode) Color(0xFF9D8290) else Color(0xFF806A76)

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(
        primary = Color(0xFFFF4F9A), secondary = Color(0xFFFF79B4),
        background = pageBg, surface = pageSurface, onSurface = pageText
    ) else lightColorScheme(
        primary = Color(0xFFE83283), secondary = Color(0xFFFF79B4),
        background = pageBg, surface = pageSurface, onSurface = pageText
    )) {
        Box(Modifier.fillMaxSize().background(pageBg)) {
            if (activeTab == 0) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ALAMIN", color = Color(0xFFFF4F9A), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("Alamin Lyrics and Keeps", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("${lyrics.size} saved lyrics", color = Color(0xFFB68A9F), fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        TextButton(onClick = { backupLauncher.launch("Alamin-Lyrics-Backup.zip") }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("Backup", color = Color(0xFFFF4F9A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("Restore", color = Color(0xFFFF4F9A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search lyrics...") },
                    leadingIcon = { Text("⌕", fontSize = 23.sp) },
                    singleLine = true, shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF79B4), unfocusedBorderColor = Color(0xFF6B3A52),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (selectedFolder == "All") "ALL LOCATIONS" else selectedFolder.substringAfterLast("/").uppercase(),
                            color = Color(0xFFB68A9F), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                        Text(if (selectedFolder == "All") "Browse your library" else "📁 " + selectedFolder,
                            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = keepFontFamily(folderFont))
                    }
                    if (selectedFolder != "All") {
                        TextButton(onClick = { selectedFolder = selectedFolder.substringBeforeLast("/", "All").ifBlank { "All" } }) {
                            Text("‹ Parent", color = Color(0xFFFF8DBD))
                        }
                    }
                }
                if (childFolders.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 190.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(childFolders, key = { it }) { path ->
                            Column(Modifier.fillMaxWidth().clickable { selectedFolder = path }) {
                                Row(Modifier.padding(horizontal = 5.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("📁", fontSize = 17.sp)
                                    Spacer(Modifier.width(11.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(path.substringAfterLast("/"), color = pageText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = keepFontFamily(folderFont))
                                        if (showFolderPath) Text(path, color = pageMuted, fontSize = 10.sp)
                                    }
                                    Text("›", color = Color(0xFFFF4F9A), fontSize = 22.sp)
                                }
                                Divider(color = Color(0x22FF4F9A), thickness = 0.7.dp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your collection", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${visible.size} items", color = Color(0xFFB68A9F), fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("♪", fontSize = 42.sp, color = Color(0xFFFF4F9A))
                            Text(if (query.isEmpty()) "No lyrics yet" else "No matching lyrics", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tap + to save your first lyric", color = Color(0xFFB68A9F), fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(visible, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = { if (item.audioPath.isNotBlank()) startTrack(item) else preview = item },
                                    onLongClick = { actionLyric = item }
                                ),
                                shape = RoundedCornerShape(20.dp), color = Color(0xFF261720),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF4F9A))
                            ) {
                                Column(Modifier.padding(17.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = keepFontFamily(nameFont), modifier = Modifier.weight(1f))
                                        Text(item.folder, color = Color(0xFFFF8DBD), fontSize = 11.sp, fontFamily = keepFontFamily(folderFont))
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(item.body, color = Color(0xFFE4C8D6), maxLines = 3, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = keepFontFamily(lyricsFont))
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 176.dp)) {
                FloatingActionButton(onClick = { showCreateMenu = true },
                    containerColor = Color(0xFFFF4F9A), contentColor = Color(0xFF2A1020),
                    shape = RoundedCornerShape(19.dp)) { Text("+", fontSize = 29.sp, fontWeight = FontWeight.Light) }
                DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                    DropdownMenuItem(text = { Text("Create Folder") }, leadingIcon = { Text("📁") }, onClick = {
                        parentFolder = if (selectedFolder == "All") "Root" else selectedFolder
                        newFolder = ""; showCreateMenu = false; showFolders = true
                    })
                    DropdownMenuItem(text = { Text("Create Lyrics File") }, leadingIcon = { Text("♫") }, onClick = {
                        editing = null; title = ""; body = ""; coverPath = ""; audioPath = ""
                        folder = if (selectedFolder == "All") "General" else selectedFolder
                        showCreateMenu = false; showEditor = true
                    })
                }
            }
            } else if (activeTab == 3) {
                LyricsKeepSettingsScreen(
                    darkMode = darkMode, onDarkModeChange = { darkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() },
                    compactCards = compactCards, onCompactCardsChange = { compactCards = it; prefs.edit().putBoolean("compact_cards", it).apply() },
                    showFolderPath = showFolderPath, onShowFolderPathChange = { showFolderPath = it; prefs.edit().putBoolean("show_folder_path", it).apply() },
                    keepScreenOn = keepScreenOn, onKeepScreenOnChange = { keepScreenOn = it; prefs.edit().putBoolean("keep_screen_on", it).apply() },
                    autoBackup = autoBackup, onAutoBackupChange = { autoBackup = it; prefs.edit().putBoolean("auto_backup", it).apply() },
                    fontScale = fontScale, onFontScaleChange = { fontScale = it; prefs.edit().putFloat("font_scale", it).apply() },
                    nameFont = nameFont, onNameFontChange = { nameFont = it; prefs.edit().putString("name_font", it).apply() },
                    lyricsFont = lyricsFont, onLyricsFontChange = { lyricsFont = it; prefs.edit().putString("lyrics_font", it).apply() },
                    folderFont = folderFont, onFolderFontChange = { folderFont = it; prefs.edit().putString("folder_font", it).apply() },
                    defaultSort = defaultSort, onDefaultSortChange = { defaultSort = it; prefs.edit().putString("sort_order", it).apply() },
                    defaultFolder = defaultFolder, onDefaultFolderChange = { defaultFolder = it; prefs.edit().putString("default_folder", it).apply() }
                )
            } else if (activeTab == 1) {
                LyricsKeepDiaryNotesScreen()
            } else {
                LyricsKeepPlaceholderScreen(title = "Later", subtitle = "A space for upcoming LyricsKeep features.")
            }
            LyricsKeepBottomBar(activeTab = activeTab, onTabSelected = { activeTab = it })
        }


        if (preview == null && currentTrackId != -1L && durationMs > 0) {
            val playingItem = lyrics.firstOrNull { it.id == currentTrackId }
            Box(Modifier.fillMaxSize()) {
            Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp, start = 14.dp, end = 14.dp).fillMaxWidth()
                .clickable { playingItem?.let { preview = it } },
                color = Color(0xFF281720), shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x77FF4F9A)), shadowElevation = 12.dp) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(playingItem?.title ?: "LyricsKeep Player", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(if (isPlaying) "Playing in background" else "Paused", color = Color(0xFFFF8DBD), fontSize = 11.sp)
                    }
                    IconButton(onClick = { context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_PREVIOUS)) }) { Text("◀", color = Color.White) }
                    IconButton(onClick = { context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_TOGGLE)) }) { Text(if (isPlaying) "Ⅱ" else "▶", color = Color(0xFFFF4F9A), fontSize = 20.sp) }
                    IconButton(onClick = { context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_NEXT)) }) { Text("▶", color = Color.White) }
                }
            }
            }
        }
        if (preview != null) {
            val item = preview!!
            Surface(Modifier.fillMaxSize(), color = Color(0xFF100A0E)) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.clickable { handleBack() }, color = Color(0xCC351B2A), shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x77FF4F9A))) {
                            Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("‹", color = Color(0xFFFF8DBD), fontSize = 22.sp)
                                Spacer(Modifier.width(5.dp)); Text("Back", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(Modifier.clickable {
                            editing = item; title = item.title; body = item.body; folder = item.folder
                            coverPath = item.coverPath; audioPath = item.audioPath
                            preview = null; showEditor = true
                        }, color = Color(0xCC351B2A), shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x77FF4F9A))) {
                            Text("✎  Edit", Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                color = Color(0xFFFF8DBD), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = keepFontFamily(nameFont))
                        Text("📁 " + item.folder, color = Color(0xFFB68A9F), fontSize = 13.sp, fontFamily = keepFontFamily(folderFont), modifier = Modifier.padding(8.dp))
                        Divider(Modifier.padding(vertical = 12.dp))
                        Text(item.body, color = Color(0xFFE4ECE6), fontSize = 21.sp, lineHeight = 38.sp, textAlign = TextAlign.Center, fontFamily = keepFontFamily(lyricsFont), modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp))
                    }
                    if (item.audioPath.isNotBlank()) {
                        val playerCover = remember(item.coverPath) {
                            if (item.coverPath.isNotBlank()) runCatching {
                                BitmapFactory.decodeFile(item.coverPath)?.asImageBitmap()
                            }.getOrNull() else null
                        }
                        Surface(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF281720),
                            shadowElevation = 10.dp
                        ) {
                            Box(Modifier.fillMaxWidth().heightIn(min = 172.dp).clip(RoundedCornerShape(24.dp))) {
                                // The cover becomes a soft, blurred glass backdrop instead of a large image above the reader.
                                playerCover?.let { cover ->
                                    Image(
                                        bitmap = cover,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(210.dp).blur(26.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(Modifier.matchParentSize().background(Color(0xD91A1016)))
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Slider(value = currentMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1).toFloat()), onValueChange = { currentMs = it.toInt() }, onValueChangeFinished = {
                                        context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_SEEK).putExtra(LyricsPlaybackService.EXTRA_POSITION, currentMs))
                                    }, valueRange = 0f..durationMs.coerceAtLeast(1).toFloat())
                                    Row {
                                        Text("%d:%02d".format(currentMs/60000,(currentMs/1000)%60), color = Color.White, fontSize = 11.sp)
                                        Spacer(Modifier.weight(1f))
                                        Text("%d:%02d".format(durationMs/60000,(durationMs/1000)%60), color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_PREVIOUS)) }) { Text("◀◀", color = Color.White) }
                                        Button(onClick = { if (currentTrackId != item.id) startTrack(item) else context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_TOGGLE)) }) {
                                            Text(if (isPlaying) "Ⅱ" else "▶", fontSize = 21.sp)
                                        }
                                        TextButton(onClick = { context.startService(Intent(context, LyricsPlaybackService::class.java).setAction(LyricsPlaybackService.ACTION_NEXT)) }) { Text("▶▶", color = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (actionLyric != null) {
            val chosen = actionLyric!!
            AlertDialog(onDismissRequest = { actionLyric = null }, title = { Text(chosen.title) },
                text = { Text("Choose an action for this lyrics file.") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            editing = chosen; title = chosen.title; body = chosen.body; folder = chosen.folder
                            coverPath = chosen.coverPath; audioPath = chosen.audioPath
                            actionLyric = null; showEditor = true
                        }) { Text("Edit", color = Color(0xFFFF4F9A)) }
                        TextButton(onClick = { save(lyrics.filterNot { it.id == chosen.id }); actionLyric = null }) {
                            Text("Delete", color = Color(0xFFFF7185))
                        }
                    }
                },
                dismissButton = { TextButton(onClick = {
                    if (chosen.audioPath.isNotBlank()) startTrack(chosen) else preview = chosen
                    actionLyric = null
                }) { Text("Open") } }
            )
        }
        if (showEditor) {
            AlertDialog(
                onDismissRequest = { handleBack() },
                title = { Text(if (editing == null) "New lyric" else "Edit lyric") },
                text = {
                    Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Lyrics name") }, singleLine = true)
                        OutlinedTextField(folder, { folder = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Save in folder") }, singleLine = true, supportingText = { Text("Example: Songs/Old songs") })
                        OutlinedTextField(body, { body = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Write lyrics here") }, minLines = 6)
                        Text("THUMBNAIL IMAGE", color = Color(0xFFFF4F9A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (coverPath.isNotBlank()) {
                            val thumbnail = remember(coverPath) { runCatching { BitmapFactory.decodeFile(coverPath)?.asImageBitmap() }.getOrNull() }
                            thumbnail?.let { Image(it, contentDescription = "Selected thumbnail", modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) }
                        }
                        OutlinedButton(onClick = { coverPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text(if (coverPath.isBlank()) "＋ Add thumbnail from Gallery" else "✓ Thumbnail added • Change image") }
                        Text("MP3 SONG", color = Color(0xFFFF4F9A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(if (audioPath.isBlank()) "No MP3 selected" else "♫ " + File(audioPath).name, color = Color(0xFFE4C8D6), fontSize = 12.sp)
                        OutlinedButton(onClick = { audioPicker.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) { Text(if (audioPath.isBlank()) "＋ Add MP3 from device" else "✓ MP3 added • Change song") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (title.isNotBlank() && body.isNotBlank()) {
                            val item = Lyric(editing?.id ?: System.currentTimeMillis(), title.trim(), body.trim(), folder.trim().ifBlank { "General" }, coverPath, audioPath)
                            val parts = item.folder.split("/").filter { it.isNotBlank() }
                            persistFolders(folders + parts.indices.map { i -> parts.take(i + 1).joinToString("/") })
                            save(if (editing == null) listOf(item) + lyrics else lyrics.map { if (it.id == item.id) item else it })
                            showEditor = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        if (editing != null) TextButton(onClick = { save(lyrics.filterNot { it.id == editing!!.id }); showEditor = false }) { Text("Delete", color = Color(0xFFFF8B9A)) }
                        TextButton(onClick = { showEditor = false }) { Text("Cancel") }
                    }
                }
            )
        }
        if (showFolders) {
            AlertDialog(onDismissRequest = { handleBack() }, title = { Text("Folder & Subfolder manager") },
                text = { Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    Text("Create a main folder or choose a parent to create a subfolder.", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(newFolder, { newFolder = it }, label = { Text("New folder name") }, singleLine = true)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(parentFolder, {}, readOnly = true, label = { Text("Parent folder") },
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Root folder") }, onClick = { parentFolder = "Root"; expanded = false })
                            folders.forEach { f -> DropdownMenuItem(text = { Text(f) }, onClick = { parentFolder = f; expanded = false }) }
                        }
                    }
                    Button(onClick = {
                        val name = newFolder.trim().trim('/')
                        if (name.isNotBlank()) {
                            val path = if (parentFolder == "Root") name else "$parentFolder/$name"
                            persistFolders(folders + path); selectedFolder = path; newFolder = ""; parentFolder = "Root"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Create folder") }
                    Divider()
                    Text("Folders", fontWeight = FontWeight.Bold)
                    folders.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f, color = Color.White, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            TextButton(onClick = { selectedFolder = f; showFolders = false }) { Text("Open") }
                            TextButton(onClick = {
                                save(lyrics.map { if (it.folder == f || it.folder.startsWith("$f/")) it.copy(folder = "General") else it })
                                persistFolders(folders.filterNot { it == f || it.startsWith("$f/") } + "General")
                            }) { Text("Delete") }
                        }
                    }
                } },
                confirmButton = { TextButton(onClick = { showFolders = false }) { Text("Done") } })
        }

    }
}


private fun keepFontFamily(label: String): FontFamily = when (label) {
    "Serif" -> FontFamily.Serif
    "Monospace" -> FontFamily.Monospace
    "Cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}
