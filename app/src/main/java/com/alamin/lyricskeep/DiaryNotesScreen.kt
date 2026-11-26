package com.alamin.lyricskeep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class KeepEntry(val id: Long, val type: String, val title: String, val body: String, val created: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsKeepDiaryNotesScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lyrics_keep_journal", 0) }
    fun load(): List<KeepEntry> = runCatching {
        val a = JSONArray(prefs.getString("entries", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            KeepEntry(o.optLong("id"), o.optString("type", "Diary"), o.optString("title"), o.optString("body"), o.optLong("created"))
        }.sortedByDescending { it.created }
    }.getOrDefault(emptyList())
    var entries by remember { mutableStateOf(load()) }
    var selected by remember { mutableStateOf("Diary") }
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<KeepEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var actionEntry by remember { mutableStateOf<KeepEntry?>(null) }
    var createMenu by remember { mutableStateOf(false) }
    val green = Color(0xFFDB2777)
    val muted = Color(0xFF946C7B)
    val filtered = entries.filter { it.type == selected && (it.title.contains(search, true) || it.body.contains(search, true)) }

    fun persist(list: List<KeepEntry>) {
        val a = JSONArray()
        list.forEach { e -> a.put(JSONObject().put("id", e.id).put("type", e.type).put("title", e.title).put("body", e.body).put("created", e.created)) }
        prefs.edit().putString("entries", a.toString()).apply()
        entries = list.sortedByDescending { it.created }
    }
    fun openNew(type: String) {
        selected = type; editing = null; title = ""; body = ""; showEditor = true
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(25.dp))
        Text("PERSONAL SPACE", color = green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Diary & Notes", color = Color(0xFF351B2A), fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text("Keep moments, thoughts and quick notes together.", color = muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf("Diary", "Notes").forEach { tab ->
                Surface(Modifier.weight(1f).clickable { selected = tab; search = "" },
                    shape = RoundedCornerShape(15.dp),
                    color = if (selected == tab) Color(0xFFFCEEF2) else Color.White,
                    border = BorderStroke(1.dp, if (selected == tab) green.copy(alpha = .65f) else Color(0xFFF3C6D4))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (tab == "Diary") "✎  Diary" else "▤  Notes", color = if (selected == tab) green else Color.White, fontWeight = FontWeight.Bold)
                        Text(if (tab == "Diary") "Daily entries" else "Quick notepad", color = muted, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search " + selected.lowercase() + "...") }, singleLine = true,
            shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green, unfocusedBorderColor = Color(0xFF29362D),
                focusedTextColor = Color(0xFF351B2A), unfocusedTextColor = Color(0xFF351B2A)))
        Spacer(Modifier.height(17.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (selected == "Diary") "Your diary" else "Your notes", color = Color(0xFF351B2A), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("${filtered.size} entries", color = muted, fontSize = 12.sp)
            }
            Box {
                FilledTonalButton(onClick = { createMenu = true }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3B29), contentColor = green)) { Text("+ Create") }
                DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                    DropdownMenuItem(text = { Text("Create Diary") }, onClick = { createMenu = false; openNew("Diary") })
                    DropdownMenuItem(text = { Text("Create Note") }, onClick = { createMenu = false; openNew("Notes") })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (selected == "Diary") "✎" else "▤", color = green, fontSize = 42.sp)
                    Text(if (search.isBlank()) "Nothing here yet" else "No matching entries", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (selected == "Diary") "Create a diary entry for today." else "Create a quick note to remember.", color = muted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    val stamp = remember(entry.created) { SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault()).format(Date(entry.created)) }
                    Surface(Modifier.fillMaxWidth().combinedClickable(onClick = {
                        editing = entry; title = entry.title; body = entry.body; showEditor = true
                    }, onLongClick = { actionEntry = entry }),
                        color = Color(0xFF151D18), shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF29362D))) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.title.ifBlank { if (selected == "Diary") "Untitled diary" else "Untitled note" },
                                    color = Color(0xFF351B2A), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Text(if (selected == "Diary") "DIARY" else "NOTE", color = green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(stamp, color = muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
                            Text(entry.body, color = Color(0xFF684354), fontSize = 13.sp, lineHeight = 19.sp, maxLines = 3)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        val isDiary = selected == "Diary"
        AlertDialog(onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) if (isDiary) "Create Diary" else "Create Note" else if (isDiary) "Edit Diary" else "Edit Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") }, placeholder = { Text(if (isDiary) "Today's title" else "Note title") }, singleLine = true)
                    if (isDiary) {
                        val now = remember { SimpleDateFormat("EEEE, dd MMM yyyy  •  hh:mm a", Locale.getDefault()).format(Date()) }
                        Text(now, color = muted, fontSize = 11.sp)
                    }
                    OutlinedTextField(value = body, onValueChange = { body = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        label = { Text(if (isDiary) "Diary" else "Write your note") }, minLines = 7)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() || body.isNotBlank()) {
                        val item = KeepEntry(editing?.id ?: System.currentTimeMillis(), selected, title.trim(), body.trim(), editing?.created ?: System.currentTimeMillis())
                        persist(if (editing == null) listOf(item) + entries else entries.map { if (it.id == item.id) item else it })
                    }
                    showEditor = false
                }) { Text("Save", color = green) }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } })
    }
    if (actionEntry != null) {
        val item = actionEntry!!
        AlertDialog(onDismissRequest = { actionEntry = null }, title = { Text(item.title) },
            text = { Text("Edit or delete this entry?") },
            confirmButton = {
                Row {
                    TextButton(onClick = { editing = item; selected = item.type; title = item.title; body = item.body; showEditor = true; actionEntry = null }) { Text("Edit", color = green) }
                    TextButton(onClick = { persist(entries.filterNot { it.id == item.id }); actionEntry = null }) { Text("Delete", color = Color(0xFFFF7185)) }
                }
            },
            dismissButton = { TextButton(onClick = { actionEntry = null }) { Text("Cancel") } })
    }
}
