package com.alamin.lyricskeep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeepGreen = Color(0xFFDB2777)
private val KeepDarkCard = Color(0xFFFCEEF2)
private val KeepBorder = Color(0xFFF3C6D4)
private val KeepMuted = Color(0xFF946C7B)

@Composable
fun BoxScope.LyricsKeepBottomBar(activeTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Lyrics", Icons.Filled.LibraryMusic, 0),
        Triple("Diary & Notes", Icons.Filled.EditNote, 1),
        Triple("Later", Icons.Filled.Schedule, 2),
        Triple("Settings", Icons.Filled.Settings, 3)
    )
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp),
        color = Color.White, shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color(0xFFF3C6D4)), shadowElevation = 12.dp
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { (label, icon, index) ->
                val selected = activeTab == index
                Surface(Modifier.height(46.dp).clickable { onTabSelected(index) }, shape = RoundedCornerShape(24.dp), color = if (selected) Color(0xFFDB2777) else Color.Transparent) {
                    Row(Modifier.padding(horizontal = if (selected) 17.dp else 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(icon, contentDescription = label, tint = if (selected) Color.White else Color(0xFF946C7B), modifier = Modifier.size(21.dp))
                        if (selected) { Spacer(Modifier.width(8.dp)); Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsKeepPlaceholderScreen(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp).padding(top = 34.dp, bottom = 110.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.EditNote, contentDescription = null, tint = KeepGreen, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color(0xFF351B2A), fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = KeepMuted, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Surface(color = Color(0xFFFCEEF2), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, KeepBorder)) {
            Text("Coming soon", color = Color(0xFFDB2777), modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LyricsKeepSettingsScreen(
    darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit,
    compactCards: Boolean, onCompactCardsChange: (Boolean) -> Unit,
    showFolderPath: Boolean, onShowFolderPathChange: (Boolean) -> Unit,
    keepScreenOn: Boolean, onKeepScreenOnChange: (Boolean) -> Unit,
    autoBackup: Boolean, onAutoBackupChange: (Boolean) -> Unit,
    fontScale: Float, onFontScaleChange: (Float) -> Unit,
    nameFont: String, onNameFontChange: (String) -> Unit,
    lyricsFont: String, onLyricsFontChange: (String) -> Unit,
    folderFont: String, onFolderFontChange: (String) -> Unit,
    defaultSort: String, onDefaultSortChange: (String) -> Unit,
    defaultFolder: String, onDefaultFolderChange: (String) -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val titleColor = Color(0xFF351B2A)
    val muted = Color(0xFF946C7B)
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Text("Settings", color = titleColor, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Make Alamin Lyrics and Keeps yours", color = muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            SettingsSectionHeader("PAGE & BEHAVIOR", Icons.Filled.Tune, false)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleCard("Keep screen on", "Prevent screen sleep while reading", Icons.Filled.ScreenLockPortrait, keepScreenOn, onKeepScreenOnChange, false, Modifier.weight(1f))
                SettingsToggleCard("Folder path", "Show folder location on lyric cards", Icons.Filled.FolderOpen, showFolderPath, onShowFolderPathChange, darkMode, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleCard("Compact cards", "Show a tighter lyrics list", Icons.Filled.ViewCompact, compactCards, onCompactCardsChange, darkMode, Modifier.weight(1f))
                SettingsToggleCard("Auto backup", "Enable backup preference", Icons.Filled.CloudUpload, autoBackup, onAutoBackupChange, darkMode, Modifier.weight(1f))
            }
        }
        item {
            SettingsSectionHeader("APPEARANCE", Icons.Filled.Palette, false)
            SettingsValueCard("Reading size", "Adjust lyric text size", Icons.Filled.FormatSize, "${(fontScale * 100).toInt()}%", false, Modifier.fillMaxWidth()) { dialog = "font" }
            Spacer(Modifier.height(10.dp))
            SettingsValueCard("Name font", "Font for lyric titles and names", Icons.Filled.Title, nameFont, false, Modifier.fillMaxWidth()) { dialog = "nameFont" }
            Spacer(Modifier.height(10.dp))
            SettingsValueCard("Lyrics font", "Font for the lyrics text", Icons.Filled.TextFields, lyricsFont, darkMode, Modifier.fillMaxWidth()) { dialog = "lyricsFont" }
            Spacer(Modifier.height(10.dp))
            SettingsValueCard("Folder font", "Font for folder names and paths", Icons.Filled.Folder, folderFont, darkMode, Modifier.fillMaxWidth()) { dialog = "folderFont" }
        }
        item {
            SettingsSectionHeader("EDITOR & ENGINE", Icons.Filled.Tune, false)
            SettingsValueCard("Default sort", "Choose how lyrics are ordered", Icons.Filled.Sort, defaultSort, darkMode, Modifier.fillMaxWidth()) { dialog = "sort" }
            Spacer(Modifier.height(10.dp))
            SettingsValueCard("Default folder", "Choose where new lyrics are saved", Icons.Filled.Folder, defaultFolder, darkMode, Modifier.fillMaxWidth()) { dialog = "folder" }
            Spacer(Modifier.height(10.dp))
            Surface(color = Color(0xFFFCEEF2), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFF3C6D4))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.FolderZip, contentDescription = null, tint = Color(0xFFDB2777)); Spacer(Modifier.width(10.dp)); Column { Text("Backup & restore", color = Color(0xFF351B2A), fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Export or restore your lyrics library", color = Color(0xFF946C7B), fontSize = 11.sp) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onBackup, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777), contentColor = Color.White)) { Text("Backup") }
                        OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDB2777)), border = BorderStroke(1.dp, Color(0xFFF3C6D4))) { Text("Restore") }
                    }
                }
            }
        }
        item {
            Text("ALAMIN LYRICS AND KEEPS", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Your personal lyrics library.", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
    when (dialog) {
        "font" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Reading font size") }, text = {
            Column {
                Text("Current: ${(fontScale * 100).toInt()}%", color = titleColor)
                Slider(value = fontScale, onValueChange = onFontScaleChange, valueRange = 0.8f..1.5f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Small", color = muted); Text("Large", color = muted) }
            }
        }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("Done") } })
        "nameFont" -> ChoiceDialog("Name font", listOf("Sans Serif", "Serif", "Monospace", "Cursive"), nameFont, { onNameFontChange(it); dialog = null }) { dialog = null }
        "lyricsFont" -> ChoiceDialog("Lyrics font", listOf("Sans Serif", "Serif", "Monospace", "Cursive"), lyricsFont, { onLyricsFontChange(it); dialog = null }) { dialog = null }
        "folderFont" -> ChoiceDialog("Folder font", listOf("Sans Serif", "Serif", "Monospace", "Cursive"), folderFont, { onFolderFontChange(it); dialog = null }) { dialog = null }
        "sort" -> ChoiceDialog("Default sort", listOf("Recently added", "Title A–Z", "Title Z–A", "Folder"), defaultSort, { onDefaultSortChange(it); dialog = null }) { dialog = null }
        "folder" -> ChoiceDialog("Default folder", listOf("General", "Favorites", "Unsorted"), defaultFolder, { onDefaultFolderChange(it); dialog = null }) { dialog = null }
    }
}

