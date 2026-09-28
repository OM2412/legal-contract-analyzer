# EVAL-006: invoice mentioned without a payment obligation

Source: `EVAL-006-agreement.txt`

Document type: Agreement

Sample type: self-written synthetic excerpt

## Expected annotation

> This Agreement does not require Customer to pay any implementation fee.

| Field | Expected value |
| --- | --- |
| Payment obligations | 0 |
| Clause status | Negated |
| Payer / payee | No payable obligation to extract |
| Days and day unit | Not stated |
| Trigger | Not stated |

## Reviewer notes

The invoice is for administrative tracking. The Agreement explicitly says Customer is not required to pay an implementation fee. Negotiating terms in a future order form does not create a payment obligation in this Agreement.

Any extracted payable term from this excerpt is a false positive. A warning asking for human review can be recorded separately from payment-term extraction.

## Observed result

## Observed result

Baseline CLI run:

- Annotated Agreement payment obligations: **0**
- Extracted Agreement payment obligations: **0**
- Agreement false positives: **0**
- Negative sample correctly left without a payable term: **yes**
- Overall CLI review status: `INCOMPLETE`
- Agreement message: `payment clause not recognized`
- Precedence scan: `NOT_LOCATED`

The SOW supplied to the CLI is outside the EVAL-006 Agreement score. This result does not establish that every negated wording will be handled correctly.