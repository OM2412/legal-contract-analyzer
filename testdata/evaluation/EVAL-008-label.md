# EVAL-008: quoted payment example is not an obligation

Source: `EVAL-008-agreement.txt`

Document type: Agreement

Sample type: self-written synthetic excerpt

## Expected annotation

> The following sentence is an illustrative example only and creates no payment obligation: "Client shall pay Provider the project fee within 30 calendar days after receipt of the invoice." No project fee is payable under this Agreement.

| Field | Expected value |
| --- | --- |
| Payment obligations | 0 |
| Clause status | Illustrative example; payment explicitly negated |
| Payer / payee | No payable obligation to extract |
| Days and day unit | Not applicable to this Agreement |
| Trigger | Not applicable to this Agreement |

## Reviewer notes

The 30-day sentence appears inside quotation marks as an illustrative example. The surrounding text expressly says it creates no obligation and that no project fee is payable. Extracting the quoted sentence as an operative payment term would be a false positive.

## Observed result
## Observed result

Baseline run before any EVAL-008 fix:

- Annotated Agreement payment obligations: **0**
- Extracted Agreement payment terms: **1**
- Agreement false positives: **1**
- CLI review status: `REVIEW_REQUIRED`
- Incorrect Agreement assessment: `WITHIN_POLICY`
- Incorrect comparison with SOW: `POTENTIAL_DIFFERENCE`
- Matched text offsets: `91..186`
- Maven CLI execution: `BUILD SUCCESS`

The extracted quote is inside an explicitly illustrative example. The surrounding text says it creates no payment obligation and that no project fee is payable. `REVIEW_REQUIRED` does not make the Agreement assessment or comparison correct.

The SOW supplied to the CLI is outside the EVAL-008 Agreement score. Keep this baseline after any later fix.
## Post-fix run

- Annotated Agreement obligations: **0**
- Extracted Agreement obligations: **0**
- Agreement false positives in this sample: **0**
- CLI review status: `REVIEW_REQUIRED`
- Agreement output: `payment clause not recognized`
- Agreement policy assessment: **not produced**
- Agreement–SOW comparison: **not produced**
- Maven CLI execution: `BUILD SUCCESS`

The extractor excludes this specifically marked, quoted, non-operative example. This one result does not prove that all quotation or disclaimer styles are handled correctly.