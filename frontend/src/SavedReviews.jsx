import { useEffect, useState } from 'react'
import { apiFetch } from './apiClient'

function readable(value) {
  return value ? value.replaceAll('_', ' ') : 'Not assessed'
}

function EvidenceGroup({ title, items }) {
  if (!items?.length) return null

  return (
    <section className="archive-evidence-group">
      <h3>{title}</h3>
      {items.map((item, index) => (
        <div
          className="archive-quote"
          key={`${item.documentId}-${item.start}-${index}`}
        >
          <blockquote>{item.quote}</blockquote>
          <span>
            {item.firstPage
              ? `Page ${item.firstPage}`
              : 'TXT document'}
            {' · '}Text offsets {item.start}–{item.end}
          </span>
        </div>
      ))}
    </section>
  )
}

function SavedReviews({ onBack }) {
  const [page, setPage] = useState(0)
  const [list, setList] = useState(null)
  const [detail, setDetail] = useState(null)
  const [opening, setOpening] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      try {
        const response = await apiFetch(
          `/api/reviews?page=${page}`,
          { signal: controller.signal }
        )
        const data = await response.json().catch(() => ({}))

        if (!response.ok) {
          throw new Error(
            response.status === 401
              ? 'Session expired. Please sign in again.'
              : data.error || 'Could not load saved reviews.'
          )
        }

        setList(data)
      } catch (caught) {
        if (caught.name !== 'AbortError') {
          setError(caught.message)
        }
      }
    }

    load()
    return () => controller.abort()
  }, [page])

  async function openReview(id) {
    setOpening(true)
    setError('')
    setDetail(null)

    try {
      const response = await apiFetch(`/api/reviews/${id}`)
      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(
          data.error || 'Could not open this saved review.'
        )
      }

      setDetail(data)
    } catch (caught) {
      setError(caught.message)
    } finally {
      setOpening(false)
    }
  }

  function changePage(nextPage) {
    setError('')
    setList(null)
    setDetail(null)
    setPage(nextPage)
  }

  const result = detail?.result

  return (
    <main className="archive-page" id="main">
      <div className="archive-shell">
        <header className="archive-header">
          <div>
            <span className="eyebrow">YOUR PRIVATE WORKSPACE</span>
            <h1>Review archive<span aria-hidden="true">.</span></h1>
            <p>Return to the findings you chose to save.</p>
          </div>

          <button type="button" onClick={onBack}>
            Back to workspace ↗
          </button>
        </header>

        {error && (
          <p className="archive-error" role="alert">{error}</p>
        )}

        {!list && !error && (
          <p role="status">Loading saved reviews…</p>
        )}

        {list?.items?.length === 0 && (
          <section className="archive-empty">
            <span aria-hidden="true">✳</span>
            <h2>No saved reviews yet.</h2>
            <p>Run a document review, then select Save review.</p>
          </section>
        )}

        {list?.items?.length > 0 && (
          <>
            <div className="archive-count">
              {list.totalItems} saved review
              {list.totalItems === 1 ? '' : 's'}
            </div>

            <div className="archive-grid">
              {list.items.map((item) => (
                <button
                  className="archive-card"
                  type="button"
                  key={item.id}
                  onClick={() => openReview(item.id)}
                >
                  <span className="eyebrow">DOCUMENT REVIEW</span>
                  <strong>{item.agreementFilename}</strong>
                  <span>with {item.sowFilename}</span>
                  <span className="archive-card-status">
                    {readable(item.reviewStatus)}
                  </span>
                  <small>
                    {new Date(item.createdAt).toLocaleString()}
                    {' · '}Policy limit: {item.policyMaxCalendarDays} days
                  </small>
                </button>
              ))}
            </div>

            {list.totalPages > 1 && (
              <nav className="archive-pagination" aria-label="Review pages">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => changePage(page - 1)}
                >
                  Previous
                </button>
                <span>Page {page + 1} of {list.totalPages}</span>
                <button
                  type="button"
                  disabled={page + 1 >= list.totalPages}
                  onClick={() => changePage(page + 1)}
                >
                  Next
                </button>
              </nav>
            )}
          </>
        )}

        {opening && <p role="status">Opening review…</p>}

        {result && (
          <article className="archive-detail">
            <div className="archive-detail-heading">
              <span className="eyebrow">SAVED FINDINGS</span>
              <h2>{readable(result.reviewStatus)}</h2>
              {result.reviewReason && <p>{result.reviewReason}</p>}
            </div>

            <div className="archive-detail-summary">
              <p>
                Comparison: <strong>
                  {readable(result.comparisonStatus)}
                </strong>
              </p>
              <p>
                Payment priority: <strong>
                  {readable(result.precedenceStatus)}
                </strong>
              </p>
            </div>

            <EvidenceGroup
              title="Agreement evidence"
              items={result.agreementMatches}
            />
            <EvidenceGroup
              title="SOW evidence"
              items={result.sowMatches}
            />
            <EvidenceGroup
              title="Agreement wording to check"
              items={result.agreementUnrecognized}
            />
            <EvidenceGroup
              title="SOW wording to check"
              items={result.sowUnrecognized}
            />
            <EvidenceGroup
              title="Payment priority wording"
              items={result.precedenceEvidence}
            />

            <p className="archive-notice">
              These are saved extracted-text quotes. Reopen the
              original files to verify the wording and scanned pages.
            </p>
          </article>
        )}
      </div>
    </main>
  )
}

export default SavedReviews