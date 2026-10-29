# Legal Contract Analyzer

Folio is an evidence-based Java and React application for reviewing payment terms in a software services Agreement and Statement of Work (SOW). It extracts source evidence, compares supported terms, identifies selected contract-risk signals, and applies an illustrative payment policy. It is a review aid, not legal advice.

## Features

- Upload an Agreement and SOW as PDF or UTF-8 TXT.
- Extract supported payment clauses and show exact source quotes, offsets, pages, and OCR provenance.
- Compare payment days, day units, triggers, parties, and payment scope.
- Scan supported wording about which document's payment term has priority.
- Apply a configurable maximum-days policy (default: 30 calendar days after invoice receipt).
- Explore evidence-led findings in Risk Studio.
- Inspect every quote against the uploaded original and print or save a review as PDF.
- Register, sign in, and save/reopen reviews when the database profile is enabled.
- Optionally display AI Studio, which sends documents to Ollama for quote suggestions.

Rule-based findings and AI suggestions are deliberately separate. AI suggestions never change the rule-based result automatically.

## Architecture

| Layer | Technology | Role |
| --- | --- | --- |
| Frontend | React 19, Vite 8 | Upload, review, Risk Studio, AI Studio, accounts, and saved-review UI |
| Backend | Java 25, Spring Boot 4.1, Maven | HTTP API, document extraction, authentication, review computation |
| Persistence | PostgreSQL, Spring Data JPA, Flyway | Accounts and saved reviews |
| PDF/OCR | Apache PDFBox, Tesseract | Embedded PDF text and supported scanned/mixed PDF pages |
| Optional AI | Ollama `qwen3:4b` | Local quote discovery and payment-detail extraction |
| Deployment | Multi-stage Docker image, Render | Builds Vite, packages the frontend into the Spring Boot JAR, and runs the web service |

The backend serves the compiled Vite files from the executable JAR. A separate frontend server is not required in production.

## Branches and deployment files

| Branch/file | Purpose |
| --- | --- |
| `main` | Stable demo workflow; uses `render.yaml` and does not enable the external database profile. |
| `feature/accounts-and-saved-reviews` | Accounts, sessions, saved reviews, and Neon/PostgreSQL preview; uses `render.accounts.yaml`. |
| `Dockerfile` | Builds the React bundle first, then packages it with Maven and runs Spring Boot. |
| `compose.db.yaml` | Starts a local PostgreSQL 17 database on `127.0.0.1:5433`. |

Before deploying, confirm that the Render service is connected to the intended branch and manifest. Do not merge the accounts branch into `main` until the `main` service has matching database configuration.

## Requirements

- Java 25
- Maven 3.9+
- Node.js 24 and npm (needed for the frontend build outside Docker)
- Docker Desktop (optional, for local database/container verification)
- Ollama with the `qwen3:4b` model (optional; required only for AI suggestions)
- Tesseract OCR with English language data (optional locally; required for scanned PDF OCR)

## Run locally without accounts

This mode uses the default local database configuration and is intended for the basic review workflow.

```powershell
mvn test
mvn org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.LegalContractWebApplication'
```

Open `http://127.0.0.1:8080`. The rule-based review does not require Ollama.

## Run locally with accounts and saved reviews

Start PostgreSQL first. `compose.db.yaml` uses port 5433 to avoid conflicting with a default PostgreSQL installation.

```powershell
$env:FOLIO_DB_PASSWORD = "choose-a-local-password"
docker compose -f compose.db.yaml up -d
$env:FOLIO_DB_JDBC_URL = "jdbc:postgresql://127.0.0.1:5433/folio"
$env:FOLIO_DB_USER = "folio"
$env:FOLIO_DB_PASSWORD = "choose-a-local-password"
$env:SPRING_PROFILES_ACTIVE = "db"
mvn test
mvn org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.LegalContractWebApplication'
```

Flyway creates the schema on startup. Stop the database when finished:

```powershell
docker compose -f compose.db.yaml down
```

Never commit local passwords, connection strings, cookies, uploaded contracts, or database dumps.

## Run the optional local AI

The AI client in `src/main/java/com/omjadon/contractanalyzer/ai/LocalOllamaClient.java` is intentionally local and currently uses the fixed endpoint `http://127.0.0.1:11434`. Install and start Ollama on the same machine as the backend, then pull the model:

```powershell
& "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe" pull qwen3:4b
```

Set `FOLIO_AI_ENABLED=true` when running the backend if the endpoint is otherwise disabled. The frontend AI Studio route is compiled by Vite; the Dockerfile currently uses `VITE_AI_ENABLED=true`.

If Ollama is unavailable, the normal document review still works. AI Studio will show an error when **Find quotes** is selected.

## Try the demo

Upload `testdata/agreement.pdf` as the Agreement and `testdata/sow.pdf` as the SOW, if those sample files are present. The sample Agreement states 30 calendar days and the SOW states 60 calendar days. With the default 30-day illustrative policy, the Agreement is within policy and the SOW deviates. A 90-day limit puts both recognized sample terms within that policy.

## Render deployment

