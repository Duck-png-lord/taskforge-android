package dev.taskforge.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { TaskForgeApp() } }
}

private val navItems = listOf("Tasks", "Board", "Calendar", "Notes", "Snippets", "Checklist", "Converters", "Java", "Attachments", "Timer")

@Composable fun TaskForgeApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var state by remember { mutableStateOf(store.load()) }
    fun update(next: ForgeState) { state = next; store.save(next) }
    var page by remember { mutableStateOf("Tasks") }
    var dark by remember { mutableStateOf(false) }
    var welcome by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var lofi by remember { mutableStateOf(false) }
    val player = remember { LofiPlayer() }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? -> uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { w -> w.write(store.encode(state)) } } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> store.decode(r.readText()) } }.onSuccess(::update).onFailure { welcome = true } } }
    val colors = if (dark) darkColorScheme(primary = Color(0xFFB4C5FF), secondary = Color(0xFFB7DCCB), background = Color(0xFF111318), surface = Color(0xFF191B22)) else lightColorScheme(primary = Color(0xFF405A91), secondary = Color(0xFF426657), background = Color(0xFFF6F7FB), surface = Color.White)
    MaterialTheme(colorScheme = colors) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val wide = maxWidth >= 760.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    navItems.forEach { item -> NavigationRailItem(selected = page == item, onClick = { page = item }, icon = { Icon(iconFor(item), item) }, label = { Text(item) }, alwaysShowLabel = false) }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopAppBar(
                        title = { Column { Text("TaskForge", fontWeight = FontWeight.Bold); Text(page, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                        actions = {
                            IconButton(onClick = { lofi = player.toggle() }, modifier = Modifier.semanticsLabel(if (lofi) "Stop lo-fi beats" else "Play lo-fi beats")) { Icon(if (lofi) Icons.Default.Pause else Icons.Default.MusicNote, "Lo-fi beats") }
                            IconButton(onClick = { dark = !dark }) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Toggle theme") }
                            IconButton(onClick = { exportLauncher.launch("taskforge-backup.json") }) { Icon(Icons.Default.Upload, "Export backup") }
                            IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) { Icon(Icons.Default.Download, "Import backup") }
                        }
                    )
                    if (!wide) ScrollableTabRow(selectedTabIndex = navItems.indexOf(page), edgePadding = 8.dp) { navItems.forEach { item -> Tab(page == item, { page = item }, text = { Text(item) }) } }
                    when (page) {
                        "Tasks" -> TasksPage(state, search, { search = it }, filter, { filter = it }, { update(state.copy(tasks = state.tasks + it)) }, { task -> update(state.copy(tasks = state.tasks.map { if (it.id == task.id) task else it })) }, { task -> update(state.copy(tasks = state.tasks.filterNot { it.id == task.id })) })
                        "Board" -> BoardPage(state, { task -> update(state.copy(tasks = state.tasks.map { if (it.id == task.id) task else it })) })
                        "Calendar" -> CalendarPage(state)
                        "Notes" -> ContentPage("Notes", state.notes.map { "${it.title}\n${it.body}" }, "Add note") { title, body -> update(state.copy(notes = state.notes + ForgeNote(title = title, body = body))) }
                        "Snippets" -> ContentPage("Snippets", state.snippets.map { "${it.title} · ${it.language}\n${it.body}" }, "Add snippet") { title, body -> update(state.copy(snippets = state.snippets + ForgeSnippet(title = title, language = "Text", body = body))) }
                        "Checklist" -> ChecklistPage(state, { update(state.copy(checklist = state.checklist + it)) }, { update(state.copy(checklist = state.checklist - it)) })
                        "Converters" -> ConverterPage()
                        "Java" -> JavaPage()
                        "Attachments" -> AttachmentsPage(state, { uri ->
                            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            update(state.copy(attachments = state.attachments + ForgeFile(uri.lastPathSegment ?: "Attachment", uri.toString())))
                        }, { file -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(file.uri))) } })
                        else -> TimerPage()
                    }
                }
                if (wide) Column(Modifier.width(220.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    Text("WORKSPACE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp)); Text("${state.tasks.count { it.done }} of ${state.tasks.size} tasks complete", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { if (state.tasks.isEmpty()) 0f else state.tasks.count { it.done }.toFloat() / state.tasks.size }, Modifier.fillMaxWidth().padding(vertical = 10.dp))
                    Text("Projects", fontWeight = FontWeight.Bold); state.projects.forEach { Text("•  $it", Modifier.padding(vertical = 5.dp), style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.weight(1f)); Text("Saved on this device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (welcome) AlertDialog(onDismissRequest = { welcome = false }, title = { Text("TaskForge") }, text = { Text("Welcome! Your workspace stays on this device. This file could not be imported; make sure it is a TaskForge JSON backup.") }, confirmButton = { TextButton(onClick = { welcome = false }) { Text("Got it") } })
        }
    }
}

@Composable private fun Modifier.semanticsLabel(label: String) = this.then(Modifier.semantics { contentDescription = label })
@Composable private fun iconFor(page: String) = when (page) {
    "Tasks" -> Icons.Default.CheckCircle; "Board" -> Icons.Default.ViewKanban; "Calendar" -> Icons.Default.CalendarMonth; "Notes" -> Icons.Default.StickyNote2; "Snippets" -> Icons.Default.Code; "Checklist" -> Icons.Default.Checklist; "Converters" -> Icons.Default.Transform; "Java" -> Icons.Default.Terminal; "Attachments" -> Icons.Default.AttachFile; else -> Icons.Default.Timer
}

@Composable private fun TasksPage(state: ForgeState, search: String, onSearch: (String) -> Unit, filter: String, onFilter: (String) -> Unit, add: (ForgeTask) -> Unit, change: (ForgeTask) -> Unit, remove: (ForgeTask) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var project by remember { mutableStateOf("All") }
    val tasks = state.tasks.filter { (search.isBlank() || "${it.title} ${it.tags.joinToString()} ${it.project}".contains(search, true)) && (filter == "All" || if (filter == "Open") !it.done else it.priority == filter) && (project == "All" || it.project == project) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth().padding(top = 8.dp), placeholder = { Text("Search tasks and tags") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            var expanded by remember { mutableStateOf(false) }
            Box { TextButton(onClick = { expanded = true }) { Text("Project: $project") }; DropdownMenu(expanded, { expanded = false }) { (listOf("All") + state.projects).forEach { DropdownMenuItem(text = { Text(it) }, onClick = { project = it; expanded = false }) } } }
            var expandedFilter by remember { mutableStateOf(false) }
            Box { TextButton(onClick = { expandedFilter = true }) { Text("Filter: $filter") }; DropdownMenu(expandedFilter, { expandedFilter = false }) { listOf("All", "Open", "High", "Normal", "Low").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onFilter(it); expandedFilter = false }) } } }
            Spacer(Modifier.weight(1f)); FilledTonalButton(onClick = { dialog = true }) { Icon(Icons.Default.Add, null); Text("Task") }
        }
        if (tasks.isEmpty()) EmptyState("Nothing here yet", "Add a task or adjust your search and filters.", Icons.Default.TaskAlt)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(tasks, key = { it.id }) { task -> TaskCard(task, { change(task.copy(done = !task.done)) }, { remove(task) }) } }
    }
    if (dialog) TaskDialog(state.projects, { add(it); dialog = false }, { dialog = false })
}

