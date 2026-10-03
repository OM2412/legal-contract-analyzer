import { useState } from 'react'
import './RiskStudio.css'

const CATEGORIES = [
  { id: 'PAYMENT', name: 'Payment', detail: 'Terms and timing', mark: '01' },
  { id: 'TERMINATION', name: 'Termination', detail: 'Exit wording', mark: '02' },
  { id: 'LIABILITY', name: 'Liability', detail: 'Limits and exceptions', mark: '03' },
]

const SIGNALS = [
  ['ALL', 'All signals'],
  ['DOCUMENT_DIFFERENCE', 'Differences'],
  ['POLICY_DEVIATION', 'Demo policy'],
  ['COVERAGE_GAP', 'Coverage'],
  ['AMBIGUOUS_WORDING', 'Ambiguity'],
  ['CLAUSE_FOR_REVIEW', 'Clause review'],
]

const SIGNAL_LABELS = {
  DOCUMENT_DIFFERENCE: 'Document difference',
  POLICY_DEVIATION: 'Demo policy deviation',
  COVERAGE_GAP: 'Coverage gap',
  AMBIGUOUS_WORDING: 'Multiple clauses',
  CLAUSE_FOR_REVIEW: 'Clause for review',
}

function readable(value) {
  return value?.replaceAll('_', ' ') ?? 'Review'
}

function documentRole(documentId) {
  if (documentId === 'agreement-upload') return 'Agreement'
  if (documentId === 'sow-upload') return 'Statement of Work'
  return 'Source document'
}

function sourceLocation(item) {
  const page = item.firstPage
    ? `Page ${item.firstPage}` +
      (item.lastPage !== item.firstPage
        ? `-${item.lastPage}`
        : '')
    : 'TXT document'

  const source = {
    OCR_TEXT: 'OCR text - verify scanned page',
    PDF_TEXT: 'PDF text',
    MIXED_OCR_AND_PDF_TEXT: 'Mixed text - verify original pages',
    TXT: 'Extracted text',
  }[item.textSource]

  return [page, source, `Offsets ${item.start}-${item.end}`]
    .filter(Boolean)
    .join(' / ')
}

function EvidenceSource({ item, files, onInspect, index }) {
  const role = documentRole(item.documentId)

  const file = item.documentId === 'agreement-upload'
    ? files?.agreement
    : item.documentId === 'sow-upload'
      ? files?.sow
      : null

  return (
    <div className="risk-studio__source">
      <div className="risk-studio__source-top">
        <span>EXCERPT {String(index + 1).padStart(2, '0')}</span>
        <span>{role}</span>
      </div>

      <blockquote>{item.quote}</blockquote>

      <div className="risk-studio__source-bottom">
        <span>{sourceLocation(item)}</span>

        {file && typeof onInspect === 'function' && (
          <button
            type="button"
            className="risk-studio__inspect"
            onClick={() => onInspect(file, item)}
            aria-label={`View original ${role} for excerpt ${index + 1}`}
          >
            View original <span aria-hidden="true">↗</span>
          </button>
        )}
      </div>
    </div>
  )
}