### Accounts preview (`feature/accounts-and-saved-reviews`)

Create or update a Render Docker Web Service with:

- Repository: `OM2412/legal-contract-analyzer`
- Branch: `feature/accounts-and-saved-reviews`
- Dockerfile: `./Dockerfile`
- Docker context: `.`
- Health-check path: `/`
- Region: Singapore, or the region nearest the database

Configure these values in the Render Dashboard. Keep passwords and connection strings as secrets:

```text
SPRING_PROFILES_ACTIVE=db
FOLIO_DB_URL=postgresql://<user>:<password>@<host>/<database>?sslmode=require
FOLIO_DB_USER=<database-user>
FOLIO_DB_PASSWORD=<database-password>
FOLIO_DEMO_AUTH_ENABLED=false
FOLIO_DEMO_USERNAME=mentor
FOLIO_DEMO_PASSWORD=<optional-unused-demo-gate-password>
FOLIO_AI_ENABLED=true
FOLIO_PASSWORD_RESET_ENABLED=false
FOLIO_PASSWORD_RECOVERY_WORKER_ENABLED=false
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY=true
SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=lax
```

`render.accounts.yaml` contains the same deployment intent. `FOLIO_DEMO_AUTH_ENABLED=false` is required for a normal public preview. If it is true, `DemoAccessFilter` returns `401 Unauthorized` before the React login page can load.

### Stable demo (`main`)

The root `render.yaml` defines `folio-contract-demo`, disables the external database profile, and enables the AI endpoint flag. Use it only for the stateless/demo configuration unless its environment is expanded with the database settings described above.

### AI limitation on Render

Setting `VITE_AI_ENABLED=true` makes the AI Studio navigation and route visible. Setting `FOLIO_AI_ENABLED=true` allows the backend endpoint through its availability filter. However, the current Java Ollama client still calls `127.0.0.1:11434` inside the Render container. It cannot reach Ollama running on a developer's computer, so **AI Studio quote generation is not production-ready on Render yet**.

For a working hosted AI deployment, the client must be changed to use a securely configured, reachable hosted model endpoint (with a Render secret for authentication), or Ollama must run beside the application in an intentionally networked service. Do not expose Ollama publicly without authentication and network controls. Until that change is made, keep AI enabled for UI testing only or set `FOLIO_AI_ENABLED=false` to avoid presenting a non-working action.

After changing environment variables or pushing a deployment commit:

1. Save the variables and trigger **Manual Deploy > Deploy latest commit**. A new Docker build is required because `VITE_AI_ENABLED` is embedded into the frontend bundle.
2. Wait for Render to show the service as **Live**.
3. Check the root response:

   ```powershell
   curl.exe -i https://<service-name>.onrender.com/
   ```

   A healthy public preview returns `200 OK`. A `401` response with `WWW-Authenticate: Basic realm="Folio Demo"` means the outer demo gate is enabled or the service has not redeployed.
4. Test registration/login, upload both documents, run a review, and reopen a saved review before testing AI Studio.
5. If AI Studio is visible but **Find quotes** fails, inspect Render logs for the expected loopback Ollama connection failure.

The Docker entrypoint validates the database URL, derives the JDBC URL, binds Spring Boot to Render's `PORT`, and listens on `0.0.0.0`. For startup failures, inspect Render logs for database URL format, Flyway migration, or missing environment variables. Do not paste secrets into issue reports.

## API

`POST /api/review` accepts multipart fields:

- `agreement`: PDF or TXT file
- `sow`: PDF or TXT file
- `maxDays`: optional integer from 1 to 3650; defaults to 30

`POST /api/ai/quotes` accepts one multipart field, `document`. It returns exact quote suggestions and verified payment candidates. The endpoint requires `FOLIO_AI_ENABLED=true` and a reachable Ollama instance using model `qwen3:4b`.

Authenticated accounts and saved-review endpoints require the database profile and an authenticated application session.

## Validation commands

Run the smallest relevant checks after a change:

```powershell
mvn test
Push-Location frontend
npm ci
npm run lint
npm run build
Pop-Location
```

The Docker build runs both the frontend build and Maven package:

```powershell
docker build -t legal-contract-analyzer:local .
```

## Limits and safety

- Text-based PDFs use embedded text. Fully image-only PDFs of up to 20 pages use local Tesseract OCR with English data.
- Mixed PDFs are processed page by page; no more than 20 pages per PDF may require OCR.
- On Windows, the default Tesseract path is `C:\Program Files\Tesseract-OCR\tesseract.exe`; set `TESSERACT_PATH` if it is installed elsewhere.
- PDF uploads are limited to 10 MB and 100 pages; TXT uploads to 1 MB.
- The rule-based extractor recognizes a narrow set of payment wording.
- AI input is limited to 12,000 code points and depends on the configured model.
- OCR can misread characters. Check every quote against the original scanned page.
- A quote matching the source confirms wording, not legal meaning or legal priority.
- The policy is a user-selected illustrative business preference, not a legal standard.
- Free hosted databases may expire or have limited durability; use a managed production database with backup and retention policies for real data.

Read the complete original documents and verify every quote before making a decision.