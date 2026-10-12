# Named-party Agreement extraction regression

Verified on 13 October 2026 using the self-written PAIR-001
development pilot. These results are not independent benchmark accuracy.

## Implemented scope

A narrow active-voice fee or charge obligation where Client/Customer
explicitly receives the named payee's invoice.

Supports both apostrophe-s and apostrophe-only possessives for names
ending in s. Existing non-binding-example and duplicate-match guards
are applied by PaymentTermExtractor.

## Verified Agreement fields

- Days: 24
- Unit: CALENDAR_DAYS
- Trigger: INVOICE_RECEIPT
- Payer: Customer
- Payee: Atlas Systems
- Scope: monthly support charge
- Exact evidence offsets: 37-168

## Pilot scoring

- Gold obligations: 3
- Predicted obligations: 1
- Matched: 1
- False positives: 0
- Misses: 2
- Exact extracted evidence: 1
- Fields checked: 6
- Field errors: none
- Review status: REVIEW_REQUIRED
- Review-status errors: 0

The two SOW installments remain unsupported by extraction and are
flagged as coverage gaps. Payment priority remains NOT_LOCATED.

Targeted and full Maven tests completed successfully as reported
during verification. Original baseline and coverage-only results
are preserved alongside the extraction response and score.