import { useState } from 'react'
import EvidenceLens from './EvidenceLens'
import RiskStudio from './RiskStudio'
import ReviewChecklist from './ReviewChecklist'
import SaveReviewButton from './SaveReviewButton'
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

function EvidenceItem({ item, file, onInspect, number, label = 'Verified source excerpt' }) {
  return (
    <article className="result-evidence-item">
      <div className="evidence-card-head">
        <span className="evidence-card-label">{number ? `${String(number).padStart(2, '0')} / ` : ''}{label}</span>
        <span className="evidence-card-page">{item.firstPage ? `Page ${item.firstPage}` : 'Text source'}</span>
      </div>
      <blockquote>{item.quote}</blockquote>

      <div className="result-evidence-bottom">
        <span className="result-location">
          {evidenceLocation(item)}
        </span>

        {file && (
          <button
            type="button"
            className="result-evidence-open"
            onClick={() => onInspect(file, item)}
          >
            Inspect source <span aria-hidden="true">↗</span>
          </button>
        )}
      </div>
    </article>
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
    { label: 'Agreement', days: agreement.days, kind: 'agreement'},
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
  file,
  assessment,
  explanation,
  evidence,
  matches,
  unrecognized,
  onInspect,
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
          <h3>{file?.name || title}</h3>
          <span className="document-clause-count">{recognized.length} recognized {recognized.length === 1 ? 'clause' : 'clauses'}</span>
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
          <EvidenceItem
            item={item}
            file={file}
            onInspect={onInspect}
            number={index + 1}
          />
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
              file={file}
              onInspect={onInspect}
              label="Additional wording to verify"
            />
          ))}
        </div>
      )}
    </article>
  )
}
function ReviewResult({
  data,
  files,
  checklist,
  onChecklistChange,
  panel = 'all',
}) {
  const [selectedEvidence, setSelectedEvidence] = useState(null)

  if (!data) return null

  function inspect(file, item) {
    setSelectedEvidence({ file, item })
  }

  function fileForEvidence(item) {
    if (item.documentId === 'agreement-upload') {
      return files?.agreement
    }

    if (item.documentId === 'sow-upload') {
      return files?.sow
    }

    return null
  }

  return (
    <>
      <section
       className={`review-result-section section-spacing${panel === 'risks' ? ' app-result-panel--hidden' : ''}`}
        id="review-result"
        aria-labelledby="result-heading"
        tabIndex="-1"
      >
        <div className="page-container">
          <div className="result-topline">
            <span className="eyebrow">FOLIO / DOCUMENT REVIEW REPORT</span>
            <div className="result-actions">
            <button
              type="button"
              className="result-print"
              onClick={() => window.print()}
            >
              Print / Save as PDF
            </button>
            <SaveReviewButton
  key={`${data.agreementVersion}-${data.sowVersion}-${data.policyMaxDays}`}
  files={files}
  maxDays={data.policyMaxDays ?? 30}
  reviewStatus={data.reviewStatus}
/>
            <ReviewChecklist
  data={data}
  checklist={checklist}
  onChange={onChecklistChange}
/>
            </div>
          </div>

          <div className="result-heading-row">
            <div>
          <h2 id="result-heading">
            {readable(data.reviewStatus)}
          </h2>

          <p className="result-intro">
            Read each quote against the original document before
            relying on a result.
          </p>
            </div>
            <span className="result-report-chip"><span aria-hidden="true">●</span> Evidence-first report</span>
          </div>

          {data.reviewReason && (
            <div className="result-alert" role="status">
              <strong>Why this needs attention</strong>
              <p>{data.reviewReason}</p>
            </div>
          )}

          <div className="result-summary">
            <div className="result-summary-card result-summary-card--lime">
              <span className="summary-card-index">01 / ANALYSIS</span>
              <span>TERM COMPARISON</span>
              <strong>{readable(data.comparisonStatus)}</strong>
              {data.comparisonExplanation && (
                <p>{data.comparisonExplanation}</p>
              )}
            </div>

            <div className="result-summary-card result-summary-card--blue">
              <span className="summary-card-index">02 / PRIORITY</span>
              <span>PAYMENT PRIORITY SCAN</span>
              <strong>{readable(data.precedenceStatus)}</strong>
              <p>Locate hierarchy wording and check the exact source.</p>
            </div>

            <div className="result-summary-card result-summary-card--cream">
              <span className="summary-card-index">03 / REFERENCE</span>
              <span>DEMO POLICY LIMIT</span>
              <strong>
                {data.policyMaxDays ?? 30} calendar days
              </strong>
              <p>Illustrative benchmark, not a legal determination.</p>
            </div>
          </div>

          <DurationChart data={data} />

          <div className="result-section-heading"><div><span className="eyebrow">SOURCE-BACKED FINDINGS</span><h3>Document evidence</h3></div><span>Open a card to inspect its original passage.</span></div>
          <div className="result-documents">
            <DocumentResult
              title="AGREEMENT"
              file={files?.agreement}
              assessment={data.agreementAssessment}
              explanation={data.agreementExplanation}
              evidence={data.agreementEvidence}
              matches={data.agreementMatches}
              unrecognized={data.agreementUnrecognized}
              onInspect={inspect}
            />

            <DocumentResult
              title="STATEMENT OF WORK"
              file={files?.sow}
              assessment={data.sowAssessment}
              explanation={data.sowExplanation}
              evidence={data.sowEvidence}
              matches={data.sowMatches}
              unrecognized={data.sowUnrecognized}
              onInspect={inspect}
            />
          </div>

          <article className="result-precedence">
            <span className="eyebrow">
              PAYMENT PRIORITY WORDING
            </span>
            <h3>Which document takes priority?</h3>
            <p className="result-precedence-intro">Source passages mentioning payment priority appear below. Interpret them against the complete signed documents.</p>

            {data.precedenceEvidence?.length ? (
              data.precedenceEvidence.map((item) => (
                <EvidenceItem
                  key={`${item.documentId}-${item.start}-${item.end}`}
                  item={item}
                  file={fileForEvidence(item)}
                  onInspect={inspect}
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

     <div
  className={
    panel === 'review'
      ? 'app-result-panel--hidden'
      : undefined
  }
>
  <RiskStudio
    data={data}
    files={files}
    onInspect={inspect}
  />
</div>

      {selectedEvidence && (
        <EvidenceLens
          file={selectedEvidence.file}
          evidence={selectedEvidence.item}
          onClose={() => setSelectedEvidence(null)}
        />
      )}
    </>
  )
}

export default ReviewResult
