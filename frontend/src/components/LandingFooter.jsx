import { FolioIcon } from './FolioUI'
import './LandingFooter.css'

function LandingFooter() {
  return (
    <footer className="landing-footer">
      <div className="page-container">
        <div className="landing-footer__banner">
          <div>
            <span className="landing-footer__eyebrow">
              YOUR NEXT REVIEW STARTS HERE
            </span>

            <h2>
              Less guesswork.
              <br />
              <em>More evidence.</em>
            </h2>

            <p>
              Bring an Agreement and SOW.
              Keep the original wording in view.
            </p>
          </div>

          <a
            className="landing-footer__start"
            href="#workspace"
          >
            Open workspace
            <FolioIcon name="arrow" size={22} />
          </a>
        </div>

        <div className="landing-footer__grid">
          <div className="landing-footer__identity">
            <a
              className="landing-footer__brand"
              href="#top"
              aria-label="Folio home"
            >
              <span>
                <FolioIcon name="moon" size={30} />
              </span>
              folio.
            </a>

            <p>
              Evidence-led document review.
              Built to support human judgment.
            </p>

            <span className="landing-footer__badge">
              STUDENT PROJECT / PREVIEW
            </span>
          </div>

          <nav aria-label="Explore Folio">
            <h3>Explore</h3>
            <a href="#workspace">Document workspace</a>
            <a href="#sample">Sample review</a>
            <a href="#how-it-works">How it works</a>
            <a href="#about">About Folio</a>
          </nav>

          <nav aria-label="Privacy and review information">
            <h3>Know the boundaries</h3>
            <a href="#privacy">Privacy & data</a>
            <a href="#review-limitations">Review limitations</a>

            <p>
              Findings are prompts for review.
              They are not legal advice.
            </p>
          </nav>

          <div className="landing-footer__team">
            <h3>People behind Folio</h3>
            <p>Om Singh Jadon</p>
            <p>Karan Pratap Singh</p>

            <span>PROJECT MENTOR</span>
            <p>Mr. Anil Kumar Fatehpuria</p>
          </div>
        </div>

        <div className="landing-footer__bottom">
          <span>
            MITS Gwalior · CSBS · Academic project
          </span>

          <span>Evidence first. Human judgment always.</span>

          <a href="#top">
            Back to top
            <span aria-hidden="true">↑</span>
          </a>
        </div>
      </div>
    </footer>
  )
}

export default LandingFooter