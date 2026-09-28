# Legal Contract Analyzer

A local Java application that reviews payment terms in a software services Agreement and Statement of Work (SOW). It shows source quotes, compares supported terms, checks an illustrative payment policy, and offers locally generated AI suggestions.

## What it does

- Upload two text-based PDFs or UTF-8 TXT files.
- Extract supported payment clauses and display their exact source quotes.
- Compare Agreement and SOW payment terms.
- Scan for supported wording about which document's payment term takes priority.
- Check recognized terms against a configurable maximum-days policy.
- Suggest additional payment quotes using a local Ollama model.
- Show AI payment candidates only when their days and day unit match the source quote.
- Print the review or save it as a PDF from the browser.

The rule-based review and AI suggestions are shown separately. AI suggestions do not change the review verdict automatically.

## Requirements

- Java 25
- Maven
- Ollama with the `qwen3:4b` model for optional AI suggestions

## Run locally

From the project folder, run the tests:

```powershell
mvn test
```

Start the web application:

```powershell
mvn org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.LegalContractWebApplication'
```

Open `http://127.0.0.1:8080` in your browser.

The document review works without Ollama. To use the AI suggestions, install Ollama, ensure its local service is running, and download the model:

```powershell
& "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe" pull qwen3:4b
```

## Try the demo

Upload `testdata/agreement.pdf` as the Agreement and `testdata/sow.pdf` as the SOW if those generated sample files are present.

The sample Agreement states 30 calendar days; the SOW states 60 calendar days. With the default 30-day demo policy, the Agreement is within policy and the SOW deviates.

You can change the policy limit in the upload form. For example, a 90-day limit puts both recognized sample terms within that illustrative policy.

To see why AI suggestions are separate, upload `testdata/alternate-agreement.txt` with the sample SOW. The rule-based review may be incomplete, while local AI can suggest the exact 45-day sentence as a candidate.

## API

`POST /api/review` accepts multipart fields:

- `agreement`: PDF or TXT file
- `sow`: PDF or TXT file
- `maxDays`: optional integer from 1 to 3650; defaults to 30

`POST /api/ai/quotes` accepts one multipart field, `document`. It returns exact quote suggestions and any payment candidates that pass the quote checks. This endpoint requires local Ollama.

## Current limits

- Text-based PDFs use embedded text. Fully image-only PDFs of up to 20 pages use local Tesseract OCR with English language data.
- Tesseract must be installed on the machine running the backend. On Windows the default path is `C:\Program Files\Tesseract-OCR\tesseract.exe`; set `TESSERACT_PATH` if installed elsewhere.
- Mixed PDFs containing both text pages and scanned pages are not fully supported yet: OCR currently runs only when the PDF has no extractable text at all.
- OCR can misread characters. Check every quoted result against the original scanned page before relying on it.
- PDF uploads are limited to 10 MB and 100 pages; TXT uploads to 1 MB.
- The rule-based extractor recognizes a narrow set of payment wording.
- AI suggestions are limited to documents of 12,000 code points and depend on the local model.
- A quote matching the source confirms its wording, not its legal meaning.
- The policy is a user-selected demo business preference, not a legal standard.

Read the complete original documents and verify every quote before making a decision.