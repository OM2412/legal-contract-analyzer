import { useState } from 'react'

const MAX_PDF_BYTES = 10 * 1024 * 1024
const MAX_TXT_BYTES = 1024 * 1024

function validateFile(file) {
  if (!file) {
    return 'Choose a file.'
  }

  const name = file.name.toLowerCase()

  if (name.endsWith('.pdf')) {
    if (file.size > MAX_PDF_BYTES) {
      return 'PDF must be 10 MB or smaller.'
    }
    return null
  }

  if (name.endsWith('.txt')) {
    if (file.size > MAX_TXT_BYTES) {
      return 'TXT file must be 1 MB or smaller.'
    }
    return null
  }

  return 'Choose a PDF or TXT file.'
}

function formatSize(bytes) {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function FileDropZone({ label, name, file, disabled, onFileChange }) {
  const [dragging, setDragging] = useState(false)

  function handleDrop(event) {
    event.preventDefault()
    setDragging(false)

    if (disabled) return

    const droppedFile = event.dataTransfer.files?.[0]
    if (droppedFile) {
      onFileChange(name, droppedFile)
    }
  }

  return (
    <div
      className={`upload-zone${dragging ? ' upload-zone-active' : ''}`}
      onDragOver={(event) => {
        event.preventDefault()
        if (!disabled) setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
    >
      <div className="upload-zone-heading">
        <span className="upload-zone-name">{label}</span>
        <span className="upload-zone-type">PDF / TXT</span>
      </div>

      <span className="upload-zone-icon" aria-hidden="true">
        {file ? '✓' : '+'}
      </span>

      <label className="upload-file-label">
        <span>{file ? 'Replace document' : 'Choose a document'}</span>
        <input
          className="upload-input"
          name={name}
          type="file"
          accept=".pdf,.txt"
          disabled={disabled}
          onChange={(event) => {
            const selected = event.target.files?.[0]
            if (selected) onFileChange(name, selected)
            event.target.value = ''
          }}
        />
      </label>

      <p className="upload-zone-hint">
        {file ? file.name : 'or drag and drop it here'}
      </p>

      {file && (
        <div className="upload-file-meta">
          <span>{formatSize(file.size)}</span>
          <button
            type="button"
            disabled={disabled}
            onClick={() => onFileChange(name, null)}
          >
            Remove
          </button>
        </div>
      )}
    </div>
  )
}

function ReviewWorkspace({ onReview }) {
  const [files, setFiles] = useState({
    agreement: null,
    sow: null,
  })
  const [maxDays, setMaxDays] = useState('30')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleFileChange(name, file) {
    setFiles((current) => ({ ...current, [name]: file }))
    setError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    const agreementError = validateFile(files.agreement)
    if (agreementError) {
      setError(`Agreement: ${agreementError}`)
      return
    }

    const sowError = validateFile(files.sow)
    if (sowError) {
      setError(`Statement of Work: ${sowError}`)
      return
    }

    const days = Number(maxDays)
    if (!Number.isInteger(days) || days < 1 || days > 3650) {
      setError('Enter a policy limit between 1 and 3650 days.')
      return
    }

    const upload = new FormData()
    upload.append('agreement', files.agreement)
    upload.append('sow', files.sow)
    upload.append('maxDays', String(days))

    setSubmitting(true)

    try {
      const response = await fetch('/api/review', {
        method: 'POST',
        body: upload,
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.error || 'These documents could not be reviewed.')
      }

      onReview?.(data, {
        agreement: files.agreement,
        sow: files.sow,
      })
    } catch (caught) {
      setError(
        caught instanceof TypeError
          ? 'Could not connect to the review service. Please try again.'
          : caught.message
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="workspace-section section-spacing" id="workspace">
      <div className="page-container">
        <div className="section-heading">
          <span className="eyebrow">YOUR WORKSPACE / NEW REVIEW</span>
          <h2>Bring the documents.<br />We’ll trace the difference.</h2>
          <p>
            Add one Agreement and one Statement of Work. Review the
            extracted quotes against your original files.
          </p>
        </div>

        <form
          className="workspace-form"
          onSubmit={handleSubmit}
          aria-busy={submitting}
        >
          <div className="workspace-file-grid">
            <FileDropZone
              label="Agreement"
              name="agreement"
              file={files.agreement}
              disabled={submitting}
              onFileChange={handleFileChange}
            />

            <FileDropZone
              label="Statement of Work"
              name="sow"
              file={files.sow}
              disabled={submitting}
              onFileChange={handleFileChange}
            />
          </div>

          <div className="workspace-controls">
            <label htmlFor="maxDays">
              Demo policy: maximum calendar days after invoice receipt
            </label>
            <input
              id="maxDays"
              name="maxDays"
              type="number"
              min="1"
              max="3650"
              value={maxDays}
              disabled={submitting}
              onChange={(event) => setMaxDays(event.target.value)}
            />
            <span>
              This is an illustrative business preference, not a legal rule.
            </span>
          </div>

          {error && (
            <p className="workspace-error" role="alert">
              {error}
            </p>
          )}

          <div className="workspace-footer">
            <p>
              PDF up to 10 MB · TXT up to 1 MB · Scanned PDF pages may
              need extra processing
            </p>

            <button
              className="button button-lime"
              type="submit"
              disabled={submitting}
            >
              {submitting ? 'Reviewing documents…' : 'Review payment terms'}
              {!submitting && <span aria-hidden="true">↗</span>}
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}

export default ReviewWorkspace