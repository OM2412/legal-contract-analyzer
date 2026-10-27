import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { randomBytes, randomUUID, createHash } from 'node:crypto'
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

const startedAt = new Date().toISOString()
const runId = randomUUID()
const cases = []
const jars = []

const agreement = readFileSync(resolve(
  'testdata/product-evaluation/heldout/PAIR-001-agreement.txt'
))
const sow = readFileSync(resolve(
  'testdata/product-evaluation/heldout/PAIR-001-sow.txt'
))

const version = (bytes) =>
  `sha256:${createHash('sha256').update(bytes).digest('hex')}`

let savedReviewId = null
let failure = null

function check(name, actual, expected) {
 const passed = Array.isArray(expected)
  ? expected.includes(actual)
  : actual === expected
  cases.push({ name, expected, actual, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) {
    throw new Error(`Check failed: ${name}`)
  }
}

function session() {
  const jar = new Map()
  jars.push(jar)

  return {
    async request(path, options = {}) {
      const headers = new Headers(options.headers)

      if (jar.size) {
        headers.set(
          'Cookie',
          [...jar]
            .map(([key, value]) => `${key}=${value}`)
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

        const key = first.slice(0, separator)
        const value = first.slice(separator + 1)

        if (
          !value ||
          /(?:^|;)\s*Max-Age=0(?:;|$)/i.test(cookie)
        ) {
          jar.delete(key)
        } else {
          jar.set(key, value)
        }
      }

      const text = await response.text()
      let data = null

      try {
        data = JSON.parse(text)
      } catch {
        // Empty and non-JSON error bodies are allowed.
      }

      return { status: response.status, data }
    },

    async csrf() {
      const response = await this.request('/api/auth/csrf')

      if (
        response.status !== 200 ||
        !jar.get('XSRF-TOKEN')
      ) {
        throw new Error('CSRF setup failed.')
      }

      return decodeURIComponent(jar.get('XSRF-TOKEN'))
    },
  }
}

function account(letter) {
  return {
    email: `folio-security-${runId}-${letter}@example.test`,
    displayName: `Folio Security Test ${letter}`,
    password: randomBytes(24).toString('hex'),
  }
}

async function register(client, user) {
  return client.request('/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': await client.csrf(),
    },
    body: JSON.stringify(user),
  })
}

async function login(client, user, password = user.password) {
  return client.request('/api/auth/login', {
    method: 'POST',
    headers: {
      'X-XSRF-TOKEN': await client.csrf(),
    },
    body: new URLSearchParams({
      email: user.email,
      password,
    }),
  })
}

function upload() {
  const form = new FormData()

  form.append(
    'agreement',
    new Blob([agreement], { type: 'text/plain' }),
    'PAIR-001-agreement.txt'
  )
  form.append(
    'sow',
    new Blob([sow], { type: 'text/plain' }),
    'PAIR-001-sow.txt'
  )
  form.append('maxDays', '30')

  return form
}

