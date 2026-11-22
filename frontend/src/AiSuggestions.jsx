import { useEffect, useRef, useState } from 'react'
import AiComparison from './AiComparison'
import { apiFetch } from './apiClient'
async function requestSuggestions(file, signal) {
  const form = new FormData()
  form.append('document', file)

 const response = await apiFetch('/api/ai/quotes', {
    method: 'POST',
    body: form,
    signal,
  })

  const data = await response.json()

  if (!response.ok) {
    throw new Error(data.error || 'Quote suggestion request failed.')
  }

  return data
}

function location(item) {
  const page = item.firstPage
    ? `Page ${item.firstPage}` +
      (item.lastPage !== item.firstPage
        ? `-${item.lastPage}`
        : '')
    : 'TXT document'

  return `${page} | Text offsets ${item.start}-${item.end}`
}

function SuggestionGroup({ title, file, data, onInspect }) {
  if (!data) return null

  const quotes = data.evidence ?? []
  const candidates = data.candidates ?? []

  return (
    <section className="ai-suggestion-group">
      <div className="ai-suggestion-group-head">
        <div><span className="eyebrow">DOCUMENT FINDINGS</span><h4>{title}</h4></div>
        <span className="ai-count-pill">{quotes.length} {quotes.length === 1 ? 'quote' : 'quotes'}</span>
      </div>

      {quotes.length === 0 && (
        <p>No exact payment quote suggestion returned.</p>
      )}

      {quotes.map((item, index) => (
        <article
          className="ai-suggestion-quote"
          key={`${item.documentId}-${item.start}-${item.end}`}
        >
          <div className="evidence-card-head"><span className="evidence-card-label">{String(index + 1).padStart(2, '0')} / Exact quote</span><span className="evidence-card-page">{item.firstPage ? `Page ${item.firstPage}` : 'Text source'}</span></div>
          <blockquote>{item.quote}</blockquote>

          <div className="result-evidence-bottom">
            <span className="result-location">
              {location(item)}
            </span>

            <button
              type="button"
              className="result-evidence-open"
              onClick={() => onInspect(file, item)}
            >
              Inspect source <span aria-hidden="true">↗</span>
            </button>
          </div>
        </article>
      ))}

      {candidates.map((candidate) => (
        <div
          className="ai-suggestion-candidate"
          key={
            `${candidate.evidence.start}-` +
            `${candidate.evidence.end}-${candidate.days}`
          }
        >
          <span className="eyebrow">MODEL-EXTRACTED CANDIDATE</span>
          <div className="ai-candidate-details">
            <div><span>Payment term</span><strong>{candidate.days} {candidate.dayUnit?.replaceAll('_', ' ').toLowerCase()}</strong></div>
            <div><span>Invoice trigger</span><strong>{candidate.invoiceReceiptConfirmed ? 'Source supported' : 'Not confirmed'}</strong></div>
          </div>
        </div>
      ))}
    </section>
  )
}

function AiSuggestions({
  files,
  session,
  onSessionChange,
  onInspect,
}) {
  const [busy, setBusy] = useState(false)
  const controllerRef = useRef(null)

  const {
    agreementData = null,
    sowData = null,
    progress = '',
    error = '',
  } = session ?? {}

  useEffect(() => {
    return () => controllerRef.current?.abort()
  }, [])

  function updateSession(changes) {
    onSessionChange((previous) => ({
      ...previous,
      ...changes,
    }))
  }

  async function runSuggestions() {
    if (!files?.agreement || !files?.sow || busy) return

    const controller = new AbortController()
    controllerRef.current = controller

    setBusy(true)
    updateSession({
      agreementData: null,
      sowData: null,
      progress: 'Checking Agreement for payment quotes...',
      error: '',
    })

    try {
      const agreement = await requestSuggestions(
        files.agreement,
        controller.signal
      )

      if (controller.signal.aborted) return

      updateSession({
        agreementData: agreement,
        progress: 'Checking Statement of Work for payment quotes...',
      })

      const sow = await requestSuggestions(
        files.sow,
        controller.signal
      )

      if (controller.signal.aborted) return

      updateSession({
        sowData: sow,
        progress:
          'Suggestions complete. Verify each quote in the original files.',
      })
    } catch (requestError) {
      if (requestError.name === 'AbortError') {
        updateSession({
          progress: 'AI suggestion request cancelled.',
        })
      } else {
        updateSession({
          error: requestError.message || 'Quote suggestion request failed.',
          progress: '',
        })
      }
    } finally {
      setBusy(false)

      if (controllerRef.current === controller) {
        controllerRef.current = null
      }
    }
  }

  return (
    <section
      className="ai-suggestions"
      aria-labelledby="ai-suggestions-heading"
    >
      <div className="ai-suggestions-heading">
        <div>
          <span className="eyebrow">AI ASSISTED / EVIDENCE CHECK</span>
          <h3 id="ai-suggestions-heading">
            Quote suggestions
          </h3>
        </div>

        <button
          type="button"
          className="ai-suggestions-run"
          disabled={busy || !files?.agreement || !files?.sow}
          onClick={runSuggestions}
        >
          {busy ? 'Checking...' : 'Find quotes'}
        </button>
      </div>

      <div className="ai-guidance-card"><span className="ai-guidance-symbol" aria-hidden="true">i</span><div><strong>Suggestions, not a legal verdict</strong><p>
        Suggestions use a configured language model when available,
        with evidence-checked rule-based extraction as a fallback.
        They do not change the primary review or determine legal priority.
      </p><p>
        When hosted AI is enabled, extracted text from both uploaded
        documents is sent to the configured AI provider for processing.
        Do not upload confidential contracts without permission.
      </p></div></div>

      {busy && (
        <button
          type="button"
          className="ai-suggestions-cancel"
          onClick={() => controllerRef.current?.abort()}
        >
          Cancel AI check
        </button>
      )}

      {progress && <p className="ai-progress-card" role="status">{busy && <span className="ai-progress-pulse" aria-hidden="true" />} {progress}</p>}
      {error && (
        <p className="result-warning" role="alert">
          {error}
        </p>
      )}

      <div className="ai-suggestions-grid">
        <SuggestionGroup
          title="Agreement suggestions"
          file={files?.agreement}
          data={agreementData}
          onInspect={onInspect}
        />

        <SuggestionGroup
          title="Statement of Work suggestions"
          file={files?.sow}
          data={sowData}
          onInspect={onInspect}
        />
      </div>
      <AiComparison
  agreementData={agreementData}
  sowData={sowData}
/>
    </section>
  )
}

export default AiSuggestions
