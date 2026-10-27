# External dataset manifest

Inventory date: 2026-10-03 (Asia/Kolkata). Raw archives and extracted records are kept under `data/external/` and are ignored by Git. This document records provenance and the observed **training-file** inventory. The held-out `test.json` contents were not inspected for labels or examples.

## CUAD v1

- Publisher: The Atticus Project.
- Official page: https://www.atticusprojectai.org/cuad/
- Archive URL used: https://github.com/The-Atticus-Project/cuad/raw/refs/heads/main/data.zip
- Local archive: `data/external/cuad.zip`
- SHA-256: `F8161D18BEA4E9C05E78FA6DDA61C19C846FB8087EA969C172753BC2F45B999A`
- Archive entries: `CUADv1.json`, `train_separate_questions.json`, `test.json`.
- License: CC BY 4.0 per publisher's official page. Retain attribution to The Atticus Project and cite the CUAD paper.

Observed `train_separate_questions.json`: 408 distinct titles, 408 data entries, 408 paragraphs, 22,450 question instances and 41 distinct question labels. These are **question-instance counts**, not counts of independent contracts or distinct legal clauses. Do not aggregate answered instance counts into contract-level prevalence without deduplication and schema checks.

| Relevant exact training label | Question instances | Answered instances | Product mapping |
| --- | ---: | ---: | --- |
| `Termination For Convenience` | 459 | 205 | Subset of termination clauses; current generic cue is not equivalent to this label |
| `Cap On Liability` | 731 | 554 | Future liability clause retrieval |
| `Uncapped Liability` | 461 | 151 | Future liability clause retrieval |
| `Ip Ownership Assignment` | 564 | 257 | Future IP clause retrieval |
| `Post-Termination Services` | 623 | 368 | Separate post-termination obligation retrieval |
| `Notice Period To Terminate Renewal` | 417 | 104 | Renewal notice, not every termination notice |

The payment-timing comparator and Agreement-versus-SOW precedence are **not** equivalent to these CUAD tasks. Evaluate them on separately labeled document pairs.

## ContractNLI

- Publisher: ContractNLI authors and project hosted at Stanford NLP.
- Official page and terms: https://stanfordnlp.github.io/contract-nli/
- Archive URL used: https://stanfordnlp.github.io/contract-nli/resources/contract-nli.zip
- Local archive: `data/external/contract-nli.zip`
- SHA-256: `E03FC77BBF8B53E2976A250E81D8A294BC3D5E5FB014521E477DEE9340D6287B`
- Archive: 615 entries, including `train.json`, `dev.json`, `test.json`, `README.md`, `LICENSE`, `TERMS` and original raw files. Only JSON and documentation were extracted.
- License: CC BY 4.0; retain the supplied license and terms and cite the ContractNLI paper.

Observed `train.json`: 423 documents, 17 fixed NDA hypotheses and 7,191 document-hypothesis annotations: Entailment 3,530; Contradiction 841; NotMentioned 2,820. This distribution is **training only**. It describes NDA hypothesis inference, not two-document payment comparisons.

Candidate future product mappings to inspect, without treating them as existing product scores: `nda-16` return of confidential information, `nda-19` survival of obligations, `nda-7` sharing with third parties, and `nda-8` notice on compelled disclosure. Preserve the original hypothesis wording and evidence span labels for evaluation.

## Evaluation boundaries

1. Inventory and tune only on training data. Use development data for selection; keep test data sealed until a frozen run.
2. For CUAD, verify question/answer offsets against context and group all chunks of a source contract together. Measure clause retrieval and evidence separately.
3. For ContractNLI, report the three-class task and evidence identification separately. The training label counts above are not model results.
4. The project's EVAL-001–020 fixtures were used during development. Do not mix their results with held-out performance or call them CUAD/ContractNLI scores.
5. Neither source supplies a validated `HIGH`/`MEDIUM`/`LOW` business risk rubric for this application. Current automated findings remain `REVIEW_ONLY`.

## Papers

- CUAD: https://arxiv.org/abs/2103.06268
- ContractNLI: https://aclanthology.org/2021.findings-emnlp.164/
