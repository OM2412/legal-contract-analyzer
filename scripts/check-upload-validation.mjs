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
  base.username || base.password ||
  base.pathname !== '/' || base.search || base.hash
) {
  throw new Error('Use a local HTTP base URL.')
}

const runId = randomUUID()
const startedAt = new Date().toISOString()
const checks = []
const cookies = new Map()

const agreement = readFileSync(
  'testdata/product-evaluation/heldout/PAIR-001-agreement.txt'
)
const sow = readFileSync(
  'testdata/product-evaluation/heldout/PAIR-001-sow.txt'
)

const version = (bytes) =>
  `sha256:${createHash('sha256').update(bytes).digest('hex')}`

const user = {
  email: `folio-upload-${runId}@example.test`,
  displayName: 'Folio Upload Test',
  password: randomBytes(24).toString('hex'),
}

let failure = null

function check(name, actual, expected) {
  const passed = actual === expected
  checks.push({ name, actual, expected, passed })

  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) {
    throw new Error(`Check failed: ${name}`)
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers)

  if (cookies.size) {
    headers.set(
      'Cookie',
      [...cookies]
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
    const index = first.indexOf('=')

    if (index < 1) continue

    const key = first.slice(0, index)
    const value = first.slice(index + 1)

    if (
      !value ||
      /(?:^|;)\s*Max-Age=0(?:;|$)/i.test(cookie)
    ) {
      cookies.delete(key)
    } else {
      cookies.set(key, value)
    }
  }

  const text = await response.text()
  let data = null

  try {
    data = JSON.parse(text)
  } catch {
    // Non-JSON responses are checked explicitly below.
  }

  return {
    status: response.status,
    data,
    text,
    type: response.headers.get('content-type') ?? '',
  }
}

async function csrf() {
  const response = await request('/api/auth/csrf')

  if (
    response.status !== 200 ||
    !cookies.get('XSRF-TOKEN')
  ) {
    throw new Error('CSRF setup failed.')
  }

  return decodeURIComponent(cookies.get('XSRF-TOKEN'))
}

function form(options = {}) {
  const {
    agreementBytes = agreement,
    agreementName = 'PAIR-001-agreement.txt',
    sowBytes = sow,
    sowName = 'PAIR-001-sow.txt',
    maxDays = '30',
  } = options

  const upload = new FormData()

  if (agreementBytes !== null) {
    upload.append(
      'agreement',
      new Blob([agreementBytes]),
      agreementName
    )
  }

  if (sowBytes !== null) {
    upload.append('sow', new Blob([sowBytes]), sowName)
  }

  upload.append('maxDays', String(maxDays))
  return upload
}

function blankPdf() {
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] ' +
      '/Resources << >> /Contents 4 0 R >>',
    '<< /Length 0 >>\nstream\n\nendstream',
  ]

  let pdf = '%PDF-1.4\n'
  const offsets = []

  for (let index = 0; index < objects.length; index++) {
    offsets.push(Buffer.byteLength(pdf, 'ascii'))
    pdf += `${index + 1} 0 obj\n${objects[index]}\nendobj\n`
  }

  const xref = Buffer.byteLength(pdf, 'ascii')
  pdf += 'xref\n0 5\n0000000000 65535 f \n'

  for (const offset of offsets) {
    pdf += `${String(offset).padStart(10, '0')} 00000 n \n`
  }

  pdf +=
    'trailer\n<< /Size 5 /Root 1 0 R >>\n' +
    `startxref\n${xref}\n%%EOF\n`

  return Buffer.from(pdf, 'ascii')
}

async function review(options) {
  return request('/api/review', {
    method: 'POST',
    body: form(options),
    headers: { 'X-XSRF-TOKEN': await csrf() },
  })
}

async function rejected(name, options, status = 400) {
  const response = await review(options)

  check(`${name} status`, response.status, status)

  check(
    `${name} JSON error`,
    response.type.includes('json') &&
      response.data !== null &&
      ['error', 'detail', 'message', 'title'].some((key) =>
        typeof response.data[key] === 'string' &&
        response.data[key].trim().length > 0
      ),
    true
  )

  check(
    `${name} no internal error details`,
    !/contract-review-|java\.[a-z]|org\.[a-z]|\/tmp\/|\/app\/|[A-Z]:\\|stackTrace/i
      .test(response.text),
    true
  )
}

