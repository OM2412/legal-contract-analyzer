import { useEffect, useState } from 'react'
import ReviewWorkspace from './ReviewWorkspace'
import ReviewResult from './ReviewResult'
import AiStudio from './AiStudio'
import './App.css'
const AI_ENABLED = import.meta.env.VITE_AI_ENABLED !== 'false'

const sample = {
  agreement: {
    label: 'Agreement',
    days: '30 calendar days',
    quote:
      'Client shall pay Provider the project fee within 30 calendar days after receipt of the invoice.',
    location: 'Page 1 · PDF text',
    assessment: 'Within demo policy',
  },
  sow: {
    label: 'Statement of Work',
    days: '60 calendar days',
    quote:
      'Client shall pay Provider the project fee within 60 calendar days after receipt of the invoice.',
    location: 'Page 1 · PDF text',
    assessment: 'Outside demo policy',
  },
}

function DocumentScene() {
  return (
    <div className="document-scene" aria-hidden="true">
      <div className="scene-grid" />

      <div className="scene-sheet scene-sheet-agreement">
        <div className="scene-sheet-top">
          <span>01 / AGREEMENT</span>
          <span>PDF</span>
        </div>
        <div className="scene-sheet-title" />
        <div className="scene-sheet-line scene-sheet-line-long" />
        <div className="scene-sheet-line" />
        <div className="scene-sheet-line scene-sheet-line-short" />
        <div className="scene-highlight">
          <span>PAYMENT TERM</span>
          <strong>30 days</strong>
        </div>
        <div className="scene-sheet-line scene-sheet-line-long" />
        <div className="scene-sheet-line scene-sheet-line-short" />
      </div>

      <div className="scene-connector">
        <span>COMPARE</span>
      </div>

      <div className="scene-sheet scene-sheet-sow">
        <div className="scene-sheet-top">
          <span>02 / SOW</span>
          <span>PDF</span>
        </div>
        <div className="scene-sheet-title" />
        <div className="scene-sheet-line scene-sheet-line-long" />
        <div className="scene-sheet-line" />
        <div className="scene-sheet-line scene-sheet-line-short" />
        <div className="scene-highlight">
          <span>PAYMENT TERM</span>
          <strong>60 days</strong>
        </div>
        <div className="scene-sheet-line scene-sheet-line-long" />
        <div className="scene-sheet-line" />
        <div className="scene-sheet-line scene-sheet-line-short" />
      </div>

      <div className="scene-result">
        <span className="scene-result-dot" />
        Difference found
      </div>
    </div>
  )
}

function SampleReview() {
  const [activeDocument, setActiveDocument] = useState('agreement')
  const selected = sample[activeDocument]

  return (
    <div className="sample-board">
      <div className="sample-board-top">
        <div>
          <span className="eyebrow">ILLUSTRATIVE EXAMPLE</span>
          <h3>One payment. Two timelines.</h3>
        </div>
        <span className="sample-status">Potential difference</span>
      </div>

      <div className="sample-comparison">
        <div className="sample-term">
          <span>AGREEMENT</span>
          <strong>30</strong>
          <small>calendar days</small>
        </div>
        <div className="sample-versus" aria-hidden="true">↔</div>
        <div className="sample-term sample-term-sow">
          <span>STATEMENT OF WORK</span>
          <strong>60</strong>
          <small>calendar days</small>
        </div>
      </div>

      <div className="sample-evidence">
        <div className="sample-evidence-heading">
          <div>
            <span className="eyebrow">FOLLOW THE EVIDENCE</span>
            <h4>Read the source, not just the result.</h4>
          </div>

          <div className="sample-switch" aria-label="Choose sample document">
            <button
              type="button"
              aria-pressed={activeDocument === 'agreement'}
              onClick={() => setActiveDocument('agreement')}
            >
              Agreement
            </button>
            <button
              type="button"
              aria-pressed={activeDocument === 'sow'}
              onClick={() => setActiveDocument('sow')}
            >
              SOW
            </button>
          </div>
        </div>

        <blockquote key={activeDocument}>
          “{selected.quote}”
        </blockquote>

        <div className="sample-evidence-footer">
          <span>{selected.label} · {selected.location}</span>
          <span>{selected.assessment}</span>
        </div>
      </div>

      <p className="sample-disclaimer">
        This is synthetic sample data. P-DEMO-30 is an illustrative business
        preference; review the complete original documents before a decision.
      </p>
    </div>
  )
}

