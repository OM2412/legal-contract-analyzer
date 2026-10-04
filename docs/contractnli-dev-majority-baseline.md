# ContractNLI train-to-dev majority baseline

Run date: 4 October 2026
Source: ContractNLI `train.json` and `dev.json`
Test split: `test.json` not opened
Baseline: per-hypothesis majority choice from train annotations
Tie rule: `Entailment`, then `Contradiction`, then `NotMentioned`
Source commit: not supplied with the run output

This baseline predicts the same train-majority choice for every development document under a given fixed hypothesis. It does not read document text to make a prediction and does not predict evidence spans.

| Split | Documents | Hypothesis instances |
| --- | ---: | ---: |
| Train | 423 | 7,191 |
| Development | 61 | 1,037 |

## Development confusion matrix

Rows are gold labels; columns are predictions.

| Actual / Predicted | Entailment | Contradiction | NotMentioned | Support |
| --- | ---: | ---: | ---: | ---: |
| Entailment | 402 | 19 | 98 | 519 |
| Contradiction | 19 | 56 | 20 | 95 |
| NotMentioned | 128 | 47 | 248 | 423 |
| Predicted total | 549 | 122 | 366 | 1,037 |

## Development metrics

| Class | Precision | Recall | F1 |
| --- | ---: | ---: | ---: |
| Entailment | 0.732 | 0.775 | 0.753 |
| Contradiction | 0.459 | 0.589 | 0.516 |
| NotMentioned | 0.678 | 0.586 | 0.629 |

Accuracy: **706 / 1,037 = 0.681**
Macro F1: **0.633**

The diagonal sums to 706 and all nine confusion-matrix cells sum to 1,037. Undefined evidence quality: this baseline predicts no evidence spans; no evidence precision or recall is reported.

## Per-hypothesis decisions

| Hypothesis key | Train-majority prediction | Correct on dev |
| --- | --- | ---: |
| nda-1 | NotMentioned | 32 / 61 |
| nda-10 | NotMentioned | 32 / 61 |
| nda-11 | NotMentioned | 53 / 61 |
| nda-12 | Entailment | 40 / 61 |
| nda-13 | Entailment | 47 / 61 |
| nda-15 | Entailment | 39 / 61 |
| nda-16 | NotMentioned | 37 / 61 |
| nda-17 | NotMentioned | 44 / 61 |
| nda-18 | NotMentioned | 50 / 61 |
| nda-19 | Entailment | 43 / 61 |
| nda-2 | Contradiction | 44 / 61 |
| nda-20 | Contradiction | 12 / 61 |
| nda-3 | Entailment | 45 / 61 |
| nda-4 | Entailment | 50 / 61 |
| nda-5 | Entailment | 54 / 61 |
| nda-7 | Entailment | 39 / 61 |
| nda-8 | Entailment | 45 / 61 |

## Interpretation and next comparison

These numbers measure a label-prior baseline on development NDAs. They are not payment-term extraction, Agreement-versus-SOW comparison, risk severity, or the application's current Risk Studio accuracy. Do not present them as a learned contract-text model. A future text-aware candidate should use the released train split for fitting, the development split for model selection, and the untouched test split once for its final frozen evaluation. Document the candidate's compute and evidence verification separately.
