# Payment wording evaluation

This is a small, synthetic evaluation set for the rule-based extractor. It is not a benchmark of real-world contract coverage.

## How to check

1. Run `mvn test`.
2. For browser checks, upload each Agreement file with `testdata/sow.pdf`.
3. Record the actual result only after checking the source quote, days, day unit, and trigger.
4. When a pattern changes, rerun the whole set to detect regressions.

## Expected single-clause extraction

| Agreement file | Days | Day unit | Trigger | Wording being checked |
| --- | ---: | --- | --- | --- |
| `testdata/agreement.txt` | 30 | Calendar | Invoice receipt | Original `shall pay` wording |
| `testdata/variant-agreement.txt` | 45 | Calendar | Invoice receipt | `must pay` and `after receiving` |
| `testdata/final-acceptance-agreement.txt` | 30 | Calendar | Final acceptance | Different payment trigger |
| `testdata/business-days-agreement.txt` | 30 | Business | Invoice receipt | No calendar-day conversion |
| `testdata/unspecified-days-agreement.txt` | 30 | Unknown | Invoice receipt | Missing day unit stays unknown |
| `testdata/invoice-date-agreement.txt` | 30 | Calendar | Invoice date | Invoice date differs from receipt |
| `testdata/no-later-than-agreement.txt` | 30 | Calendar | Invoice receipt | `no later than` deadline |
| `testdata/of-receipt-agreement.txt` | 30 | Calendar | Invoice receipt | `of receipt of the invoice` |
| `testdata/alternate-agreement.txt` | 45 | Calendar | Invoice receipt | Two linked sentences |
| `testdata/due-and-payable-agreement.txt` | 30 | Calendar | Invoice receipt | Explicit payer and payee |

## Expected ambiguity

| Agreement file | Expected behavior |
| --- | --- |
| `testdata/conflicting-payment-agreement.txt` | Show every matched clause; no single Agreement assessment |
| `testdata/mixed-payment-agreement.txt` | Show both wording types; review required; no single Agreement assessment |
## Expected partial coverage

| Agreement file | Expected behavior |
| --- | --- |
| `testdata/partially-recognized-agreement.txt` | Recognize the 30-day clause, flag the unparsed `ninety (90)` clause with exact source evidence, and set overall status to `REVIEW_REQUIRED` |

The coverage scanner checks only specific Client-to-Provider project-fee wording starts. A document with no warning is not proof that every payment clause was found.
## Expected negative control

| Agreement file | Expected behavior |
| --- | --- |
| `testdata/negative-payment-agreement.txt` | No supported Client-to-Provider payment term: one sentence negates payment and the other reverses payer and payee |
## Guardrails to retain

- A standalone `Client must pay that invoice ...` sentence must not establish the project-fee scope.
- `Provider` paying `Client` must not be labelled as `Client` paying `Provider`.
- Equal durations starting on invoice date versus invoice receipt must remain a potential difference.
- AI quote suggestions must not change the rule-based review status.

## Results

Not recorded yet. Add the date, Git commit, number of cases checked, failures, and examples of any incorrect matches after running this evaluation.