import { randomBytes, randomUUID } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

function localUrl(value) {
  const url = new URL(value)

  if (
    url.protocol !== 'http:' ||
    !['127.0.0.1', 'localhost'].includes(url.hostname) ||
    url.username || url.password ||
    url.search || url.hash ||
    url.pathname !== '/'
  ) {
    throw new Error('This script only accepts local HTTP root URLs.')
  }

  return url.origin
}

const base = localUrl(process.argv[2] ?? 'http://127.0.0.1:8081')
const inbox = localUrl(process.argv[3] ?? 'http://127.0.0.1:8025')
const runId = randomUUID()
const email = `recovery-${runId}@example.test`
const oldPassword = randomBytes(24).toString('hex')
const newPassword = randomBytes(24).toString('hex')
const finalPassword = randomBytes(24).toString('hex')

const report = {
  runId,
  startedAt: new Date().toISOString(),
  baseUrl: base,
  completed: false,
  checks: [],
}

function check(name, actual, expected) {
  const passed = Array.isArray(expected)
    ? expected.includes(actual)
    : actual === expected

  report.checks.push({ name, actual, expected, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) {
    throw new Error(`Check failed: ${name}`)
  }
}

function client() {
  const cookies = new Map()

  async function request(path, options = {}) {
    const headers = new Headers(options.headers)

    if (cookies.size) {
      headers.set(
        'Cookie',
        [...cookies].map(([name, value]) =>
          `${name}=${value}`
        ).join('; ')
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

      if (value === '' || /max-age=0(?:;|$)/i.test(cookie)) {
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
      // Some authentication failures have no JSON body.
    }

    return {
      status: response.status,
      data,
      retryAfter: response.headers.get('retry-after'),
    }
  }

  async function csrfHeaders() {
    const response = await request('/api/auth/csrf')

    if (response.status !== 200 || !cookies.get('XSRF-TOKEN')) {
      throw new Error('Could not prepare CSRF request.')
    }

    return {
      'X-XSRF-TOKEN': decodeURIComponent(
        cookies.get('XSRF-TOKEN')
      ),
    }
  }

  async function post(path, body) {
    return request(path, {
      method: 'POST',
      headers: {
        ...await csrfHeaders(),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })
  }

  async function login(password) {
    return request('/api/auth/login', {
      method: 'POST',
      headers: {
        ...await csrfHeaders(),
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({ email, password }),
    })
  }

  return { request, post, login }
}

async function mailpit(path) {
  const response = await fetch(`${inbox}${path}`, {
    redirect: 'manual',
    signal: AbortSignal.timeout(10000),
  })

  if (!response.ok) {
    throw new Error('Could not read local Mailpit inbox.')
  }

  return response.json()
}

async function messages() {
  const result = await mailpit('/api/v1/messages?limit=100')

  if (!Array.isArray(result.messages)) {
    throw new Error('Unexpected Mailpit message-list format.')
  }

  return result.messages
}

const anonymous = client()
const owner = client()

async function requestLink() {
  const existing = new Set((await messages()).map(item => item.ID))

  const response = await anonymous.post(
    '/api/auth/forgot-password',
    { email }
  )

  check('Reset request accepted', response.status, 202)

  for (let attempt = 0; attempt < 20; attempt++) {
    const item = (await messages()).find(message =>
      !existing.has(message.ID) &&
      message.To?.some(recipient =>
        recipient.Address?.toLowerCase() === email
      )
    )

    if (item) {
      const message = await mailpit(
        `/api/v1/message/${encodeURIComponent(item.ID)}`
      )

      const match = message.Text?.match(
        /http:\/\/127\.0\.0\.1:5173\/#reset-password=([A-Za-z0-9_-]{43})(?![A-Za-z0-9_-])/
      )

      check('Reset email contains expected link', Boolean(match), true)
      return { token: match[1], acknowledgement: response.data?.message }
    }

    await delay(500)
  }

  throw new Error('Reset email was not received in local Mailpit.')
}

try {
  let response = await owner.post('/api/auth/register', {
    email,
    displayName: 'Recovery API Test',
    password: oldPassword,
  })
  check('Register test account', response.status, 201)

  response = await owner.login(oldPassword)
  check('Login before reset', response.status, 200)

  response = await owner.request('/api/auth/me')
  check('Old session works before reset', response.status, 200)
  check('Original account identity', response.data?.email === email, true)

  const first = await requestLink()

  response = await anonymous.post('/api/auth/forgot-password', {
    email: `unknown-${runId}@example.test`,
  })
  check('Unknown account acknowledgement', response.status, 202)
  check(
    'Known and unknown acknowledgements match',
    response.data?.message === first.acknowledgement,
    true
  )

  response = await owner.request('/api/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      token: first.token,
      password: newPassword,
    }),
  })
  check('Authenticated reset without CSRF denied', response.status, 403)

  response = await anonymous.post('/api/auth/reset-password', {
    token: first.token,
    password: 'short',
  })
  check('Short password rejected', response.status, 400)

  response = await anonymous.post('/api/auth/reset-password', {
    token: first.token,
    password: newPassword,
  })
  check('Valid password reset', response.status, 200)

  response = await owner.request('/api/auth/me')
  check('Previously valid session rejected after reset', response.status, 401)

  response = await anonymous.post('/api/auth/reset-password', {
    token: first.token,
    password: newPassword,
  })
  check('Used token rejected', response.status, 400)

  const renewed = client()

  response = await renewed.login(oldPassword)
  check('Old password rejected', response.status, 401)

  response = await renewed.login(newPassword)
  check('New password login', response.status, 200)

  response = await renewed.request('/api/auth/me')
  check('New session works', response.status, 200)
  check('Account identity preserved', response.data?.email === email, true)

  const superseded = await requestLink()
  const latest = await requestLink()

  check('New reset token differs', latest.token !== superseded.token, true)

  response = await anonymous.post('/api/auth/reset-password', {
    token: superseded.token,
    password: finalPassword,
  })
  check('Superseded reset link rejected', response.status, 400)

  // Independent clients send reset requests concurrently.
  const contenderA = client()
  const contenderB = client()

  const outcomes = await Promise.all([
    contenderA.post('/api/auth/reset-password', {
      token: latest.token,
      password: finalPassword,
    }),
    contenderB.post('/api/auth/reset-password', {
      token: latest.token,
      password: finalPassword,
    }),
  ])

  check(
    'Concurrent reset has exactly one success',
    outcomes.filter(item => item.status === 200).length,
    1
  )
  check(
    'Concurrent reset rejects the other request',
    outcomes.filter(item => item.status === 400).length,
    1
  )

  response = await renewed.request('/api/auth/me')
  check('Second old session rejected after reset', response.status, 401)

  const finalSession = client()
  response = await finalSession.login(finalPassword)
  check('Concurrent reset password works', response.status, 200)

  response = await finalSession.request('/api/auth/me')
  check('Final account identity', response.data?.email === email, true)

  response = await anonymous.post('/api/auth/forgot-password', { email })
  check('Fourth email request is rate limited', response.status, 429)
  check('Rate limit includes Retry-After', response.retryAfter, '900')

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

  const file = join(folder, `recovery-${runId}.json`)
  writeFileSync(file, JSON.stringify(report, null, 2) + '\n')

  console.log(`Report saved: ${file}`)
  console.log(
    `Checks: ${report.total}; passed: ${report.passed}; completed: ${report.completed}`
  )
}