# EVAL-016: quoted example followed by an operative clause

Source: `EVAL-016-agreement.txt`
Document type: Agreement
Origin: Self-written synthetic example

> Example wording: "Client shall pay Provider the project fee within 30 calendar days after receipt of the invoice." This example creates no payment obligation. Client shall pay Provider the project fee within 45 calendar days after receipt of the invoice.

## Expected label

- The quoted 30-day sentence is explicitly described as an example that creates no payment obligation. Clause status: nonoperative example.
- The final 45-day sentence is the one operative payment obligation.
- Operative payer: Client; payee: Provider; scope: project fee.
- Operative deadline: 45 calendar days after invoice receipt.
- Expected extracted obligations: one, supported by the final sentence.
- Extracting the quoted 30-day example as an obligation is a false positive.
- If both sentences are extracted, `REVIEW_REQUIRED` prevents choosing one silently, but the extra extracted obligation must still be counted as a false positive.

## Observed result

- Baseline CLI review status: `REVIEW_REQUIRED`.
- Extracted Agreement matches: two.
- Match 1: quoted 30-calendar-day example, offsets 18–113. False positive: one.
- Match 2: operative 45-calendar-day clause, offsets 159–254. Correct extraction: one.
- Misses: zero. No field error was seen on the operative 45-day match.
- Because there were two matches, the review did not select one Agreement term or issue an Agreement policy assessment.
- The SOW assessment belongs to the separate comparison fixture.
## After the quoted-example guard change

- Agreement terms extracted: one.
- The operative 45-calendar-day clause was extracted with evidence at offsets 159–254.
- The quoted 30-day example was excluded after its explicit disclaimer was recognized.
- Agreement assessment under illustrative policy `P-DEMO-30`: `DEVIATION`.
- Overall CLI status remains `REVIEW_REQUIRED` because the Agreement and SOW terms differ and no supported priority wording was found.
- Preserve the baseline count of one false positive when reporting evaluation results; the post-fix run is a separate result.