function RiskStudio({ data, files, onInspect }) {
  const [activeCategory, setActiveCategory] = useState('ALL')
  const [activeSignal, setActiveSignal] = useState('ALL')
  const [selectedId, setSelectedId] = useState(null)

  if (!data) return null

  const hasFindingsField = Array.isArray(data.riskFindings)
  const findings = hasFindingsField ? data.riskFindings : []

  const visible = findings.filter((finding) =>
    (activeCategory === 'ALL' ||
      finding.category === activeCategory) &&
    (activeSignal === 'ALL' ||
      finding.signal === activeSignal)
  )

  const selected = visible.find((finding) =>
    finding.findingId === selectedId
  ) ?? visible[0]

  const activeCategoryCount = activeCategory === 'ALL'
    ? findings.length
    : findings.filter((finding) =>
        finding.category === activeCategory
      ).length

  function chooseCategory(category) {
    setActiveCategory(category)
    setActiveSignal('ALL')
    setSelectedId(null)
  }

  return (
    <section
      className="risk-studio"
      id="risk-studio"
      aria-labelledby="risk-studio-heading"
    >
      <div className="page-container risk-studio__container">
        <header className="risk-studio__header">
          <div className="risk-studio__hero-copy">
            <span className="risk-studio__eyebrow">
              <span
                className="risk-studio__live-dot"
                aria-hidden="true"
              />
              FOLIO / RISK STUDIO
            </span>

            <h2 id="risk-studio-heading">
              Follow the signal.
              <span>Find the source.</span>
            </h2>

            <p className="risk-studio__intro">
              Explore wording that needs attention, then open its
              source. Findings guide human review; they do not decide
              legal effect or enforceability.
            </p>
          </div>

          <div className="risk-studio__orbit" aria-hidden="true">
            <div className="risk-studio__orbit-ring risk-studio__orbit-ring--outer" />
            <div className="risk-studio__orbit-ring risk-studio__orbit-ring--inner" />

            <div className="risk-studio__orbit-core">
              <strong>{findings.length}</strong>
              <span>SOURCE SIGNALS</span>
            </div>

            <span className="risk-studio__orbit-label risk-studio__orbit-label--a">
              01 / SCAN
            </span>
            <span className="risk-studio__orbit-label risk-studio__orbit-label--b">
              02 / VERIFY
            </span>
          </div>
        </header>

        <div
          className="risk-studio__journey"
          aria-label="Review workflow"
        >
          <span><b>01</b> Discover</span>
          <span
            aria-hidden="true"
            className="risk-studio__journey-line"
          />
          <span><b>02</b> Narrow</span>
          <span
            aria-hidden="true"
            className="risk-studio__journey-line"
          />
          <span><b>03</b> Verify source</span>
        </div>

        <div className="risk-studio__section-heading">
          <div>
            <span className="risk-studio__section-index">
              01 / SIGNAL MAP
            </span>
            <h3>Choose a clause area</h3>
          </div>
          <p>Counts indicate found wording, not a risk score.</p>
        </div>

        <div
          className="risk-studio__category-grid"
          aria-label="Clause areas"
        >
          <button
            type="button"
            className="risk-studio__category risk-studio__category--all"
            aria-pressed={activeCategory === 'ALL'}
            onClick={() => chooseCategory('ALL')}
          >
            <span className="risk-studio__category-top">
              <span>00 / OVERVIEW</span>
              <span aria-hidden="true">↗</span>
            </span>
            <strong>{findings.length}</strong>
            <span className="risk-studio__category-name">
              All findings
            </span>
            <small>Every supported signal</small>
          </button>

          {CATEGORIES.map((category) => {
            const count = findings.filter((finding) =>
              finding.category === category.id
            ).length

            return (
              <button
                key={category.id}
                type="button"
                className={
                  `risk-studio__category ` +
                  `risk-studio__category--${category.id.toLowerCase()}`
                }
                aria-pressed={activeCategory === category.id}
                aria-label={`${category.name}: ${count} findings`}
                onClick={() => chooseCategory(category.id)}
              >
                <span className="risk-studio__category-top">
                  <span>{category.mark} / CLAUSE AREA</span>
                  <span aria-hidden="true">↗</span>
                </span>
                <strong>{count}</strong>
                <span className="risk-studio__category-name">
                  {category.name}
                </span>
                <small>{category.detail}</small>
              </button>
            )
          })}
        </div>

        <div className="risk-studio__section-heading risk-studio__section-heading--queue">
          <div>
            <span className="risk-studio__section-index">
              02 / FINDING QUEUE
            </span>
            <h3>Inspect one signal at a time</h3>
          </div>
          <p>
            {visible.length} shown / {activeCategoryCount} in this area
          </p>
        </div>

        {hasFindingsField && findings.length > 0 && (
          <div
            className="risk-studio__rail"
            aria-label="Filter signal type"
          >
            {SIGNALS.map(([value, label]) => {
              const count = findings.filter((finding) =>
                (activeCategory === 'ALL' ||
                  finding.category === activeCategory) &&
                (value === 'ALL' ||
                  finding.signal === value)
              ).length

              return (
                <button
                  key={value}
                  type="button"
                  className="risk-studio__filter"
                  aria-pressed={activeSignal === value}
                  onClick={() => {
                    setActiveSignal(value)
                    setSelectedId(null)
                  }}
                >
                  {label} <span>{count}</span>
                </button>
              )
            })}
          </div>
        )}

        {visible.length > 0 ? (
          <div className="risk-studio__workbench">
            <div
              className="risk-studio__queue"
              aria-label="Findings"
            >
              {visible.map((finding, index) => (
                <button
                  key={finding.findingId}
                  type="button"
                  className="risk-studio__queue-item"
                  aria-pressed={
                    selected?.findingId === finding.findingId
                  }
                  aria-controls="risk-studio-detail"
                  onClick={() =>
                    setSelectedId(finding.findingId)
                  }
                  style={{ '--risk-index': index }}
                >
                  <span className="risk-studio__queue-number">
                    {String(index + 1).padStart(2, '0')}
                  </span>

                  <span className="risk-studio__queue-copy">
                    <small>{readable(finding.category)}</small>
                    <strong>{finding.title}</strong>
                    <em>
                      {SIGNAL_LABELS[finding.signal] ||
                        readable(finding.signal)}
                    </em>
                  </span>

                  <span
                    className="risk-studio__queue-arrow"
                    aria-hidden="true"
                  >
                    ↗
                  </span>
                </button>
              ))}
            </div>

            <article
             key={selected.findingId}
              className="risk-studio__detail"
              id="risk-studio-detail"
              aria-labelledby="risk-studio-detail-title"
            >
              <div className="risk-studio__detail-top">
                <span>03 / EVIDENCE DESK</span>
                <span>
                  {visible.indexOf(selected) + 1} OF {visible.length}
                </span>
              </div>

              <div className="risk-studio__detail-meta">
                <span>{readable(selected.category)}</span>
                <span>
                  {SIGNAL_LABELS[selected.signal] ||
                    readable(selected.signal)}
                </span>
                <span>{readable(selected.priority)}</span>
              </div>

              <h3 id="risk-studio-detail-title">
                {selected.title}
              </h3>

              <p className="risk-studio__explanation">
                {selected.explanation}
              </p>

              <div className="risk-studio__detail-divider">
                <span>TRACEABLE SOURCE</span>
                <span>
                  {selected.evidence?.length ?? 0} excerpt(s)
                </span>
              </div>

              {selected.evidence?.length ? (
                <div className="risk-studio__evidence">
                  {selected.evidence.map((item, index) => (
                    <EvidenceSource
                      key={
                        `${item.documentId}-${item.start}-` +
                        `${item.end}-${index}`
                      }
                      item={item}
                      files={files}
                      onInspect={onInspect}
                      index={index}
                    />
                  ))}
                </div>
              ) : (
                <p className="risk-studio__no-source">
                  This coverage warning has no individual quote.
                  Review both complete documents before relying
                  on a result.
                </p>
              )}

              <p className="risk-studio__detail-caution">
                Review the clause in context. A source match does
                not establish legal meaning, priority, or
                enforceability.
              </p>
            </article>
          </div>
        ) : (
          <div className="risk-studio__empty">
            <span aria-hidden="true">◇</span>

            <strong>
              {!hasFindingsField
                ? 'This review has no Risk Studio data'
                : findings.length === 0
                  ? 'No supported signals located'
                  : 'Nothing matches these filters'}
            </strong>

            <p>
              {!hasFindingsField
                ? 'Run a new review to generate source-backed findings.'
                : 'This does not establish that the documents are risk-free. Check the full review and original documents.'}
            </p>

            {findings.length > 0 && (
              <button
                type="button"
                className="risk-studio__reset"
                onClick={() => chooseCategory('ALL')}
              >
                Clear filters
              </button>
            )}
          </div>
        )}
{findings.length > 0 && (
  <div className="risk-studio__print-report">
    {findings.map((finding, index) => (
      <article
        className="risk-studio__print-item"
        key={finding.findingId}
      >
        <small>
          {index + 1}. {readable(finding.category)} /{' '}
          {readable(finding.priority)}
        </small>
        <h4>{finding.title}</h4>
        <p>{finding.explanation}</p>
        {(finding.evidence ?? []).map((item, sourceIndex) => (
          <div
            key={`${item.documentId}-${item.start}-${sourceIndex}`}
          >
            <blockquote>{item.quote}</blockquote>
            <p>
              {documentRole(item.documentId)} /{' '}
              {sourceLocation(item)}
            </p>
          </div>
        ))}
      </article>
    ))}
  </div>
)}
        <footer className="risk-studio__note">
          Current scope: supported payment timing, narrow
          termination wording, and narrow liability wording.
          The demo policy is a business preference. Check OCR
          quotes against scanned pages.
        </footer>
      </div>
    </section>
  )
}

export default RiskStudio