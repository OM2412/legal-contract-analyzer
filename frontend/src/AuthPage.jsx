import { useState } from 'react'
import './AuthPage.css'
import { apiFetch } from './apiClient'

async function errorMessage(response, fallback) {
  const data = await response.json().catch(() => null)
  return data?.error || fallback
}

function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const registering = mode === 'register'

  function changeMode(nextMode) {
    setMode(nextMode)
    setError('')
    setPassword('')
  }

  async function submit(event) {
    event.preventDefault()
    if (busy) return

    setBusy(true)
    setError('')

    try {
      if (registering) {
        const registration = await apiFetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: email.trim(),
            displayName: displayName.trim(),
            password,
          }),
        })

        if (!registration.ok) {
          throw new Error(
            await errorMessage(registration, 'Could not create your account.')
          )
        }
      }

      const login = await apiFetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
          email: email.trim(),
          password,
        }),
      })

      if (!login.ok) {
        throw new Error(
          await errorMessage(login, 'Email or password is incorrect.')
        )
      }

      setPassword('')
      onAuthenticated?.()
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Something went wrong. Please try again.'
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-story" aria-label="About Folio">
        <a className="auth-brand" href="/">
          FOLIO<span aria-hidden="true">✳</span>
        </a>

        <div className="auth-story-content">
          <span className="auth-eyebrow">DOCUMENT INTELLIGENCE</span>
          <h1>
            See the clause.
            <br />
            Understand the risk.
          </h1>
          <p>
            Review contract wording with source evidence, clear
            comparisons, and space for your own judgment.
          </p>
        </div>

        <span className="auth-story-footer">
          Legal document analysis &amp; risk identification
        </span>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-inner">
          <span className="auth-eyebrow">YOUR WORKSPACE</span>
          <h2>{registering ? 'Create your account' : 'Welcome back'}</h2>
          <p className="auth-form-intro">
            {registering
              ? 'Set up your workspace to review and save findings.'
              : 'Sign in to continue your document reviews.'}
          </p>

          <div className="auth-switch" aria-label="Account action">
            <button
              type="button"
              className={registering ? '' : 'active'}
              aria-pressed={!registering}
              onClick={() => changeMode('login')}
            >
              Sign in
            </button>
            <button
              type="button"
              className={registering ? 'active' : ''}
              aria-pressed={registering}
              onClick={() => changeMode('register')}
            >
              Create account
            </button>
          </div>

          <form className="auth-form" onSubmit={submit}>
            {registering && (
              <label>
                Display name
                <input
                  type="text"
                  name="displayName"
                  value={displayName}
                  onChange={(event) => setDisplayName(event.target.value)}
                  autoComplete="name"
                  maxLength={120}
                  required
                />
              </label>
            )}

            <label>
              Email address
              <input
                type="email"
                name="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                maxLength={320}
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                name="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete={
                  registering ? 'new-password' : 'current-password'
                }
                minLength={registering ? 12 : undefined}
                required
              />
            </label>

            {registering && (
              <p className="auth-password-hint">
                Use at least 12 characters.
              </p>
            )}

            {error && (
              <p className="auth-error" role="alert">
                {error}
              </p>
            )}

            <button
              className="auth-submit"
              type="submit"
              disabled={busy}
            >
              {busy
                ? 'Please wait…'
                : registering
                  ? 'Create account'
                  : 'Sign in'}
              <span aria-hidden="true">↗</span>
            </button>
          </form>

          <p className="auth-footnote">
            Folio helps organize potential risks. Verify findings
            against the original documents.
          </p>
        </div>
      </section>
    </main>
  )
}

export default AuthPage