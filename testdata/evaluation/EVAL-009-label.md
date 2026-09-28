# EVAL-009: payment deadline with disputed-amount exception

Source: `EVAL-009-agreement.txt`

Document type: Agreement

Sample type: self-written synthetic excerpt

## Expected obligation

> Client shall pay Provider the project fee within 30 calendar days after receipt of a valid invoice, except that amounts disputed in good faith may be withheld until the dispute is resolved.

| Field | Expected value |
| --- | --- |
| Payer | Client |
| Payee | Provider |
| Payment scope | Project fee, subject to the disputed-amount exception |
| Days | 30 for amounts not withheld under the exception |
| Day unit | Calendar days |
| Trigger | Invoice receipt |
| Clause status | Payment obligation with an exception |

## Reviewer notes

The 30-day period must not be presented as an unconditional deadline for every amount. Disputed amounts may be withheld until the dispute is resolved; the excerpt does not give a numeric deadline for those amounts. Evidence for a complete interpretation must include the exception.

If the system cannot represent the exception, it should request human review rather than issue an unqualified payment assessment.

## Observed result

## Observed result

Baseline run:

- Annotated Agreement obligations: **1**, with disputed-amount exception
- Correct complete Agreement extractions: **0**
- Agreement misses: **1**
- Agreement false positives: **0**
- CLI review status: `REVIEW_REQUIRED`
- Agreement output: `payment clause not recognized`
- Agreement policy assessment: **not produced**
- Agreement–SOW comparison: **not produced**
- Maven CLI execution: `BUILD SUCCESS`

The system abstained from an unqualified assessment, which is safer for this exception. It still did not extract or explain the complete obligation, so this is a miss rather than a correct extraction.

The SOW supplied to the CLI is outside the EVAL-009 Agreement score.