# Legal Contract Analyzer - Project State

## Goal

Build an evidence-based contract review platform. The current working feature compares payment terms in a software service Agreement and Statement of Work (SOW).

## Current implementation

- Java 25, Maven, Spring Boot, and a static HTML/JavaScript interface.
- Upload text-based PDF or UTF-8 TXT documents.
- Extract supported payment clauses with exact source quotes, text offsets, and PDF page numbers.
- Compare Agreement and SOW payment terms and scan supported payment-priority wording.
- Show every recognized clause when a document contains multiple matches; flag the review for human attention instead of selecting one silently.
- Apply an illustrative maximum-calendar-days policy, configurable in the browser; default is 30 days after invoice receipt.
- Keep invoice receipt, invoice date, and final acceptance as distinct payment triggers.
- Treat unspecified day units as unknown; do not convert business days into calendar days.
- Use local Ollama (`qwen3:4b`) for optional quote suggestions. Verify suggested quotes and extracted details against source text.
- Keep AI suggestions separate from the rule-based review verdict.
- Print the browser review or save it as a PDF.
- Maven tests cover supported wording, ambiguity, policy decisions, and AI candidate verification.

## Supported rule-based wording examples

- "Client shall pay Provider the project fee within 30 calendar days after receipt of the invoice."
- "The Client must pay the Provider the project fee within 45 calendar days after receiving the invoice."
- "Client shall pay Provider the project fee within 30 calendar days after final acceptance."
- "Client shall pay Provider the project fee within 30 calendar days after the invoice date."
- "Client shall pay Provider the project fee no later than 30 calendar days after receipt of the invoice."

These are examples of narrow supported patterns, not a claim that equivalent contract language is generally covered.

## Example files

The `testdata/` folder contains Agreement and SOW samples, including invoice-date, final-acceptance, business-day, unspecified-day, multiple-clause, and "no later than" examples.

`alternate-agreement.txt` uses different wording that the rule-based extractor does not currently recognize. Local AI can suggest its exact quote, but that suggestion does not complete the rule-based review.

## Run

From the project folder:

```powershell
mvn test
mvn org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.LegalContractWebApplication'
```

Open `http://127.0.0.1:8080/`.

Ollama is required only for optional AI suggestions. On this Windows machine its executable is at `$env:LOCALAPPDATA\Programs\Ollama\ollama.exe`.

## Current limits

- Scanned PDFs require OCR and are not supported yet.
- Rule-based extraction covers only tested wording patterns.
- A recognized quote does not establish whether the entire document is legally effective or which term controls.
- AI suggestions must be checked against the original documents.
- Policy `P-DEMO-30` is an illustrative business preference, not a legal standard.

## Planned phases

1. Payment-term MVP: implemented for supported wording.
2. Payment wording coverage and ambiguity handling: in progress.
3. Broader contract clauses, document processing, and evaluation benchmark: planned.
4. Persistent review data and user accounts: planned.
5. React interface with a polished, animated 3D landing page and accessible review experience: planned.
6. Deployment, security review, and production readiness: planned.

These phases describe direction, not a completion percentage.

## Working style

The user edits files manually in VS Code on Windows. Give one file or step at a time, its PowerShell open/create command, complete code when replacing a file, and a check for the result. Do not provide a ZIP.

Inspect an existing file before changing it. Use `mvn test` without `clean` because OneDrive has previously locked files under `target/`.

## Git

The project has local Git commits on the `main` branch. Check `git status --short` and `git log -3 --oneline` before describing the latest commit or staging further changes.

## Next step

Continue payment wording coverage one case at a time. First verify the invoice-date and "no later than" browser results, then choose the next real wording example and add a focused regression test.