# Legal Contract Analyzer — Project State

## Goal

Build an evidence-based contract review platform for software service Agreements and Statements of Work (SOWs). The current working feature reviews payment terms, shows source evidence, and flags uncertainty for a human reviewer. It does not provide legal advice.

## Development environment

- Windows 11, VS Code
- Java 25, Maven 3.9.16
- Spring Boot backend with a static HTML/JavaScript browser interface
- Apache PDFBox for text-based PDF extraction
- Optional local Ollama model: `qwen3:4b`
- Project folder: `C:\Users\omjad\OneDrive\Desktop\legal-contract-analyzer`
- Git branch: `main`
- Latest confirmed commit: `698b33b` — `Record negative payment evaluation case`
- No Git remote was shown by `git remote -v` when this file was updated.

## Working features

- Upload an Agreement and SOW as text-based PDF or UTF-8 TXT.
- Extract supported payment obligations and show exact quotes, text offsets, and PDF page numbers.
- Compare days, day units, trigger, payer, payee, and payment scope.
- Scan supported payment-priority wording.
- Show all recognized matches if a document contains multiple payment clauses, and require human review instead of selecting one silently.
- Apply a configurable illustrative calendar-day policy; default is 30 days after invoice receipt.
- Distinguish invoice receipt, invoice date, final acceptance, and other/unknown triggers.
- Leave business-day conversion and unclear wording unassessed where appropriate.
- Show explanations, warnings, document identity, and Print / Save as PDF in the browser.
- Offer optional local Ollama quote suggestions, verified against extracted document text. These suggestions cannot change the rule-based review verdict.

## Latest evaluation work

See `docs/payment-evaluation-protocol.md` and `testdata/evaluation/`.

- EVAL-001–005 were used during development. Do not report their post-fix results as independent benchmark accuracy.
- EVAL-004: passive-voice payment clause. The extractor recognizes Customer as payer, Vendor as payee, 20 calendar days, and invoice receipt.
- EVAL-005: two installments, 40% after signing and 60% after final acceptance. Both are separately extracted; the review requires human attention. The CLI now displays both matches and their source quotes.
- EVAL-006: a negated payment statement mentioning an invoice. Baseline extraction reports zero Agreement payment obligations and zero false positives for this one sample.
- EVAL-002 has a partial-coverage fixture; verify whether its annotation file exists before treating the evaluation set as completely labeled.
- The planned set is 20 varied, pre-labeled excerpts. Freeze labels before computing evaluation metrics. Report development examples separately from held-out results.
- Never commit private client contracts or identifying details.

## Run locally

From the project folder:

```powershell
mvn test
mvn org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.LegalContractWebApplication'
## Continuation checkpoint — 28 September 2026

- Git branch: `main`; latest confirmed GitHub commit: `dbe3c99` (`Record payment evaluations and flag milestone wording`).
- `mvn test` passed before that commit. `main` and `origin/main` were aligned afterward.
- `pom.xml` has an uncommitted change. Inspect it before deciding whether to keep or commit it.
- Payment evaluation is in progress under `testdata/evaluation/`; keep baseline observations separate from improvements made after seeing a sample.
- EVAL-009: a payment obligation with a disputed-amount exception was missed; review status was `REVIEW_REQUIRED`.
- EVAL-010: a 20-business-day term triggered by invoice date was extracted with the expected fields. Its policy assessment was `UNABLE_TO_ASSESS` because the policy uses calendar days.
- EVAL-011: two milestone payment obligations without day counts were both missed. Initially the review was `INCOMPLETE`. A narrow coverage-scanner change now flags the sentence and produces `REVIEW_REQUIRED`, but does not extract either obligation.
- Next: continue labeling varied evaluation samples before running them. Record misses and false positives honestly; do not report accuracy from samples used to tune extraction. Keep the optional Ollama suggestions separate from deterministic results.
- Working style: one file or step at a time; give PowerShell command, paste-ready content, and verification. Do not use `mvn clean` on this OneDrive folder.
## Evaluation continuation — 28 September 2026, late evening

- EVAL-012: late-fee threshold was correctly not extracted as a primary payment deadline; baseline false positives: zero.
- EVAL-013: `thirty (30) calendar days` was missed. The scanner returned `REVIEW_REQUIRED`; written and numeric values have not yet been implemented or cross-checked.
- EVAL-014: payment method `by electronic bank transfer` initially caused one miss. A narrow extractor change now extracts the 30-calendar-day invoice-receipt term; its baseline miss remains recorded separately.
- EVAL-015: 45 days after Provider **submits** an invoice was missed and flagged `REVIEW_REQUIRED`. Do not relabel invoice submission as invoice receipt.
- EVAL-016: a quoted 30-day nonoperative example was initially extracted as one false positive alongside the correct 45-day clause. A narrow guard now excludes the disclaimed example. The baseline false positive remains in its label.
- Evaluation samples are self-written and were used to discover and fix defects. Do not present their post-fix results as independent benchmark accuracy. The 20 varied-excerpt starting target is not yet complete; EVAL-017 through EVAL-020 remain.
- Check `git status -sb` and `git log -4 --oneline` to verify the latest pushed commits. `pom.xml` had an uncommitted packaging change; inspect its diff before using or committing it.
- Next implementation sequence: keep the 20 self-written evaluation examples and their baseline/post-fix observations distinct; verify OCR on varied multi-page documents, then continue persistent data, React UI, security and deployment. These examples are development cases, not an independent accuracy benchmark.
## Packaging checkpoint — 28 September 2026

- Latest confirmed GitHub commit: `a1725b1` (`Package executable Spring Boot JAR`); `main` and `origin/main` are aligned and the working tree was clean.
- `mvn package` completed successfully after configuring the Spring Boot Maven plugin with classifier `exec`.
- Executable output: `target/legal-contract-analyzer-0.1.0-SNAPSHOT-exec.jar`.
- Run locally with `java -jar "target/legal-contract-analyzer-0.1.0-SNAPSHOT-exec.jar"` and open `http://127.0.0.1:8080/`. - Executable JAR verified on port 8081: the PDF review API returned Agreement and SOW evidence with page numbers, a 30-versus-60-day comparison, and `SOW_TEXT_CANDIDATE`.
- An earlier package attempt failed while renaming a JAR inside OneDrive's `target` folder; the classifier resolved that build failure.
- The executable JAR served the PDF review API locally; the two-page image-only OCR test passed. Fully image-only PDFs of up to 20 pages now use local Tesseract. Mixed text/scanned PDFs now use per-page OCR for pages without extractable text; the two-page mixed-PDF test passed; production deployment, database and React/3D UI remain future work.