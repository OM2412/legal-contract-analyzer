# EVAL-013: number word with parenthesized digits

Source: `EVAL-013-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Client shall pay Provider the project fee within thirty (30) calendar days after receipt of the invoice.

## Expected label

| Field | Expected value |
| --- | --- |
| Clause status | Payment obligation |
| Payer | Client |
| Payee | Provider |
| Scope | Project fee |
| Days | 30 |
| Day unit | Calendar days |
| Trigger | Invoice receipt |

The word `thirty` and the parenthesized digits `(30)` agree. Both describe one 30-day deadline, not two separate obligations. An extraction with days other than 30 would be a field error.

## Observed result

- Baseline CLI review status: `REVIEW_REQUIRED`.
- Agreement payment terms extracted: zero.
- Expected obligations: one; correct extractions: zero; misses: one.
- False positives: zero. Field errors: not applicable because no term was extracted.
- The scanner flags the unparsed wording for manual review.
- No Agreement policy assessment was issued.
- Supporting `thirty (30)` requires checking that the written and numeric values agree; this baseline has not been changed to add that support.
- The SOW assessment belongs to the separate comparison fixture.