import { useEffect, useRef, useState } from 'react'
import { FolioIcon } from './FolioUI'
import './LandingHeader.css'

function LandingHeader({ accountStatus }) {
  const [expanded, setExpanded] = useState(false)
  const [activeHash, setActiveHash] = useState(
    () => window.location.hash || '#top'
  )
  const menuButton = useRef(null)

  useEffect(() => {
    function handleHashChange() {
      setActiveHash(window.location.hash || '#top')
      setExpanded(false)
    }

    window.addEventListener('hashchange', handleHashChange)

    return () => {
      window.removeEventListener('hashchange', handleHashChange)
    }
  }, [])

  useEffect(() => {
    if (!expanded) return

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setExpanded(false)
        menuButton.current?.focus()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [expanded])

  const links = [
    ['#how-it-works', 'How it works'],
    ['#sample', 'Sample review'],
    ['#about', 'About'],
    ['#review-limitations', 'Review limits'],
    ...(accountStatus === 'signed-in'
      ? [['#my-reviews', 'My reviews']]
      : [['#sign-in', 'Sign in']]),
  ]

  return (
    <header className="landing-header">
      <nav
        className="page-container landing-header__bar"
        aria-label="Main navigation"
      >
        <a
          className="landing-header__brand"
          href="#top"
          aria-label="Folio home"
          onClick={() => setExpanded(false)}
        >
          <span className="landing-header__moon">
            <FolioIcon name="moon" size={25} />
          </span>
          folio.
        </a>

        <button
          ref={menuButton}
          className="landing-header__toggle"
          type="button"
          aria-expanded={expanded}
          aria-controls="landing-navigation"
          aria-label={expanded ? 'Close navigation' : 'Open navigation'}
          onClick={() => setExpanded((value) => !value)}
        >
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            aria-hidden="true"
          >
            {expanded ? (
              <path d="m6 6 12 12M18 6 6 18" />
            ) : (
              <path d="M4 7h16M4 12h11M4 17h16" />
            )}
          </svg>
        </button>

        <div
          id="landing-navigation"
          className="landing-header__menu"
          data-expanded={expanded ? 'true' : 'false'}
        >
          <div className="landing-header__links">
            {links.map(([href, label]) => (
              <a
                key={href}
                href={href}
                aria-current={
                  activeHash === href ? 'location' : undefined
                }
                onClick={() => setExpanded(false)}
              >
                {label}
              </a>
            ))}
          </div>

          <a
            className="landing-header__action"
            href="#workspace"
            onClick={() => setExpanded(false)}
          >
            Open workspace
            <FolioIcon name="arrow" size={19} />
          </a>
        </div>
      </nav>
    </header>
  )
}

export default LandingHeader