# Real-contract evaluation intake — discovery stage

**Status:** Two real SEC-filed MSA/SOW sources found; **zero** independently labeled cases, **zero** real-world accuracy measurements. `testdata/product-evaluation/real-candidates.json` contains source locators only. It is intentionally not a gold-label file.

## Verified source candidates

1. **UTi / Gene Ochi (2015)** — SEC EDGAR [Exhibit 10.40](https://www.sec.gov/Archives/edgar/data/1124827/000119312515116229/d849038dex1040.htm). The filing includes an MSA and an integrated `Statement of Work #1`. Both refer to compensation and invoicing; the MSA and SOW should be reviewed as a pair.
2. **Sun Life / se² (2013)** — SEC EDGAR [Master Services Agreement exhibit](https://www.sec.gov/Archives/edgar/data/1074760/000119312515155139/d892742dex99i2.htm). Includes a SOW with charges and invoicing, plus agreement terms; more complex and longer than the first candidate.

These are public filings but **public access is not a blanket redistribution license**. Some filings carry confidential markings or omissions. We have not copied their full text into Git or uploaded them to an AI provider. Before downloading/excerpting, review the applicable reuse rights, redact sensitive information where necessary, and record a permission decision. Do not modify source meaning while redacting; preserve an unmodified local reference for offset verification if legally permitted.

## Blind labeling workflow

1. Independently identify the agreement and SOW boundaries. Keep each file's complete relevant section and surrounding context; don't select only clauses a scanner is likely to recognize.
2. Record source URL, filing identifier, access date, section range, reuse decision, UTF-8 normalization method and SHA-256 per document. Store permitted raw sources under ignored `data/external/`, not in the public repository by default.
3. Have an annotator who has **not seen Folio predictions** mark every actual payment obligation, including negative or contingent cases. For each span record Unicode code-point offsets, exact quote, payer, payee, amount/scope, days, unit, trigger and ambiguity notes. Use `null` when a fact is genuinely unknown, not a guessed value.
4. Have a second independent reader adjudicate disagreements and freeze labels/hashes before invoking the app. Do not use Folio output to revise gold labels.
5. With appropriate reuse permission, prepare a separate evaluation manifest following `docs/payment-evaluation-protocol.md`. Run `scripts/validate-product-labels.mjs` and `scripts/score-product-review.mjs` once on the frozen set. Preserve code SHA, model/provider, request settings and error ledger.
6. Report deterministic extraction, model-assisted quote retrieval, quote provenance, multi-obligation fields, review abstention and legal interpretation as **separate dimensions**. Neither SEC filing supplies legal-risk severity labels or independently audited legal conclusions.

## Explicit exclusions

- Existing `EVAL-001–020` and `PAIR-001` are development fixtures, not unseen data.
- SEC-filing discovery does not mean these pairs have been annotated or tested.
- CUAD and ContractNLI labels do not directly validate Folio's paired-document payment-comparison task.
- A three-case synthetic success rate is not a generalization estimate.

**Next decision gate:** Do not publish a real-world accuracy number until there are source-rights-cleared, independently labeled, genuinely unseen document pairs and a frozen evaluation protocol.
