# EVAL-017: invoice deadline conditional on acceptance

Source: `EVAL-017-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Client shall pay Provider the project fee within 30 calendar days after receipt of the invoice, provided that the deliverables have been accepted.

## Expected label

| Field | Expected value |
| --- | --- |
| Clause status | Conditional payment obligation |
| Payer | Client |
| Payee | Provider |
| Scope | Project fee |
| Days | 30 |
| Day unit | Calendar days |
| Stated deadline reference | Invoice receipt |
| Additional condition | Deliverables have been accepted |

The acceptance condition affects when payment is owed. Extracting an unconditional `INVOICE_RECEIPT` term while omitting that condition would be a field error. If the system cannot represent both facts, it should flag the clause for manual review and avoid an Agreement policy verdict based on an incomplete term.

## Observed result

- Baseline CLI review status: `REVIEW_REQUIRED`.
- Agreement payment terms extracted: zero.
- Expected conditional obligations: one; correct extractions: zero; misses: one.
- False positives: zero. No incomplete invoice-receipt term was reported.
- Agreement policy assessment: absent, so the acceptance condition was not silently dropped from a policy decision.
- The SOW assessment belongs to the separate comparison fixture.