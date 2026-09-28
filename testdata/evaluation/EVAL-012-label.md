# EVAL-012: late-fee threshold is not the payment deadline

Source: `EVAL-012-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Provider may charge a late fee if Client pays an invoice more than 30 calendar days after receipt. This clause does not set the invoice payment deadline.

## Expected label

- Primary invoice-payment deadline: not stated.
- The 30-calendar-day value is a condition for a possible late fee, not an instruction to pay within 30 days.
- Clause status: conditional late-fee provision; no supported primary payment deadline.
- Expected extracted primary payment terms: zero.
- Reporting `30 calendar days after invoice receipt` as the primary payment term would be a false positive.
- Without a primary Agreement payment term, the system must not issue an Agreement policy-compliance verdict.

## Observed result

- Baseline CLI review status: `INCOMPLETE`.
- Agreement: `payment clause not recognized`; zero primary payment terms extracted.
- Agreement policy assessment: absent.
- False positives: zero. No primary payment-deadline miss is counted because the excerpt states no such deadline.
- The SOW `DEVIATION` belongs to the separate comparison fixture, not this Agreement's label.