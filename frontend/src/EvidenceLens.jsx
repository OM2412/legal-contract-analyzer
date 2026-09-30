import { useEffect, useRef, useState } from 'react'

function EvidenceLens({ file, evidence, onClose }) {
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const pdfFrame = useRef(null)
  const closeButton = useRef(null)

  const isPdf = file?.name?.toLowerCase().endsWith('.pdf')
  const page = Number.isInteger(evidence?.firstPage)
    ? evidence.firstPage
    : 1

  useEffect(() => {
    if (!file || !isPdf || !pdfFrame.current) {
      return undefined
    }

    const url = URL.createObjectURL(file)
    pdfFrame.current.src = `${url}#page=${page}`

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
        if (active) {
          setError('Could not open the original text file.')
        }
      })

    return () => {
      active = false
    }
  }, [file, isPdf])

  useEffect(() => {
    const previouslyFocused = document.activeElement
    closeButton.current?.focus()

    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose()
    }

    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      previouslyFocused?.focus?.()
    }
  }, [onClose])

  if (!file || !evidence) return null

  const characters = Array.from(text)
  const quoteFromOffsets = characters
    .slice(evidence.start, evidence.end)
    .join('')
  const offsetsMatch =
    text.length > 0 && quoteFromOffsets === evidence.quote

  return (
    <div
      className="evidence-lens-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <aside
        className="evidence-lens"
        role="dialog"
        aria-modal="true"
        aria-labelledby="evidence-lens-title"
      >
        <header className="evidence-lens-header">
          <div>
            <span className="eyebrow">SOURCE CHECK</span>
            <h2 id="evidence-lens-title">{file.name}</h2>
          </div>

          <button
            ref={closeButton}
            type="button"
            onClick={onClose}
            aria-label="Close source preview"
          >
            Close
          </button>
        </header>

        <div className="evidence-lens-body">
          <div className="evidence-lens-preview">
            {isPdf && (
              <iframe
                ref={pdfFrame}
                title={`Original PDF: ${file.name}`}
              />
            )}

            {!isPdf && error && <p role="alert">{error}</p>}

            {!isPdf && !error && text && (
              <pre>
                {offsetsMatch ? (
                  <>
                    {characters.slice(0, evidence.start).join('')}
                    <mark>{quoteFromOffsets}</mark>
                    {characters.slice(evidence.end).join('')}
                  </>
                ) : (
                  text
                )}
              </pre>
            )}
          </div>

          <div className="evidence-lens-details">
            <span className="eyebrow">CITED PASSAGE</span>
            <blockquote>{evidence.quote}</blockquote>

            <p>
              {isPdf ? `Page ${page}` : 'TXT document'}
              {' | '}
              Text offsets {evidence.start}-{evidence.end}
            </p>

            {evidence.textSource === 'OCR_TEXT' && (
              <p className="evidence-lens-warning">
                OCR text: verify the quote against the scanned page.
              </p>
            )}

            {!isPdf && text && !offsetsMatch && (
              <p className="evidence-lens-warning">
                Exact offsets did not match this local file.
                Check that it is the same document version.
              </p>
            )}

            <p className="evidence-lens-note">
              Compare this passage with the original document
              before relying on the review.
            </p>
          </div>
        </div>
      </aside>
    </div>
  )
}

export default EvidenceLens