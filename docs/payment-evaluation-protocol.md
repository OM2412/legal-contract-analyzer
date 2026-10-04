# Payment extraction evaluation protocol

## Purpose and dataset boundary

Evaluate the product on fresh Agreement/SOW pairs that were not used to write rules or inspect earlier failures. The existing EVAL-001–020 examples and other synthetic fixtures are development regressions; report them separately. A `heldout` filename alone does not make a case independent: choose and label the sources before running this version of the analyzer on them.

Do not commit private client contracts or identifying details. Record the source and permission to use each public excerpt. Prefer complete short documents or coherent excerpts with enough context to identify the parties and obligations.

## Select and freeze pairs

Choose a varied collection of Agreement/SOW pairs, including no-payment negatives, multiple installments, invoice receipt versus invoice date, acceptance, business days, exceptions, and unclear wording. Keep all excerpts from one underlying contract in the same split.

1. Put each UTF-8 `.txt` source in `testdata/product-evaluation/heldout/`. PDF/OCR cases need a separate extraction protocol; this first set evaluates the text review pipeline.
2. Annotate both documents without seeing the analyzer output. For each genuine payment obligation, record exact continuous quote, code-point start/end offsets, days (or `null` if unstated), day unit, trigger, payer, payee and scope. Set a field to `null` when the source does not support it. Use an empty `paymentObligations` array for a genuine negative. Keep conditional obligations and ambiguous wording in reviewer notes, not as assumed definite terms.
3. Enter each file's SHA-256 digest in a JSON manifest. The following is a **shape example, not a valid completed label**; replace paths, hashes, offsets and facts with independently annotated data:

```json
{
  "schemaVersion": 1,
  "split": "heldout",
  "cases": [
    {
      "id": "PAIR-001",
      "agreement": {
        "path": "testdata/product-evaluation/heldout/PAIR-001-agreement.txt",
        "sha256": "<64-hex-character-file-hash>",
        "paymentObligations": [
          {
            "start": 0,
            "end": 0,
            "quote": "<exact source text>",
            "days": 30,
            "dayUnit": "CALENDAR_DAYS",
            "trigger": "INVOICE_RECEIPT",
            "payer": "Customer",
            "payee": "Provider",
            "scope": "project fee"
          }
        ]
      },
      "sow": {
        "path": "testdata/product-evaluation/heldout/PAIR-001-sow.txt",
        "sha256": "<64-hex-character-file-hash>",
        "paymentObligations": []
      },
      "reviewerNotes": "Explain ambiguous or conditional wording here."
    }
  ]
}
```

Offsets count Unicode code points, with `end` exclusive. Day units: `CALENDAR_DAYS`, `BUSINESS_DAYS`, `UNKNOWN`. Triggers: `INVOICE_RECEIPT`, `INVOICE_DATE`, `FINAL_ACCEPTANCE`, `OTHER`, `UNKNOWN`. The validator checks file hashes, exact quote locations and field shapes; it cannot decide whether a payment label is legally correct. Have a second reader review disputed labels before freezing them.

In PowerShell, compute a source hash and validate the completed manifest:

```powershell
(Get-FileHash "testdata/product-evaluation/heldout/PAIR-001-agreement.txt" -Algorithm SHA256).Hash.ToLowerInvariant()
node scripts/validate-product-labels.mjs testdata/product-evaluation/heldout/labels.json
```

Commit the sources, reviewed labels and validator output hash **before** running `/api/review` on these pairs. If any label must change later, record why, the previous version and whether the case was already seen by the system. Do not silently revise labels to match predictions.

## Scoring after freeze

Score obligation retrieval against the frozen labels with a declared one-to-one evidence matching rule. Report true positives, false positives and misses per document, then precision, recall and F1 with denominators. Report quote overlap and exact quote validity separately. For matched obligations, count errors in days, unit, trigger, payer, payee and scope separately; do not count an unknown gold field as a correct guessed value.

Record review status and appropriate `REVIEW_REQUIRED` abstention for multiple or partly unparsed clauses. Score comparison and illustrative policy assessment separately from obligation extraction. Do not treat priority wording, risk signals or policy deviation as legal enforceability or legal risk severity labels.

The current API exposes structured `agreementTerm`/`sowTerm` only for a selected single term; `agreementMatches`/`sowMatches` expose evidence for multiple matches. Until the API exposes fields for every match, do not claim multi-obligation field accuracy from those evidence-only arrays.

## Run record

For each run save date, Git commit, manifest SHA-256, document hashes, policy limit, counts, denominators and an error ledger. Keep this held-out report separate from EVAL-001–020 regressions, CUAD clause-candidate audits and ContractNLI inference experiments.
