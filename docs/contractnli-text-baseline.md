# ContractNLI text-aware development baseline

Run date: 4 October 2026
Data: train.json for fitting; dev.json for evaluation
Test split: test.json not opened
Model: separate hashed Bernoulli Naive Bayes classifier per hypothesis
Features: 4,096 document word-presence hash buckets
Smoothing: Laplace alpha 1
Evidence prediction: none

| Metric | Train-majority baseline | Text-aware NB |
| --- | ---: | ---: |
| Dev documents | 61 | 61 |
| Dev hypothesis instances | 1,037 | 1,037 |
| Correct predictions | 706 | 703 |
| Accuracy | 0.681 | 0.678 |
| Macro F1 | 0.633 | 0.634 |

## Text-aware NB confusion matrix

Rows are gold labels; columns are predicted labels.

| Actual / predicted | Entailment | Contradiction | NotMentioned |
| --- | ---: | ---: | ---: |
| Entailment | 383 | 28 | 108 |
| Contradiction | 20 | 57 | 18 |
| NotMentioned | 120 | 40 | 263 |

| Class | Support | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: |
| Entailment | 519 | 0.732 | 0.738 | 0.735 |
| Contradiction | 95 | 0.456 | 0.600 | 0.518 |
| NotMentioned | 423 | 0.676 | 0.622 | 0.648 |

The text-aware model improved nda-20 from 12/61 to 31/61 correct
development predictions. Its overall accuracy fell by three predictions,
and the rounded macro F1 increased by only 0.001. This does not establish
a useful overall improvement.

No evidence spans were predicted or scored. These NDA inference results
do not measure the application's payment review, risk severity, or
Agreement-versus-SOW comparison. Keep the test split unopened until a
candidate and evaluation procedure are frozen.