function App() {
  const [review, setReview] = useState(null)
  const [reviewedFiles, setReviewedFiles] = useState(null)
  const [aiSession, setAiSession] = useState({
    agreementData: null,
    sowData: null,
    progress: '',
    error: '',
  })
  const [reviewChecklist, setReviewChecklist] = useState({
    verified: {},
    notes: '',
  })
 const [view, setView] = useState(() =>
  AI_ENABLED && window.location.hash === '#ai-studio'
    ? 'ai-studio'
    : 'site'
)

  useEffect(() => {
    function syncView() {
     setView(
  AI_ENABLED && window.location.hash === '#ai-studio'
    ? 'ai-studio'
    : 'site'
)
    }

    window.addEventListener('hashchange', syncView)
    return () => window.removeEventListener('hashchange', syncView)
  }, [])

  function handleReview(data, files) {
    setReview(data)
    setReviewedFiles(files)
    setReviewChecklist({
      verified: {},
      notes: '',
    })
    setAiSession({
      agreementData: null,
      sowData: null,
      progress: '',
      error: '',
    })

    window.requestAnimationFrame(() => {
      const section = document.getElementById('review-result')
      section?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
      section?.focus({ preventScroll: true })
    })
  }

  function openAiStudio() {
    window.location.hash = 'ai-studio'
    setView('ai-studio')
    window.scrollTo(0, 0)
  }

  function backToReview() {
    const target = review ? 'review-result' : 'workspace'

    window.location.hash = target
    setView('site')

    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => {
        document.getElementById(target)?.scrollIntoView({
          block: 'start',
        })
      })
    })
  }

  if (view === 'ai-studio') {
    return (
      <>
        <a className="skip-link" href="#main">
          Skip to content
        </a>
        <AiStudio
          files={reviewedFiles}
          session={aiSession}
          onSessionChange={setAiSession}
          onBack={backToReview}
        />
      </>
    )
  }

  return (
    <div className="site-shell">
      <a className="skip-link" href="#main">
        Skip to content
      </a>

      <header className="site-header">
        <nav
          className="site-nav page-container"
          aria-label="Main navigation"
        >
          <a className="brand" href="#top" aria-label="Folio home">
            <span className="brand-mark" aria-hidden="true">
              <span />
              <span />
            </span>
            <span>
              folio<span className="brand-period">.</span>
            </span>
          </a>

          <div className="nav-links">
            <a href="#how-it-works">How it works</a>
            <a href="#sample">Sample review</a>
            <a href="#about">About</a>
          </div>

          <a className="nav-action" href="#workspace">
            Open workspace <span aria-hidden="true">↗</span>
          </a>
        </nav>
      </header>

      <main id="main">
        <section className="hero-section" id="top">
          <div className="hero-inner page-container">
            <div className="hero-copy">
              <span className="hero-kicker">
                <span className="live-indicator" />
                CONTRACT INTELLIGENCE, WITH RECEIPTS
              </span>

              <h1>
                Find the difference.
                <em> Follow the evidence.</em>
              </h1>

              <p className="hero-description">
                Compare payment terms across an Agreement and Statement of Work.
                See what differs, why it matters for your review, and the exact
                source text behind each finding.
              </p>

              <div className="hero-actions">
                <a className="button button-lime" href="#workspace">
                  Review documents
                  <span aria-hidden="true">↗</span>
                </a>
                <a className="button button-outline" href="#how-it-works">
                  See how it works
                </a>
              </div>

              <div className="hero-footnote">
                PDF + TXT <span aria-hidden="true">/</span>
                Local OCR support <span aria-hidden="true">/</span>
                Human review required
              </div>
            </div>

            <DocumentScene />
          </div>

          <div className="hero-bottom page-container">
            <span>AGREEMENT ↔ SOW</span>
            <span>SCROLL TO EXPLORE ↓</span>
          </div>
        </section>

        <section
          className="intro-strip"
          aria-label="The review workflow"
        >
          <div className="page-container intro-strip-inner">
            <span>01 / UPLOAD</span>
            <span>02 / COMPARE</span>
            <span>03 / VERIFY</span>
          </div>
        </section>

        <section
          className="process-section section-spacing"
          id="how-it-works"
        >
          <div className="page-container">
            <div className="section-heading">
              <span className="eyebrow">THE PROCESS / 001–003</span>
              <h2>Less searching.<br />More understanding.</h2>
              <p>
                Each finding leads back to a quote. Unclear or unsupported
                wording stays visible for a human reviewer.
              </p>
            </div>

            <div className="process-grid">
              <article className="process-card">
                <span className="process-number">01</span>
                <div
                  className="process-art process-art-upload"
                  aria-hidden="true"
                >
                  <span className="mini-document">AGREEMENT</span>
                  <span className="mini-document">SOW</span>
                </div>
                <h3>Bring both documents.</h3>
                <p>
                  Add an Agreement and SOW as PDFs or text files. Scanned PDF
                  pages can use local OCR.
                </p>
                <span className="process-caption">DOCUMENT INTAKE</span>
              </article>

              <article className="process-card">
                <span className="process-number">02</span>
                <div
                  className="process-art process-art-compare"
                  aria-hidden="true"
                >
                  <span>30 DAYS</span>
                  <span>60 DAYS</span>
                </div>
                <h3>See what changed.</h3>
                <p>
                  Compare supported payment periods, day units, triggers and
                  payment-priority wording.
                </p>
                <span className="process-caption">STRUCTURED COMPARISON</span>
              </article>

              <article className="process-card">
                <span className="process-number">03</span>
                <div
                  className="process-art process-art-evidence"
                  aria-hidden="true"
                >
                  <span>“...within 60 calendar days...”</span>
                  <small>PAGE 01 · SOURCE QUOTE</small>
                </div>
                <h3>Follow the evidence.</h3>
                <p>
                  Inspect exact extracted quotes and page references. Verify
                  OCR text against the original scanned page.
                </p>
                <span className="process-caption">HUMAN VERIFICATION</span>
              </article>
            </div>
          </div>
        </section>

        <section className="sample-section section-spacing" id="sample">
          <div className="page-container">
            <div className="section-heading sample-section-heading">
              <span className="eyebrow">
                A CLOSER LOOK / SAMPLE REVIEW
              </span>
              <h2>
                The details make<br />
                <em>the difference.</em>
              </h2>
              <p>
                Explore a synthetic example of the Agreement and SOW review.
                Switch documents to inspect each exact quote.
              </p>
            </div>

            <SampleReview />
          </div>
        </section>

        <ReviewWorkspace onReview={handleReview} />
        <ReviewResult
          data={review}
          files={reviewedFiles}
          checklist={reviewChecklist}
          onChecklistChange={setReviewChecklist}
        />

       {AI_ENABLED && review && reviewedFiles && (
          <section className="ai-studio-entry section-spacing">
            <div className="page-container ai-studio-entry-inner">
              <div>
                <span className="eyebrow">OPTIONAL / LOCAL AI</span>
                <h2>Take a second look at the wording.</h2>
                <p>
                  Open a separate space for source-verified AI quote
                  suggestions. Your rule-based review stays available here.
                </p>
              </div>

              <button
                type="button"
                className="ai-studio-entry-button"
                onClick={openAiStudio}
              >
                Enter AI Studio
              </button>
            </div>
          </section>
        )}

        <section className="about-section section-spacing" id="about">
          <div className="page-container about-layout">
            <div>
              <span className="eyebrow">BUILT WITH INTENTION</span>
              <h2>Contracts deserve<br />a closer read.</h2>
            </div>

            <div className="about-copy">
              <p>
                Folio is an evidence-focused student project built at MITS
                Gwalior by Om Singh Jadon and Karan Pratap Singh. The current
                working feature reviews supported payment wording across an
                Agreement and Statement of Work.
              </p>
              <p>
                It helps surface differences and passages needing attention.
                A reviewer still checks the original documents and makes the
                final decision.
              </p>
              <a
                href="https://github.com/OM2412/legal-contract-analyzer"
                target="_blank"
                rel="noopener noreferrer"
              >
                Explore the project on GitHub
                <span aria-hidden="true"> ↗</span>
              </a>
            </div>
          </div>
        </section>
      </main>

      <footer className="site-footer">
        <div className="page-container footer-inner">
          <span className="footer-brand">folio.</span>
          <span>Evidence first. Human judgment always.</span>
          <a href="#top">Back to top ↑</a>
        </div>
      </footer>
    </div>
  )
}

export default App