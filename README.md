# TaskForge

A local-first Android productivity and developer-toolkit starter built with Jetpack Compose. The UI adapts to narrow phone windows and wider ChromeOS/laptop-sized windows: wide layouts use a navigation rail and workspace summary; compact layouts use a horizontally scrolling tab bar. It supports light/dark themes, keyboard-friendly native controls, touch and mouse input, and resize-aware Compose layouts.

> **Build status:** This repository contains the Android source project, not a prebuilt APK. The project has not been built or device-tested in this environment. Java compilation and real JAR creation are intentionally not claimed: Android does not ship a Java compiler. Build with the documented Android toolchain and inspect the resulting APK on a Chromebook/emulator before distribution.

## Build an APK

Requirements: JDK 17, Android SDK Platform 35, Android Build Tools, and Gradle 8.9 (or a compatible Gradle installation). Android Studio can install/configure the SDK components.

```sh
gradle assembleDebug
```

The debug APK should be at `app/build/outputs/apk/debug/app-debug.apk`. For a signed release, configure a private signing config and run `gradle assembleRelease`; never commit signing keys. No Gradle wrapper is checked in yet, so use Android Studio or install Gradle 8.9. After the first successful build, you may generate a wrapper with `gradle wrapper --gradle-version 8.9` and commit its wrapper files.

## Features in this scaffold

- Local SharedPreferences-backed task, notes, snippets, checklist, project and attachment-reference data; sample data on first launch.
- Task priorities, due-date text, reminder metadata, recurrence selection, tags, subtasks, projects, search, filters, completion progress, board and date-grouped calendar views.
- JSON workspace export/import through Android's Storage Access Framework. Attachment selection stores a document URI rather than copying file contents; the app asks for no broad storage permission.
- Offline converters: HTML → JSX (basic attribute and void-element normalization), HTML → Markdown (common headings, emphasis, links and lists), JSON → YAML (JSON data model), and a conservative regex-based Java → Kotlin preview. Converters are deliberately not general-purpose parsers; review results. Unsupported Java constructs are rejected when recognized. Unsupported conversions report why.
- HTML → C# is not offered as a conversion: HTML has no single C# representation. A future version must explicitly select an output such as an ASP.NET Razor view or correctly escaped C# string.
- Java source editor placeholder and explicit export-JAR explanation. No Java compilation is performed. APK cannot compile Java into `.class` files without a separately provisioned compiler/toolchain. The visible JAR action does not claim success or generate an invalid archive; it explains this limitation.
- Notes, snippets, a simple checklist, focus timer, and 32 named lo-fi beat presets. The current player synthesizes a quiet procedural drum/chord loop offline; names are preset labels, not separate licensed recordings. No network or audio permission is used.
- Light/dark theme toggle, keyboard-operable standard Compose controls, semantic labels for key actions, empty states and error messages.

## Limitations and follow-up work

This is a functional starter, not a complete commercial task manager. Reminder is stored as metadata and does **not** schedule notifications yet. Calendar is a due-date list, not a month grid; board has To do/Done columns. Subtasks are stored as text entries and not independently trackable. Snippet/note editing and deletion, project/tag management, rich code-editor syntax highlighting, resource-aware Java project tree, and robust parsers are follow-up work. The Java editor is a plain monospace field and no code is run or compiled. JAR export requires actual compiled `.class` files; use a desktop JDK/Gradle/Maven build and archive the outputs with a standard JAR tool.

No network permission, account, analytics, or cloud sync is included. All app state is local; Android's system backup behavior is controlled by the OS/device settings. Export backups can contain private task and note content—store them carefully.

## Supported conversions and reliability

- **HTML → JSX:** small markup fragments, common attributes (`class`, `for`), comments, and common void tags. Does not parse JavaScript, guarantee valid JSX for arbitrary HTML, or convert inline event handlers/complex attributes.
- **HTML → Markdown:** common headings, bold/emphasis, links, lists, and basic block spacing. Not a complete HTML/Markdown implementation; tables, nested lists, entities, and unusual markup need manual review.
- **JSON → YAML:** JSON objects, arrays, strings, numbers, booleans, and null. Output is quoted conservatively. Confirm compatibility with your YAML consumer.
- **Java → Kotlin:** only a small, best-effort subset of simple classes, `main`, basic print calls and primitive/String variable declarations. It is not a compiler or full Java parser; manual review and compilation are required. Certain recognizable constructs are rejected rather than guessed.

## ChromeOS notes

Install the APK through Android Studio/ADB or the Chromebook's supported Android app installation workflow. Validate window resizing, keyboard navigation, mouse focus, display scaling and file-picker behavior on the target device. This project has not been certified for ChromeOS. No privileged permissions are requested; file access is user-driven via the system picker.
