# Compatibility fixes for the legacy ChxUtility client

This contribution contains the source changes recovered from a locally patched Windows 1.0 installation. The patch was developed with assistance from DeepSeek Harness. ChxUtility.java and ChxGUI.java retain the recovered source layout, which accounts for the larger diff. The original upstream tree-node classes are retained, and ChxResource only gains the four fields needed by the patch.

## Changes

- Use HTTPS for legacy API requests and tolerate unavailable or non-JSON account-information endpoints.
- Recover account metadata from login responses and cookies; retry saved-account login when session checks fail.
- Parse Chinese resource descriptions without depending on the Windows default charset, with fallback extraction for malformed HTML.
- Handle missing attachment arrays and surface background loading failures instead of leaving the GUI on its loading message.
- Adapt video progress requests to the current web endpoints and attachment metadata, with temporary metadata caching and retries.
- Record UTF-8 diagnostic logs and show unexpected startup errors in the GUI.

## Build from source

Requires JDK 11 or later. From the repository root in PowerShell:

```powershell
$sources = Get-ChildItem src/net/vomega/chx -Filter *.java | ForEach-Object FullName
javac --release 11 -encoding UTF-8 -cp lib/gson/gson-2.8.6.jar -d bin @sources
java -cp "bin;lib/gson/gson-2.8.6.jar" net.vomega.chx.ChxGUI
```

On macOS/Linux, use `:` instead of `;` in the runtime classpath.

## Validation and limitations

The contributor reports that the patched client works on their Windows installation. Submission preparation separately verified compilation targeting Java 11 and offline checks of JSON error handling, embedded-object extraction, and serialization compatibility. No live account login or course progress was exercised during submission preparation.

Document/non-video tasks remain unsupported when the service no longer supplies a `jtoken`. Faster playback rates and compatibility across other accounts or courses have not been verified. The patch retains broad exception handlers and detailed diagnostic logging from the local repair; these may warrant further maintainer review.

Diagnostic files (`chx-debug.log` and `chx-debug-cards.html`) are written under the user's home directory and can contain account identifiers, course details, and temporary tokens. Inspect and redact them before sharing. Saved sessions, logs, real response examples, installed runtimes, and packaged binaries are excluded from this contribution.
