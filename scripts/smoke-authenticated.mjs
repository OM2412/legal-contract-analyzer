// End-to-end API check against an isolated test database, never production.
// Run only when FOLIO_E2E_ISOLATED_DB=true is explicitly set.
import { readFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'

if (process.env.FOLIO_E2E_ISOLATED_DB !== 'true') {
  console.error('Refusing to run without FOLIO_E2E_ISOLATED_DB=true')
  process.exit(2)
}

const origin = process.argv[2] || 'http://127.0.0.1:18080'
const target = new URL(origin)
if (!['127.0.0.1', 'localhost'].includes(target.hostname)) {
  console.error('Authenticated smoke test only permits loopback hosts')
  process.exit(2)
}

const cookies = new Map()
const email = `folio-e2e-${randomUUID()}@example.invalid`
const password = `Folio-test-${randomUUID()}!`

function cookieHeader() {
  return [...cookies].map(([name, value]) => `${name}=${value}`).join('; ')
}

async function request(path, { method = 'GET', body, headers = {}, secure = false } = {}) {
  if (secure) await request('/api/auth/csrf')
  const outgoing = new Headers(headers)
  if (cookies.size) outgoing.set('cookie', cookieHeader())
  if (secure) outgoing.set('x-xsrf-token', decodeURIComponent(cookies.get('XSRF-TOKEN') || ''))
  const response = await fetch(new URL(path, target), {
    method,
    body,
    headers: outgoing,
    redirect: 'manual',
    signal: AbortSignal.timeout(45000),
  })
  for (const setCookie of response.headers.getSetCookie()) {
    const pair = setCookie.split(';', 1)[0]
    const index = pair.indexOf('=')
    if (index > 0) cookies.set(pair.slice(0, index), pair.slice(index + 1))
  }
  return response
}

async function expect(response, status, label) {
  if (response.status !== status) {
    const details = (await response.text()).slice(0, 300)
    throw new Error(`${label}: expected HTTP ${status}, got ${response.status}: ${details}`)
  }
  console.log(`PASS ${label} (HTTP ${status})`)
  return response
}

async function documents() {
  const form = new FormData()
  form.append('agreement', new Blob([await readFile('testdata/agreement.txt')], { type: 'text/plain' }), 'agreement.txt')
  form.append('sow', new Blob([await readFile('testdata/sow.txt')], { type: 'text/plain' }), 'sow.txt')
  form.append('maxDays', '30')
  return form
}

try {
  await expect(await request('/api/auth/me'), 401, 'anonymous session rejected')

  await expect(await request('/api/auth/register', {
    method: 'POST', secure: true,
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ email, displayName: 'Folio CI Tester', password }),
  }), 201, 'register isolated test account')

  const login = new URLSearchParams({ email, password })
  await expect(await request('/api/auth/login', {
    method: 'POST', secure: true,
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: login,
  }), 200, 'login')

  const me = await expect(await request('/api/auth/me'), 200, 'authenticated session')
  if ((await me.json()).email !== email) throw new Error('Session user mismatch')

  const review = await expect(await request('/api/review', {
    method: 'POST', secure: true, body: await documents(),
  }), 200, 'analyze synthetic documents')
  const analysis = await review.json()
  if (!analysis.reviewStatus || !analysis.agreementVersion || !analysis.sowVersion) {
    throw new Error('Review result is missing required fields')
  }

  const aiForm = new FormData()
  aiForm.append('document', new Blob([await readFile('testdata/agreement.txt')], { type: 'text/plain' }), 'agreement.txt')
  const ai = await expect(await request('/api/ai/quotes', {
    method: 'POST', secure: true, body: aiForm,
  }), 200, 'AI discovery or verified fallback')
  const suggestions = await ai.json()
  if (suggestions.status !== 'SUGGESTIONS_ONLY' || !Array.isArray(suggestions.evidence)) {
    throw new Error('Invalid AI suggestions response')
  }

  const saved = await expect(await request('/api/reviews', {
    method: 'POST', secure: true, body: await documents(),
  }), 201, 'save review')
  const savedReview = await saved.json()
  if (!savedReview.id) throw new Error('Saved review missing id')

  const list = await expect(await request('/api/reviews'), 200, 'list own reviews')
  if (!(await list.json()).items.some(item => item.id === savedReview.id)) {
    throw new Error('Saved review not found in archive')
  }

  await expect(await request(`/api/reviews/${savedReview.id}`), 200, 'retrieve own review')
  await expect(await request(`/api/reviews/${savedReview.id}`, {
    method: 'DELETE', secure: true,
  }), 204, 'delete test review')
  await expect(await request(`/api/reviews/${savedReview.id}`), 404, 'deleted review inaccessible')

  await expect(await request('/api/auth/logout', { method: 'POST', secure: true }), 204, 'logout')
  await expect(await request('/api/auth/me'), 401, 'session invalidated')
  console.log('Authenticated end-to-end test passed against isolated database.')
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}
