import { useState } from 'react'
import { apiFetch } from './apiClient'

function SaveReviewButton({ files, maxDays, reviewStatus }) {
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(null)
  const [error, setError] = useState('')

  if (!files?.agreement || !files?.sow) return null

  async function save() {
    if (busy || saved) return

    setBusy(true)
    setError('')

    try {
      const upload = new FormData()
      upload.append('agreement', files.agreement)
      upload.append('sow', files.sow)
      upload.append('maxDays', String(maxDays))

      const response = await apiFetch('/api/reviews', {
        method: 'POST',
        body: upload,
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Session expired. Sign in again and retry.')
        }

        throw new Error(data.error || 'Could not save this review.')
      }

      if (!data.id) {
        throw new Error('Server did not return a saved review ID.')
      }

      setSaved(data)
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Could not save this review.'
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="review-save-action">
      <button
        type="button"
        className="result-print"
        onClick={save}
        disabled={busy || Boolean(saved)}
      >
        {busy
          ? 'Saving review…'
          : saved
            ? 'Review saved ✓'
            : 'Save review ↗'}
      </button>

      {busy && (
        <p role="status">
          Checking the documents again before saving. Scanned PDFs
          may take longer.
        </p>
      )}

      {saved && (
        <p role="status">
          Saved to your account.
          {saved.reviewStatus !== reviewStatus &&
            ' The new review status differs from the one currently displayed; open the saved review to inspect it.'}
        </p>
      )}

      {error && (
        <p className="result-warning" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export default SaveReviewButton