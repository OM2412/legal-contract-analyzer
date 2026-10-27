# CUAD liability held-out candidate retrieval audit

Run date: 4 October 2026  
Source: CUAD `test.json` (released test split)  
Scanner: `LiabilitySignalFinder`  
Source commit: record the hash printed immediately before the run; it was not included in the supplied result  
Unit: contract title with a target question  
Execution: `BUILD SUCCESS` (12.340 seconds)

| Measure | Cap On Liability | Uncapped Liability |
| --- | ---: | ---: |
| Evaluated contracts / contexts | 102 / 102 | 102 / 102 |
| Target question instances | 102 | 102 |
| Gold answer spans | 118 | 16 |
| Gold-positive contracts | 44 | 13 |
| Scanner-positive contracts | 46 | 46 |
| Both positive | 37 | 13 |
| Scanner only | 9 | 33 |
| Gold only | 7 | 0 |
| Both negative | 49 | 56 |
| Both positive with at least one evidence overlap | 27 | 7 |
| Both positive without evidence overlap | 10 | 6 |
| Candidate flag precision | 37 / 46 = **0.804** | 13 / 46 = **0.283** |
| Candidate flag recall | 37 / 44 = **0.841** | 13 / 13 = **1.000** |
| Candidate flag F1 | 74 / 90 = **0.822** | 26 / 59 = **0.441** |

For each label, the four contract-level cells sum to 102. Evidence overlap asks whether **at least one** predicted and annotated span overlap in a both-positive contract. It is not a span-level precision or recall estimate.

## Interpretation

`LiabilitySignalFinder` flags generic liability wording. It does not decide whether a clause caps or uncaps liability. In particular, the `Uncapped Liability` audit has 33 scanner-only contracts against the narrower CUAD label. Its 1.000 candidate recall is therefore not an uncapped-liability classification score. Neither label supplies legal risk severity or evaluates the Agreement-versus-SOW product comparison.

These test-split results are separate from the training-split exploratory audit used during development. Do not modify the scanner using test examples and continue to call this same split untouched held-out evaluation. Preserve the scanner version and dataset archive hash alongside any published comparison.
