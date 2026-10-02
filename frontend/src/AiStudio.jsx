import { useEffect, useRef, useState } from 'react'
import AiSuggestions from './AiSuggestions'

function SourcePreview({ file, evidence }) {
  const frame = useRef(null)
  const [text, setText] = useState('')
  const [error, setError] = useState('')

  const isPdf = file?.name?.toLowerCase().endsWith('.pdf')
  const page = Number.isInteger(evidence?.firstPage)
    ? evidence.firstPage
    : 1

  useEffect(() => {
    if (!file || !isPdf || !frame.current) {
      return undefined
    }

    const url = URL.createObjectURL(file)
    frame.current.src = `${url}#page=${page}`

    return () => URL.revokeObjectURL(url)
  }, [file, isPdf, page])

  useEffect(() => {
    if (!file || isPdf) return undefined

    let active = true

    file.text()
      .then((content) => {
        if (active) setText(content)
      })
      .catch(() => {
        if (active) setError('Could not open this text file.')
      })

    return () => {
      active = false
    }
  }, [file, isPdf])

  if (!file) {
    return (
      <div className="ai-studio-no-document">
        Upload an Agreement and SOW in the Review Desk first.
      </div>
    )
  }

  if (isPdf) {
    return (
      <iframe
        ref={frame}
        title={`Original document: ${file.name}`}
        className="ai-studio-pdf"
      />
    )
  }

  if (error) {
    return <p role="alert">{error}</p>
  }

  const characters = Array.from(text)
  const selectedText = evidence
    ? characters.slice(evidence.start, evidence.end).join('')
    : ''
  const exactMatch = evidence && selectedText === evidence.quote

  return (
    <pre className="ai-studio-text">
      {exactMatch ? (
        <>
          {characters.slice(0, evidence.start).join('')}
          <mark>{selectedText}</mark>
          {characters.slice(evidence.end).join('')}
        </>
      ) : (
        text || 'Opening document...'
      )}
    </pre>
  )
}

function AiStudio({ files, session, onSessionChange, onBack }) {
  const [activeDocument, setActiveDocument] = useState('agreement')
  const [selectedEvidence, setSelectedEvidence] = useState(null)

  const agreement = files?.agreement
  const sow = files?.sow
  const activeFile = activeDocument === 'agreement'
    ? agreement
    : sow

  const visibleEvidence =
    selectedEvidence?.file === activeFile
      ? selectedEvidence.item
      : null

  function inspect(file, item) {
    setActiveDocument(file === agreement ? 'agreement' : 'sow')
    setSelectedEvidence({ file, item })
  }

  function switchDocument(document) {
    setActiveDocument(document)
    setSelectedEvidence(null)
  }

  return (
    <main className="ai-studio-page" id="main">
      <header className="ai-studio-nav">
        <span className="ai-studio-brand">
          folio<span>.</span> / AI Studio
        </span>

        <button type="button" onClick={onBack}>
          Back to Review Desk
        </button>
      </header>

      <div className="ai-studio-intro">
        <span className="eyebrow">LOCAL MODEL / SOURCE CHECK</span>
        <h1>
          Explore the clause.
          <em> Inspect the source.</em>
        </h1>
        <p>
          AI suggestions help locate payment wording. Every quote
          must be checked in the original document. Suggestions do
          not change the rule-based review result.
        </p>
      </div>

      <div className="ai-studio-layout">
        <section
          className="ai-studio-source"
          aria-labelledby="ai-studio-source-title"
        >
          <div className="ai-studio-source-header">
            <div>
              <span className="eyebrow">01 / ORIGINAL DOCUMENT</span>
              <h2 id="ai-studio-source-title">
                Source viewer
              </h2>
            </div>

            <div
              className="ai-studio-tabs"
              aria-label="Choose source document"
            >
              <button
                type="button"
                aria-pressed={activeDocument === 'agreement'}
                onClick={() => switchDocument('agreement')}
              >
                Agreement
              </button>
              <button
                type="button"
                aria-pressed={activeDocument === 'sow'}
                onClick={() => switchDocument('sow')}
              >
                SOW
              </button>
            </div>
          </div>

          <p className="ai-studio-filename">
            {activeFile?.name || 'No document selected'}
          </p>

          <div className="ai-studio-preview">
            <SourcePreview
              key={activeFile?.name + activeFile?.lastModified}
              file={activeFile}
              evidence={visibleEvidence}
            />
          </div>

          {visibleEvidence && (
            <div className="ai-studio-selected">
              <span className="eyebrow">
                SELECTED EXACT QUOTE
              </span>
              <blockquote>{visibleEvidence.quote}</blockquote>
              <small>
                {visibleEvidence.firstPage
                  ? `Page ${visibleEvidence.firstPage}`
                  : 'TXT document'}
                {' | '}
                Offsets {visibleEvidence.start}-
                {visibleEvidence.end}
              </small>
            </div>
          )}
        </section>

        <div className="ai-studio-discovery">
          <div className="ai-studio-discovery-heading">
            <span className="eyebrow">
              02 / SUGGESTED PASSAGES
            </span>
            <p>
              Select a quote to inspect its source on the left.
            </p>
          </div>

         <AiSuggestions
  files={files}
  session={session}
  onSessionChange={onSessionChange}
  onInspect={inspect}
/>
        </div>
      </div>

      <footer className="ai-studio-footer">
        <span>Suggestions only / Human verification required</span>
        <button type="button" onClick={onBack}>
          Return to review
        </button>
      </footer>
    </main>
  )
}

export default AiStudio