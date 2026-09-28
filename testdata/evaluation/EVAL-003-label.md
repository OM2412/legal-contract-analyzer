# EVAL-003: mixed wording and business days

Source: `EVAL-003-agreement.txt`

> The Customer must pay the Provider the implementation fee no later than 15 business days following receipt of an invoice.

| Field | Expected value |
| --- | --- |
| Payer | Customer |
| Payee | Provider |
| Scope | Implementation fee |
| Days | 15 |
| Day unit | Business days |
| Trigger | Invoice receipt |
| Clause status | Payment obligation |

Expected policy handling: `UNABLE_TO_ASSESS` under the illustrative calendar-day policy. Business days must not be converted to calendar days.

## Observed result


Run date: 2026-09-28

Observed review status: `SEPARATE_OBLIGATIONS`.

Observed comparison: `DIFFERENT_OBLIGATIONS` against the separate Client/Provider project-fee SOW sample.

Observed Agreement assessment: `UNABLE_TO_ASSESS`.

Observed evidence: the Agreement payment sentence was cited at text offsets 0–121.

CLI build: `BUILD SUCCESS`.

Direct structured-field check: `PaymentEvaluation003Test` passed for payer, payee, scope, 15 business days, invoice-receipt trigger, exact quote, and `UNABLE_TO_ASSESS` policy handling. This is one synthetic wording combination, not a real-world accuracy estimate.