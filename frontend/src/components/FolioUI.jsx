import './FolioUI.css'

const paths = {
  arrow: ['M5 19 19 5', 'M5 5h14v14'],
  upload: [
    'M12 16V3m-5 5 5-5 5 5',
    'M4 15v5a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-5',
  ],
  document: [
    'M14 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8z',
    'M14 3v5h5M8 12h8m-8 4h5',
  ],
  shield: [
    'M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6z',
    'm8 12 3 3 5-6',
  ],
  save: [
    'M5 3h12l4 4v13a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z',
    'M7 3v6h9V3M7 21v-7h10v7',
  ],
  search: [
    'M16 16 21 21',
    'M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  ],
  close: ['m6 6 12 12M6 18 18 6'],
  check: ['m5 12 4 4L19 6'],
  moon: ['M20 15A9 9 0 0 1 9 4a9 9 0 1 0 11 11'],
}

export function FolioIcon({ name = 'arrow', size = 22, label }) {
  return (
    <svg
      className="fui-icon"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden={label ? undefined : true}
      role={label ? 'img' : undefined}
      aria-label={label}
      focusable="false"
    >
      {(paths[name] ?? paths.document).map((path, index) => (
        <path key={index} d={path} />
      ))}
    </svg>
  )
}

export function MoonLoader({
  label = 'Loading',
  decorative = false,
}) {
  return (
    <span
      className="fui-moon"
      role={decorative ? undefined : 'status'}
      aria-label={decorative ? undefined : label}
      aria-hidden={decorative ? true : undefined}
    >
      <svg viewBox="0 0 48 48" aria-hidden="true" focusable="false">
        <circle
          className="fui-moon__orbit"
          cx="24" cy="24" r="20"
        />
        <circle
          className="fui-moon__body"
          cx="24" cy="24" r="12"
        />
        <circle
          className="fui-moon__crater"
          cx="20" cy="21" r="3"
        />
        <circle
          className="fui-moon__crater"
          cx="28" cy="27" r="2"
        />
        <g className="fui-moon__satellite">
          <circle cx="24" cy="4" r="3.5" />
        </g>
      </svg>
    </span>
  )
}

export function FolioButton({
  children,
  loading = false,
  loadingLabel = 'Working...',
  icon = 'arrow',
  variant = 'primary',
  disabled = false,
  className = '',
  type = 'button',
  ...props
}) {
  const appearance = ['primary', 'secondary', 'ghost'].includes(variant)
    ? variant
    : 'primary'

  return (
    <>
      <button
        {...props}
        type={type}
        className={`fui-button fui-button--${appearance} ${className}`}
        disabled={disabled || loading}
        aria-busy={loading}
      >
        <span className="fui-button__label">
          <span aria-hidden={loading}>{children}</span>
          <span aria-hidden={!loading}>{loadingLabel}</span>
        </span>

        <span className="fui-button__symbol">
          {loading
            ? <MoonLoader decorative />
            : icon && <FolioIcon name={icon} />}
        </span>
      </button>

      <span className="fui-sr-only" role="status" aria-live="polite">
        {loading ? loadingLabel : ''}
      </span>
    </>
  )
}