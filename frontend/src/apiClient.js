const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

function readCookie(name) {
  const prefix = `${name}=`

  const entry = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))

  return entry ? decodeURIComponent(entry.slice(prefix.length)) : null
}

export async function apiFetch(url, options = {}) {
  if (typeof url !== 'string' || !url.startsWith('/api/')) {
    throw new Error('apiFetch only accepts local /api/ paths.')
  }

  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (!SAFE_METHODS.has(method)) {
    const csrfResponse = await fetch('/api/auth/csrf', {
      credentials: 'same-origin',
      cache: 'no-store',
      signal: options.signal,
    })

    if (!csrfResponse.ok) {
      throw new Error('Could not prepare the secure request.')
    }

    const token = readCookie('XSRF-TOKEN')

    if (!token) {
      throw new Error('CSRF token cookie is unavailable.')
    }

    headers.set('X-XSRF-TOKEN', token)
  }

  return fetch(url, {
    ...options,
    method,
    headers,
    credentials: 'same-origin',
  })
}