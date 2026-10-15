import { mkdirSync, writeFileSync } from 'node:fs'
import { randomBytes, randomUUID } from 'node:crypto'
import process from 'node:process'

const base = new URL(
  process.argv[2] ?? 'http://127.0.0.1:8081'
)

if (
  base.protocol !== 'http:' ||
  !['127.0.0.1', 'localhost'].includes(base.hostname) ||
  base.username ||
  base.password ||
  base.pathname !== '/' ||
  base.search ||
  base.hash
) {
  throw new Error('Use a local HTTP base URL.')
}

const runId = randomUUID()
const startedAt = new Date().toISOString()
const checks = []
const cookies = new Map()
let failure = null

function check(name, actual, expected) {
  const passed = actual === expected
  checks.push({ name, actual, expected, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) throw new Error(`Check failed: ${name}`)
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers)

  if (cookies.size) {
    headers.set(
      'Cookie',
      [...cookies]
        .map(([name, value]) => `${name}=${value}`)
        .join('; ')
    )
  }

  const response = await fetch(new URL(path, base), {
    ...options,
    headers,
    redirect: 'manual',
    signal: AbortSignal.timeout(60000),
  })

  for (const cookie of response.headers.getSetCookie()) {
    const first = cookie.split(';', 1)[0]
    const separator = first.indexOf('=')

    if (separator < 1) continue

    const name = first.slice(0, separator)
    const value = first.slice(separator + 1)

    if (
      !value ||
      /(?:^|;)\s*Max-Age=0(?:;|$)/i.test(cookie)
    ) {
      cookies.delete(name)
    } else {
      cookies.set(name, value)
    }
  }

  const text = await response.text()
  let data = null

  try {
    data = JSON.parse(text)
  } catch {
    // Security rejection responses may have empty bodies.
  }

  return {
    status: response.status,
    headers: response.headers,
    data,
  }
}

async function csrf() {
  const response = await request('/api/auth/csrf')
  const token = cookies.get('XSRF-TOKEN')

  if (response.status !== 200 || !token) {
    throw new Error('Could not obtain CSRF token.')
  }

  return decodeURIComponent(token)
}

function verifyLimited(name, response) {
  check(`${name}: status`, response.status, 429)

  const retryAfter = response.headers.get('Retry-After')

  check(
    `${name}: Retry-After`,
    /^\d+$/.test(retryAfter ?? '') &&
      Number(retryAfter) >= 1 &&
      Number(retryAfter) <= 900,
    true
  )

  check(
    `${name}: JSON error`,
    response.headers.get('Content-Type')
      ?.includes('application/json') === true &&
      response.data?.error ===
        'Too many attempts. Please try again later.',
    true
  )

  check(
    `${name}: no-store`,
    response.headers.get('Cache-Control'),
    'no-store'
  )
}

async function run() {
  const token = await csrf()

  const loginBody = () => new URLSearchParams({
    email: `nonexistent-rate-test-${runId}@example.test`,
    password: randomBytes(24).toString('hex'),
  })

  const noCsrf = await request('/api/auth/login', {
    method: 'POST',
    body: loginBody(),
  })

  check(
    'Login without CSRF rejected',
    [401, 403].includes(noCsrf.status),
    true
  )

  const invalidCsrf = await request('/api/auth/login', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': 'invalid-token' },
    body: loginBody(),
  })

  check(
    'Login with invalid CSRF rejected',
    [401, 403].includes(invalidCsrf.status),
    true
  )

  for (let attempt = 1; attempt <= 30; attempt++) {
    const response = await request('/api/auth/login', {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': token },
      body: loginBody(),
    })

    check(
      `Login attempt ${attempt} reaches authentication`,
      response.status,
      401
    )
  }

  verifyLimited(
    'Login attempt 31',
    await request('/api/auth/login', {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': token },
      body: loginBody(),
    })
  )

  verifyLimited(
    'Forwarded header cannot bypass local login limit',
    await request('/api/auth/login', {
      method: 'POST',
      headers: {
        'X-XSRF-TOKEN': token,
        'X-Forwarded-For': '192.0.2.50',
        'Forwarded': 'for=192.0.2.50',
      },
      body: loginBody(),
    })
  )

  for (let attempt = 1; attempt <= 10; attempt++) {
    const response = await request('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': token,
      },
      body: JSON.stringify({
        email: 'invalid-email',
        displayName: 'Rate Limit Test',
        password: 'short',
      }),
    })

    check(
      `Registration attempt ${attempt} reaches validation`,
      response.status,
      400
    )
  }

  verifyLimited(
    'Registration attempt 11',
    await request('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': token,
      },
      body: JSON.stringify({
        email: 'invalid-email',
        displayName: 'Rate Limit Test',
        password: 'short',
      }),
    })
  )

  check(
    'CSRF endpoint still available',
    (await request('/api/auth/csrf')).status,
    200
  )

  check(
    'Anonymous profile still denied',
    (await request('/api/auth/me')).status,
    401
  )
}

try {
  await run()
} catch (error) {
  failure = error instanceof Error
    ? error.message
    : 'Rate-limit checks could not complete.'

  console.error(failure)
  process.exitCode = 1
} finally {
  const folder = 'testdata/security-evaluation'
  const path = `${folder}/auth-rate-limits-${runId}.json`

  mkdirSync(folder, { recursive: true })

  writeFileSync(
    path,
    JSON.stringify({
      runId,
      startedAt,
      finishedAt: new Date().toISOString(),
      baseUrl: base.origin,
      completed: failure === null,
      totalChecks: checks.length,
      passedChecks: checks.filter((item) => item.passed).length,
      failure,
      checks,
      note: 'Local single-instance HTTP checks using invalid credentials '
        + 'and invalid registration data. No accounts are created. '
        + 'Cookies, CSRF tokens and passwords are not recorded. '
        + 'Hosted proxy behaviour requires separate verification.',
    }, null, 2) + '\n',
    'utf8'
  )

  console.log(`Report saved: ${path}`)
  console.log(
    `Checks: ${checks.length}; `
    + `passed: ${checks.filter((item) => item.passed).length}; `
    + `completed: ${failure === null}`
  )
}