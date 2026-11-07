# Tender Intelligence AI roadmap

## Current prototype
- Android app independent of Termux
- Local title/text scoring and shortlist
- Import TXT and export CSV
- OpenRouter model listing and text analysis with user-supplied API key

## Next milestones
1. Verify GitHub Actions APK build and install on Android.
2. Implement selectable PDF text extraction with document-size and privacy controls.
3. Add structured local database, tender reference, source URL, deduplication and deadline validation.
4. Implement approved/public GeM, CPPP and IREPS adapters with documented rate limits and clear per-source coverage.
5. Add date-aware relevance ranking, user-editable keyword sets, and optional notifications.
6. Security review: minimize submitted text, explicit consent for OpenRouter requests, secure API key storage option.

**Important:** An AI model cannot independently search government tender portals. Portal connectors require their own access method and compliance review.
