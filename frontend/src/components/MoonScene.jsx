import { useState } from 'react'
import { FolioIcon } from './FolioUI'
import './MoonScene.css'

function PaperCard({ type, days, className }) {
  return (
    <div className={`moon-scene__paper ${className}`}>
      <div className="moon-scene__paper-top">
        <FolioIcon name="document" size={20} />
        <span>{type}</span>
        <span>TXT</span>
      </div>

      <div className="moon-scene__paper-lines">
        <i />
        <i />
        <i />
      </div>

      <div className="moon-scene__term">
        <span>PAYMENT TERM</span>
        <strong>{days}</strong>
        <small>calendar days</small>
      </div>

      <div className="moon-scene__paper-lines">
        <i />
        <i />
      </div>

      <span className="moon-scene__source">
        Exact wording. Clear evidence.
      </span>
    </div>
  )
}

function MoonScene() {
  const [paused, setPaused] = useState(false)

  return (
    <div
      className="moon-scene"
      data-paused={paused ? 'true' : 'false'}
    >
      <div className="moon-scene__stage" aria-hidden="true">
        <div className="moon-scene__stars" />
        <div className="moon-scene__halo" />

        <div className="moon-scene__orbit moon-scene__orbit--one" />
        <div className="moon-scene__orbit moon-scene__orbit--two" />

        <div className="moon-scene__moon">
          <div className="moon-scene__surface" />
          <span className="moon-scene__crater moon-scene__crater--one" />
          <span className="moon-scene__crater moon-scene__crater--two" />
          <span className="moon-scene__crater moon-scene__crater--three" />
        </div>

        <span className="moon-scene__spark moon-scene__spark--one">✦</span>
        <span className="moon-scene__spark moon-scene__spark--two">✧</span>

        <div className="moon-scene__paper-flight moon-scene__paper-flight--agreement">
          <PaperCard
            type="AGREEMENT"
            days="30"
            className="moon-scene__paper--agreement"
          />
        </div>

        <div className="moon-scene__paper-flight moon-scene__paper-flight--sow">
          <PaperCard
            type="SOW"
            days="60"
            className="moon-scene__paper--sow"
          />
        </div>

        <div className="moon-scene__verdict">
          <FolioIcon name="search" size={18} />
          <span>Same payment. Different timelines.</span>
        </div>

        <span className="moon-scene__sticker">
          LESS GUESSING.
          <br />
          MORE RECEIPTS.
        </span>
      </div>

      <div className="moon-scene__footer">
        <span>ILLUSTRATIVE EXAMPLE · NOT YOUR DOCUMENTS</span>

        <button
          type="button"
          className="moon-scene__motion"
          aria-pressed={paused}
          onClick={() => setPaused((value) => !value)}
        >
          {paused ? 'Resume motion' : 'Pause motion'}
        </button>
      </div>
    </div>
  )
}

export default MoonScene