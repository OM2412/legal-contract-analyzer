function unitLabel(unit) {
  return unit
    ? unit.replaceAll('_', ' ').toLowerCase()
    : 'unknown day unit'
}

function AiComparison({ agreementData, sowData }) {
  if (!agreementData || !sowData) return null

  const agreementCandidates = agreementData.candidates ?? []
  const sowCandidates = sowData.candidates ?? []

  if (
    agreementCandidates.length !== 1 ||
    sowCandidates.length !== 1
  ) {
    return (
      <section className="ai-comparison" aria-labelledby="ai-comparison-title">
        <span className="eyebrow">AI CANDIDATE COMPARISON</span>
        <h3 id="ai-comparison-title">Manual comparison needed</h3>
        <p>
          A single candidate was not found in each document. Check all
          suggested quotes against the originals before comparing terms.
        </p>
      </section>
    )
  }

  const agreement = agreementCandidates[0]
  const sow = sowCandidates[0]
  const sameUnit =
    agreement.dayUnit === sow.dayUnit &&
    agreement.dayUnit !== 'UNKNOWN'

  const maximum = Math.max(agreement.days, sow.days, 1)
  const difference = Math.abs(sow.days - agreement.days)
  const unit = unitLabel(agreement.dayUnit)

  let observation

  if (!sameUnit) {
    observation =
      'The candidates use different or unknown day units. ' +
      'Their durations cannot be compared directly.'
  } else if (agreement.days === sow.days) {
    observation = `Both candidates mention ${agreement.days} ${unit}.`
  } else {
    const longer = sow.days > agreement.days
      ? 'SOW'
      : 'Agreement'

    observation =
      `${longer} candidate mentions ${difference} ${unit} more.`
  }

  const bothTriggersConfirmed =
    agreement.invoiceReceiptConfirmed &&
    sow.invoiceReceiptConfirmed

  return (
    <section className="ai-comparison" aria-labelledby="ai-comparison-title">
      <span className="eyebrow">AI CANDIDATE COMPARISON</span>
      <h3 id="ai-comparison-title">Two quotes, one closer look.</h3>

      <div className="ai-comparison-grid">
        <div className="ai-comparison-term">
          <span>AGREEMENT</span>
          <strong>
            {agreement.days}
            <small> {unitLabel(agreement.dayUnit)}</small>
          </strong>
          {sameUnit && (
            <div
              className="ai-comparison-track"
              role="img"
              aria-label={`Agreement candidate: ${agreement.days} ${unit}`}
            >
              <span
                style={{
                  width: `${(agreement.days / maximum) * 100}%`,
                }}
              />
            </div>
          )}
        </div>

        <div className="ai-comparison-term">
          <span>STATEMENT OF WORK</span>
          <strong>
            {sow.days}
            <small> {unitLabel(sow.dayUnit)}</small>
          </strong>
          {sameUnit && (
            <div
              className="ai-comparison-track"
              role="img"
              aria-label={`SOW candidate: ${sow.days} ${unit}`}
            >
              <span
                style={{
                  width: `${(sow.days / maximum) * 100}%`,
                }}
              />
            </div>
          )}
        </div>
      </div>

      <p className="ai-comparison-observation">{observation}</p>

      {!bothTriggersConfirmed && (
        <p className="ai-comparison-caution">
          Invoice receipt is not independently confirmed for at least
          one candidate. Check the exact source wording.
        </p>
      )}

      <p className="ai-comparison-disclaimer">
        These are source-checked AI suggestions. This comparison does
        not determine policy compliance or which clause legally controls.
      </p>
    </section>
  )
}

export default AiComparison