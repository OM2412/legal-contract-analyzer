# Payment extraction evaluation protocol

## Purpose

Measure how often the rule-based system finds and correctly describes payment obligations in varied Agreement and SOW excerpts. The 14 synthetic fixture scenarios remain regression checks; report them separately from this evaluation.

## Sample selection

Start with 20 varied, public or self-written excerpts. Include wording unlike the existing `testdata/` examples: multiple obligations, different payer/payee names, installments, exceptions, invoice date, invoice receipt, acceptance, business days, and no payment clause.

Do not commit private client contracts or identifying details to this repository.

## Annotation for each excerpt

Record one row per payment obligation:

| Field | Meaning |
| --- | --- |
| Sample ID | Stable identifier for the excerpt |
| Document type | Agreement or SOW |
| Exact quote | Continuous text copied from the source |
| Payer and payee | Parties explicitly supported by the quote |
| Payment scope | What is being paid |
| Days and day unit | Number and calendar/business/unknown |
| Trigger | Invoice receipt, invoice date, acceptance, other, or unknown |
| Clause status | Payment obligation, negated, conditional, or unclear |
| Reviewer notes | Why a label is uncertain |

Also record excerpts with **no payment obligation** so false positives can be measured.

## Scoring

- **Correct extraction:** the matched source quote supports the obligation and every reported field.
- **False positive:** the system reports an obligation the annotation does not support.
- **Miss:** an annotated obligation has no correct system match.
- **Field error:** a matching obligation has an incorrect day count, unit, trigger, party, or scope.
- Report counts first. Calculate precision and recall only after the sample set and labels are fixed.
- Track how many documents correctly receive `REVIEW_REQUIRED` when multiple or partly unparsed clauses exist.

Do not treat textual precedence or policy assessment as a legal enforceability label.

## Run record

For every evaluation run, save the date, Git commit, sample-set version, counts, example errors, and any changed annotation decisions. Keep the 14 synthetic regression scenarios in their separate checklist.