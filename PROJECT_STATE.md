# Folio - Project State

Updated: 3 October 2026

## Goal

Build an evidence-based legal document analyzer and contract risk identification system. The current workflow reviews a software service Agreement and Statement of Work (SOW). It shows source evidence and flags uncertainty for human review. It does not provide legal advice.

## Branches and deployment

- `main`: existing `folio-contract-demo` Render service. Its current deployment is separate from the accounts preview.
- `feature/accounts-and-saved-reviews`: React interface, Risk Studio, accounts, PostgreSQL, and saved reviews.
- Latest confirmed feature-branch commit: `47de4d2` (`Prepare isolated accounts preview deployment`), pushed to GitHub.
- `folio-accounts-preview`: separate Render web service and PostgreSQL database, created from `render.accounts.yaml`.
- The hosted accounts workflow was reported successful: register, review, save, refresh, and reopen a saved review.
- Render Free PostgreSQL expires after 30 days. This preview is not a durable production database.
- `frontend/src/RiskSignalMap.css` is untracked. Inspect its use before adding or removing it.

## Stack

- Java 25, Spring Boot, Maven, and PostgreSQL
- Flyway migration `V1__create_accounts_and_reviews.sql`
- React and Vite frontend, packaged into the executable Spring Boot JAR
- PDFBox for PDF text and page evidence
- Tesseract for local OCR of supported scanned and mixed PDFs
- Optional local Ollama quote suggestions; AI is disabled in the hosted preview
- Docker for local image verification and Render deployment

## Working review features

- Upload Agreement and SOW as UTF-8 TXT or supported PDF.
- Extract supported payment terms and compare their days, units, triggers, parties, and payment scope.
- Show exact source quotes, text offsets, PDF pages, and OCR provenance.
- Display all recognized payment clauses when multiple obligations are found.
- Flag additional payment wording that the extractor cannot fully parse.
- Scan supported payment-priority wording.
- Apply a configurable illustrative calendar-day policy. The default limit is 30 days after invoice receipt.
- Show evidence-led Risk Studio findings for supported payment signals and narrow termination and liability wording. These signals require human interpretation.
- Inspect quotes against the uploaded original and print or save the review as PDF.
- Keep optional AI suggestions separate from the rule-based verdict.

## Accounts and saved reviews

- Users can register, sign in, run a review, and save a backend-computed result.
- Saved review queries are scoped to the signed-in account.
- A local two-account check returned `404` when Account B requested Account A's saved review.
- The hosted preview's register, review, save, refresh, and reopen flow was reported successful.
- The preview has an additional demo password gate. Do not put its password or database credentials in Git.

## Verification

- `mvn test` passed after the database configuration update.
- Local Docker image `folio-accounts-preview:dev` built successfully; its build ran the React build and Maven package.
- The Docker container served the application with `HTTP 200`.
- Local PostgreSQL reported Flyway migration version `1` with `success = t`.
- The separate Render accounts preview was reported deployed successfully.

## Evaluation and limits

- `testdata/evaluation/` contains 20 self-written payment excerpts and their observations. Samples used to improve rules are development cases, not independent accuracy results.
- CUAD and ContractNLI archives are local and ignored by Git. Exploratory CUAD training audits informed narrow risk signals; they are not validated legal-risk accuracy scores.
- Keep held-out evaluation data separate from wording used to develop rules. Report misses, false positives, and evidence overlap honestly.
- OCR quotes need checking against scanned pages.
- A finding does not establish which contract term is legally controlling.
- Uploaded private contracts and dataset archives must not be committed.

## Next work

1. Freeze the current Risk Studio rules and evaluation protocol before using held-out benchmark data.
2. Evaluate supported clause categories with source evidence; record misses, false positives, and limits.
3. Polish the hosted demo workflow and responsive Risk Studio interface.
4. Address production requirements before treating the preview as a persistent service: durable database, backup and retention policy, session behavior, and security review.
5. Plan the eventual `main` merge with its database deployment changes. Do not merge the feature branch into `main` while the existing main service lacks the required database configuration.

## Working style

The project is edited manually in VS Code on Windows. Work one file or step at a time, with PowerShell commands and paste-ready code when replacing a file. Inspect existing files before changing them. Use `mvn test` without `clean` because OneDrive has previously locked files under `target/`.