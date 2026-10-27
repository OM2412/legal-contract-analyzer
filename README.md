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

## Render deployment

The repository currently contains three branches with different purposes:

| Branch | Purpose | Deployment notes |
| --- | --- | --- |
| `main` | Stable local/demo application | Uses `render.yaml`; it does not configure the external database profile. |
| `feature/accounts-and-saved-reviews` | Accounts, sessions, saved reviews, and Neon PostgreSQL preview | Uses `render.accounts.yaml` and is the branch used by `folio-accounts-preview`. |
| `security/release-hardening-01` | Release-security verification and hardening work | Review and validate this branch before promoting it to the preview or stable deployment. |

### Deploy the accounts preview

Create or update a Render Docker Web Service with:

- Repository: `OM2412/legal-contract-analyzer`
- Branch: `feature/accounts-and-saved-reviews`
- Dockerfile: `./Dockerfile`
- Docker context: `.`
- Health-check path: `/`
- Region: Singapore (or the region nearest the database)

Set these environment variables in the Render Dashboard. Never commit their values:

```text
SPRING_PROFILES_ACTIVE=db
FOLIO_DB_URL=postgresql://<user>:<password>@<host>/<database>?sslmode=require
FOLIO_DB_USER=<database-user>
FOLIO_DB_PASSWORD=<database-password>
FOLIO_DEMO_AUTH_ENABLED=false
FOLIO_DEMO_USERNAME=mentor
FOLIO_DEMO_PASSWORD=<optional-unused-demo-gate-password>
FOLIO_AI_ENABLED=false
FOLIO_PASSWORD_RESET_ENABLED=false
FOLIO_PASSWORD_RECOVERY_WORKER_ENABLED=false
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY=true
SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=lax
```

`FOLIO_DEMO_AUTH_ENABLED` must be `false` for a normal public website preview. When it is `true`, `DemoAccessFilter` rejects every request without HTTP Basic credentials, including `/`; the React login page cannot load and the service returns `401 Unauthorized`. This outer demo gate is separate from the application's account login. The application still protects authenticated API routes through Spring Security.

After changing environment variables or pushing a deployment commit:

1. Save the variables and trigger **Manual Deploy > Deploy latest commit**.
2. Wait for Render to show the service as **Live**.
3. Check the root response:

   ```powershell
   curl.exe -i https://<service-name>.onrender.com/
   ```

   A healthy public preview returns `200 OK`. A `401` response containing `WWW-Authenticate: Basic realm="Folio Demo"` means the outer demo gate is still enabled in Render or the service has not redeployed.
4. Open the root URL in a private browser window and test registration/login before testing protected review and saved-review routes.

The Docker entrypoint validates `FOLIO_DB_URL`, derives the JDBC URL, binds Spring Boot to Render's `PORT`, and binds to `0.0.0.0`. If the service fails before responding, inspect the Render deploy logs for database URL format, Flyway migration, or missing environment-variable errors. Do not paste passwords, connection strings, cookies, or session values into issue reports.

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
- Mixed PDFs are processed page by page: pages with embedded text keep that text, and pages without extractable text use local OCR. At most 20 pages per PDF can require OCR; blank pages also count toward this limit.
- OCR can misread characters. Check every quoted result against the original scanned page before relying on it.
- PDF uploads are limited to 10 MB and 100 pages; TXT uploads to 1 MB.
- The rule-based extractor recognizes a narrow set of payment wording.
- AI suggestions are limited to documents of 12,000 code points and depend on the local model.
- A quote matching the source confirms its wording, not its legal meaning.
- The policy is a user-selected demo business preference, not a legal standard.

Read the complete original documents and verify every quote before making a decision.