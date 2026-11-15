# Release hardening batch 1

## Changes

- Add workspace/home sign-out controls. Clear review results, original-file
  references, checklist notes, AI state and reset token before awaiting logout.
  A failed/unconfirmed logout blocks private screens and offers retry instead
  of claiming that the server session ended. A server 401 is treated as an
  already-ended session.
- Abort pending review uploads on workspace unmount and ignore late responses.
  This prevents an old upload result from restoring private content after logout.
  Client abort does not guarantee that server-side OCR stops; that is a separate
  processing-cancellation gate.
- Return a fixed, no-store upload error instead of arbitrary exception messages.
  Disable framework message/trace/binding details in default error responses.
- Restore PAIR-001 source bytes to their existing frozen hashes. The original
  files contain a UTF-8 BOM, LF internal newlines and a final CRLF. Git previously
  normalized that final CRLF. No labels, offsets or expected source hashes changed.
  `.gitattributes` now preserves these exact bytes across checkouts.
- Add frontend lifecycle tests and a Java upload-error privacy regression.
- Fix OCR test prerequisites: the old Windows-only path check silently skipped
  both OCR tests on Linux. Detect the configured/PATH executable and English
  language data; CI and Docker builds now require OCR tests rather than skipping.
- Add a least-privilege CI workflow using a disposable PostgreSQL service,
  synthetic documents and pinned GitHub Action revisions. Production credentials
  and databases are not used. Require remote checks to pass before merging.
- Expand secret-file ignore rules and establish a readiness ledger.

## Local verification

- Frontend lifecycle suite: 4 tests passed in 2 files.
- ESLint and Vite production build passed.
- Frozen label validation passed: 1 pair, 2 documents, 3 obligations.
- Scoring archived pilot responses passed provenance validation: 3 matches,
  0 misses, 0 false positives, 2 exact gold-quote matches, 6 fields checked.
  This is a replay of development evidence, not new live predictions or unseen accuracy.
- Java 25 / Maven 3.9.16 suite: 120 tests, 0 failures, 0 errors, 0 skipped,
  including the 2 OCR integration tests and upload-error privacy regression.
  Initial toolchain attempts failed on runtime proxy/CA configuration; using the
  environment's system trust store resolved this without disabling TLS validation.
- Production frontend dependency audit: npm reported 0 known vulnerabilities.
  This does not establish absence of vulnerabilities or audit backend dependencies.
- Deployed API/browser checks and remote CI execution remain separate gates;
  local unit/OCR results do not establish those outcomes.

## Remaining boundaries

No claim of cross-tab logout synchronization, disk/memory secure erasure,
infrastructure-administrator exclusion, complete parser isolation, independent
penetration testing, or production certification is made by this batch.
