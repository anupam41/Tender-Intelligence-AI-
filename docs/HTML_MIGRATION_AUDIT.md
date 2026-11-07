# Migration notes: uploaded HTML tender radar

The original HTML is the behavioral reference, not a trusted scanner implementation.

## Existing search behavior
- Eight query batches targeting coolers, exchangers, condensers, bundles, heaters, and vessels.
- Tavily POST /search with include_domains, and Serper POST /search with site: operators.
- Official domain filtering, keyword matches, snippets, local shortlist, GeM text paste, PDF.js extraction, OpenRouter summaries, HTML-as-Word export.

## Problems to fix in native Android
1. Search failures are swallowed by empty catch blocks: expose per-provider status, HTTP errors and rate limits.
2. Search-index results are not confirmed portal listings. Use states DISCOVERED, OFFICIAL_URL, DOCUMENT_VERIFIED, DEADLINE_VERIFIED. Do not display VERIFIED solely because of a URL whitelist.
3. Unknown deadline must be UNKNOWN, not active; never parse a generic 'dated' value as closing date. Support extensions/corrigenda.
4. Uploaded PDFs must remain USER_IMPORTED until source URL or bid ID is independently verified. Do not attach a generic GeM URL as evidence of that document.
5. Deduplicate by normalized bid ID plus portal, or canonical tender URL; never by shared portal homepage.
6. Enforce HTTPS and exact approved hosts, not every arbitrary .gov.in URL. A .com host may be a real PSU portal; verify ownership per portal.
7. Handle false positives from generic terms like 'cooler' and negative terms (drinking water cooler, AC appliance, clamps, raw tubes).
8. Secure key handling: don't log keys or store unencrypted in local preferences; let users explicitly consent before sending tender text to AI.
9. HTML output inserts external titles/snippets into innerHTML without escaping: prevent injection in any WebView or export.
10. Record real API requests and retrieved candidate counts separately; 'pages scanned' is misleading when only search snippets were fetched.

## Native app architecture
- SearchProvider interface: TavilyProvider, SerperProvider, optional compliant official portal adapters.
- TenderCandidate: title, portal, sourceUrl, bidId, buyer, deadline, equipmentMatches, evidence, retrievalTime, verificationState.
- Filtering pipeline: URL -> tender evidence -> equipment relevance -> deadline -> dedup -> optional AI summary.
- Android repository/database with clear export and delete operations.
- PDF parser for user-selected documents; scanned-image PDFs need separate OCR support.
- UI: dark dashboard, 1-tap scan, provider diagnostics, verified-vs-unverified counts, result cards, API/model settings.

## Current app
The current APK is a prototype with portal links and manual TXT analysis. Automated multi-provider scanning and PDF import are NOT yet implemented. Do not claim otherwise.
