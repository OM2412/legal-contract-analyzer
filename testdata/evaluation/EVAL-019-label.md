# EVAL-019: explicit currency amount within a fee clause

Source: `EVAL-019-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Customer shall pay Vendor the implementation fee of INR 50,000 within 30 calendar days after receiving a valid invoice.

## Expected label

| Field | Expected value |
| --- | --- |
| Clause status | Payment obligation |
| Payer | Customer |
| Payee | Vendor |
| Scope | Implementation fee |
| Amount | INR 50,000 |
| Days | 30 |
| Day unit | Calendar days |
| Trigger | Receipt of a valid invoice |

The amount is part of the fee description. It does not change the 30-calendar-day deadline. If the current structured term cannot store the amount, it must not invent a different amount or silently claim complete field coverage.

## Observed result

- Baseline CLI review status: `REVIEW_REQUIRED`.
- Agreement payment terms extracted: zero.
- Expected obligations: one; correct extractions: zero; misses: one.
- False positives: zero. Field errors: not applicable because no term was extracted.
- The scanner flagged unparsed payment wording for manual review.
- No Agreement policy assessment or structured amount was reported.
- The SOW assessment belongs to the separate comparison fixture.