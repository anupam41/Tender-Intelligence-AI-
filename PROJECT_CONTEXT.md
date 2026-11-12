# Tender Radar — Codex Project Handoff
Updated: 2026-11-12

## Project and repository
- App: **Tender Radar**, Android industrial/government tender discovery and management.
- GitHub: https://github.com/anupam41/Tender-Intelligence-AI-
- Android Java app. Main UI and GeM scanning implementation are largely in `app/src/main/java/com/tenderintelligence/ai/GemBrowserActivity.java`.
- User develops and tests **on Android phone only**, using GitHub Actions to produce installable APK artifacts.
- Priority: preserve working features and scanner/search/AI logic. Current UI design has been approved/locked; do not redesign it without a specific request.

## Product goal
Search Indian government industrial tenders, especially heat exchangers and related fabrication/equipment categories. GeM is the active implemented discovery source. Broader intended sources include CPPP, IREPS, states and PSUs, but do not claim these are fully integrated.

## Current workflow
- GeM bid listing scanner searches across configured groups/pages, deduplicates collected bids, filters for industrial matches, and shows live progress.
- Dashboard shows scan progress, Matches, Reviewed, Analyzing, Waiting, sort choices (Relevance, Closing soon, AI score), and matching tender cards.
- Tender cards show title, GeM bid ID, category, deadline, AI status, and actions: Tender details, GeM document, Add to shortlist.
- AI review is optional and uses user-configured local/OpenAI-compatible endpoint. Prior test used OmniRoute in Termux at `http://127.0.0.1:20128/v1` with an OpenRouter model. An AI connection failure can simply mean the service has not been connected; avoid altering scanning logic because of it.
- Shortlisting stores tender data locally and hides shortlisted bids from future search results on the same phone.
- Tender management tracks stages Shortlisted/Pending/Quoted/Won/Lost and offers Word report generation, saved report access, report history, per-tender Word and GeM document actions, and removal from shortlist.
- Session-specific Word report history and automatic persistence were tested by the user. Maintain data compatibility.

## Visual decisions (approved)
- ColorOS-inspired frameless page with soft gray background, white rounded cards in light mode, near-black/dark cards in dark mode, and muted purple accent.
- Header: Tender Radar title, subtitle, compact tender-management shortcut/count and top-right settings gear.
- Scan progress card has circular groups progress and Find/Stop tenders button.
- Four rounded status cards with clear icons (search, check-circle, analyzing progress, clock).
- Matching tenders heading with inline sort control, above 2x2 stats cards.
- Tender card actions are text rows separated by thin dividers, not three giant pill buttons.
- Tender management dialog has grouped Word reports and Saved tenders sections.
- Preserve spinner beside “Analyzing technical documents”.
- User specifically requested no UI jumping as the scan status line changes length: reserve fixed height or constrain text lines. Confirm implementation when touching that code.
- The user said the current appearance looks fine and to lock it.

## Prior problems and constraints
- Previously duplicate buttons opened the same review/details view; consolidated to one Tender details action.
- Sort dialog had inconsistent native appearance; consider matching app theme only when asked.
- Do not mistake the phone's floating YouTube PiP overlay for app UI.
- Play Protect prompts for debug/sideloaded builds are not proof of malicious code. Signed release was postponed.

## Build and signing
- GitHub Actions workflow: `.github/workflows/build-apk.yml`.
- `gradle :app:assembleDebug` produces artifact `TenderIntelligence-debug-apk`.
- Optional signed release secrets: `TENDER_KEYSTORE_BASE64`, `TENDER_STORE_PASSWORD`, `TENDER_KEY_ALIAS`, `TENDER_KEY_PASSWORD`.
- On 2026-11-11 run 43093673353: debug build/upload passed, but release signing validation failed.
- On 2026-11-12 commit `74373047e2a7113116a4ee3ca0deefc5474209a1`, signed release became opt-in via manual `workflow_dispatch` input `signed_release`; normal pushes should only build debug. Verify fresh run instead of assuming.
- Never commit `.jks` files, passwords, keys or secrets.

## Recent commits / reference points
- `d0c905a`: grouped Tender management UI and settings gear moved to top-right.
- `7d36fd1`: removed duplicate tender-card action, retained analyzing spinner.
- `7437304`: release signing opt-in.
- More recent commits may exist: inspect `main` and GitHub Actions before modifying.

## Important implementation notes
- `shortlisted` is a local map backed by preferences (e.g. `shortlisted_tenders`).
- `persistShortlist()` updates saved data and Word report/history.
- `showShortlist()` builds the Tender management dialog; `showTenderDetails()` builds details.
- `TenderWordExport` supports Word generation.
- There may be a mismatch between the “Open saved report” action and direct file opening; inspect before changing.
- If compilation fails, check helper methods and references introduced by UI patches (e.g. `uiMuted()`), then use exact build logs to diagnose.

## Next workflow for Codex
1. Read this file, `AGENTS.md`, the source tree, and latest workflow results.
2. Ask for the next specific task rather than implementing unrequested features.
3. Keep fixes focused, test/build with GitHub Actions, commit to the repository when authorized, and provide the commit SHA and APK artifact location.
4. The original ChatGPT screenshots/chat transcript are not embedded here; this is a curated handoff, not a verbatim export.
