package dev.taskforge.app

import org.json.JSONArray
import org.json.JSONObject

/** Deliberately conservative, dependency-free converters. Unsupported syntax is reported instead of guessed. */
object Converters {
    fun convert(kind: String, source: String): String = when (kind) {
        "HTML → JSX" -> htmlToJsx(source)
        "HTML → Markdown" -> htmlToMarkdown(source)
        "JSON → YAML" -> jsonToYaml(source)
        "Java → Kotlin" -> javaToKotlin(source)
        "HTML → C#" -> error("HTML → C# is intentionally unsupported: there is no single reliable target. Choose an explicit ASP.NET Razor view or C# string-escaping format in a future version.")
        else -> error("This conversion is not supported. Supported conversions: HTML → JSX, HTML → Markdown, JSON → YAML, Java → Kotlin.")
    }

    private fun htmlToJsx(input: String): String {
        require(input.isNotBlank()) { "Enter some HTML first." }
        var out = input.trim()
        val replacements = listOf("class" to "className", "for" to "htmlFor", "tabindex" to "tabIndex", "readonly" to "readOnly")
        replacements.forEach { (from, to) -> out = Regex("\\b$from(?=\\s*=)").replace(out, to) }
        out = Regex("<(img|br|hr|input|meta|link|area|base|embed|source|track|wbr)(\\s[^<>]*?)?(?<!/)>", RegexOption.IGNORE_CASE)
            .replace(out) { "<${it.groupValues[1]}${it.groupValues[2]}/ >".replace("/ >", "/>") }
        out = out.replace(Regex("<!--([\\s\\S]*?)-->")) { "{/*${it.groupValues[1]}*/}" }
        return "// JSX markup only; expressions, event handlers, and complex attributes may need manual review.\n<>\n${out.lines().joinToString("\n") { "  $it" }}\n</>"
    }

    private fun htmlToMarkdown(input: String): String {
        require(input.isNotBlank()) { "Enter some HTML first." }
        var s = input.replace(Regex("(?is)<(script|style)[^>]*>[\\s\\S]*?</\\1>"), "")
        s = Regex("(?is)<h([1-6])[^>]*>(.*?)</h\\1>").replace(s) { "${"#".repeat(it.groupValues[1].toInt())} ${strip(it.groupValues[2])}\n\n" }
        s = Regex("(?is)<(strong|b)[^>]*>(.*?)</\\1>").replace(s) { "**${strip(it.groupValues[2])}**" }
        s = Regex("(?is)<(em|i)[^>]*>(.*?)</\\1>").replace(s) { "*${strip(it.groupValues[2])}*" }
        s = Regex("(?is)<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>").replace(s) { "[${strip(it.groupValues[2])}](${it.groupValues[1]})" }
        s = Regex("(?is)<li[^>]*>(.*?)</li>").replace(s) { "- ${strip(it.groupValues[1])}\n" }
        s = Regex("(?is)<br\\s*/?>|</p>|</div>|</ul>|</ol>").replace(s, "\n\n")
        s = Regex("(?is)<p[^>]*>|<div[^>]*>|<ul[^>]*>|<ol[^>]*>").replace(s, "")
        return strip(s).replace(Regex("\\n{3,}"), "\n\n").trim()
    }
    private fun strip(s: String) = s.replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")

    private fun jsonToYaml(input: String): String {
        val root = org.json.JSONTokener(input).nextValue()
        return yamlValue(root, 0).trimEnd()
    }
    private fun yamlValue(v: Any?, indent: Int): String {
        val pad = " ".repeat(indent)
        return when (v) {
            is JSONObject -> v.keys().asSequence().joinToString("\n") { key ->
                val value = v.get(key)
                if (value is JSONObject || value is JSONArray) "$pad${yamlKey(key)}:\n${yamlValue(value, indent + 2)}"
                else "$pad${yamlKey(key)}: ${yamlScalar(value)}"
            }
            is JSONArray -> (0 until v.length()).joinToString("\n") { i ->
                val value = v.get(i)
                if (value is JSONObject || value is JSONArray) "$pad-\n${yamlValue(value, indent + 2)}" else "$pad- ${yamlScalar(value)}"
            }
            else -> "$pad${yamlScalar(v)}"
        }
    }
    private fun yamlKey(s: String) = if (s.matches(Regex("[A-Za-z_][A-Za-z0-9_-]*"))) s else "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    private fun yamlScalar(v: Any?): String = when (v) {
        null, JSONObject.NULL -> "null"
        is Number, is Boolean -> v.toString()
        else -> "\"${v.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
    }

    private fun javaToKotlin(input: String): String {
        require(input.isNotBlank()) { "Enter Java source first." }
        val unsupported = listOf("interface ", "enum ", "try (", "switch (", " synchronized ", "native ", "record ", "extends ", "implements ")
        if (unsupported.any { input.contains(it) }) error("This source uses Java features not handled by the safe basic converter. Conversion was not attempted; use a dedicated Java-to-Kotlin IDE conversion for this file.")
        var s = input.replace(Regex("(?m)^\\s*package\\s+[\\w.]+;\\s*"), "")
            .replace(Regex("(?m)^\\s*import\\s+([\\w.*]+);"), "import $1")
            .replace(Regex("\\bpublic\\s+class\\s+(\\w+)"), "class $1")
            .replace(Regex("\\bclass\\s+(\\w+)\\s*\\{\\s*public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\[\\]\\s+(\\w+)\\s*\\)\\s*\\{"), "class $1 {\n    companion object {\n        @JvmStatic fun main($2: Array<String>) {")
            .replace(Regex("\\bSystem\\.out\\.println\\("), "println(")
            .replace(Regex("\\bSystem\\.out\\.print\\("), "print(")
            .replace(Regex("\\b(boolean|int|long|double|float|String)\\s+(\\w+)\\s*=\\s*([^;]+);")) { m ->
                val type = mapOf("boolean" to "Boolean", "int" to "Int", "long" to "Long", "double" to "Double", "float" to "Float", "String" to "String")[m.groupValues[1]]
                "val ${m.groupValues[2]}: $type = ${m.groupValues[3]}"
            }
            .replace(Regex("(?m);\\s*$"), "")
        return "// BEST-EFFORT OUTPUT: review and compile in a Java/Kotlin IDE. Not a general Java parser.\n$s"
    }
}