@Composable private fun TaskCard(task: ForgeTask, toggle: () -> Unit, remove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(task.done, { toggle() }); Column(Modifier.weight(1f)) {
            Text(task.title, fontWeight = FontWeight.SemiBold, color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            Text(listOf(task.project, task.priority, task.due.takeIf { it.isNotBlank() }, task.recurrence.takeIf { it != "Never" }, task.tags.takeIf { it.isNotEmpty() }?.joinToString { "#$it" }).filterNotNull().joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (task.subtasks.isNotEmpty()) Text("${task.subtasks.size} subtasks", style = MaterialTheme.typography.labelSmall)
        }
        if (task.reminder) Icon(Icons.Default.NotificationsActive, "Reminder requested", tint = MaterialTheme.colorScheme.primary)
        IconButton(onClick = remove) { Icon(Icons.Default.DeleteOutline, "Delete ${task.title}") }
    } }
}

@Composable private fun TaskDialog(projects: List<String>, save: (ForgeTask) -> Unit, dismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }; var due by remember { mutableStateOf("") }; var tags by remember { mutableStateOf("") }; var priority by remember { mutableStateOf("Normal") }; var recurrence by remember { mutableStateOf("Never") }; var subtasks by remember { mutableStateOf("") }; var reminder by remember { mutableStateOf(false) }; var project by remember { mutableStateOf(projects.firstOrNull() ?: "Personal") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("New task") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Task title") }, singleLine = true)
        OutlinedTextField(due, { due = it }, label = { Text("Due date (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma separated") }, singleLine = true)
        OutlinedTextField(subtasks, { subtasks = it }, label = { Text("Subtasks, comma separated") })
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Priority"); listOf("Low", "Normal", "High").forEach { FilterChip(priority == it, { priority = it }, label = { Text(it) }, modifier = Modifier.padding(start = 4.dp)) } }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Repeat"); listOf("Never", "Daily", "Weekly", "Monthly").forEach { FilterChip(recurrence == it, { recurrence = it }, label = { Text(it) }, modifier = Modifier.padding(start = 3.dp)) } }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(reminder, { reminder = it }); Text("Reminder (saved as task metadata)") }
        var expanded by remember { mutableStateOf(false) }; Box { TextButton(onClick = { expanded = true }) { Text("Project: $project") }; DropdownMenu(expanded, { expanded = false }) { projects.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { project = it; expanded = false }) } } }
    } }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { save(ForgeTask(title = title.trim(), project = project, priority = priority, due = due.trim(), reminder = reminder, recurrence = recurrence, tags = tags.split(",").map(String::trim).filter(String::isNotEmpty), subtasks = subtasks.split(",").map(String::trim).filter(String::isNotEmpty))) }) { Text("Add") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable private fun BoardPage(state: ForgeState, change: (ForgeTask) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Move tasks between columns by changing status on the task list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(false to "To do", true to "Done").forEach { (done, label) -> Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); state.tasks.filter { it.done == done }.forEach { TaskCard(it, { change(it.copy(done = !it.done)) }, {}) } } } }
    }
}
@Composable private fun CalendarPage(state: ForgeState) {
    Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Due-date calendar", style = MaterialTheme.typography.headlineSmall); Text("Tasks with a date, grouped by date", style = MaterialTheme.typography.bodySmall); val dated = state.tasks.filter { it.due.isNotBlank() }.groupBy { it.due }.toSortedMap(); if (dated.isEmpty()) EmptyState("No dated tasks", "Add a due date to a task to see it here.", Icons.Default.CalendarMonth) else LazyColumn { dated.forEach { (date, tasks) -> item { Text(date, Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold) }; items(tasks) { Text("•  ${it.title} · ${it.priority}", Modifier.padding(8.dp)) } } } }
}

