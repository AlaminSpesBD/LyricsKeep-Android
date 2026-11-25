package com.alamin.lyricskeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var editing by remember { mutableStateOf<Lyric?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("General") }
    var showFolders by remember { mutableStateOf(false) }

    fun save(items: List<Lyric>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title)
                .put("body", item.body).put("folder", item.folder))
        }
        prefs.edit().putString("lyrics", array.toString()).apply()
        lyrics = items
    }

    val folders = listOf("All") + lyrics.map { it.folder }.filter { it.isNotBlank() }.distinct().sorted()
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
                    Text("FOLDERS", color = Color(0xFF9AA5C4), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showFolders = true }) { Text("+ Folder") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    folders.take(4).forEach { name ->
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
                                modifier = Modifier.fillMaxWidth().clickable {
                                    editing = item; title = item.title; body = item.body; folder = item.folder; showEditor = true
                                },
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

        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text(if (editing == null) "New lyric" else "Edit lyric") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                        OutlinedTextField(folder, { folder = it }, label = { Text("Folder") }, singleLine = true)
                        OutlinedTextField(body, { body = it }, label = { Text("Lyrics") }, minLines = 5)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (title.isNotBlank() && body.isNotBlank()) {
                            val item = Lyric(editing?.id ?: System.currentTimeMillis(), title.trim(), body.trim(), folder.trim().ifBlank { "General" })
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
            var newFolder by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showFolders = false },
                title = { Text("Create folder") },
                text = { OutlinedTextField(newFolder, { newFolder = it }, label = { Text("Folder name") }, singleLine = true) },
                confirmButton = { TextButton(onClick = {
                    if (newFolder.isNotBlank()) selectedFolder = newFolder.trim()
                    showFolders = false
                }) { Text("Done") } },
                dismissButton = { TextButton(onClick = { showFolders = false }) { Text("Cancel") } }
            )
        }
    }
}
