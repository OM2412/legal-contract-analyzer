# EVAL-020: invoicing deadline is not a payment deadline

Source: `EVAL-020-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Provider shall issue an invoice for the project fee within 5 business days after final acceptance. This provision sets an invoicing deadline and does not require Client to pay by any particular date.

## Expected label

- Clause status: deadline for Provider to issue an invoice.
- Current Client-to-Provider payment obligations: zero.
- The 5-business-day period concerns invoice issuance, not payment.
- Reporting a 5-business-day payment term would be a false positive.
- No Agreement payment-policy assessment should be issued.

## Observed result

- Baseline CLI review status: `INCOMPLETE`.
- Agreement payment terms extracted: zero.
- Client-to-Provider payment obligations annotated: zero; false positives: zero.
- Agreement policy assessment: absent.
- The SOW `DEVIATION` belongs to the separate comparison fixture.