# Named-party explicit installment regression

Verified on 13 October 2026 using the self-written PAIR-001
development pilot. These are post-fix development results,
not independent benchmark accuracy.

## Implemented scope

Extract an explicit percentage-of-fee payment obligation
triggered by invoice date. Preserve the stated day unit.

The existing main extractor applies non-binding-example and
duplicate-match guards to these candidates.

## Verified SOW fields

- Days: 12
- Unit: BUSINESS_DAYS
- Trigger: INVOICE_DATE
- Payer: Customer
- Payee: Atlas Systems
- Scope: 35 percent of the migration fee
- Exact evidence offsets: 45-156
- Policy assessment: UNABLE_TO_ASSESS

## Pilot scoring

- Gold obligations: 3
- Predicted obligations: 2
- Matched: 2
- False positives: 0
- Misses: 1
- Exact extracted evidence: 2
- Review status: REVIEW_REQUIRED
- Review-status errors: 0

The scorer checked six Agreement fields with zero errors.
SOW fields were inspected in the API response and asserted in
unit tests; the current scorer does not check structured fields
for documents with multiple gold obligations.

The remaining 65-percent clause is still flagged for human review.
Its scope has not yet been resolved from preceding context.
Payment priority remains NOT_LOCATED.

Targeted and full Maven tests completed successfully as reported
during verification. Earlier baseline and extraction results remain
preserved alongside this installment response and score.