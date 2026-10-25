import { useEffect, useRef, useState } from 'react'
import HomePage from './HomePage'
import ReviewWorkspace from './ReviewWorkspace'
import ReviewResult from './ReviewResult'
import AiStudio from './AiStudio'
import AuthPage from './AuthPage'
import SavedReviews from './SavedReviews'
import PasswordRecovery from './PasswordRecovery'
import ReviewLimitations from './ReviewLimitations'
import PrivacyPage from './PrivacyPage'
import { FolioIcon, MoonLoader } from './components/FolioUI'
import { apiFetch } from './apiClient'

import './App.css'
import './SavedReviews.css'
import './AppNavigation.css'

const AI_ENABLED = import.meta.env.VITE_AI_ENABLED !== 'false'

const PROTECTED = [
  'workspace',
  'review',
  'risks',
  'ai-studio',
  'archive',
]

const TITLES = {
  workspace: ['01 / DOCUMENT DOCK', 'Bring your documents.'],
  review: ['02 / REVIEW DESK', 'Follow the evidence.'],
  risks: ['03 / RISK STUDIO', 'Explore the signals.'],
  'ai-studio': ['OPTIONAL / AI STUDIO', 'Take a second look.'],
  archive: ['YOUR PRIVATE ARCHIVE', 'Pick up where you left off.'],
}

const emptyAi = () => ({
  agreementData: null,
  sowData: null,
  progress: '',
  error: '',
})

const emptyChecklist = () => ({
  verified: {},
  notes: '',
})

function viewFromHash() {
  const hash = window.location.hash

  if (hash.startsWith('#reset-password')) return 'reset'
  if (hash === '#forgot-password') return 'forgot'
  if (hash === '#sign-in') return 'auth'
  if (hash === '#workspace') return 'workspace'

  if (hash === '#review' || hash === '#review-result') {
    return 'review'
  }

  if (hash === '#risk-studio') return 'risks'
  if (hash === '#my-reviews') return 'archive'

  if (AI_ENABLED && hash === '#ai-studio') {
    return 'ai-studio'
  }

  if (hash === '#review-limitations') return 'limitations'
  if (hash === '#privacy') return 'privacy'

  return 'site'
}

function resetTokenFromHash() {
  return window.location.hash.match(
    /^#reset-password=([A-Za-z0-9_-]{43})$/
  )?.[1] ?? ''
}

function WorkspaceFrame({ view, children, onSignOut }) {
  const [eyebrow, title] = TITLES[view] ?? TITLES.workspace

  const Content = ['archive', 'ai-studio'].includes(view)
    ? 'div'
    : 'main'

  const links = [
    ['workspace', '#workspace', 'Workspace', 'upload'],
    ['review', '#review', 'Review', 'document'],
    ['risks', '#risk-studio', 'Risk Studio', 'shield'],
    ...(AI_ENABLED
      ? [['ai-studio', '#ai-studio', 'AI Studio', 'moon']]
      : []),
    ['archive', '#my-reviews', 'My reviews', 'save'],
  ]

  return (
    <>
      <a className="skip-link" href="#app-main">
        Skip to content
      </a>

      <header className="app-frame__header">
        <div className="page-container app-frame__bar">
          <a
            className="app-frame__brand"
            href="#top"
            aria-label="Folio home"
          >
            <FolioIcon name="moon" size={27} />
            folio.
          </a>

          <nav
            className="app-frame__nav"
            aria-label="Workspace navigation"
          >
            {links.map(([name, href, label, icon]) => (
              <a
                key={name}
                href={href}
                aria-current={view === name ? 'page' : undefined}
              >
                <FolioIcon name={icon} size={18} />
                {label}
              </a>
            ))}
          </nav>

          <a className="app-frame__home" href="#top">
            Home
            <FolioIcon name="arrow" size={17} />
          </a>
          <button type="button" className="app-frame__sign-out" onClick={onSignOut}>
            Sign out
          </button>
        </div>
      </header>

      <Content
        id="app-main"
        className="app-frame__main"
        tabIndex={-1}
      >
        <div className="page-container app-frame__intro">
          <span className="app-frame__eyebrow">
            {eyebrow}
          </span>

          <h1 id="app-route-heading" tabIndex={-1}>
            {title}
          </h1>

          <p>Evidence first. Your judgment stays in control.</p>
        </div>

        {children}
      </Content>
    </>
  )
}

