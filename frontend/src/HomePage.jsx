import { useState } from 'react'
import MoonScene from './components/MoonScene'
import LandingHeader from './components/LandingHeader'
import LandingFooter from './components/LandingFooter'
import { FolioIcon } from './components/FolioUI'

const sample = {
  agreement: {
    label: 'Agreement',
    quote:
      'Client shall pay Provider the project fee within 30 calendar days after receipt of the invoice.',
    location: 'Illustrative source excerpt',
    assessment: 'Within demo policy',
  },
  sow: {
    label: 'Statement of Work',
    quote:
      'Client shall pay Provider the project fee within 60 calendar days after receipt of the invoice.',
    location: 'Illustrative source excerpt',
    assessment: 'Outside demo policy',
  },
}

function SampleReview() {
  const [activeDocument, setActiveDocument] =
    useState('agreement')

  const selected = sample[activeDocument]

  return (
    <div className="sample-board">
      <div className="sample-board-top">
        <div>
          <span className="eyebrow">
            ILLUSTRATIVE EXAMPLE
          </span>

          <h3>One payment. Two timelines.</h3>
        </div>

        <span className="sample-status">
          Potential difference
        </span>
      </div>

      <div className="sample-comparison">
        <div className="sample-term">
          <span>AGREEMENT</span>
          <strong>30</strong>
          <small>calendar days</small>
        </div>

        <div className="sample-versus" aria-hidden="true">
          ↔
        </div>

        <div className="sample-term sample-term-sow">
          <span>STATEMENT OF WORK</span>
          <strong>60</strong>
          <small>calendar days</small>
        </div>
      </div>

      <div className="sample-evidence">
        <div className="sample-evidence-heading">
          <div>
            <span className="eyebrow">
              FOLLOW THE EVIDENCE
            </span>

            <h4>Read the source, not just the result.</h4>
          </div>

          <div
            className="sample-switch"
            role="group"
            aria-label="Choose sample document"
          >
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
          <span>
            {selected.label} · {selected.location}
          </span>

          <span>{selected.assessment}</span>
        </div>
      </div>

      <p className="sample-disclaimer">
        This is synthetic sample data. P-DEMO-30 is an
        illustrative business preference; review the complete
        original documents before a decision.
      </p>
    </div>
  )
}

function HomePage({ accountStatus }) {
  return (
    <div className="site-shell">
      <a className="skip-link" href="#main">
        Skip to content
      </a>

      <LandingHeader accountStatus={accountStatus} />

      <main id="main">
        <section className="hero-section" id="top">
          <div className="hero-inner page-container">
            <div className="hero-copy">
              <span className="hero-kicker">
                <span
                  className="live-indicator"
                  aria-hidden="true"
                />
                CONTRACT INTELLIGENCE, WITH RECEIPTS
              </span>

              <h1>
                Find the difference.
                <em> Follow the evidence.</em>
              </h1>

              <p className="hero-description">
                Compare payment terms across an Agreement and
                Statement of Work. See what differs, why it
                matters for your review, and the exact source
                text behind each finding.
              </p>

              <div className="hero-actions">
                <a
                  className="button button-lime"
                  href="#workspace"
                >
                  Review documents
                  <FolioIcon name="arrow" size={20} />
                </a>

                <a
                  className="button button-outline"
                  href="#how-it-works"
                >
                  See how it works
                </a>
              </div>

              <div className="hero-footnote">
                PDF + TXT
                {' '}
                <span aria-hidden="true">/</span>
                {' '}
                OCR for scanned pages
                {' '}
                <span aria-hidden="true">/</span>
                {' '}
                Human review required
              </div>
            </div>

            <MoonScene />
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
          aria-labelledby="process-heading"
        >
          <div className="page-container">
            <div className="section-heading">
              <span className="eyebrow">
                THE PROCESS / 001–003
              </span>

              <h2 id="process-heading">
                Less searching.
                <br />
                More understanding.
              </h2>

              <p>
                Each finding leads back to a quote. Unclear
                or unsupported wording stays visible for a
                human reviewer.
              </p>
            </div>

            <div className="process-grid">
              <article className="process-card">
                <span className="process-number">01</span>

                <div
                  className="process-art process-art-upload"
                  aria-hidden="true"
                >
                  <span className="mini-document">
                    AGREEMENT
                  </span>
                  <span className="mini-document">SOW</span>
                </div>

                <h3>Bring both documents.</h3>

                <p>
                  Add an Agreement and SOW as PDFs or text
                  files. Scanned PDF pages can use OCR on the
                  application server.
                </p>

                <span className="process-caption">
                  DOCUMENT INTAKE
                </span>
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

                <h3>See what differs.</h3>

                <p>
                  Compare supported payment periods, day units,
                  triggers and payment-priority wording.
                </p>

                <span className="process-caption">
                  STRUCTURED COMPARISON
                </span>
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
                  Inspect exact extracted quotes and available
                  page references. Verify OCR text against the
                  original scanned page.
                </p>

                <span className="process-caption">
                  HUMAN VERIFICATION
                </span>
              </article>
            </div>
          </div>
        </section>

        <section
          className="sample-section section-spacing"
          id="sample"
          aria-labelledby="sample-heading"
        >
          <div className="page-container">
            <div className="section-heading sample-section-heading">
              <span className="eyebrow">
                A CLOSER LOOK / SAMPLE REVIEW
              </span>

              <h2 id="sample-heading">
                The details make
                <br />
                <em>the difference.</em>
              </h2>

              <p>
                Explore a synthetic example of the Agreement
                and SOW review. Switch documents to inspect
                each exact quote.
              </p>
            </div>

            <SampleReview />
          </div>
        </section>

        <section
          className="about-section section-spacing"
          id="about"
          aria-labelledby="about-heading"
        >
          <div className="page-container about-layout">
            <div>
              <span className="eyebrow">
                BUILT WITH INTENTION
              </span>

              <h2 id="about-heading">
                Contracts deserve
                <br />
                a closer read.
              </h2>
            </div>

            <div className="about-copy">
              <p>
                Folio is an evidence-focused student project
                built at MITS Gwalior by Om Singh Jadon and
                Karan Pratap Singh, under the guidance of
                Mr. Anil Kumar Fatehpuria.
              </p>

              <p>
                The current working feature reviews supported
                payment wording across an Agreement and
                Statement of Work. It surfaces differences
                and passages needing attention. A reviewer
                checks the original documents and makes the
                final decision.
              </p>

              <a
                href="https://github.com/OM2412/legal-contract-analyzer"
                target="_blank"
                rel="noopener noreferrer"
              >
                Explore the project on GitHub
                {' '}
                <FolioIcon name="arrow" size={18} />
              </a>
            </div>
          </div>
        </section>
      </main>

      <LandingFooter />
    </div>
  )
}

export default HomePage