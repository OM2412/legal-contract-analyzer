import { useEffect, useRef, useState } from 'react'
import { apiFetch } from './apiClient'
import { FolioIcon, MoonLoader } from './components/FolioUI'
import './SavedReviewActions.css'

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

function DeleteDialog({ item, onClose, onDeleted }) {
  const dialog = useRef(null)
  const busyRef = useRef(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const element = dialog.current
    element.showModal()

    return () => {
      if (element.open) element.close()
    }
  }, [])

  async function confirmDelete() {
    if (busyRef.current) return

    busyRef.current = true
    setBusy(true)
    setError('')

    try {
      const response = await apiFetch(
        `/api/reviews/${item.id}`,
        { method: 'DELETE' }
      )

      if (response.status === 204) {
        onDeleted(item.id, 'Saved review deleted.')
        return
      }

      if (response.status === 404) {
        onDeleted(
          item.id,
          'This review is no longer available. The archive has been refreshed.'
        )
        return
      }

      throw new Error(
        response.status === 401
          ? 'Session expired. Please sign in again.'
          : response.status === 403
            ? 'The security check failed. Refresh the page and try again.'
            : 'Could not delete this review. Please try again.'
      )
    } catch (caught) {
      setError(caught.message)
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  return (
    <dialog
      ref={dialog}
      className="archive-delete-dialog"
      aria-labelledby="delete-review-title"
      aria-describedby="delete-review-description"
      onCancel={(event) => {
        event.preventDefault()
        if (!busyRef.current) onClose()
      }}
    >
      <span className="archive-delete-dialog__icon">
        <FolioIcon name="document" size={30} />
      </span>

      <span className="archive-delete-dialog__eyebrow">
        YOUR DATA / YOUR CONTROL
      </span>

      <h2 id="delete-review-title">
        Delete this saved review?
      </h2>

      <p id="delete-review-description">
        This removes the saved result and its source quotes
        from your review archive. There is no undo in the app.
        Original files on your device are unaffected.
      </p>

      <div className="archive-delete-dialog__files">
        <strong>{item.agreementFilename}</strong>
        <span>with {item.sowFilename}</span>
      </div>

      {error && (
        <p className="archive-error" role="alert">
          {error}
        </p>
      )}

      <div className="archive-delete-dialog__actions">
        <button
          type="button"
          className="archive-action archive-action--keep"
          disabled={busy}
          onClick={onClose}
          autoFocus
        >
          Keep review
        </button>

        <button
          type="button"
          className="archive-action archive-action--danger"
          disabled={busy}
          aria-busy={busy}
          onClick={confirmDelete}
        >
          {busy && <MoonLoader decorative />}
          {busy ? 'Deleting…' : 'Delete saved review'}
        </button>
      </div>

      <span className="archive-actions-sr" role="status">
        {busy ? 'Deleting saved review. Please wait.' : ''}
      </span>
    </dialog>
  )
}

function SavedReviews({ onBack }) {
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [list, setList] = useState(null)
  const [detail, setDetail] = useState(null)
  const [opening, setOpening] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)

  const heading = useRef(null)
  const openRequest = useRef(null)

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      setError('')

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
              : 'Could not load saved reviews.'
          )
        }

        if (controller.signal.aborted) return

        if (page > 0 && data.items?.length === 0) {
          setPage(Math.max(0, (data.totalPages ?? 1) - 1))
          return
        }

        setList(data)
      } catch (caught) {
        if (!controller.signal.aborted) {
          setError(caught.message)
        }
      }
    }

    load()

    return () => controller.abort()
  }, [page, revision])

  useEffect(() => {
    return () => openRequest.current?.abort()
  }, [])

  function cancelOpening() {
    openRequest.current?.abort()
    openRequest.current = null
    setOpening(false)
  }

  async function openReview(id) {
    cancelOpening()

    const controller = new AbortController()
    openRequest.current = controller

    setOpening(true)
    setError('')
    setDetail(null)

    try {
      const response = await apiFetch(
        `/api/reviews/${id}`,
        { signal: controller.signal }
      )

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(
          response.status === 401
            ? 'Session expired. Please sign in again.'
            : response.status === 404
              ? 'This saved review is no longer available.'
              : 'Could not open this saved review.'
        )
      }

      if (!controller.signal.aborted) {
        setDetail(data)
      }
    } catch (caught) {
      if (!controller.signal.aborted) {
        setError(caught.message)
      }
    } finally {
      if (openRequest.current === controller) {
        openRequest.current = null
        setOpening(false)
      }
    }
  }

  function changePage(nextPage) {
    cancelOpening()
    setError('')
    setNotice('')
    setList(null)
    setDetail(null)
    setPage(nextPage)
  }

  function handleDeleted(id, message) {
    cancelOpening()
    setDeleteTarget(null)

    if (detail?.summary?.id === id) {
      setDetail(null)
    }

    setNotice(message)
    setError('')
    setList(null)
    setRevision((value) => value + 1)

    window.requestAnimationFrame(() => {
      heading.current?.focus()
    })
  }

  const result = detail?.result

  return (
    <main className="archive-page" id="main">
      <div className="archive-shell">
        <header className="archive-header">
          <div>
            <span className="eyebrow">
              YOUR PRIVATE WORKSPACE
            </span>

            <h1 ref={heading} tabIndex={-1}>
              Review archive
              <span aria-hidden="true">.</span>
            </h1>

            <p>Return to the findings you chose to save.</p>
          </div>

          <button type="button" onClick={onBack}>
            Back to review <FolioIcon name="arrow" size={18} />
          </button>
        </header>

        {error && (
          <p className="archive-error" role="alert">
            {error}
          </p>
        )}

        <div role="status" aria-live="polite">
          {notice && (
            <p className="archive-success">{notice}</p>
          )}
        </div>

        {!list && !error && (
          <div className="archive-loading">
            <MoonLoader label="Loading saved reviews" />
            <span>Loading saved reviews…</span>
          </div>
        )}

        {list?.items?.length === 0 && (
          <section className="archive-empty">
            <FolioIcon name="moon" size={38} />
            <h2>No saved reviews yet.</h2>
            <p>
              Run a document review, then select Save review.
            </p>
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
                <article
                  className="archive-review-tile"
                  key={item.id}
                >
                  <button
                    className="archive-card"
                    type="button"
                    onClick={() => openReview(item.id)}
                  >
                    <span className="eyebrow">
                      DOCUMENT REVIEW
                    </span>

                    <strong>{item.agreementFilename}</strong>
                    <span>with {item.sowFilename}</span>

                    <span className="archive-card-status">
                      {readable(item.reviewStatus)}
                    </span>

                    <small>
                      {new Date(item.createdAt).toLocaleString()}
                      {' · '}Policy limit:{' '}
                      {item.policyMaxCalendarDays} days
                    </small>
                  </button>

                  <div className="archive-review-tile__actions">
                    <button
                      type="button"
                      className="archive-delete-trigger"
                      aria-label={
                        `Delete saved review: ${item.agreementFilename}`
                        + ` with ${item.sowFilename}`
                      }
                      onClick={() => setDeleteTarget(item)}
                    >
                      Delete saved review
                    </button>
                  </div>
                </article>
              ))}
            </div>

            {list.totalPages > 1 && (
              <nav
                className="archive-pagination"
                aria-label="Review pages"
              >
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => changePage(page - 1)}
                >
                  Previous
                </button>

                <span>
                  Page {page + 1} of {list.totalPages}
                </span>

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

        {opening && (
          <div className="archive-loading">
            <MoonLoader label="Opening saved review" />
            <span>Opening review…</span>
          </div>
        )}

        {result && (
          <article className="archive-detail">
            <div className="archive-detail-heading">
              <span className="eyebrow">SAVED FINDINGS</span>
              <h2>{readable(result.reviewStatus)}</h2>
              {result.reviewReason && (
                <p>{result.reviewReason}</p>
              )}
            </div>

            <div className="archive-detail-summary">
              <p>
                Comparison:{' '}
                <strong>
                  {readable(result.comparisonStatus)}
                </strong>
              </p>

              <p>
                Payment priority:{' '}
                <strong>
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

      {deleteTarget && (
        <DeleteDialog
          key={deleteTarget.id}
          item={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDeleted={handleDeleted}
        />
      )}
    </main>
  )
}

export default SavedReviews