# EVAL-018: completed payment is not a new obligation

Source: `EVAL-018-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Client paid Provider the project fee within 30 calendar days after receipt of the invoice. No further payment is due under this completed transaction.

## Expected label

- Clause status: historical description of a completed payment.
- Current payment obligations: zero.
- The 30-day period describes when a past payment occurred; it is not a current deadline.
- Extracting a current 30-calendar-day payment term would be a false positive.
- No Agreement policy-compliance assessment should be issued.

## Observed result

- Baseline CLI review status: `INCOMPLETE`.
- Agreement payment terms extracted: zero.
- Current Agreement obligations annotated: zero; false positives: zero.
- Agreement policy assessment: absent.
- The SOW `DEVIATION` belongs to the separate comparison fixture.