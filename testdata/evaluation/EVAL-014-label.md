# EVAL-014: payment method inside the deadline clause

Source: `EVAL-014-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Client shall pay Provider the project fee by electronic bank transfer within 30 calendar days after receipt of the invoice.

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
| Payment method | Electronic bank transfer |

The payment method does not change the 30-calendar-day deadline. There is one obligation in this excerpt.

## Observed result

- Baseline CLI review status: `REVIEW_REQUIRED`.
- Agreement payment terms extracted: zero.
- Expected obligations: one; correct extractions: zero; misses: one.
- False positives: zero. Field errors: not applicable because no term was extracted.
- The unparsed payment wording triggered manual review; no Agreement policy assessment was issued.
- The SOW assessment belongs to the separate comparison fixture.
## After the extractor change

- The Agreement has one extracted term: Client pays Provider the project fee within 30 calendar days after invoice receipt.
- Evidence covers the entire source sentence, including `by electronic bank transfer`.
- Agreement policy assessment under `P-DEMO-30`: `WITHIN_POLICY`.
- The SOW fixture states 60 calendar days, so the comparison is `POTENTIAL_DIFFERENCE`. With no supported priority wording, overall status is `REVIEW_REQUIRED`.
- This post-fix result must not replace the baseline miss when reporting evaluation performance.