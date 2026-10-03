# CUAD termination contract-level training audit

Run date: 4 October 2026  
Source: `data/external/cuad/train_separate_questions.json`  
Rule code revision before this audit: `79c603c`  
Held-out `test.json`: not read

## Task boundary

The application's scanner flags generic termination wording. CUAD's
`Termination For Convenience` label is narrower. This audit measures
candidate flag overlap with that label on training data. It does not
measure correct legal classification or performance on unseen contracts.

The unit is a contract title with at least one target question. Repeated
target questions for the same contract are grouped together. A contract
is gold-positive if any target answer span exists. It is scanner-positive
if any scanner evidence span exists. Evidence overlap requires a predicted
span and gold span to overlap within the same supplied context.

## Counts

| Measure | Count |
| --- | ---: |
| Evaluated contracts | 408 |
| Evaluated contexts | 408 |
| Target question instances | 459 |
| Gold answer spans | 205 |
| Gold-positive contracts | 154 |
| Scanner-positive contracts | 243 |
| Both positive | 115 |
| Scanner only | 128 |
| Gold only | 39 |
| Both negative | 126 |
| Both positive with at least one evidence overlap | 84 |
| Both positive without evidence overlap | 31 |

## Candidate flag calculations

- Precision against this narrower label: `115 / (115 + 128) = 0.473`
- Recall against this narrower label: `115 / (115 + 39) = 0.747`
- F1 from those two values: `0.579`
- Evidence overlap among jointly positive contracts: `84 / 115 = 0.730`

These are descriptive training-split calculations. The scanner was
developed after inspecting training examples. The 128 scanner-only
contracts cannot all be called legally incorrect: some may contain
other kinds of termination clauses. Likewise, any span overlap is
insufficient to establish a correct without-cause classification.

## Next evaluation work

Freeze a narrower classification rule and its exact output meaning
before running it on held-out CUAD data. Keep clause presence and
evidence overlap as separate measures. Report the rule revision,
denominators, misses, and unsupported examples with the result.