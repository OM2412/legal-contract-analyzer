# EVAL-005: two installment payments

Source: `EVAL-005-agreement.txt`
Document type: Agreement
Sample type: self-written synthetic excerpt

## Obligation 1

> Customer shall pay Vendor 40% of the implementation fee within 10 calendar days after signing this Agreement.

| Field | Expected value |
| --- | --- |
| Payer | Customer |
| Payee | Vendor |
| Payment scope | First installment: 40% of implementation fee |
| Days | 10 |
| Day unit | Calendar days |
| Trigger | Signing of the Agreement (other) |
| Clause status | Payment obligation |

## Obligation 2

> Customer shall pay Vendor the remaining 60% of the implementation fee within 15 calendar days after final acceptance of the deliverables.

| Field | Expected value |
| --- | --- |
| Payer | Customer |
| Payee | Vendor |
| Payment scope | Remaining installment: 60% of implementation fee |
| Days | 15 |
| Day unit | Calendar days |
| Trigger | Final acceptance |
| Clause status | Payment obligation |

## Reviewer notes

These are two separate installments for the implementation fee. The first deadline starts after signing; the second starts after final acceptance. They must not be merged into one payment period or treated as an invoice-receipt obligation.

## Observed result

## Observed result

Baseline run before extractor changes:

- Agreement obligations annotated: **2**
- Correct Agreement extractions: **0**
- Missed Agreement obligations: **2**
- Agreement false positives: **0**
- CLI review status: `INCOMPLETE`
- CLI Agreement output: `payment clause not recognized`
- Maven CLI execution: `BUILD SUCCESS`

The comparison SOW was supplied only because the CLI requires two documents. Its payment result is outside the EVAL-005 Agreement score.

Do not revise the expected labels to match this baseline. Record a later run separately after implementation changes.
## Post-fix run

Run date: 28 September 2026
Baseline commit: `3c04b59`
Implementation state: working tree, not yet committed

| Agreement-only measure | Result |
| --- | ---: |
| Annotated obligations | 2 |
| Correct extractions | 2 |
| Misses | 0 |
| False positives | 0 |

The CLI reports `REVIEW_REQUIRED` and displays both obligations separately:

1. `10 CALENDAR_DAYS`, trigger `OTHER` (signing), Customer to Vendor, scope `implementation_fee_40_percent`, evidence offsets `0..109`.
2. `15 CALENDAR_DAYS`, trigger `FINAL_ACCEPTANCE`, Customer to Vendor, scope `implementation_fee_60_percent`, evidence offsets `110..247`.

Both displayed evidence quotes match the two annotated sentences. No single Agreement payment term or policy assessment is selected.

The CLI execution finished with `BUILD SUCCESS`. Record the full Maven test result separately before committing.