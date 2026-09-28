# EVAL-002: conflicting deadlines for the same fee

Source: `EVAL-002-partial.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Customer shall pay Vendor the implementation fee within 20 calendar days after receiving a valid invoice.
>
> Customer shall pay Vendor the implementation fee within forty-five (45) calendar days after receiving a valid invoice.

## Expected labels

| Field | First obligation | Second obligation |
| --- | --- | --- |
| Payer | Customer | Customer |
| Payee | Vendor | Vendor |
| Scope | Implementation fee | Implementation fee |
| Days | 20 | 45 |
| Day unit | Calendar days | Calendar days |
| Trigger | Receipt of a valid invoice | Receipt of a valid invoice |
| Clause status | Payment obligation | Payment obligation |

Both sentences concern the same parties and fee but state conflicting deadlines. The second sentence uses agreeing written and numeric values, `forty-five (45)`. No source wording says which deadline controls. A review must flag the conflict and must not silently choose either sentence as the controlling term.

## Observed result

- This excerpt was created during earlier development, but a dated CLI result for this exact file was not preserved.
- Related partial-coverage behavior is covered by `PaymentPartialCoverageReviewTest`.
- Do not invent a historical baseline result or include this file in measured extraction counts until its exact run is recorded.