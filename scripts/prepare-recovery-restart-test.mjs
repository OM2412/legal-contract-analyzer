import { randomBytes, randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const base = 'http://127.0.0.1:8081'
const runId = randomUUID()
const email = `recovery-restart-${runId}@example.test`
const cookies = new Map()

async function request(path, options = {}) {
  const headers = new Headers(options.headers)

  if (cookies.size) {
    headers.set(
      'Cookie',
      [...cookies].map(([name, value]) => `${name}=${value}`).join('; ')
    )
  }

  const response = await fetch(`${base}${path}`, {
    ...options,
    headers,
    redirect: 'manual',
    signal: AbortSignal.timeout(15000),
  })

  for (const cookie of response.headers.getSetCookie()) {
    const pair = cookie.split(';', 1)[0]
    const separator = pair.indexOf('=')

    if (separator > 0) {
      cookies.set(
        pair.slice(0, separator),
        pair.slice(separator + 1)
      )
    }
  }

  await response.text()
  return response.status
}

async function post(path, body) {
  const status = await request('/api/auth/csrf')
  const token = cookies.get('XSRF-TOKEN')

  if (status !== 200 || !token) {
    throw new Error('Could not prepare CSRF request.')
  }

  return request(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': decodeURIComponent(token),
    },
    body: JSON.stringify(body),
  })
}

try {
  let status = await post('/api/auth/register', {
    email,
    displayName: 'Restart Recovery Test',
    password: randomBytes(24).toString('hex'),
  })

  if (status !== 201) {
    throw new Error(`Registration expected 201, got ${status}`)
  }
  console.log('PASS Test account registered: 201')

  status = await post('/api/auth/forgot-password', { email })

  if (status !== 202) {
    throw new Error(`Reset request expected 202, got ${status}`)
  }
  console.log('PASS Reset request accepted: 202')

  const output = execFileSync('docker', [
    'exec',
    'legal-contract-analyzer-postgres-1',
    'psql',
    '-U', 'folio',
    '-d', 'folio',
    '-v', 'ON_ERROR_STOP=1',
    '-At',
    '-c',
    `SELECT id || '|' || state || '|' || attempts
     FROM password_recovery_jobs
     WHERE email = '${email}';`,
  ], {
    encoding: 'utf8',
    timeout: 15000,
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim()

  const rows = output.split(/\r?\n/).filter(Boolean)

  if (rows.length !== 1) {
    throw new Error('Expected exactly one persisted test job.')
  }

  const [jobId, state, attempts] = rows[0].split('|')

  if (
    !/^[0-9a-f-]{36}$/i.test(jobId) ||
    state !== 'PENDING' ||
    attempts !== '0'
  ) {
    throw new Error('Job must be PENDING with zero attempts. Check worker-disabled startup.')
  }

  console.log('PASS Job persisted as PENDING with zero attempts')

  const folder = join('testdata', 'security-evaluation')
  mkdirSync(folder, { recursive: true })

  const file = join(folder, `recovery-restart-${runId}-checkpoint.json`)

  writeFileSync(file, JSON.stringify({
    runId,
    email,
    jobId,
    preparedAt: new Date().toISOString(),
    stateBeforeRestart: state,
    attemptsBeforeRestart: Number(attempts),
    completed: false,
    note: 'Preparation checkpoint only; restart delivery is not verified yet.',
  }, null, 2) + '\n')

  console.log(`Checkpoint saved: ${file}`)
} catch (error) {
  // Do not print database command details or response bodies.
  console.error(
    error?.code || error?.status
      ? 'Preparation failed. Check local backend and PostgreSQL.'
      : error.message
  )
  process.exitCode = 1
}