@Composable private fun ContentPage(title: String, entries: List<String>, action: String, add: (String, String) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.weight(1f)); FilledTonalButton(onClick = { dialog = true }) { Icon(Icons.Default.Add, null); Text(action) } }
        if (entries.isEmpty()) EmptyState("Your $title are ready", "Create your first one to keep useful information close by.", Icons.Default.NoteAdd) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(entries) { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) } } }
    }
    if (dialog) { var name by remember { mutableStateOf("") }; var body by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { dialog = false }, title = { Text(action) }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Title") }); OutlinedTextField(body, { body = it }, label = { Text("Content") }, minLines = 4) } }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) add(name, body); dialog = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancel") } }) }
}

@Composable private fun ChecklistPage(state: ForgeState, add: (String) -> Unit, remove: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Quick checklist", style = MaterialTheme.typography.headlineSmall); Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(input, { input = it }, Modifier.weight(1f), label = { Text("Checklist item") }); IconButton(onClick = { if (input.isNotBlank()) { add(input); input = "" } }) { Icon(Icons.Default.Add, "Add checklist item") } }; state.checklist.forEach { item -> Row(verticalAlignment = Alignment.CenterVertically) { var checked by remember(item) { mutableStateOf(false) }; Checkbox(checked, { checked = it }); Text(item, Modifier.weight(1f)); IconButton(onClick = { remove(item) }) { Icon(Icons.Default.Close, "Remove $item") } } } }
}