async function run() {
  const anon = session()
  const userA = session()
  const userB = session()
  const accountA = account('a')
  const accountB = account('b')

  check(
    'Anonymous profile denied',
    (await anon.request('/api/auth/me')).status,
    401
  )
  check(
    'Anonymous review list denied',
    (await anon.request('/api/reviews')).status,
    401
  )

  await userA.csrf()

    check(
    'Registration without CSRF denied',
    (await userA.request('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(accountA),
    })).status,
    [401, 403]
  )

  check('Register A', (await register(userA, accountA)).status, 201)
  check(
    'Duplicate email rejected',
    (await register(userA, accountA)).status,
    400
  )
  check(
    'Invalid login denied',
    (await login(
      userA,
      accountA,
      randomBytes(24).toString('hex')
    )).status,
    401
  )
  check('Login A', (await login(userA, accountA)).status, 200)

  const profileA = await userA.request('/api/auth/me')
  check('Authenticated profile A', profileA.status, 200)
  check(
    'Profile A identity',
    profileA.data?.email === accountA.email,
    true
  )

  check('Register B', (await register(userB, accountB)).status, 201)
  check('Login B', (await login(userB, accountB)).status, 200)

  const profileB = await userB.request('/api/auth/me')
  check('Authenticated profile B', profileB.status, 200)
  check(
    'Independent account identities',
    Boolean(
      profileA.data?.id &&
      profileB.data?.id &&
      profileA.data.id !== profileB.data.id &&
      profileB.data.email === accountB.email
    ),
    true
  )

  await userA.csrf()

  check(
    'Upload without CSRF denied',
    (await userA.request('/api/review', {
      method: 'POST',
      body: upload(),
    })).status,
    403
  )
  check(
    'Upload with invalid CSRF denied',
    (await userA.request('/api/review', {
      method: 'POST',
      body: upload(),
      headers: { 'X-XSRF-TOKEN': 'invalid-token' },
    })).status,
    403
  )

  const review = await userA.request('/api/review', {
    method: 'POST',
    body: upload(),
    headers: { 'X-XSRF-TOKEN': await userA.csrf() },
  })

  check('Authenticated upload succeeds', review.status, 200)
  check(
    'Agreement source version',
    review.data?.agreementVersion,
    version(agreement)
  )
  check(
    'SOW source version',
    review.data?.sowVersion,
    version(sow)
  )
  check(
    'Multiple-clause review status',
    review.data?.reviewStatus,
    'REVIEW_REQUIRED'
  )
  check(
    'Agreement match count',
    review.data?.agreementMatches?.length,
    1
  )
  check('SOW match count', review.data?.sowMatches?.length, 2)

  const saved = await userA.request('/api/reviews', {
    method: 'POST',
    body: upload(),
    headers: { 'X-XSRF-TOKEN': await userA.csrf() },
  })

  check('Save computed review', saved.status, 201)
  savedReviewId = saved.data?.id

  check(
    'Saved review has UUID',
    typeof savedReviewId === 'string' &&
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
        .test(savedReviewId),
    true
  )

  const detail = await userA.request(
    `/api/reviews/${savedReviewId}`
  )

  check('Owner reads saved review', detail.status, 200)
  check(
    'Saved Agreement version preserved',
    detail.data?.result?.agreementVersion,
    version(agreement)
  )
  check(
    'Saved SOW version preserved',
    detail.data?.result?.sowVersion,
    version(sow)
  )
  check(
    'Saved status preserved',
    detail.data?.result?.reviewStatus,
    'REVIEW_REQUIRED'
  )

  const listA = await userA.request('/api/reviews?page=0')
  check('Owner list succeeds', listA.status, 200)
  check(
    'Owner list includes review',
    listA.data?.items?.some((item) => item.id === savedReviewId),
    true
  )

  check(
    'Other user cannot read owner review',
    (await userB.request(`/api/reviews/${savedReviewId}`)).status,
    404
  )

  const listB = await userB.request('/api/reviews?page=0')
  check('Other user list succeeds', listB.status, 200)
  check(
    'Other user list excludes owner review',
    Array.isArray(listB.data?.items) &&
      !listB.data.items.some((item) => item.id === savedReviewId),
    true
  )
  check(
    'Anonymous saved-review access denied',
    (await anon.request(`/api/reviews/${savedReviewId}`)).status,
    401
  )
  check(
    'Unknown review returns 404',
    (await userA.request(`/api/reviews/${randomUUID()}`)).status,
    404
  )

  check(
    'Logout without CSRF denied',
    (await userA.request('/api/auth/logout', {
      method: 'POST',
    })).status,
    403
  )
  check(
    'Valid logout succeeds',
    (await userA.request('/api/auth/logout', {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': await userA.csrf() },
    })).status,
    204
  )
  check(
    'Profile denied after logout',
    (await userA.request('/api/auth/me')).status,
    401
  )
  check(
    'Saved review denied after logout',
    (await userA.request(`/api/reviews/${savedReviewId}`)).status,
    401
  )

  accountA.password = null
  accountB.password = null
}

try {
  await run()
} catch (error) {
  failure = error.name === 'TypeError'
    ? 'HTTP request failed; check backend availability.'
    : error.message

  console.error(failure)
  process.exitCode = 1
} finally {
  for (const jar of jars) jar.clear()

  const directory = resolve('testdata/security-evaluation')
  mkdirSync(directory, { recursive: true })

  const path = resolve(directory, `accounts-${runId}.json`)
  const passed = cases.filter((item) => item.passed).length

  writeFileSync(path, JSON.stringify({
    runId,
    startedAt,
    finishedAt: new Date().toISOString(),
    baseUrl: base.origin,
    fixtureVersions: {
      agreement: version(agreement),
      sow: version(sow),
    },
    savedReviewId,
    checksRun: cases.length,
    passed,
    failed: cases.length - passed,
    completed: failure === null,
    failure,
    cases,
    note:
      'Live local API checks, not a penetration test. Generated test ' +
      'accounts and a synthetic review may remain in the local database. ' +
      'No passwords, cookies or tokens are stored in this report.',
  }, null, 2) + '\n', 'utf8')

  console.log(`Report saved: ${path}`)
  console.log(
    `Checks: ${cases.length}; passed: ${passed}; ` +
    `completed: ${failure === null}`
  )
}