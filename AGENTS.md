# Codex Instructions — Tender Radar

## Core rules
- Read `PROJECT_CONTEXT.md` first.
- Work in this repository's existing Java/Android architecture. Inspect actual code before assuming a method, dependency, or feature exists.
- **Do not alter working GeM scanning, filtering, deduplication, AI assessment, shortlist persistence, Word export/history, or search logic** when the request is UI-only.
- User approved the current ColorOS-inspired light/dark UI. Preserve it unless explicitly instructed otherwise.
- Make minimal targeted changes. Avoid unrelated refactors, new features, and unsolicited redesigns.
- Maintain backward compatibility with existing app preferences and saved tender data.
- No secrets, signing keystores, API keys, or credentials in commits or logs.

## Validation
- Run or trigger `gradle :app:assembleDebug` through the existing GitHub Actions workflow (user may have no PC).
- Verify the latest workflow conclusion and APK artifact; do not say “build succeeded” without evidence.
- Debug APK should build on normal pushes. Signed release is optional and should not block debug builds.
- For layout updates, verify both automatic light and dark themes, narrow Android screens, dialog scrolling, and that live scan-status text does not make the page jump.
- Keep UI controls wired to existing actions and maintain accessible labels and touch targets.

## Collaboration
- User prefers direct implementation in GitHub rather than instructions to run commands on a PC.
- Explain exactly what changed, which files, commit SHA, whether the APK build passed, and where to get the artifact.
- When something is unverified, say so. Ask only for missing essential details.
- Do not change project scope or treat conceptual future integrations as already working.
