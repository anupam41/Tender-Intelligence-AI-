# Tender Intelligence AI

Native Android prototype with **no Termux dependency**. Java, Android SDK and Gradle.

## Features in v0.1
- Import selectable PDF text (Android PdfRenderer cannot extract text, so this version supports **TXT import**; PDF analysis will be added with a proper extractor).
- Save local tender notes with keyword scoring and optional deadlines.
- OpenRouter API key entered at runtime; fetch available models and choose one; analyze tender text using the chosen model.
- Export locally saved tender data as CSV.

**Not yet implemented:** live GeM/CPPP/IREPS crawling, automatic document download, PDF text extraction/OCR, and background alerts. OpenRouter analyzes submitted text; it does not supply tender portal data.

## Android-only APK build
Go to GitHub Actions > Build Android APK > Run workflow, or push a commit to main. Download the `TenderIntelligence-debug-apk` artifact, unzip and install `app-debug.apk`.

Never commit API keys. The key is held in memory only and is sent to OpenRouter over HTTPS when requested. Model usage may incur charges.
