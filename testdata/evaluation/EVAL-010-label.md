# EVAL-010: business days after invoice date

Source: `EVAL-010-agreement.txt`

Document type: Agreement

Sample type: self-written synthetic excerpt

## Expected obligation

> Customer must pay Vendor the implementation fee no later than 20 business days after the invoice date.

| Field | Expected value |
| --- | --- |
| Payer | Customer |
| Payee | Vendor |
| Payment scope | Implementation fee |
| Days | 20 |
| Day unit | Business days |
| Trigger | Invoice date |
| Clause status | Payment obligation |

## Reviewer notes

"After the invoice date" differs from receiving an invoice. Twenty business days cannot be directly assessed against the illustrative policy's maximum calendar days without a supported conversion.

## Observed result

## Observed result

Baseline run:

- Annotated Agreement obligations: **1**
- Correct Agreement extractions: **1**
- Agreement misses: **0**
- Agreement false positives: **0**
- Checked field errors: **0**
- Extracted fields: Customer to Vendor, `implementation_fee`, 20 `BUSINESS_DAYS`, `INVOICE_DATE`
- Agreement assessment: `UNABLE_TO_ASSESS`
- Exact Agreement evidence: text offsets `0..102`
- Maven CLI execution: `BUILD SUCCESS`

The illustrative policy uses calendar days, so the business-day period was not converted or given an unsupported compliance verdict.

The comparison SOW concerns a different payer, payee, and payment scope. Its result is outside the EVAL-010 Agreement score.