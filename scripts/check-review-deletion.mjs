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
let failure = null

function check(name, actual, expected) {
  const passed = actual === expected
  checks.push({ name, actual, expected, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) {
    throw new Error(`Check failed: ${name}`)
  }
}

function session() {
  const cookies = new Map()

  return {
    async request(path, options = {}) {
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
        // Empty responses are expected for successful deletion.
      }

      return { status: response.status, data }
    },

    async csrf() {
      const response = await this.request('/api/auth/csrf')
      const token = cookies.get('XSRF-TOKEN')

      if (response.status !== 200 || !token) {
        throw new Error('Could not obtain a local CSRF token.')
      }

      return decodeURIComponent(token)
    },
  }
}

async function createAccount(client, suffix) {
  const account = {
    email: `folio-delete-${runId}-${suffix}@example.test`,
    displayName: `Deletion Test ${suffix}`,
    password: randomBytes(24).toString('hex'),
  }

  const registration = await client.request(
    '/api/auth/register',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': await client.csrf(),
      },
      body: JSON.stringify(account),
    }
  )

  check(`Register ${suffix}`, registration.status, 201)

  const login = await client.request('/api/auth/login', {
    method: 'POST',
    headers: {
      'X-XSRF-TOKEN': await client.csrf(),
    },
    body: new URLSearchParams({
      email: account.email,
      password: account.password,
    }),
  })

  check(`Login ${suffix}`, login.status, 200)
}

function documents() {
  const form = new FormData()

  form.append(
    'agreement',
    new Blob([
      'Client shall pay Provider the project fee within '
      + '30 calendar days after receipt of the invoice.',
    ], { type: 'text/plain' }),
    'deletion-test-agreement.txt'
  )

  form.append(
    'sow',
    new Blob([
      'Client shall pay Provider the project fee within '
      + '60 calendar days after receipt of the invoice.',
    ], { type: 'text/plain' }),
    'deletion-test-sow.txt'
  )

  form.append('maxDays', '30')
  return form
}

async function save(client, label) {
  const response = await client.request('/api/reviews', {
    method: 'POST',
    headers: {
      'X-XSRF-TOKEN': await client.csrf(),
    },
    body: documents(),
  })

  check(`Save ${label}`, response.status, 201)

  const id = response.data?.id

  check(
    `${label} has UUID`,
    typeof id === 'string' &&
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id),
    true
  )

  return id
}

async function remove(client, id) {
  return client.request(`/api/reviews/${id}`, {
    method: 'DELETE',
    headers: {
      'X-XSRF-TOKEN': await client.csrf(),
    },
  })
}

async function run() {
  const owner = session()
  const other = session()
  const anonymous = session()

  await createAccount(owner, 'A')
  await createAccount(other, 'B')

  const target = await save(owner, 'owner target')
  const retained = await save(owner, 'owner control')
  const otherReview = await save(other, 'other user control')

  check(
    'Owner reads target before deletion',
    (await owner.request(`/api/reviews/${target}`)).status,
    200
  )

  check(
    'Anonymous DELETE with CSRF denied',
    (await remove(anonymous, target)).status,
    401
  )

  check(
    'Target survives anonymous DELETE',
    (await owner.request(`/api/reviews/${target}`)).status,
    200
  )

  await owner.csrf()

  check(
    'Owner DELETE without CSRF denied',
    (await owner.request(`/api/reviews/${target}`, {
      method: 'DELETE',
    })).status,
    403
  )

  check(
    'Owner DELETE with invalid CSRF denied',
    (await owner.request(`/api/reviews/${target}`, {
      method: 'DELETE',
      headers: { 'X-XSRF-TOKEN': 'invalid-token' },
    })).status,
    403
  )

  check(
    'Target survives rejected CSRF requests',
    (await owner.request(`/api/reviews/${target}`)).status,
    200
  )

  check(
    'Other account cannot delete owner review',
    (await remove(other, target)).status,
    404
  )

  check(
    'Target survives other-account DELETE',
    (await owner.request(`/api/reviews/${target}`)).status,
    200
  )

  check(
    'Unknown review DELETE returns 404',
    (await remove(owner, randomUUID())).status,
    404
  )

  check(
    'Owner deletes target',
    (await remove(owner, target)).status,
    204
  )

  check(
    'Deleted target cannot be opened',
    (await owner.request(`/api/reviews/${target}`)).status,
    404
  )

  check(
    'Repeated deletion returns 404',
    (await remove(owner, target)).status,
    404
  )

  const ownerList = await owner.request('/api/reviews?page=0')

  check('Owner archive loads', ownerList.status, 200)

  check(
    'Owner archive excludes deleted target',
    Array.isArray(ownerList.data?.items) &&
      !ownerList.data.items.some((item) => item.id === target),
    true
  )

  check(
    'Owner control review remains',
    (await owner.request(`/api/reviews/${retained}`)).status,
    200
  )

  check(
    'Other account control remains',
    (await other.request(`/api/reviews/${otherReview}`)).status,
    200
  )

  check(
    'Owner cannot delete other account control',
    (await remove(owner, otherReview)).status,
    404
  )

  check(
    'Other account control survives',
    (await other.request(`/api/reviews/${otherReview}`)).status,
    200
  )

  check(
    'Clean up owner test control',
    (await remove(owner, retained)).status,
    204
  )

  check(
    'Clean up other test control',
    (await remove(other, otherReview)).status,
    204
  )
}

try {
  await run()
} catch (error) {
  failure = error instanceof Error
    ? error.message
    : 'Deletion checks could not complete.'

  console.error(failure)
  process.exitCode = 1
} finally {
  const folder = 'testdata/security-evaluation'
  const path = `${folder}/review-deletion-${runId}.json`

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
      note: 'Dedicated local test accounts and synthetic documents. '
        + 'Test accounts remain; test reviews are removed on a successful run. '
        + 'Passwords, session cookies and CSRF tokens are not recorded.',
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