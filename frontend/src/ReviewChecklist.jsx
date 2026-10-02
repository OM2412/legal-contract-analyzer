function quoteItems(evidenceList, prefix, label) {
  return evidenceList.map((evidence, index) => ({
    id:
      `${prefix}-${evidence.documentId}-` +
      `${evidence.start}-${evidence.end}`,
    label: `${label} ${index + 1}`,
    quote: evidence.quote,
  }))
}

function ReviewChecklist({ data, checklist, onChange }) {
  if (!data) return null

  const agreementMatches = data.agreementMatches?.length
    ? data.agreementMatches
    : data.agreementEvidence
      ? [data.agreementEvidence]
      : []

  const sowMatches = data.sowMatches?.length
    ? data.sowMatches
    : data.sowEvidence
      ? [data.sowEvidence]
      : []

  const items = [
    ...quoteItems(
      agreementMatches,
      'agreement-match',
      'Agreement payment quote'
    ),
    ...quoteItems(
      sowMatches,
      'sow-match',
      'SOW payment quote'
    ),
    ...quoteItems(
      data.agreementUnrecognized ?? [],
      'agreement-unparsed',
      'Agreement additional wording'
    ),
    ...quoteItems(
      data.sowUnrecognized ?? [],
      'sow-unparsed',
      'SOW additional wording'
    ),
    ...quoteItems(
      data.precedenceEvidence ?? [],
      'priority',
      'Payment priority quote'
    ),
  ]

  const verified = checklist.verified ?? {}
  const checkedCount = items.filter((item) => verified[item.id]).length

  function setVerified(id, checked) {
    onChange((previous) => ({
      ...previous,
      verified: {
        ...previous.verified,
        [id]: checked,
      },
    }))
  }

  function setNotes(notes) {
    onChange((previous) => ({
      ...previous,
      notes,
    }))
  }

  return (
    <section
      className="review-checklist"
      aria-labelledby="review-checklist-heading"
    >
      <div className="review-checklist-heading">
        <div>
          <span className="eyebrow">HUMAN REVIEW / SOURCE CHECK</span>
          <h3 id="review-checklist-heading">
            Verify before you decide.
          </h3>
        </div>

        <span className="review-checklist-count">
          {checkedCount}/{items.length} passages checked
        </span>
      </div>

      <p>
        Open the original Agreement and SOW. Mark a passage only after
        checking that it matches the source and is relevant in context.
        Read both complete documents, including wording the system may
        have missed.
      </p>

      {items.length === 0 ? (
        <p className="review-checklist-empty">
          No supported source passages are available to check here.
          Read both complete documents manually.
        </p>
      ) : (
        <div className="review-checklist-items">
          {items.map((item) => (
            <label className="review-checklist-item" key={item.id}>
              <input
                type="checkbox"
                checked={Boolean(verified[item.id])}
                onChange={(event) =>
                  setVerified(item.id, event.target.checked)
                }
              />

              <span>
                <strong>{item.label}</strong>
                <small>{item.quote}</small>
              </span>
            </label>
          ))}
        </div>
      )}

      <label
        className="review-checklist-notes-label"
        htmlFor="reviewer-notes"
      >
        Reviewer notes
      </label>

      <textarea
        id="reviewer-notes"
        className="review-checklist-notes"
        rows={5}
        maxLength={2000}
        value={checklist.notes ?? ''}
        onChange={(event) => setNotes(event.target.value)}
        placeholder="Record questions, exceptions, or follow-up needed..."
      />

      <div
        className="review-checklist-notes-print"
        aria-hidden="true"
      >
        {checklist.notes?.trim() || 'No reviewer notes recorded.'}
      </div>

      <p className="review-checklist-footnote">
        Checks and notes are kept only in this open browser session.
        They do not change the automated findings or establish a legal
        conclusion. Print or save the report if you need a copy.
      </p>
    </section>
  )
}

export default ReviewChecklist