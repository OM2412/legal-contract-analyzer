import { createHash, randomBytes, randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

const base = 'http://127.0.0.1:8081'
const inbox = 'http://127.0.0.1:8025'
const runId = randomUUID()
const email = `recovery-expiry-${runId}@example.test`
const password = randomBytes(24).toString('hex')
const proposedPassword = randomBytes(24).toString('hex')

const report = {
  runId,
  startedAt: new Date().toISOString(),
  baseUrl: base,
  method: 'Controlled expiry of one dedicated test-account token in local PostgreSQL',
  completed: false,
  checks: [],
}

function check(name, actual, expected) {
  const passed = actual === expected
  report.checks.push({ name, actual, expected, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) throw new Error(`Check failed: ${name}`)
}

function client() {
  const cookies = new Map()

  async function request(path, options = {}) {
    const headers = new Headers(options.headers)

    if (cookies.size) {
      headers.set(
        'Cookie',
        [...cookies].map(([key, value]) => `${key}=${value}`).join('; ')
      )
    }

    const response = await fetch(`${base}${path}`, {
      ...options,
      headers,
      redirect: 'manual',
      signal: AbortSignal.timeout(30000),
    })

    for (const cookie of response.headers.getSetCookie()) {
      const pair = cookie.split(';', 1)[0]
      const separator = pair.indexOf('=')
      if (separator < 1) continue

      const name = pair.slice(0, separator)
      const value = pair.slice(separator + 1)

      if (!value || /max-age=0(?:;|$)/i.test(cookie)) {
        cookies.delete(name)
      } else {
        cookies.set(name, value)
      }
    }

    const text = await response.text()
    let data = null
    try { data = JSON.parse(text) } catch {}

    return { status: response.status, data }
  }

  async function csrf() {
    const response = await request('/api/auth/csrf')
    const token = cookies.get('XSRF-TOKEN')

    if (response.status !== 200 || !token) {
      throw new Error('Could not prepare secure request.')
    }

    return { 'X-XSRF-TOKEN': decodeURIComponent(token) }
  }

  async function post(path, body) {
    return request(path, {
      method: 'POST',
      headers: {
        ...await csrf(),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })
  }

  async function login(candidate) {
    return request('/api/auth/login', {
      method: 'POST',
      headers: {
        ...await csrf(),
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({ email, password: candidate }),
    })
  }

  return { request, post, login }
}

async function mail(path) {
  const response = await fetch(`${inbox}${path}`, {
    redirect: 'manual',
    signal: AbortSignal.timeout(10000),
  })

  if (!response.ok) throw new Error('Local Mailpit is unavailable.')
  return response.json()
}

function sql(statement) {
  try {
    return execFileSync('docker', [
      'exec',
      'legal-contract-analyzer-postgres-1',
      'psql',
      '-U', 'folio',
      '-d', 'folio',
      '-v', 'ON_ERROR_STOP=1',
      '-At',
      '-c', statement,
    ], {
      encoding: 'utf8',
      timeout: 15000,
      stdio: ['ignore', 'pipe', 'pipe'],
    }).trim()
  } catch {
    throw new Error('Local PostgreSQL expiry-test operation failed.')
  }
}

try {
  const owner = client()
  const anonymous = client()

  let response = await owner.post('/api/auth/register', {
    email,
    displayName: 'Expiry Test',
    password,
  })
  check('Register dedicated expiry account', response.status, 201)

  response = await owner.login(password)
  check('Login before expiry test', response.status, 200)

  response = await owner.request('/api/auth/me')
  check('Session works before expiry test', response.status, 200)
  check('Test account identity', response.data?.email === email, true)

  const userId = response.data?.id

  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(userId)) {
    throw new Error('Expected a valid test-account UUID.')
  }

  const versionBefore = sql(
    `SELECT credential_version FROM app_users WHERE id = '${userId}' AND email = '${email}';`
  )
  check('New account credential version', versionBefore, '0')

  response = await anonymous.post('/api/auth/forgot-password', { email })
  check('Expiry test reset request accepted', response.status, 202)

  let rawToken = null

  for (let attempt = 0; attempt < 20; attempt++) {
    const listing = await mail('/api/v1/messages?limit=100')
    const item = listing.messages?.find(message =>
      message.To?.some(recipient => recipient.Address === email)
    )

    if (item) {
      const message = await mail(
        `/api/v1/message/${encodeURIComponent(item.ID)}`
      )
      rawToken = message.Text?.match(
        /#reset-password=([A-Za-z0-9_-]{43})(?![A-Za-z0-9_-])/
      )?.[1] ?? null

      if (rawToken) break
    }

    await delay(500)
  }

  check('Reset email received', Boolean(rawToken), true)

  const hash = createHash('sha256').update(rawToken).digest('hex')

  const changed = sql(`
    WITH expired AS (
      UPDATE password_reset_tokens
      SET created_at = now() - interval '20 minutes',
          expires_at = now() - interval '1 minute'
      WHERE token_hash = '${hash}'
        AND user_id = '${userId}'
        AND consumed_at IS NULL
        AND EXISTS (
          SELECT 1 FROM app_users
          WHERE id = '${userId}' AND email = '${email}'
        )
      RETURNING 1
    )
    SELECT count(*) FROM expired;
  `)
  check('Exactly one test token expired', changed, '1')

  response = await anonymous.post('/api/auth/reset-password', {
    token: rawToken,
    password: proposedPassword,
  })
  rawToken = null

  check('Expired token rejected by API', response.status, 400)
  check(
    'Expired token receives safe error',
    response.data?.error,
    'Reset link is invalid or expired'
  )

  response = await owner.request('/api/auth/me')
  check('Existing session remains valid', response.status, 200)
  check('Existing session identity preserved', response.data?.email === email, true)

  const fresh = client()
  response = await fresh.login(password)
  check('Original password still works', response.status, 200)

  const rejected = client()
  response = await rejected.login(proposedPassword)
  check('Proposed replacement password does not work', response.status, 401)

  const versionAfter = sql(
    `SELECT credential_version FROM app_users WHERE id = '${userId}' AND email = '${email}';`
  )
  check('Credential version unchanged', versionAfter, versionBefore)

  const untouched = sql(`
    SELECT count(*) FROM password_reset_tokens
    WHERE token_hash = '${hash}'
      AND user_id = '${userId}'
      AND consumed_at IS NULL
      AND expires_at < now();
  `)
  check('Expired token remains unconsumed', untouched, '1')

  report.completed = true
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
} finally {
  report.finishedAt = new Date().toISOString()
  report.total = report.checks.length
  report.passed = report.checks.filter(item => item.passed).length

  const folder = join('testdata', 'security-evaluation')
  mkdirSync(folder, { recursive: true })

  const file = join(folder, `recovery-expiry-${runId}.json`)
  writeFileSync(file, JSON.stringify(report, null, 2) + '\n')

  console.log(`Report saved: ${file}`)
  console.log(
    `Checks: ${report.total}; passed: ${report.passed}; completed: ${report.completed}`
  )
}