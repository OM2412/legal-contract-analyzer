# Dataset evaluation plan: CUAD and ContractNLI

## Status and purpose

This plan describes work to be done. The present application uses tested payment wording rules, an optional pretrained local Ollama model for verified quote suggestions, and a narrow termination cue. It has not been trained on CUAD or ContractNLI. Synthetic fixtures used while building these rules are development checks, not unseen accuracy estimates.

The immediate goal is to measure clause retrieval and evidence quality on independent documents. Fine-tuning is a later decision based on recorded errors and a held-out comparison with the existing system.

## Sources and fit

| Source | Official task | Product use | Boundary |
| --- | --- | --- | --- |
| CUAD v1 | Extract annotated clause context and answers from 510 commercial contracts, across 41 categories | Evaluate selected clause discovery and evidence spans | CUAD labels are not risk severity labels; its categories cannot automatically evaluate our payment timing comparator |
| ContractNLI | For 17 fixed hypotheses in 607 NDAs, predict Entailment, Contradiction or NotMentioned and identify supporting spans where applicable | Separate evidence-backed hypothesis experiment | NDA inference is not an Agreement-versus-SOW payment comparison benchmark |
| Our Agreement/SOW set | Annotate payment obligations, triggers, units, parties, scope, precedence wording and document pairs | Direct product evaluation | Existing EVAL-001–020 development examples must be reported separately from unseen test documents |

CUAD candidate mapping for the first inventory: `Termination For Convenience` is narrower than our generic termination cue; `Cap On Liability` and `Uncapped Liability` may support future liability clause retrieval. Verify names, label definitions, positive counts and sample spans against the downloaded official files before implementing a mapper. Do not force CUAD categories onto payment timing, priority clauses, or legal risk levels.

## Acquisition and provenance

1. Download CUAD from The Atticus Project's official repository/dataset page and ContractNLI from its official Stanford-hosted page. Preserve their original README, license and released splits.
2. Store raw downloads under `data/external/cuad/` and `data/external/contractnli/`; ignore these directories in Git. Commit only our own manifests, scripts, small derived aggregate results and permitted short examples with attribution.
3. Record source URL, retrieval date, dataset version or commit, SHA-256 of each archive, license, expected files, and parsing assumptions in `docs/dataset-manifest.md`.
4. Inspect schemas and selected labels before writing transformations. Never silently fix or relabel gold annotations to match model outputs.

Both sources state CC BY 4.0. Cite the dataset papers and authors in reports and attribute the datasets in the project documentation.

## Evaluation protocol

### CUAD clause retrieval

- Choose a small, declared set of labels after inspecting the official taxonomy. Report the exact labels and the number of positive and negative contracts for each.
- Treat a contract as the split unit. Respect any official split applicable to the chosen format; if constructing a new split, group all chunks, questions and spans of the same contract together. Keep a held-out set untouched during rule tuning.
- Run the same frozen documents through (a) current deterministic scanner, (b) optional verified AI suggestion route, and, only after a separate experiment, (c) any trained model.
- Count true positives, false positives and misses by clause category. Report precision, recall and F1 with denominators. Separately measure whether predicted spans overlap the annotated clause and whether each quote exactly occurs in the supplied text.
- Log OCR failure, missing text, truncation, unsupported wording and ambiguous annotations as separate error types. Do not interpret absence of a matched span as proof the legal right is absent.

### ContractNLI inference

- Keep this as a separate NDA experiment using the dataset's own hypotheses and released train/development/test partitions.
- Record three-class confusion matrix, macro F1 and per-label precision/recall; report evidence-span quality only for labels where the source defines evidence.
- Start with a baseline on a bounded, preselected sample of hypotheses. Do not present results as our product's payment comparison accuracy or cross-document legal priority accuracy.

### Our direct product evaluation

- Freeze a fresh Agreement/SOW set not used to invent rules. Label complete document pairs and every payment obligation, including negative, conditional, multiple, and unparseable examples.
- Measure extraction and field errors separately from document comparison, policy assessment, evidence correctness, and appropriate `REVIEW_REQUIRED` abstention.
- Keep the existing 20 EVAL examples as development/regression material; report them separately from held-out results. Record the Git commit, policy configuration and model version for every run.

## Fine-tuning decision

Fine-tune only if the frozen baseline identifies recurring misses that selected labeled data can plausibly address. Compare a trained candidate against the same held-out examples and document the compute used. A possible first candidate is a smaller clause retrieval or classification model; training the current Ollama `qwen3:4b` model is not a prerequisite. AI outputs remain suggestions until an exact source quote is verified. Legal risk severity needs a separate, reviewed policy rubric and labels; neither dataset directly supplies one.

## Next deliverables

1. Dataset manifest and ignored local raw-data directories.
2. Schema/label inventory with relevant CUAD label counts and ContractNLI hypothesis mapping.
3. Reproducible baseline evaluator and error ledger.
4. Held-out report comparing deterministic and verified-AI routes; only then decide whether to fine-tune.

## Primary references

- CUAD overview: [https://www.atticusprojectai.org/cuad/](https://www.atticusprojectai.org/cuad/)
- CUAD official repository: [https://github.com/The-Atticus-Project/cuad](https://github.com/The-Atticus-Project/cuad)
- CUAD paper: [https://arxiv.org/abs/2103.06268](https://arxiv.org/abs/2103.06268)
- ContractNLI dataset and usage terms: [https://stanfordnlp.github.io/contract-nli/](https://stanfordnlp.github.io/contract-nli/)
- ContractNLI paper: [https://aclanthology.org/2021.findings-emnlp.164/](https://aclanthology.org/2021.findings-emnlp.164/)
