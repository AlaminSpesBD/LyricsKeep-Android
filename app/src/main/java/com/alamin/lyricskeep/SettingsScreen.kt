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

private val KeepGreen = Color(0xFF1ED760)
private val KeepDarkCard = Color(0xFF151D18)
private val KeepBorder = Color(0xFF233027)
private val KeepMuted = Color(0xFF91A096)

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
        color = Color(0xFF151D18), shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color(0xFF2A382E)), shadowElevation = 18.dp
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { (label, icon, index) ->
                val selected = activeTab == index
                Surface(Modifier.height(46.dp).clickable { onTabSelected(index) }, shape = RoundedCornerShape(24.dp), color = if (selected) KeepGreen else Color.Transparent) {
                    Row(Modifier.padding(horizontal = if (selected) 17.dp else 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(icon, contentDescription = label, tint = if (selected) Color(0xFF07140B) else Color(0xFF9BA99F), modifier = Modifier.size(21.dp))
                        if (selected) { Spacer(Modifier.width(8.dp)); Text(label, color = Color(0xFF07140B), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
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
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = KeepMuted, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Surface(color = KeepDarkCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, KeepBorder)) {
            Text("Coming soon", color = KeepGreen, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold)
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
    defaultSort: String, onDefaultSortChange: (String) -> Unit,
    defaultFolder: String, onDefaultFolderChange: (String) -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val titleColor = if (darkMode) Color.White else Color(0xFF172019)
    val muted = if (darkMode) KeepMuted else Color(0xFF68766C)
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Text("Settings", color = titleColor, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Make LyricsKeep work your way", color = muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            SettingsSectionHeader("PAGE & BEHAVIOR", Icons.Filled.Tune, darkMode)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleCard("Keep screen on", "Prevent screen sleep while reading", Icons.Filled.ScreenLockPortrait, keepScreenOn, onKeepScreenOnChange, darkMode, Modifier.weight(1f))
                SettingsToggleCard("Folder path", "Show folder location on lyric cards", Icons.Filled.FolderOpen, showFolderPath, onShowFolderPathChange, darkMode, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleCard("Compact cards", "Show a tighter lyrics list", Icons.Filled.ViewCompact, compactCards, onCompactCardsChange, darkMode, Modifier.weight(1f))
                SettingsToggleCard("Auto backup", "Enable backup preference", Icons.Filled.CloudUpload, autoBackup, onAutoBackupChange, darkMode, Modifier.weight(1f))
            }
        }
        item {
            SettingsSectionHeader("APPEARANCE", Icons.Filled.Palette, darkMode)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleCard("Theme", "Switch between dark and light palettes", if (darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode, darkMode, onDarkModeChange, darkMode, Modifier.weight(1f))
                SettingsValueCard("Reading font", "Adjust lyric text size", Icons.Filled.FormatSize, "${(fontScale * 100).toInt()}%", darkMode, Modifier.weight(1f)) { dialog = "font" }
            }
        }
        item {
            SettingsSectionHeader("EDITOR & ENGINE", Icons.Filled.Tune, darkMode)
            SettingsValueCard("Default sort", "Choose how lyrics are ordered", Icons.Filled.Sort, defaultSort, darkMode, Modifier.fillMaxWidth()) { dialog = "sort" }
            Spacer(Modifier.height(10.dp))
            SettingsValueCard("Default folder", "Choose where new lyrics are saved", Icons.Filled.Folder, defaultFolder, darkMode, Modifier.fillMaxWidth()) { dialog = "folder" }
            Spacer(Modifier.height(10.dp))
            SettingsValueCard("Backup & restore", "Export or restore your lyrics library", Icons.Filled.FolderZip, "ZIP archive", darkMode, Modifier.fillMaxWidth()) { dialog = "backup" }
        }
        item {
            Text("LYRICSKEEP", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Your words, always close.", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
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
        "sort" -> ChoiceDialog("Default sort", listOf("Recently added", "Title A–Z", "Title Z–A", "Folder"), defaultSort, { onDefaultSortChange(it); dialog = null }) { dialog = null }
        "folder" -> ChoiceDialog("Default folder", listOf("General", "Favorites", "Unsorted"), defaultFolder, { onDefaultFolderChange(it); dialog = null }) { dialog = null }
        "backup" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Backup & restore") }, text = { Text("Use Backup and Restore from the Lyrics tab to export or import a ZIP archive.") }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("Got it") } })
    }
}

@Composable
private fun SettingsSectionHeader(label: String, icon: ImageVector, dark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
        Icon(icon, contentDescription = null, tint = KeepGreen, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (dark) Color(0xFF9AAC9F) else Color(0xFF617267), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun SettingsToggleCard(title: String, description: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit, dark: Boolean, modifier: Modifier = Modifier) {
    val card = if (dark) KeepDarkCard else Color.White
    val text = if (dark) Color.White else Color(0xFF172019)
    val muted = if (dark) KeepMuted else Color(0xFF68766C)
    Surface(modifier.heightIn(min = 150.dp).clickable { onCheckedChange(!checked) }, shape = RoundedCornerShape(16.dp), color = card, border = BorderStroke(1.dp, if (checked) Color(0x8855D982) else if (dark) KeepBorder else Color(0xFFDCE5DE)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = if (checked) Color(0x2222C55E) else if (dark) Color(0xFF202A23) else Color(0xFFEAF1EB)) {
                    Icon(icon, contentDescription = null, tint = if (checked) KeepGreen else muted, modifier = Modifier.padding(8.dp).size(17.dp))
                }
                Spacer(Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.height(30.dp), colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF07140B), checkedTrackColor = KeepGreen))
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
    val muted = if (dark) KeepMuted else Color(0xFF68766C)
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = card, border = BorderStroke(1.dp, if (dark) KeepBorder else Color(0xFFDCE5DE))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp), color = if (dark) Color(0xFF202A23) else Color(0xFFEAF1EB)) {
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
