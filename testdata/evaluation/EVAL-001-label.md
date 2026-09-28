# EVAL-001: implementation-fee payment

Source: `EVAL-001-agreement.txt`

## Annotated obligation

> Customer shall pay Vendor the implementation fee within 20 calendar days after receiving a valid invoice.

| Field | Expected value |
| --- | --- |
| Payer | Customer |
| Payee | Vendor |
| Scope | Implementation fee |
| Days | 20 |
| Day unit | Calendar days |
| Trigger | Invoice receipt |
| Clause status | Payment obligation |

The full sentence is the source evidence. The words "after receiving a valid invoice" support invoice receipt as the trigger.

## Baseline observation

Pending. Record the actual rule-based result, Git commit, and run date after checking the browser or an automated evaluation.
Run date: 2026-09-28

Command: `ContractAnalyzerApplication` with `EVAL-001-agreement.txt` and `testdata/agreement.txt`.

Observed review status: `INCOMPLETE`.

Observed Agreement result: `payment clause not recognized`.

Evaluation outcome: **1 missed annotated payment obligation**. No Agreement payment term was extracted, so its days, parties, scope, and trigger were not assessed.
Code revision: `e2db5c3`.
## After extractor update

Run date: 2026-09-28

Observed review status: `SEPARATE_OBLIGATIONS`.

Observed comparison: `DIFFERENT_OBLIGATIONS`, because the Agreement uses Customer/Vendor and implementation fee while the SOW sample uses Client/Provider and project fee.

Observed Agreement term: 20 calendar days after receipt of a valid invoice. The CLI cited the exact EVAL-001 sentence at text offsets 0–105.

Outcome: the baseline extraction miss is fixed in this development example. This example was used to improve the rule and must not be reported as an unseen-data accuracy result.