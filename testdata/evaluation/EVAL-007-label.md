# EVAL-007: different party names and fee scope

Source: `EVAL-007-agreement.txt`

Document type: Agreement

Sample type: self-written synthetic excerpt

## Expected obligation

> The Purchaser shall pay the Supplier the integration fee within 45 calendar days after receiving a valid invoice.

| Field | Expected value |
| --- | --- |
| Payer | Purchaser |
| Payee | Supplier |
| Payment scope | Integration fee |
| Days | 45 |
| Day unit | Calendar days |
| Trigger | Invoice receipt |
| Clause status | Payment obligation |

## Reviewer notes

The quote explicitly names both parties and the integration fee. The 45-day period starts after the Purchaser receives a valid invoice. Do not substitute Client, Customer, Provider, or Vendor for the named parties.

## Observed result

## Observed result

Baseline run before any EVAL-007 extractor changes:

- Annotated Agreement obligations: **1**
- Correct Agreement extractions: **0**
- Missed Agreement obligations: **1**
- Agreement false positives: **0**
- CLI review status: `INCOMPLETE`
- CLI Agreement message: `payment clause not recognized`
- Maven CLI execution: `BUILD SUCCESS`

The SOW was supplied only because the CLI requires two documents. Its result is outside the EVAL-007 Agreement score.

Keep this baseline even if a later development change adds support for Purchaser, Supplier, or integration fee.