function ReviewEmptyState({ ai = false }) {
  return (
    <section className="page-container app-frame__empty">
      <span
        className="app-frame__empty-icon"
        aria-hidden="true"
      >
        <FolioIcon name="document" size={38} />
      </span>

      <h2>
        {ai
          ? 'Bring a review into the studio.'
          : 'Your next review starts here.'}
      </h2>

      <p>
        {ai
          ? 'Run a review to make the uploaded originals available for AI suggestions.'
          : 'Upload an Agreement and SOW to see their comparison and source evidence.'}
      </p>

      <p>
        Unsaved reviews and original files are available only in
        this tab until you refresh or close it.
      </p>

      <div className="app-frame__empty-actions">
        <a className="button button-lime" href="#workspace">
          Open workspace
          <FolioIcon name="arrow" />
        </a>

        <a
          className="app-frame__text-link"
          href="#my-reviews"
        >
          Browse saved reviews
        </a>
      </div>
    </section>
  )
}

function App() {
  const [review, setReview] = useState(null)
  const [reviewedFiles, setReviewedFiles] = useState(null)
  const [aiSession, setAiSession] = useState(emptyAi)
  const [reviewChecklist, setReviewChecklist] =
    useState(emptyChecklist)

  const [view, setView] = useState(viewFromHash)
  const [accountStatus, setAccountStatus] = useState('checking')
  const [resetToken, setResetToken] = useState(resetTokenFromHash)
  const [recoveryEntry, setRecoveryEntry] = useState(0)
  const [signOutError, setSignOutError] = useState('')

  const authRevision = useRef(0)
  const signOutBusy = useRef(false)

  useEffect(() => {
    let active = true
    const revision = authRevision.current

    apiFetch('/api/auth/me')
      .then((response) => {
        if (active && revision === authRevision.current) {
          setAccountStatus(
            response.ok ? 'signed-in' : 'signed-out'
          )
        }
      })
      .catch(() => {
        if (active && revision === authRevision.current) {
          setAccountStatus('signed-out')
        }
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    function syncView() {
      const nextView = viewFromHash()

      if (
        nextView === 'reset' &&
        window.location.hash.startsWith('#reset-password=')
      ) {
        setResetToken(resetTokenFromHash())
        setRecoveryEntry((entry) => entry + 1)

        window.history.replaceState(
          window.history.state,
          '',
          window.location.pathname +
            window.location.search +
            '#reset-password'
        )
      } else if (nextView !== 'reset') {
        setResetToken('')
      }

      setView(nextView)
    }

    syncView()
    window.addEventListener('hashchange', syncView)

    return () => {
      window.removeEventListener('hashchange', syncView)
    }
  }, [])

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      if (view === 'site') {
        const target = window.location.hash.slice(1) || 'top'

        document.getElementById(target)?.scrollIntoView({
          behavior: 'instant',
          block: 'start',
        })
      } else {
        window.scrollTo({
          top: 0,
          behavior: 'instant',
        })

        document.getElementById('app-route-heading')?.focus({
          preventScroll: true,
        })
      }
    })

    return () => {
      window.cancelAnimationFrame(frame)
    }
  }, [view, accountStatus])

  function navigate(hash) {
    window.location.hash = hash
    setView(viewFromHash())
  }

  function handleAuthenticated() {
    authRevision.current += 1
    setReview(null)
    setReviewedFiles(null)
    setReviewChecklist(emptyChecklist())
    setAiSession(emptyAi())
    setAccountStatus('signed-in')

    const destination = viewFromHash()

    if (PROTECTED.includes(destination)) {
      setView(destination)
    } else {
      navigate('workspace')
    }
  }

  function handleReview(data, files) {
    if (signOutBusy.current) return
    setReview(data)
    setReviewedFiles(files)
    setReviewChecklist(emptyChecklist())
    setAiSession(emptyAi())
    navigate('review')
  }

  function backToReview() {
    navigate(review ? 'review' : 'workspace')
  }

  async function handleSignOut() {
    if (signOutBusy.current) return
    signOutBusy.current = true
    authRevision.current += 1
    // Unmount private screens immediately, including uploads and source previews.
    setReview(null)
    setReviewedFiles(null)
    setReviewChecklist(emptyChecklist())
    setAiSession(emptyAi())
    setResetToken('')
    setSignOutError('')
    setAccountStatus('signing-out')
    try {
      const response = await apiFetch('/api/auth/logout', { method: 'POST' })
      if (response.status !== 204 && response.status !== 401) {
        throw new Error('Sign-out was not confirmed by the server.')
      }
      setAccountStatus('signed-out')
      navigate('sign-in')
    } catch {
      setSignOutError('Private content has been cleared from this screen, but server sign-out could not be confirmed. Retry before leaving a shared device.')
    } finally {
      signOutBusy.current = false
    }
  }

  if (accountStatus === 'signing-out') {
    return (
      <main className="app-session-check">
        {signOutError ? (
          <>
            <p role="alert">{signOutError}</p>
            <button type="button" onClick={handleSignOut}>Retry sign out</button>
          </>
        ) : <MoonLoader label="Signing out" />}
      </main>
    )
  }

  if (view === 'forgot' || view === 'reset') {
    return (
      <PasswordRecovery
        key={`${view}-${recoveryEntry}`}
        mode={view === 'reset' ? 'reset' : 'forgot'}
        resetToken={resetToken}
        onCompleted={() => {
          authRevision.current += 1
          setAccountStatus('signed-out')
          setResetToken('')
          setReview(null)
          setReviewedFiles(null)
          setReviewChecklist(emptyChecklist())
          setAiSession(emptyAi())
        }}
      />
    )
  }

  if (view === 'limitations') {
    return (
      <ReviewLimitations accountStatus={accountStatus} />
    )
  }

  if (view === 'privacy') {
    return (
      <PrivacyPage accountStatus={accountStatus} />
    )
  }

  if (accountStatus === 'checking' && view !== 'site') {
    return (
      <main className="app-session-check">
        <MoonLoader label="Checking session" />
        <p>Checking your session...</p>
      </main>
    )
  }

  if (
    view === 'auth' ||
    (
      PROTECTED.includes(view) &&
      accountStatus !== 'signed-in'
    )
  ) {
    return (
      <AuthPage onAuthenticated={handleAuthenticated} />
    )
  }

  return (
    <>
      {view === 'site' && (
        <>
          {accountStatus === 'signed-in' && (
            <div className="app-frame__session-bar">
              <button type="button" className="app-frame__sign-out" onClick={handleSignOut}>Sign out</button>
            </div>
          )}
          <HomePage accountStatus={accountStatus} />
        </>
      )}

      {accountStatus === 'signed-in' && (
        <div
          className="app-frame"
          hidden={view === 'site'}
        >
          <WorkspaceFrame view={view} onSignOut={handleSignOut}>
            <div
              className="app-route"
              hidden={view !== 'workspace'}
            >
              <ReviewWorkspace onReview={handleReview} />
            </div>

            {(view === 'review' || view === 'risks') && (
              review ? (
                <ReviewResult
                  key={view}
                  panel={view === 'risks' ? 'risks' : 'review'}
                  data={review}
                  files={reviewedFiles}
                  checklist={reviewChecklist}
                  onChecklistChange={setReviewChecklist}
                />
              ) : (
                <ReviewEmptyState />
              )
            )}

            {view === 'archive' && (
              <SavedReviews onBack={backToReview} />
            )}

            {view === 'ai-studio' && (
              review && reviewedFiles ? (
                <AiStudio
                  files={reviewedFiles}
                  session={aiSession}
                  onSessionChange={setAiSession}
                  onBack={backToReview}
                />
              ) : (
                <ReviewEmptyState ai />
              )
            )}
          </WorkspaceFrame>
        </div>
      )}
    </>
  )
}

export default App
