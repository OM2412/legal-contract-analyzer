import './RiskSignalMap.css'

const AGREEMENT = 'agreement-upload'
const SOW = 'sow-upload'

function countWithSource(findings, documentId) {
  return findings.filter((finding) =>
    (finding.evidence ?? []).some(
      (item) => item.documentId === documentId
    )
  ).length
}

function RiskSignalMap({ findings, activeSource, onSelect }) {
  const agreementCount = countWithSource(findings, AGREEMENT)
  const sowCount = countWithSource(findings, SOW)
  const withoutQuote = findings.filter(
    (finding) => !(finding.evidence ?? []).length
  ).length

  return (
    <section className="risk-map" aria-label="Document signal map">
      <div className="risk-map__header">
        <span>DOCUMENT SOURCE MAP</span>
        <p>
          Select a document to see findings backed by its quotes.
        </p>
      </div>

      <div className="risk-map__network">
        <span
          className="risk-map__connector risk-map__connector--left"
          aria-hidden="true"
        />
        <span
          className="risk-map__connector risk-map__connector--right"
          aria-hidden="true"
        />

        <button
          type="button"
          className="risk-map__node"
          aria-pressed={activeSource === AGREEMENT}
          aria-label={`Show ${agreementCount} Agreement findings`}
          onClick={() => onSelect(AGREEMENT)}
        >
          <span className="risk-map__label">01 / AGREEMENT</span>
          <strong className="risk-map__value">
            {agreementCount}
          </strong>
          <span className="risk-map__caption">
            Findings with an Agreement quote
          </span>
        </button>

        <button
          type="button"
          className="risk-map__node risk-map__node--center"
          aria-pressed={activeSource === 'ALL'}
          aria-label={`Show all ${findings.length} findings`}
          onClick={() => onSelect('ALL')}
        >
          <span className="risk-map__label">FOLIO / ALL</span>
          <strong className="risk-map__value">
            {findings.length}
          </strong>
          <span className="risk-map__caption">
            Findings in this review
          </span>
        </button>

        <button
          type="button"
          className="risk-map__node"
          aria-pressed={activeSource === SOW}
          aria-label={`Show ${sowCount} Statement of Work findings`}
          onClick={() => onSelect(SOW)}
        >
          <span className="risk-map__label">02 / SOW</span>
          <strong className="risk-map__value">
            {sowCount}
          </strong>
          <span className="risk-map__caption">
            Findings with a SOW quote
          </span>
        </button>
      </div>

      <p className="risk-map__footer">
        A finding quoting both documents appears in both source
        counts.{' '}
        {withoutQuote > 0 && (
          <>
            {withoutQuote} finding(s) without an individual quote
            appear under All.
          </>
        )}
      </p>
    </section>
  )
}

export default RiskSignalMap