// End-to-end API check against an isolated test database, never production.
// Run only when FOLIO_E2E_ISOLATED_DB=true is explicitly set.
import { readFile, mkdtemp, writeFile, rm } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

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
  const health = await expect(await request('/api/health'), 200, 'anonymous readiness endpoint')
  const readiness = await health.json()
  if (readiness.status !== 'UP' || readiness.database !== 'UP') {
    throw new Error('Isolated database readiness check failed')
  }
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

  // Negative-path security checks: requests still carry the authenticated cookie.
  // An unsafe request without its anti-CSRF header must never reach the controller.
  await expect(await request('/api/review', {
    method: 'POST', body: await documents(),
  }), 403, 'authenticated mutation without CSRF rejected')

  const unsupported = await documents()
  unsupported.set('agreement', new Blob(['not a supported document']), 'payload.exe')
  await expect(await request('/api/review', {
    method: 'POST', secure: true, body: unsupported,
  }), 400, 'unsupported upload rejected')

  const invalidPolicy = await documents()
  invalidPolicy.set('maxDays', '0')
  await expect(await request('/api/review', {
    method: 'POST', secure: true, body: invalidPolicy,
  }), 400, 'out-of-range policy rejected')

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

  // Labeled synthetic *candidate retrieval* baseline, not real-world AI accuracy.
  const cases = [
    { file: 'agreement.txt', days: 30, unit: 'CALENDAR_DAYS' },
    { file: 'variant-agreement.txt', days: 45, unit: 'CALENDAR_DAYS' },
    { file: 'business-days-agreement.txt', days: 30, unit: 'BUSINESS_DAYS' },
  ]
  let candidateHits = 0
  let exactQuotes = 0
  let quoteCount = 0
  for (const sample of cases) {
    const source = (await readFile(`testdata/${sample.file}`)).toString('utf8')
    const form = new FormData()
    form.append('document', new Blob([source], { type: 'text/plain' }), sample.file)
    const response = await expect(await request('/api/ai/quotes', {
      method: 'POST', secure: true, body: form,
    }), 200, `synthetic AI fixture: ${sample.file}`)
    const data = await response.json()
    for (const item of data.evidence || []) {
      quoteCount++
      if (source.includes(item.quote)) exactQuotes++
    }
    if ((data.candidates || []).some(item =>
      item.days === sample.days && item.dayUnit === sample.unit)) candidateHits++
  }
  console.log(`SYNTHETIC AI BASELINE: candidate-hit ${candidateHits}/${cases.length}; verbatim evidence ${exactQuotes}/${quoteCount}`)
  if (exactQuotes !== quoteCount) throw new Error('Non-verbatim AI evidence detected')

  // Historical self-written workflow pilot: validates scoring plumbing, not a held-out metric.
  const pilotForm = new FormData()
  for (const role of ['agreement', 'sow']) {
    const name = `PAIR-001-${role}.txt`
    pilotForm.append(role, new Blob([await readFile(`testdata/product-evaluation/heldout/${name}`)],
      { type: 'text/plain' }), name)
  }
  const pilotResponse = await expect(await request('/api/review', {
    method: 'POST', secure: true, body: pilotForm,
  }), 200, 'score historical paired-document pilot')
  const pilotDirectory = await mkdtemp(join(tmpdir(), 'folio-scoring-'))
  try {
    await writeFile(join(pilotDirectory, 'PAIR-001.json'), JSON.stringify(await pilotResponse.json()))
    const output = execFileSync(process.execPath, [
      'scripts/score-product-review.mjs',
      'testdata/product-evaluation/heldout/labels.json', pilotDirectory,
    ], { encoding: 'utf8' })
    const pilot = JSON.parse(output)
    console.log(`HISTORICAL SELF-WRITTEN PILOT (NOT HELD-OUT): matched=${pilot.matched}/${pilot.goldObligations}; false-positives=${pilot.falsePositives}; misses=${pilot.misses}; status-errors=${pilot.statusErrors}`)
  } finally {
    await rm(pilotDirectory, { recursive: true, force: true })
  }

  // Small bounded concurrency probe. Never target production or call it a load capacity SLA.
  const latencies = []
  for (let batch = 0; batch < 4; batch++) {
    await Promise.all(Array.from({ length: 3 }, async () => {
      const start = performance.now()
      const response = await request('/api/review', {
        method: 'POST', secure: true, body: await documents(),
      })
      if (response.status !== 200) throw new Error(`Concurrent review returned HTTP ${response.status}`)
      const result = await response.json()
      if (!result.reviewStatus) throw new Error('Concurrent review result incomplete')
      latencies.push(Math.round(performance.now() - start))
    }))
  }
  const ordered = latencies.toSorted((a, b) => a - b)
  const p95 = ordered[Math.ceil(0.95 * ordered.length) - 1]
  console.log(`ISOLATED CONCURRENCY: ${latencies.length}/${latencies.length} succeeded; 3 at a time; p50=${ordered[Math.floor(ordered.length / 2)]}ms; p95=${p95}ms`)

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

  // Verify user-owned data remains private across independent sessions.
  await expect(await request('/api/auth/logout', { method: 'POST', secure: true }), 204, 'logout first user')
  const otherEmail = `folio-e2e-${randomUUID()}@example.invalid`
  const otherPassword = `Folio-test-${randomUUID()}!`
  await expect(await request('/api/auth/register', {
    method: 'POST', secure: true,
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ email: otherEmail, displayName: 'Folio Other CI Tester', password: otherPassword }),
  }), 201, 'register independent test account')
  await expect(await request('/api/auth/login', {
    method: 'POST', secure: true,
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ email: otherEmail, password: otherPassword }),
  }), 200, 'login independent test account')
  await expect(await request(`/api/reviews/${savedReview.id}`), 404, 'cross-account review read rejected')
  await expect(await request(`/api/reviews/${savedReview.id}`, {
    method: 'DELETE', secure: true,
  }), 404, 'cross-account review deletion rejected')
  await expect(await request('/api/auth/logout', { method: 'POST', secure: true }), 204, 'logout independent test account')
  await expect(await request('/api/auth/login', {
    method: 'POST', secure: true,
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ email, password }),
  }), 200, 'restore original test account')
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
