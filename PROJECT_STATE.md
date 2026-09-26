# Legal Contract Analyzer — Project State

## Goal

Build an evidence-based payment-term review for software service Agreements and Statements of Work (SOWs).

## Current implementation

- Java 25, Maven, Spring Boot web application, and a static HTML/JavaScript interface.
- Upload text-based PDF or UTF-8 TXT documents.
- Rule-based extraction and comparison of supported payment clauses.
- Exact source quotes with text offsets and PDF page numbers.
- Scan for supported Agreement/SOW payment-priority wording.
- Configurable illustrative maximum-days policy; default is 30.
- Local Ollama (`qwen3:4b`) suggests payment quotes.
- AI payment candidates appear only after their days and day unit match a verified source quote.
- AI candidates remain separate from the rule-based review verdict.
- Browser Print / Save as PDF option.
- Maven tests for core logic and AI candidate verification.

## Demonstrated examples

- Sample Agreement: 30 calendar days after invoice receipt.
- Sample SOW: 60 calendar days after invoice receipt.
- Alternate Agreement TXT: 45 calendar days with different wording. The rule-based review is incomplete; AI finds the exact quote. Its invoice trigger is not independently verified.

## Run

```powershell
mvn test
mvn org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.LegalContractWebApplication'
```

Open `http://127.0.0.1:8080`.

Ollama must be running locally for AI suggestions. Its Windows executable was found at `$env:LOCALAPPDATA\Programs\Ollama\ollama.exe`.

## Limits

- Scanned PDFs need OCR and are not supported.
- Rule-based extraction recognizes limited wording.
- AI suggestions require checking against the original documents.
- The example policy is a business preference, not a legal standard.

## Working style for continuation

The user edits files manually in VS Code on Windows. Give one file or step at a time, with the PowerShell command to open/create the file, exact code to paste, and a way to check the result. Do not provide a ZIP.

Before changing an existing file, inspect its current contents. Use `mvn test` without `clean` because OneDrive has previously locked files under `target/`.

## Git

The first local commit is `6bef181` (`Build local contract payment analyzer MVP`). No remote repository is configured yet. Check `git status` before any future commit.

## Next checks

Verify the browser's 30-day versus 90-day policy input and Print / Save as PDF flow. Then decide the next product feature based on those results.