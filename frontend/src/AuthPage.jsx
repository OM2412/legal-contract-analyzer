import { useEffect, useRef, useState } from 'react'
import './AuthPage.css'
import { apiFetch } from './apiClient'
import { FolioButton, FolioIcon } from './components/FolioUI'

async function errorMessage(response, fallback) {
  const data = await response.json().catch(() => null)

  return typeof data?.error === 'string'
    ? data.error
    : fallback
}

function retryAfterSeconds(response) {
  const value = response.headers.get('Retry-After')

  if (value && /^\d+$/.test(value)) {
    return Math.min(86400, Math.max(1, Number(value)))
  }

  const date = value ? Date.parse(value) : NaN

  if (Number.isFinite(date)) {
    return Math.min(
      86400,
      Math.max(1, Math.ceil((date - Date.now()) / 1000))
    )
  }

  return 900
}

function formatWait(seconds) {
  const minutes = Math.floor(seconds / 60)
  const remainder = String(seconds % 60).padStart(2, '0')

  return `${minutes}:${remainder}`
}

function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [cooldowns, setCooldowns] = useState({
    login: 0,
    register: 0,
  })
  const [now, setNow] = useState(() => Date.now())

  const busyRef = useRef(false)
  const registering = mode === 'register'

  const remaining = Math.max(
    0,
    Math.ceil((cooldowns[mode] - now) / 1000)
  )

  useEffect(() => {
    const deadline = Math.max(
      cooldowns.login,
      cooldowns.register
    )

    if (deadline <= Date.now()) return

    const timer = window.setInterval(() => {
      const currentTime = Date.now()
      setNow(currentTime)

      if (currentTime >= deadline) {
        window.clearInterval(timer)
      }
    }, 1000)

    return () => window.clearInterval(timer)
  }, [cooldowns])

  function changeMode(nextMode) {
    if (busyRef.current) return

    setMode(nextMode)
    setError('')
    setNotice('')
    setPassword('')
    setNow(Date.now())
  }

  function applyRateLimit(response, operation) {
    const currentTime = Date.now()
    const seconds = retryAfterSeconds(response)

    setNow(currentTime)
    setCooldowns((current) => ({
      ...current,
      [operation]: currentTime + seconds * 1000,
    }))

    return new Error(
      operation === 'register'
        ? 'Too many account creation attempts. Please wait before trying again.'
        : 'Too many sign-in attempts. Please wait before trying again.'
    )
  }

  async function submit(event) {
    event.preventDefault()

    if (
      busyRef.current ||
      cooldowns[mode] > Date.now()
    ) {
      return
    }

  

    if (
      registering &&
      (
        Array.from(password).length < 12 ||
        new TextEncoder().encode(password).length > 72
      )
    ) {
      setError(
        'Use at least 12 characters and at most 72 UTF-8 bytes.'
      )
      return
    }

    busyRef.current = true
    setBusy(true)
    setError('')
    setNotice('')

    try {
      if (registering) {
        const registration = await apiFetch(
          '/api/auth/register',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              email: email.trim(),
              displayName: displayName.trim(),
              password,
            }),
          }
        )

        if (registration.status === 429) {
          throw applyRateLimit(registration, 'register')
        }

        if (!registration.ok) {
          throw new Error(
            await errorMessage(
              registration,
              'Could not create your account.'
            )
          )
        }

        setMode('login')
        setNotice(
          'Your account has been created. Signing you in…'
        )
      }

      const login = await apiFetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type':
            'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
          email: email.trim(),
          password,
        }),
      })

      if (login.status === 429) {
        if (registering) {
          setNotice(
            'Your account was created. Sign in when the waiting period ends.'
          )
        }

        throw applyRateLimit(login, 'login')
      }

      if (!login.ok) {
        if (registering) {
          setNotice(
            'Your account was created. Please sign in to continue.'
          )
        }

        throw new Error(
          await errorMessage(
            login,
            login.status === 401
              ? 'Email or password is incorrect.'
              : login.status === 403
                ? 'The security check failed. Refresh the page and try again.'
                : 'Could not sign in. Please try again.'
          )
        )
      }

      setPassword('')
      setNotice('')
      onAuthenticated?.()
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Something went wrong. Please try again.'
      )
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-story" aria-label="About Folio">
        <a className="auth-brand" href="#top">
          FOLIO
          <FolioIcon name="moon" size={25} />
        </a>

        <div className="auth-story-content">
          <span className="auth-eyebrow">
            DOCUMENT INTELLIGENCE
          </span>

          <h1>
            See the clause.
            <br />
            Follow the evidence.
          </h1>

          <p>
            Review contract wording with source evidence,
            clear comparisons, and space for your own judgment.
          </p>
        </div>

        <span className="auth-story-footer">
          Evidence first. Human judgment always.
        </span>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-inner">
          <span className="auth-eyebrow">
            YOUR WORKSPACE
          </span>

          <h2>
            {registering
              ? 'Create your account'
              : 'Welcome back'}
          </h2>

          <p className="auth-form-intro">
            {registering
              ? 'Set up your workspace to review and save findings.'
              : 'Sign in to continue your document reviews.'}
          </p>

          <div
            className="auth-switch"
            role="group"
            aria-label="Account action"
          >
            <button
              type="button"
              className={registering ? '' : 'active'}
              aria-pressed={!registering}
              disabled={busy}
              onClick={() => changeMode('login')}
            >
              Sign in
            </button>

            <button
              type="button"
              className={registering ? 'active' : ''}
              aria-pressed={registering}
              disabled={busy}
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
                  onChange={(event) =>
                    setDisplayName(event.target.value)
                  }
                  autoComplete="name"
                  minLength={2}
                  maxLength={120}
                  disabled={busy}
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
                onChange={(event) =>
                  setEmail(event.target.value)
                }
                autoComplete="email"
                maxLength={320}
                disabled={busy}
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                name="password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                autoComplete={
                  registering
                    ? 'new-password'
                    : 'current-password'
                }
                disabled={busy}
                required
              />
            </label>

            {registering && (
              <p className="auth-password-hint">
                Use at least 12 characters and at most
                72 UTF-8 bytes.
              </p>
            )}

            {notice && (
              <p className="auth-password-hint" role="status">
                {notice}
              </p>
            )}

            {error && (
              <p className="auth-error" role="alert">
                {error}
              </p>
            )}

            {remaining > 0 && (
              <p className="auth-password-hint">
                Try again in{' '}
                <strong
                  role="timer"
                  aria-live="off"
                  aria-label={`${remaining} seconds remaining`}
                >
                  {formatWait(remaining)}
                </strong>
                . Switching modes does not reset this limit.
              </p>
            )}

            <FolioButton
              type="submit"
              loading={busy}
              disabled={remaining > 0}
              loadingLabel={
                registering
                  ? 'Creating account…'
                  : 'Signing in…'
              }
              icon="arrow"
            >
              {remaining > 0
                ? `Wait ${formatWait(remaining)}`
                : registering
                  ? 'Create account'
                  : 'Sign in'}
            </FolioButton>
          </form>

          {!registering && (
            <div className="auth-recovery-links">
              <a href="#forgot-password">
                Forgot your password?
              </a>
            </div>
          )}

          <p className="auth-footnote">
            Verify findings against the original documents.
            {' '}
            <a href="#privacy">Privacy & data</a>
            {' · '}
            <a href="#review-limitations">
              Review limitations
            </a>
          </p>
        </div>
      </section>
    </main>
  )
}

export default AuthPage