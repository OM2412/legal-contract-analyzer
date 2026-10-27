import LandingHeader from './components/LandingHeader'
import { FolioIcon } from './components/FolioUI'
import './ReviewLimitations.css'

const LIMITATIONS = [
  {
    number: '01',
    title: 'Supported wording has boundaries.',
    text: 'Payment extraction supports selected wording patterns. An unrecognized clause may still contain an important obligation. Read the complete documents.',
    icon: 'document',
  },
  {
    number: '02',
    title: 'A business preference is not a legal rule.',
    text: 'The configurable payment policy is illustrative. A deviation means a supported term differs from that preference; it does not establish illegality or unenforceability.',
    icon: 'shield',
  },
  {
    number: '03',
    title: 'Different timelines need context.',
    text: 'Business days, calendar days, invoice dates, invoice receipt and acceptance triggers are different. Folio leaves unsupported comparisons unassessed.',
    icon: 'search',
  },
  {
    number: '04',
    title: 'Multiple clauses need your judgment.',
    text: 'Installments, different payment scopes and competing clauses may require human review. Folio does not silently choose one clause from multiple recognized obligations.',
    icon: 'document',
  },
  {
    number: '05',
    title: 'Check the original evidence.',
    text: 'Quotes and offsets help locate source wording. OCR can misread scanned pages, and a displayed excerpt may omit surrounding conditions or exceptions.',
    icon: 'search',
  },
  {
    number: '06',
    title: 'AI suggestions remain suggestions.',
    text: 'Where enabled, optional AI suggestions are checked against extracted text. A matching quote does not prove a correct interpretation. AI suggestions do not change the deterministic payment review verdict.',
    icon: 'moon',
  },
]

function ReviewLimitations({ accountStatus }) {
  return (
    <div className="review-guide">
      <LandingHeader accountStatus={accountStatus} />

      <main className="page-container review-guide__main">
        <div className="review-guide__intro">
          <span className="review-guide__eyebrow">
            KNOW THE BOUNDARIES
          </span>

          <h1>
            Clear evidence.
            <br />
            <em>Human judgment.</em>
          </h1>

          <p>
            Folio is a document review aid for Agreements and
            Statements of Work. It supports review of selected
            payment wording and surfaces narrow clause cues.
            It does not provide legal advice.
          </p>
        </div>

        <aside className="review-guide__notice">
          <FolioIcon name="shield" size={25} />
          <p>
            <strong>No finding does not mean no risk.</strong>
            {' '}Check complete originals and seek a qualified
            professional when a decision needs legal interpretation.
          </p>
        </aside>

        <div className="review-guide__grid">
          {LIMITATIONS.map((item) => (
            <article
              className="review-guide__card"
              key={item.number}
            >
              <div className="review-guide__card-top">
                <span>{item.number}</span>
                <FolioIcon name={item.icon} size={25} />
              </div>

              <h2>{item.title}</h2>
              <p>{item.text}</p>
            </article>
          ))}
        </div>

        <section className="review-guide__next">
          <div>
            <h2>Review with the full picture.</h2>
            <p>
              Inspect the source, check exceptions and record
              your own review notes before relying on a result.
            </p>
          </div>

          <a href="#workspace">
            Open workspace
            <FolioIcon name="arrow" size={20} />
          </a>
        </section>
      </main>
    </div>
  )
}

export default ReviewLimitations