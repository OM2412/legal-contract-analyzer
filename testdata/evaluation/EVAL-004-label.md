# EVAL-004: passive-voice payment clause

Source: `EVAL-004-agreement.txt`

> Vendor shall be paid the implementation fee by Customer within 20 calendar days after Customer receives a valid invoice.

| Field | Expected value |
| --- | --- |
| Payer | Customer |
| Payee | Vendor |
| Scope | Implementation fee |
| Days | 20 |
| Day unit | Calendar days |
| Trigger | Invoice receipt |
| Clause status | Payment obligation |

"Vendor shall be paid ... by Customer" identifies Vendor as payee and Customer as payer. "After Customer receives a valid invoice" supports invoice receipt as the trigger.

## Observed result

The CLI recognizes the Agreement payment clause and reports:

- Agreement assessment: `WITHIN_POLICY` under illustrative policy `P-DEMO-30`.
- Agreement evidence: the passive-voice clause at text offsets `0..120`.
- Comparison with `testdata/sow.txt`: `DIFFERENT_OBLIGATIONS`.
- Overall review: `SEPARATE_OBLIGATIONS`.
- Maven CLI execution: `BUILD SUCCESS`.

The SOW concerns a project fee paid by Client to Provider, while this Agreement clause concerns an implementation fee paid by Customer to Vendor. The `SEPARATE_OBLIGATIONS` result does not mean the passive-voice clause was missed.

The `PaymentEvaluation004Test` result must be checked separately before recording its test count.