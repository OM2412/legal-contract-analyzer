import LandingHeader from './components/LandingHeader'
import { FolioIcon } from './components/FolioUI'
import './ReviewLimitations.css'

const DATA_DETAILS = [
  {
    title: 'Account information',
    text: 'Accounts use an email address, display name, password hash and account identifiers. Password recovery also processes your email address and reset-related records.',
    icon: 'shield',
  },
  {
    title: 'Uploaded documents',
    text: 'Your Agreement and SOW are sent to the application server for review. PDF extraction and OCR may process document text and scanned page images.',
    icon: 'upload',
  },
  {
    title: 'Saved review content',
    text: 'Saving a review stores its filenames, document version hashes, policy setting, review status, timestamp and result data. Result data includes source quotes, which may contain confidential or personal information.',
    icon: 'save',
  },
  {
    title: 'Temporary processing files',
    text: 'PDF and OCR processing can create temporary files. The application includes cleanup routines for these files. Temporary-file cleanup is separate from storing a saved review.',
    icon: 'document',
  },
  {
    title: 'Session and security cookies',
    text: 'The application uses session and CSRF protections for account access and requests. Signing out ends the current authenticated session; it does not delete saved reviews.',
    icon: 'shield',
  },
  {
    title: 'Optional AI processing',
    text: 'Where enabled and requested, document text is sent to the configured AI service for suggestions. The destination depends on the deployment configuration. Check that configuration before using confidential material.',
    icon: 'moon',
  },
]

function PrivacyPage({ accountStatus }) {
  return (
    <div className="review-guide">
      <LandingHeader accountStatus={accountStatus} />

      <main
        id="app-main"
        className="page-container review-guide__main"
      >
        <div className="review-guide__intro">
          <span className="review-guide__eyebrow">
            PRIVACY & DATA / PREVIEW
          </span>

          <h1 id="app-route-heading" tabIndex={-1}>
            Know what
            <br />
            <em>you share.</em>
          </h1>

          <p>
            This notice describes the current application’s
            data handling. Hosting, email-provider, retention
            and contact details must be finalized before
            public production use.
          </p>
        </div>

        <aside className="review-guide__notice">
          <FolioIcon name="document" size={25} />

          <p>
            <strong>Use sample or redacted documents during preview.</strong>
            {' '}Only upload material you are authorized to share.
            Saved results can contain passages from your contracts.
          </p>
        </aside>

        <div className="review-guide__grid">
          {DATA_DETAILS.map((item, index) => (
            <article
              className="review-guide__card"
              key={item.title}
            >
              <div className="review-guide__card-top">
                <span>
                  {String(index + 1).padStart(2, '0')}
                </span>
                <FolioIcon name={item.icon} size={25} />
              </div>

              <h2>{item.title}</h2>
              <p>{item.text}</p>
            </article>
          ))}

          <article className="review-guide__card">
            <div className="review-guide__card-top">
              <span>07</span>
              <FolioIcon name="save" size={25} />
            </div>

            <h2>Retention and deletion</h2>

           <p>
  You can delete individual saved reviews from My reviews.
  Deleting a review removes its stored result, including source
  quotes and associated filenames, from the application database.
  The app does not provide an undo action. Original files on your
  device are unaffected. This action does not delete your account
  or separately retained operational records or backups.
</p>
          </article>

          <article className="review-guide__card">
            <div className="review-guide__card-top">
              <span>08</span>
              <FolioIcon name="shield" size={25} />
            </div>

            <h2>Access boundaries</h2>

            <p>
              Saved-review access is restricted to its owning
              account through the application. Hosting and database
              administration are separate operational access paths.
              Account access controls do not guarantee absolute
              confidentiality.
            </p>
          </article>
        </div>

        <section className="review-guide__next">
          <div>
            <h2>Understand the review boundaries.</h2>
            <p>
              Read how extraction, source evidence and optional
              AI suggestions should be used.
            </p>
          </div>

          <a href="#review-limitations">
            Review limitations
            <FolioIcon name="arrow" size={20} />
          </a>
        </section>
      </main>
    </div>
  )
}

export default PrivacyPage