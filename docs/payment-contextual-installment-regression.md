# Contextual remaining installment regression

Verified on 14 October 2026 using the self-written PAIR-001
development pilot. These post-fix results are not independent
benchmark accuracy.

## Implemented behavior

Resolve a remaining installment only when:
- Its preceding explicit installment is adjacent.
- Payer and payee match.
- The payer gives final written acceptance.
- The two percentages total 100.
- The deliverable name matches the preceding fee name.

Do not link across intervening wording, blank paragraphs or page breaks.
Preserve both sentences as evidence for the inherited fee scope.
Retain separate obligations even when their evidence overlaps.
Reject explicitly non-binding quoted examples.

## Verified pilot outcome

- Agreement obligations: 1
- SOW obligations: 2
- Coverage warnings: 0
- Review status: REVIEW_REQUIRED
- Gold obligations: 3
- Predicted obligations: 3
- Matched: 3
- False positives: 0
- Misses: 0
- Review-status errors: 0
- Agreement fields checked: 6, with zero errors
- Exact gold-quote matches: 2

Remaining installment:
8 calendar days, FINAL_ACCEPTANCE, Customer to Atlas Systems,
remaining 65 percent of the migration fee.

Its contextual evidence spans offsets 45-312.
This contains both SOW payment sentences and is intentionally broader
than the annotated remaining-clause quote at offsets 157-312.

The API exposes no selected SOW term when multiple clauses exist.
SOW fields are asserted in regression tests; the current scorer
does not independently score every multi-clause structured field.

Targeted and full Maven tests completed successfully as reported
during verification. Earlier baseline and intermediate results remain
preserved. Payment priority is still NOT_LOCATED.