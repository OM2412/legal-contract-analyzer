# EVAL-011: two milestone payments without day counts

Source: `EVAL-011-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Client shall pay Provider 25% of the project fee on signing this Agreement and the remaining 75% on final acceptance.

## Expected obligations

| Field | First obligation | Second obligation |
| --- | --- | --- |
| Payer | Client | Client |
| Payee | Provider | Provider |
| Scope | 25% of project fee | Remaining 75% of project fee |
| Deadline | On signing this Agreement | On final acceptance |
| Days | Not stated | Not stated |
| Day unit | Not stated | Not stated |
| Clause status | Payment obligation | Payment obligation |

The sentence contains two payment obligations. Neither states a number of days, so assigning a `within N days` deadline would be a field error. A single-term comparison or policy verdict should not silently represent both obligations.

## Observed result
## Observed result

- Baseline CLI result: `INCOMPLETE`.
- Agreement: `payment clause not recognized`; zero obligations extracted.
- Expected obligations: two; correct extractions: zero; misses: two.
- False positives: zero. Field errors: not applicable because no Agreement term was extracted.
- The two milestone payments were not flagged for manual review. This is a coverage and review-status gap.
- The SOW assessment shown by the CLI belongs to the comparison fixture, not this Agreement's evaluation label.
## After the coverage-scanner change

- CLI review status: `REVIEW_REQUIRED`.
- Agreement obligations extracted: zero; the two annotated obligations remain extraction misses.
- The scanner flags the complete payment sentence as unrecognized wording for manual review.
- No Agreement policy assessment or single textual candidate is assigned.
- This change improves the review warning; it does not establish extraction coverage for milestone payments.