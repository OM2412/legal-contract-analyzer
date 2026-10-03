# CUAD liability contract-level training audit

Run date: 4 October 2026  
Source: `data/external/cuad/train_separate_questions.json`  
Held-out `test.json`: not read

## Task boundary

`LiabilitySignalFinder` flags generic liability wording for human
review. It does not decide whether liability is capped or uncapped.
The same scanner output was compared separately with CUAD's
`Cap On Liability` and `Uncapped Liability` labels.

The unit is a contract title with a target question. Repeated questions
for that contract are grouped. Evidence overlap requires a scanner
span and a gold span to overlap in the same supplied context.

## Results

| Measure | Cap On Liability | Uncapped Liability |
| --- | ---: | ---: |
| Evaluated contracts | 408 | 408 |
| Target question instances | 731 | 461 |
| Gold answer spans | 554 | 151 |
| Gold-positive contracts | 231 | 98 |
| Scanner-positive contracts | 240 | 240 |
| Both positive | 201 | 94 |
| Scanner only | 39 | 146 |
| Gold only | 30 | 4 |
| Both negative | 138 | 164 |
| Both positive with evidence overlap | 175 | 71 |
| Both positive without evidence overlap | 26 | 23 |
| Candidate flag precision | 0.838 | 0.392 |
| Candidate flag recall | 0.870 | 0.959 |
| Candidate flag F1 | 0.854 | 0.556 |

The displayed candidate precision is `both positive / scanner positive`.
Candidate recall is `both positive / gold positive`. F1 is calculated
from those two values. Evidence overlap is recorded separately.

## Interpretation

The identical scanner-positive count for both labels shows that this
is one generic retrieval cue, not two label-specific classifiers.
In particular, a signal overlapping an `Uncapped Liability` answer
does not prove that the application understood an uncapped exception.

These are development-split figures after training examples informed
the rule design. They are not independent accuracy estimates, risk
severity scores, or evidence of legal correctness. The CUAD held-out
test split remains unopened.

## Next step

Define and freeze the output meaning of separate cap and exception
signals before a single held-out evaluation. Keep generic liability
review cues available without presenting them as cap classifications.