async function run() {
  const registration = await request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(user),
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': await csrf(),
    },
  })

  check('Test account registration', registration.status, 201)

  const login = await request('/api/auth/login', {
    method: 'POST',
    body: new URLSearchParams({
      email: user.email,
      password: user.password,
    }),
    headers: { 'X-XSRF-TOKEN': await csrf() },
  })

  check('Test account login', login.status, 200)

  const profile = await request('/api/auth/me')

  check(
    'Authenticated identity',
    profile.status === 200 && profile.data?.email === user.email,
    true
  )

  const valid = await review()

  check('Valid TXT upload', valid.status, 200)
  check(
    'Agreement source version',
    valid.data?.agreementVersion,
    version(agreement)
  )
  check('SOW source version', valid.data?.sowVersion, version(sow))
  check(
    'Expected review status',
    valid.data?.reviewStatus,
    'REVIEW_REQUIRED'
  )

  await rejected('Empty Agreement', {
    agreementBytes: Buffer.alloc(0),
  })

  await rejected('Empty SOW', {
    sowBytes: Buffer.alloc(0),
  })

  await rejected('Whitespace TXT', {
    agreementBytes: Buffer.from(' \r\n\t '),
  })

  await rejected('Unsupported extension', {
    agreementName: 'agreement.docx',
  })

  await rejected('Invalid UTF-8', {
    agreementBytes: Buffer.from([0xc3, 0x28]),
  })

  await rejected('Null character TXT', {
    agreementBytes: Buffer.from('Contract\0text'),
  })

  await rejected('Missing Agreement', {
    agreementBytes: null,
  })

  await rejected('Missing SOW', {
    sowBytes: null,
  })

  await rejected('Policy zero', { maxDays: '0' })
  await rejected('Policy above maximum', { maxDays: '3651' })
  await rejected('Noninteger policy', { maxDays: 'abc' })

  await rejected('TXT over 1 MiB', {
    agreementBytes: Buffer.alloc(1024 * 1024 + 1, 65),
  })

  await rejected('Corrupt PDF', {
    agreementName: 'corrupt.pdf',
    agreementBytes: Buffer.from('%PDF-1.4\nbroken'),
  })

  await rejected('TXT disguised as PDF', {
    agreementName: 'disguised.pdf',
  })

  await rejected('Blank PDF', {
    agreementName: 'blank.pdf',
    agreementBytes: blankPdf(),
  })

  await rejected('PDF over 10 MiB', {
    agreementName: 'oversized.pdf',
    agreementBytes: Buffer.alloc(10 * 1024 * 1024 + 1, 65),
  }, 413)

  await rejected('Multipart request over 20 MiB', {
    agreementName: 'large-a.pdf',
    sowName: 'large-b.pdf',
    agreementBytes: Buffer.alloc(10 * 1024 * 1024, 65),
    sowBytes: Buffer.alloc(10 * 1024 * 1024, 65),
  }, 413)

  const final = await review()

  check(
    'Valid upload after rejected inputs',
    final.status,
    200
  )
  check(
    'Final Agreement matches',
    final.data?.agreementMatches?.length,
    1
  )
  check('Final SOW matches', final.data?.sowMatches?.length, 2)

  check(
    'Logout',
    (await request('/api/auth/logout', {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': await csrf() },
    })).status,
    204
  )
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
  cookies.clear()
  user.password = null

  const directory = resolve('testdata/upload-evaluation')
  mkdirSync(directory, { recursive: true })

  const path = resolve(directory, `uploads-${runId}.json`)
  const passed = checks.filter((item) => item.passed).length

  writeFileSync(path, JSON.stringify({
    runId,
    startedAt,
    finishedAt: new Date().toISOString(),
    baseUrl: base.origin,
    fixtureVersions: {
      agreement: version(agreement),
      sow: version(sow),
    },
    checksRun: checks.length,
    passed,
    failed: checks.length - passed,
    completed: failure === null,
    failure,
    checks,
    note:
      'Live local upload validation, not a performance or penetration ' +
      'test. One generated test account remains locally. No passwords, ' +
      'cookies or tokens are stored.',
  }, null, 2) + '\n', 'utf8')

  console.log(`Report saved: ${path}`)
  console.log(
    `Checks: ${checks.length}; passed: ${passed}; ` +
    `completed: ${failure === null}`
  )
}