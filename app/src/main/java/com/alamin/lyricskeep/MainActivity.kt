package com.alamin.lyricskeep

import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private data class Lyric(val id: Long, val title: String, val body: String, val folder: String)

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
            Lyric(o.optLong("id"), o.optString("title"), o.optString("body"), o.optString("folder"))
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
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) runCatching {
            val lj = JSONArray()
            lyrics.forEach { lj.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("folder", it.folder)) }
            val fj = JSONArray()
            folders.forEach { fj.put(it) }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(ZipEntry("lyrics.json"))
                    zip.write(lj.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
                    zip.putNextEntry(ZipEntry("folders.json"))
                    zip.write(fj.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            var lj = "[]"; var fj = "[]"
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val value = zip.readBytes().toString(Charsets.UTF_8)
                        if (entry.name == "lyrics.json") lj = value
                        if (entry.name == "folders.json") fj = value
                        zip.closeEntry()
                    }
                }
            }
            val a = JSONArray(lj)
            val restored = (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Lyric(o.optLong("id"), o.optString("title"), o.optString("body"), o.optString("folder", "General"))
            }
            val fa = JSONArray(fj)
            val restoredFolders = (0 until fa.length()).map { fa.getString(it) }
            save(restored)
            persistFolders(restoredFolders + restored.map { it.folder } + "General")
            selectedFolder = "All"
        }
    }
    var editing by remember { mutableStateOf<Lyric?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("General") }
    var showFolders by remember { mutableStateOf(false) }

    fun persistFolders(items: List<String>) {
        val a = JSONArray(); items.distinct().sorted().forEach { a.put(it) }
        prefs.edit().putString("folders", a.toString()).apply(); folders = items.distinct().sorted()
    }

    fun save(items: List<Lyric>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title)
                .put("body", item.body).put("folder", item.folder))
        }
        prefs.edit().putString("lyrics", array.toString()).apply()
        lyrics = items
        val paths = items.flatMap { l -> l.folder.split("/").filter { it.isNotBlank() }.indices.map { i -> l.folder.split("/").take(i + 1).joinToString("/") } }
        persistFolders(folders + paths)
    }

    val folderChoices = listOf("All") + folders
    val visible = lyrics.filter {
        (selectedFolder == "All" || it.folder == selectedFolder) &&
            (it.title.contains(query, true) || it.body.contains(query, true))
    }
    val bg = Brush.verticalGradient(listOf(Color(0xFF10152B), Color(0xFF171B35), Color(0xFF0B1020)))

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF9B8CFF), secondary = Color(0xFF67D9D0),
        background = Color(0xFF10152B), surface = Color(0xFF202743)
    )) {
        Box(Modifier.fillMaxSize().background(bg)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LYRICS KEEP", color = Color(0xFFB8ADFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("Your words, محفوظ.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("${lyrics.size} saved lyrics", color = Color(0xFF9AA5C4), fontSize = 13.sp)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF34345E)) {
                        Text("♪", Modifier.padding(horizontal = 17.dp, vertical = 10.dp), color = Color.White, fontSize = 25.sp)
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
                        focusedBorderColor = Color(0xFF9588FF), unfocusedBorderColor = Color(0xFF343D5C),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { backupLauncher.launch("LyricsKeep-Backup.zip") }) { Text("Backup") }
                    TextButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Restore") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FOLDERS", color = Color(0xFF9AA5C4), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showFolders = true }) { Text("+ Folder") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    folderChoices.take(5).forEach { name ->
                        FilterChip(selected = selectedFolder == name, onClick = { selectedFolder = name },
                            label = { Text(name) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your collection", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${visible.size} items", color = Color(0xFF9AA5C4), fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("♪", fontSize = 42.sp, color = Color(0xFF8F83E8))
                            Text(if (query.isEmpty()) "No lyrics yet" else "No matching lyrics", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tap + to save your first lyric", color = Color(0xFF9AA5C4), fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(visible, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { preview = item },
                                shape = RoundedCornerShape(20.dp), color = Color(0xCC252D4A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334F5C88))
                            ) {
                                Column(Modifier.padding(17.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        Text(item.folder, color = Color(0xFFB9AEFF), fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(item.body, color = Color(0xFFB8C0D8), maxLines = 3, fontSize = 13.sp, lineHeight = 19.sp)
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { editing = null; title = ""; body = ""; folder = if (selectedFolder == "All") "General" else selectedFolder; showEditor = true },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(22.dp),
                containerColor = Color(0xFF9184F5), contentColor = Color.White
            ) { Text("+", fontSize = 28.sp) }
        }

        if (preview != null) {
            val item = preview!!
            AlertDialog(onDismissRequest = { preview = null }, title = { Text(item.title) },
                text = { Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(item.folder, color = Color(0xFF9B8CFF), fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(item.body, color = Color.White, fontSize = 15.sp, lineHeight = 25.sp)
                } },
                confirmButton = { TextButton(onClick = {
                    editing = item; title = item.title; body = item.body; folder = item.folder
                    preview = null; showEditor = true
                }) { Text("Edit") } },
                dismissButton = { TextButton(onClick = { preview = null }) { Text("Close") } })
        }
        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text(if (editing == null) "New lyric" else "Edit lyric") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                        OutlinedTextField(folder, { folder = it }, label = { Text("Folder / Subfolder") }, singleLine = true, supportingText = { Text("Use / between levels, e.g. Songs/Old songs") })
                        OutlinedTextField(body, { body = it }, label = { Text("Lyrics") }, minLines = 5)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (title.isNotBlank() && body.isNotBlank()) {
                            val item = Lyric(editing?.id ?: System.currentTimeMillis(), title.trim(), body.trim(), folder.trim().ifBlank { "General" })
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
            AlertDialog(onDismissRequest = { showFolders = false }, title = { Text("Folder & Subfolder manager") },
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
