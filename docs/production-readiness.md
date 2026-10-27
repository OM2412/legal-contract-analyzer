# Production readiness gates

Base audit: `a2c254452b08ed134cc3f11d4eeae43c7858367c` (25 October 2026).

This is an acceptance ledger, not a security certification or a completion percentage.
Do not mark a gate verified because code exists or an older report passed.
Record the tested commit, environment, result and remaining limitations.

## Data-access model

- Other application users must never read or delete a review they do not own.
- Anonymous requests must never access account or saved-review data.
- The application server processes plaintext documents. Infrastructure/database
  administrators are separate privileged actors; application ownership checks do
  not make data inaccessible to those administrators.
- Retained source quotes and filenames can be confidential. Include them in the
  retention, export, deletion and backup policies.
- Never use private customer documents in CI or public evaluation fixtures.

## Gates

| Gate | Required evidence | Status |
| --- | --- | --- |
| Ownership isolation | Two-account read/list/delete tests on deployed release | Historical scripts exist; rerun on final release |
| Session termination | Server logout rejection plus browser private-state and late-response tests | Frontend regression verified in batch 1; deployed release pending |
| Cross-tab/session-expiry behavior | Browser tests across tabs and expired sessions | Pending |
| Safe errors | Parser/path/document text absent from client errors; no-store | Handler regression passed; deployed endpoint checks pending |
| Input/resource limits | Corrupt/oversize files, total processing deadline, bounded parallel OCR | Upload checks exist; concurrency/deadline work pending |
| Secret handling | Ignore rules, secret scan, retired credential revocation | Ignore rules strengthened; revocation/scan pending |
| Recovery | Hosted-provider reset, expiry, reuse and old-session rejection | Local historical checks only; hosted pending |
| Frozen evaluation provenance | Exact source hashes, quote offsets and reproducible checkout | Original source bytes recovered and validated in batch 1 |
| Extraction quality | Fresh pre-labelled pairs, all obligation fields, misses and false positives | Pending; PAIR-001 is development material |
| Review integrity | Owner-bound server analysis ID, idempotent save, engine/schema versions | Pending |
| Reviewer workflow | Persisted notes/decisions; saved/live rendering parity | Pending |
| CI | Frontend checks, Java verification and disposable PostgreSQL API checks | Workflow added; remote execution pending |
| Runtime assurance | Readiness, bounded workloads, processing/error metrics and alert exercise | Pending |
| Recovery operations | Automated backup, restore drill and explicit RPO/RTO | Manual migration restore completed; operating policy pending |
| Privacy/account lifecycle | Accurate provider/contact/retention notice, account deletion/export | Pending |
| User validation | Voluntary pilot, task completion/error observations, fixes and retest | Pending |
| Release UX | Keyboard/mobile checks and accessible source-preview modal | Pending |

## Release rule

Block public customer-contract onboarding while access, secret, resource-limit,
privacy and recovery gates remain unresolved. Use synthetic/redacted documents
for the controlled pilot. A green frontend build is not backend or security proof.

Avoid declaring 100% accuracy, complete clause coverage, zero risk, penetration
test certification or guaranteed confidentiality from these checks.
