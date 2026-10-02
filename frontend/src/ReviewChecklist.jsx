function ReviewChecklist({ data, checklist, onChange }) {
  if (!data) return null

  const items = [
    ...(data.agreementEvidence
      ? [{
          id: 'agreement',
          label: 'Agreement payment quote',
          quote: data.agreementEvidence.quote,
        }]
      : []),
    ...(data.sowEvidence
      ? [{
          id: 'sow',
          label: 'SOW payment quote',
          quote: data.sowEvidence.quote,
        }]
      : []),
    ...(data.precedenceEvidence ?? []).map((evidence, index) => ({
      id: `precedence-${index}`,
      label: `Payment priority quote ${index + 1}`,
      quote: evidence.quote,
    })),
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
          {checkedCount}/{items.length} quotes checked
        </span>
      </div>

      <p>
        Open the original Agreement and SOW. Mark a quote only after
        checking that it matches the source and is relevant in context.
        Also read any additional wording flagged elsewhere in this report.
      </p>

      {items.length === 0 ? (
        <p className="review-checklist-empty">
          No supported source quotes are available to check here.
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