# CUAD termination training audit

Run date: 2026-10-03 (Asia/Kolkata)

Source: `data/external/cuad/train_separate_questions.json`

Target CUAD question: `Termination For Convenience`

Split boundary: CUAD `test.json` was not read.

## Method

The audit runs the application's `TerminationSignalFinder` over training paragraphs containing the target question. One evaluated unit is a paragraph/document, not a question instance or answer span. A unit is gold-positive when at least one target question has an answer. A scanner-positive unit has at least one `REVIEW_ONLY` termination finding.

Evidence overlap means at least one scanner evidence range intersects a CUAD answer range. Gold offsets are checked against exact UTF-16 substrings; scanner code-point offsets are converted before comparison.

This compares a **generic termination wording signal** with CUAD's narrower **termination without cause** label. The counts are not classification precision or recall. A scanner-only document may contain a valid termination clause of another type. Evidence overlap does not establish legal interpretation.

## Training data inventory

| Measure | Count |
| --- | ---: |
| Evaluated paragraph/document units | 408 |
| Target question instances | 459 |
| Annotated answer spans | 205 |
| Gold-positive units | 154 |

## Scanner comparison

| Document-level observation | Original scanner | Expanded review-only scanner |
| --- | ---: | ---: |
| Gold and scanner both positive | 42 | 115 |
| Scanner positive, target CUAD label absent | 54 | 128 |
| Target CUAD label present, scanner absent | 112 | 39 |
| Both absent | 200 | 126 |
| Both positive with evidence overlap | 25 | 84 |
| Both positive without evidence overlap | 17 | 31 |

The expanded scanner covers additional generic wording involving named parties, passive termination, and termination by either party. Its findings remain `REVIEW_ONLY`. It does not determine grounds, enforceability, notice compliance, or whether termination is without cause. The Maven tests and training audit completed successfully after the change.

## Limits and next use

- Review scanner-only passages before designing a narrower convenience classifier.
- The 31 shared-positive documents without evidence overlap show why document-level co-occurrence cannot validate the cited clause.
- Inspect gold-only training examples before adding more wording patterns.
- This training data was used during development. Do not report these counts as held-out performance or model training.
- Payment timing and Agreement/SOW comparison require a separate labeled paired-document evaluation.

To reproduce from the project root:

```powershell
mvn test
mvn test-compile org.codehaus.mojo:exec-maven-plugin:3.6.4:java '-Dexec.mainClass=com.omjadon.contractanalyzer.risk.CuadTerminationTrainAudit' '-Dexec.classpathScope=test'