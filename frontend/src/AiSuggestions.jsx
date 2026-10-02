import { useEffect, useRef, useState } from 'react'
import AiComparison from './AiComparison'

async function requestSuggestions(file, signal) {
  const form = new FormData()
  form.append('document', file)

  const response = await fetch('/api/ai/quotes', {
    method: 'POST',
    body: form,
    signal,
  })

  const data = await response.json()

  if (!response.ok) {
    throw new Error(data.error || 'Local AI request failed.')
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
      <h4>{title}</h4>

      {quotes.length === 0 && (
        <p>No exact payment quote suggestion returned.</p>
      )}

      {quotes.map((item) => (
        <div
          className="ai-suggestion-quote"
          key={`${item.documentId}-${item.start}-${item.end}`}
        >
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
              View original
            </button>
          </div>
        </div>
      ))}

      {candidates.map((candidate) => (
        <div
          className="ai-suggestion-candidate"
          key={
            `${candidate.evidence.start}-` +
            `${candidate.evidence.end}-${candidate.days}`
          }
        >
          <strong>
            Suggested term: {candidate.days}{' '}
            {candidate.dayUnit?.replaceAll('_', ' ').toLowerCase()}
          </strong>

          <p>
            Invoice receipt:{' '}
            {candidate.invoiceReceiptConfirmed
              ? 'supported by the quote'
              : 'not independently confirmed'}
          </p>
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
      progress: 'Checking Agreement with local AI...',
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
        progress: 'Checking Statement of Work with local AI...',
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
          error: requestError.message || 'Local AI request failed.',
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
          <span className="eyebrow">OPTIONAL LOCAL AI</span>
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

      <p>
        The local model suggests passages. These suggestions do
        not change the rule-based review or determine legal priority.
      </p>

      {busy && (
        <button
          type="button"
          className="ai-suggestions-cancel"
          onClick={() => controllerRef.current?.abort()}
        >
          Cancel AI check
        </button>
      )}

      {progress && <p role="status">{progress}</p>}
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