@Composable private fun ConverterPage() {
    val kinds = listOf("HTML → JSX", "HTML → Markdown", "JSON → YAML", "Java → Kotlin", "HTML → C#")
    var kind by remember { mutableStateOf(kinds.first()) }; var input by remember { mutableStateOf("") }; var output by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Code converters", style = MaterialTheme.typography.headlineSmall); Text("Conservative, local conversions. Always review generated code.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState())) { kinds.forEach { FilterChip(kind == it, { kind = it; output = ""; error = if (it == "HTML → C#") "Unsupported: choose a specific target format (for example, Razor view or escaped C# string)." else "" }, label = { Text(it) }, modifier = Modifier.padding(end = 6.dp)) } }
        if (kind == "HTML → C#") Text("Unsupported: HTML has no unambiguous C# target. TaskForge will not guess whether you want a Razor view, WebForms, or a C# string.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth().heightIn(min = 180.dp), label = { Text("Input code") }, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), minLines = 6)
        Button(onClick = { error = ""; output = runCatching { Converters.convert(kind, input) }.onFailure { error = it.message ?: "Conversion failed." }.getOrDefault("") }, enabled = kind != "HTML → C#", modifier = Modifier.padding(vertical = 10.dp)) { Icon(Icons.Default.Transform, null); Text("Convert") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        OutlinedTextField(output, {}, Modifier.fillMaxWidth().heightIn(min = 180.dp), label = { Text("Output") }, readOnly = true, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), minLines = 6)
        Text("Syntax colors are intentionally omitted for conversion output in this first build; monospace editing and conversion are available offline.", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun JavaPage() {
    var source by remember { mutableStateOf("public class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, TaskForge!\");\n    }\n}") }
    var output by remember { mutableStateOf("No build performed. Compilation is not available in the Android app.") }
    val context = LocalContext.current
    val jarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/java-archive")) { uri ->
        if (uri != null) output = "JAR export is only available for existing .class/resource files selected in the Java workspace. This source editor cannot compile Java. Select compiled files with the file picker in a future build; no compilation was attempted."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Java workspace", style = MaterialTheme.typography.headlineSmall)
        Text("Local editor scaffold · syntax-highlighted editor and Java compiler are not included. Editing never runs code.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(source, { source = it }, Modifier.fillMaxWidth().heightIn(min = 260.dp), label = { Text("Hello.java") }, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), minLines = 10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { output = runCatching { Converters.convert("Java → Kotlin", source) }.fold({ it }, { "Conversion not attempted: ${it.message}" }) }) { Text("Safe Java → Kotlin preview") }
            FilledTonalButton(onClick = { jarLauncher.launch("workspace.jar") }) { Icon(Icons.Default.Archive, null); Text("Export JAR…") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(14.dp)) { Text("Build status", fontWeight = FontWeight.Bold); Text(output); Text("Android/ChromeOS does not provide a Java compiler by default. A future JAR packager can archive user-selected .class files and resources, but cannot create .class files. Build Java on a desktop JDK/Gradle toolchain, then import compiled artifacts.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) } }
        Text("Source is never compiled or executed automatically. Use an explicit build action only in a future toolchain-enabled version.", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun AttachmentsPage(state: ForgeState, add: (Uri) -> Unit, open: (ForgeFile) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(add) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Attachments", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.weight(1f)); FilledTonalButton(onClick = { launcher.launch(arrayOf("*/*")) }) { Icon(Icons.Default.AttachFile, null); Text("Choose file") } }; Text("Files remain in their original location; TaskForge stores a local document reference.", style = MaterialTheme.typography.bodySmall)
        if (state.attachments.isEmpty()) EmptyState("No attachments yet", "Choose a file to add a private local reference.", Icons.Default.AttachFile) else state.attachments.forEach { Card(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { open(it) }) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.InsertDriveFile, null); Text(it.name, Modifier.padding(start = 10.dp)) } } }
    }
}

@Composable private fun TimerPage() {
    var seconds by remember { mutableIntStateOf(25 * 60) }; var active by remember { mutableStateOf(false) }
    LaunchedEffect(active) { while (active && seconds > 0) { delay(1000); seconds-- }; if (seconds == 0) active = false }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("Focus timer", style = MaterialTheme.typography.headlineSmall); Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayLarge); Row { Button(onClick = { active = !active }) { Text(if (active) "Pause" else "Start") }; TextButton(onClick = { active = false; seconds = 25 * 60 }) { Text("Reset") } }; Text("25-minute local focus session", style = MaterialTheme.typography.bodySmall) }
}

@Composable private fun EmptyState(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxWidth().padding(vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary); Text(title, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(body, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
