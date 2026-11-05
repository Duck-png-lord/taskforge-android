package dev.taskforge.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/** Plain local-first app state. Attachments are SAF URI references; file bytes stay where the user picked them. */
data class ForgeTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val project: String = "Personal",
    val list: String = "Inbox",
    val priority: String = "Normal",
    val due: String = "",
    val reminder: Boolean = false,
    val recurrence: String = "Never",
    val tags: List<String> = emptyList(),
    val subtasks: List<String> = emptyList(),
    val done: Boolean = false
)
data class ForgeNote(val id: String = UUID.randomUUID().toString(), val title: String, val body: String)
data class ForgeSnippet(val id: String = UUID.randomUUID().toString(), val title: String, val language: String, val body: String)
data class ForgeFile(val name: String, val uri: String)
data class ForgeState(
    val tasks: List<ForgeTask> = emptyList(),
    val notes: List<ForgeNote> = emptyList(),
    val snippets: List<ForgeSnippet> = emptyList(),
    val checklist: List<String> = emptyList(),
    val attachments: List<ForgeFile> = emptyList(),
    val projects: List<String> = listOf("Personal", "Studio"),
    val tags: List<String> = listOf("focus", "home", "work")
)

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("taskforge-local", Context.MODE_PRIVATE)
    private val gson = Gson()
    fun load(): ForgeState = runCatching {
        gson.fromJson<ForgeState>(prefs.getString("state", null), object : TypeToken<ForgeState>() {}.type)
    }.getOrNull() ?: sampleState()
    fun save(state: ForgeState) { prefs.edit().putString("state", gson.toJson(state)).apply() }
    fun encode(state: ForgeState): String = gson.toJson(state)
    fun decode(text: String): ForgeState = gson.fromJson(text, ForgeState::class.java)

    private fun sampleState(): ForgeState = ForgeState(
        tasks = listOf(
            ForgeTask(title = "Plan the week", priority = "High", due = "2026-11-06", tags = listOf("focus")),
            ForgeTask(title = "Review project notes", project = "Studio", priority = "Normal", tags = listOf("work")),
            ForgeTask(title = "Try the HTML → JSX converter", project = "Studio", priority = "Low", done = true)
        ),
        notes = listOf(ForgeNote(title = "Welcome to TaskForge", body = "Your workspace is stored on this device. Try adding a task or exploring the developer tools.")),
        snippets = listOf(ForgeSnippet(title = "Kotlin greeting", language = "Kotlin", body = "fun greet(name: String) = \"Hello, $name!\"")),
        checklist = listOf("Set up my workspace", "Explore a tool")
    )
}
