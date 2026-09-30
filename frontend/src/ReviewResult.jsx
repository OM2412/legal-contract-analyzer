function readable(value) {
  return value ? value.replaceAll('_', ' ') : 'Not assessed'
}

function evidenceLocation(item) {
  const page = item.firstPage
    ? `Page ${item.firstPage}` +
      (item.lastPage !== item.firstPage
        ? `-${item.lastPage}`
        : '')
    : 'TXT document'

  const source = {
    OCR_TEXT: 'OCR text; check the scanned page',
    PDF_TEXT: 'PDF text',
    MIXED_OCR_AND_PDF_TEXT:
      'Mixed OCR and PDF text; check the original pages',
  }[item.textSource]

  return [
    page,
    source,
    `Text offsets ${item.start}-${item.end}`,
  ].filter(Boolean).join(' | ')
}

function EvidenceItem({ item }) {
  return (
    <div className="result-evidence-item">
      <blockquote>{item.quote}</blockquote>
      <div className="result-location">
        {evidenceLocation(item)}
      </div>
    </div>
  )
}

function DurationChart({ data }) {
  const agreement = data.agreementTerm
  const sow = data.sowTerm

  const canCompare =
    ['CONSISTENT', 'POTENTIAL_DIFFERENCE'].includes(
      data.comparisonStatus
    ) &&
    Number.isSafeInteger(agreement?.days) &&
    Number.isSafeInteger(sow?.days) &&
    agreement.days >= 0 &&
    sow.days >= 0 &&
    agreement.dayUnit === sow.dayUnit &&
    agreement.dayUnit !== 'UNKNOWN' &&
    agreement.trigger === sow.trigger &&
    agreement.trigger !== 'UNKNOWN'

  if (!canCompare) return null

  const unit = agreement.dayUnit === 'BUSINESS_DAYS'
    ? 'business days'
    : 'calendar days'

  const showPolicy =
    agreement.dayUnit === 'CALENDAR_DAYS' &&
    agreement.trigger === 'INVOICE_RECEIPT' &&
    Number.isSafeInteger(data.policyMaxDays) &&
    data.policyMaxDays > 0

  const rows = [
    { label: 'Agreement', days: agreement.days, kind: 'agreement' },
    { label: 'Statement of Work', days: sow.days, kind: 'sow' },
  ]

  if (showPolicy) {
    rows.push({
      label: 'Illustrative policy limit',
      days: data.policyMaxDays,
      kind: 'policy',
    })
  }

  const scale = Math.max(...rows.map((row) => row.days), 1)

  return (
    <figure
      className="result-duration-chart"
      aria-labelledby="duration-chart-heading"
    >
      <figcaption>
        <span className="eyebrow">PAYMENT TIMING</span>
        <h3 id="duration-chart-heading">
          Duration comparison
        </h3>
        <p>
          Both document terms concern the same obligation and
          start after {readable(agreement.trigger).toLowerCase()}.
          Bar lengths use a shared {unit} scale.
        </p>
      </figcaption>

      <div className="duration-chart-rows">
        {rows.map((row) => (
          <div className="duration-chart-row" key={row.kind}>
            <div className="duration-chart-label">
              <span>{row.label}</span>
              <strong>{row.days} {unit}</strong>
            </div>

            <div className="duration-chart-track">
              <div
                className={`duration-chart-fill duration-chart-fill--${row.kind}`}
                style={{ width: `${(row.days / scale) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>

      <p className="duration-chart-note">
        These bars show stated durations only. The policy line is
        an illustrative business preference; this chart does not
        decide which contract term legally controls.
      </p>
    </figure>
  )
}

function DocumentResult({
  title,
  fileName,
  assessment,
  explanation,
  evidence,
  matches,
  unrecognized,
}) {
  const recognized = matches?.length
    ? matches
    : evidence
      ? [evidence]
      : []

  return (
    <article className="result-document">
      <div className="result-document-heading">
        <div>
          <span className="eyebrow">{title}</span>
          <h3>{fileName || title}</h3>
        </div>
        <span className="result-assessment">
          {readable(assessment)}
        </span>
      </div>

      {explanation && <p>{explanation}</p>}

      {recognized.length > 1 && (
        <p className="result-warning">
          {recognized.length} recognized payment clauses. Review
          each one; no single clause was selected.
        </p>
      )}

      {recognized.length === 0 && (
        <p className="result-empty">
          No supported payment clause recognized in this document.
        </p>
      )}

      {recognized.map((item, index) => (
        <div key={`${item.documentId}-${item.start}-${item.end}`}>
          {recognized.length > 1 && (
            <strong className="result-item-label">
              Recognized clause {index + 1}
            </strong>
          )}
          <EvidenceItem item={item} />
        </div>
      ))}

      {unrecognized?.length > 0 && (
        <div className="result-unrecognized">
          <h4>Additional wording to check</h4>
          <p>
            These passages may concern payment, but their terms
            were not fully parsed.
          </p>
          {unrecognized.map((item) => (
            <EvidenceItem
              key={`${item.documentId}-${item.start}-${item.end}`}
              item={item}
            />
          ))}
        </div>
      )}
    </article>
  )
}

function ReviewResult({ data, files }) {
  if (!data) return null

  return (
    <section
      className="review-result-section section-spacing"
      id="review-result"
      aria-labelledby="result-heading"
      tabIndex="-1"
    >
      <div className="page-container">
        <div className="result-topline">
          <span className="eyebrow">YOUR DOCUMENT REVIEW</span>
          <button
            type="button"
            className="result-print"
            onClick={() => window.print()}
          >
            Print / Save as PDF
          </button>
        </div>

        <h2 id="result-heading">
          {readable(data.reviewStatus)}
        </h2>

        <p className="result-intro">
          Read each quote against the original document before
          relying on a result.
        </p>

        {data.reviewReason && (
          <div className="result-alert" role="status">
            <strong>Why this needs attention</strong>
            <p>{data.reviewReason}</p>
          </div>
        )}

        <div className="result-summary">
          <div>
            <span>TERM COMPARISON</span>
            <strong>{readable(data.comparisonStatus)}</strong>
            {data.comparisonExplanation && (
              <p>{data.comparisonExplanation}</p>
            )}
          </div>

          <div>
            <span>PAYMENT PRIORITY SCAN</span>
            <strong>{readable(data.precedenceStatus)}</strong>
          </div>

          <div>
            <span>DEMO POLICY LIMIT</span>
            <strong>
              {data.policyMaxDays ?? 30} calendar days
            </strong>
          </div>
        </div>

        <DurationChart data={data} />

        <div className="result-documents">
          <DocumentResult
            title="AGREEMENT"
            fileName={files?.agreement?.name}
            assessment={data.agreementAssessment}
            explanation={data.agreementExplanation}
            evidence={data.agreementEvidence}
            matches={data.agreementMatches}
            unrecognized={data.agreementUnrecognized}
          />

          <DocumentResult
            title="STATEMENT OF WORK"
            fileName={files?.sow?.name}
            assessment={data.sowAssessment}
            explanation={data.sowExplanation}
            evidence={data.sowEvidence}
            matches={data.sowMatches}
            unrecognized={data.sowUnrecognized}
          />
        </div>

        <article className="result-precedence">
          <span className="eyebrow">
            PAYMENT PRIORITY WORDING
          </span>
          {data.precedenceEvidence?.length ? (
            data.precedenceEvidence.map((item) => (
              <EvidenceItem
                key={`${item.documentId}-${item.start}-${item.end}`}
                item={item}
              />
            ))
          ) : (
            <p>
              No supported payment-priority wording located.
            </p>
          )}
        </article>

        {data.textualCandidateAssessment && (
          <div className="result-candidate">
            <strong>
              Textual candidate assessment:{' '}
              {readable(data.textualCandidateAssessment)}
            </strong>
            {data.textualCandidateExplanation && (
              <p>{data.textualCandidateExplanation}</p>
            )}
          </div>
        )}

        <p className="result-notice">
          {data.notice}
        </p>
      </div>
    </section>
  )
}

export default ReviewResult