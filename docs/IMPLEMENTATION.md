# TaskForge implementation notes

## Privacy and execution model

- State is serialized locally with Gson in app-private SharedPreferences.
- Import/export and attachments use Android's Storage Access Framework; users choose each file.
- No internet permission, analytics, login, telemetry, or cloud service.
- Source text is never evaluated, compiled, or executed. Converter operations occur only on tapping **Convert**.
- The lo-fi button plays a generated offline loop; it fetches no audio.

## Build/JAR behavior

The Android app does not bundle a Java compiler. It must not report a successful source build. A proper future JAR exporter should use an explicit picker for `.class` files and resources, then write a ZIP/JAR using the selected entries; it should validate manifest paths and state clearly that it archives already compiled artifacts. This scaffold deliberately avoids producing a misleading empty JAR.

## Accessibility and desktop UX

Compose controls provide focus and keyboard activation through Android's accessibility/input system. Keep meaningful content descriptions on icon-only controls, visible focus indication, large enough targets, and test with TalkBack, keyboard-only navigation, mouse, touch and ChromeOS window resizing. The rail breakpoint is 760 dp.
