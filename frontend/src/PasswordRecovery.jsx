import { useState } from 'react'
import { apiFetch } from './apiClient'
import './AuthPage.css'
import { FolioButton } from './components/FolioUI'
function PasswordRecovery({
  mode = 'forgot',
  resetToken = '',
  onCompleted,
}) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [completed, setCompleted] = useState(false)

  const resetting = mode === 'reset'
  const validToken = /^[A-Za-z0-9_-]{43}$/.test(resetToken)
  const invalidLink = resetting && !validToken && !completed

  async function submit(event) {
    event.preventDefault()
    if (busy) return

    setError('')
    setNotice('')

    if (resetting) {
      if (!validToken) {
        setError('Open a new reset link from your email.')
        return
      }

      if (
        Array.from(password).length < 12 ||
        new TextEncoder().encode(password).length > 72
      ) {
        setError(
          'Use at least 12 characters and at most 72 UTF-8 bytes.'
        )
        return
      }

      if (password !== confirmation) {
        setError('The passwords do not match.')
        return
      }
    }

    setBusy(true)

    try {
      const response = await apiFetch(
        resetting
          ? '/api/auth/reset-password'
          : '/api/auth/forgot-password',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(
            resetting
              ? { token: resetToken, password }
              : { email: email.trim() }
          ),
        }
      )

      const data = await response.json().catch(() => null)

      if (!response.ok) {
        const fallback = response.status === 429
          ? 'Too many attempts. Please try again later.'
          : response.status === 403
            ? 'Your secure request expired. Please try again.'
            : resetting
              ? 'Could not reset your password. Request a new link.'
              : 'Could not request a reset link. Please try again.'

        throw new Error(data?.error || fallback)
      }

      if (resetting) {
        setPassword('')
        setConfirmation('')
        setCompleted(true)
        onCompleted?.()
      } else {
        setNotice(
          data?.message ||
          'If an account exists for this email, a reset link will be sent.'
        )
      }
    } catch (caught) {
      setError(
        caught instanceof TypeError
          ? 'Could not connect to Folio. Please try again.'
          : caught instanceof Error
            ? caught.message
            : 'Something went wrong. Please try again.'
      )
    } finally {
      setBusy(false)
    }
  }

  const heading = completed
    ? 'Password updated'
    : invalidLink
      ? 'Open a fresh reset link'
      : resetting
        ? 'Choose a new password'
        : 'Recover your workspace'

  return (
    <main className="auth-page auth-page--recovery">
      <section className="auth-story" aria-label="About Folio">
        <a className="auth-brand" href="/">
          FOLIO<span aria-hidden="true">&#10035;</span>
        </a>

        <div className="auth-story-content">
          <span className="auth-eyebrow">YOUR FOLIO ACCOUNT</span>
          <h1>
            A fresh start.
            <br />
            Same workspace.
          </h1>
          <p>
            Recover access to your account and continue reviewing
            your documents with source evidence.
          </p>
        </div>

        <span className="auth-story-footer">
          Legal document analysis &amp; risk identification
        </span>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-inner">
          <span className="auth-eyebrow">ACCOUNT RECOVERY</span>
          <h2>{heading}</h2>

          <p className="auth-form-intro">
            {completed
              ? 'Sign in with your new password. Previous sessions have been revoked.'
              : invalidLink
                ? 'This page has no usable reset token. Open the newest link from your email or request another.'
                : resetting
                  ? 'Your email link expires after 15 minutes and can be used once.'
                  : 'Enter your account email to request a password reset link.'}
          </p>

          {completed ? (
            <a
              className="auth-submit auth-recovery-action"
              href="#sign-in"
            >
              Back to sign in
              <span aria-hidden="true">&#8599;</span>
            </a>
          ) : invalidLink ? (
            <a
              className="auth-submit auth-recovery-action"
              href="#forgot-password"
            >
              Request a new link
              <span aria-hidden="true">&#8599;</span>
            </a>
          ) : (
            <form
              className="auth-form auth-recovery-form"
              onSubmit={submit}
              aria-busy={busy}
            >
              {resetting ? (
                <>
                  <label>
                    New password
                    <input
                      type="password"
                      name="newPassword"
                      autoComplete="new-password"
                      value={password}
                      onChange={(event) =>
                        setPassword(event.target.value)
                      }
                      minLength={12}
                      maxLength={144}
                      disabled={busy}
                      required
                    />
                  </label>

                  <label>
                    Confirm new password
                    <input
                      type="password"
                      name="confirmPassword"
                      autoComplete="new-password"
                      value={confirmation}
                      onChange={(event) =>
                        setConfirmation(event.target.value)
                      }
                      minLength={12}
                      maxLength={144}
                      disabled={busy}
                      required
                    />
                  </label>

                  <p className="auth-password-hint">
                    Use at least 12 characters. Maximum 72 UTF-8
                    bytes; some characters use more than one byte.
                  </p>
                </>
              ) : (
                <label>
                  Account email
                  <input
                    type="email"
                    name="email"
                    autoComplete="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    maxLength={320}
                    disabled={busy}
                    required
                  />
                </label>
              )}

              {error && (
                <p className="auth-error" role="alert">
                  {error}
                </p>
              )}

              {notice && (
                <p className="auth-notice" role="status">
                  {notice}
                </p>
              )}

              <FolioButton
  type="submit"
  loading={busy}
  loadingLabel={resetting ? 'Updating password...' : 'Sending reset link...'}
  icon={resetting ? 'shield' : 'arrow'}
>
  {resetting ? 'Update password' : 'Send reset link'}
</FolioButton>
            </form>
          )}

          {!completed && (
            <div className="auth-recovery-links">
              <a href="#sign-in">Back to sign in</a>
              {resetting && !invalidLink && (
                <a href="#forgot-password">Request a new link</a>
              )}
            </div>
          )}

          <p className="auth-footnote">
            Never share your reset link. Folio will not ask you
            to send your password by email.
          </p>
        </div>
      </section>
    </main>
  )
}

export default PasswordRecovery