@Composable
private fun SettingsSectionHeader(label: String, icon: ImageVector, dark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
        Icon(icon, contentDescription = null, tint = KeepGreen, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color(0xFF946C7B), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun SettingsToggleCard(title: String, description: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit, dark: Boolean, modifier: Modifier = Modifier) {
    val card = Color(0xFFFCEEF2)
    val text = Color(0xFF351B2A)
    val muted = Color(0xFF946C7B)
    Surface(modifier.heightIn(min = 150.dp).clickable { onCheckedChange(!checked) }, shape = RoundedCornerShape(16.dp), color = card, border = BorderStroke(1.dp, if (checked) Color(0x88FF4F9A) else Color(0xFFF3C6D4))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFE3EF)) {
                    Icon(icon, contentDescription = null, tint = if (checked) KeepGreen else muted, modifier = Modifier.padding(8.dp).size(17.dp))
                }
                Spacer(Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.height(30.dp), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFDB2777)))
            }
            Spacer(Modifier.height(9.dp))
            Text(title, color = text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(description, color = muted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsValueCard(title: String, description: String, icon: ImageVector, value: String, dark: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val card = if (dark) KeepDarkCard else Color.White
    val text = if (dark) Color.White else Color(0xFF172019)
    val muted = if (dark) KeepMuted else Color(0xFF806A76)
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = card, border = BorderStroke(1.dp, if (dark) KeepBorder else Color(0xFFFFC5DC))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp), color = Color(0xFFFFE3EF)) {
                Icon(icon, contentDescription = null, tint = KeepGreen, modifier = Modifier.padding(10.dp).size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, color = muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, color = KeepGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = "Open", tint = muted)
        }
    }
}

@Composable
private fun ChoiceDialog(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column {
            options.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == option, onClick = { onSelect(option) })
                    Spacer(Modifier.width(8.dp))
                    Text(option)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
