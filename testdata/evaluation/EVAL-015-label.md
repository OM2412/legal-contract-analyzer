# EVAL-015: deadline after invoice submission

Source: `EVAL-015-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Client shall pay Provider the project fee within 45 calendar days after Provider submits the invoice to Client.

## Expected label

| Field | Expected value |
| --- | --- |
| Clause status | Payment obligation |
| Payer | Client |
| Payee | Provider |
| Scope | Project fee |
| Days | 45 |
| Day unit | Calendar days |
| Trigger | Provider submits the invoice to Client |

Submission is the stated starting event. The quote does not say when Client receives the invoice. Labeling this trigger `INVOICE_RECEIPT` would be a field error. If the extractor cannot represent submission reliably, it should leave the term unassessed and flag the wording for review.

## Observed result

- Baseline CLI review status: `REVIEW_REQUIRED`.
- Agreement payment terms extracted: zero.
- Expected obligations: one; correct extractions: zero; misses: one.
- False positives: zero. Field errors: not applicable because no term was extracted.
- The wording was flagged for manual review. No Agreement policy assessment or invoice-receipt trigger was assigned.
- The SOW assessment belongs to the separate comparison fixture.