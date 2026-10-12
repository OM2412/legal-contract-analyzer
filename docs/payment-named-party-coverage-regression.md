# PAIR-001 coverage fix regression

Verified on 13 October 2026.

This is a self-written development pilot, not an independent
held-out accuracy benchmark. The source files and labels were unchanged.

Manifest SHA-256:
6df715285a9ebe0beff01e44671c37c548f904e710e62da55f119e32f4be4c95

## Before the coverage fix

- Review status: INCOMPLETE
- Review-status errors: 1
- Coverage passages: Agreement 0, SOW 0
- Extracted obligations: 0
- Extraction misses: 3

## After the coverage fix

- Review status: REVIEW_REQUIRED
- Review-status errors: 0
- Coverage passages: Agreement 1, SOW 2
- Agreement evidence offsets: 37-168
- SOW evidence offsets: 45-156 and 157-312
- Extracted obligations: 0
- Extraction misses: 3

The fix flags unsupported payment wording with source evidence.
It does not add structured payment extraction or determine legal priority.

PaymentNamedPartyCoverageTest adds four regression cases:
pilot coverage, negative wording, Unicode/page boundaries,
and avoiding duplicate warnings for recognized terms.
Targeted and full Maven test commands completed successfully
as reported during verification.

The fresh-response file is a formatted transcription of the actual
browser API response. The scorer validated its source versions and
reported zero review-status errors. Coverage evidence is not included
in the scorer's extracted-obligation exactEvidence metric.

Preserve baseline-response and baseline-score alongside post-fix outputs.