import { randomBytes, randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

const base = 'http://127.0.0.1:8081'
const runId = randomUUID()
const email = `recovery-retry-${runId}@example.test`
const cookies = new Map()
let mailpitStopped = false

const report = {
  runId,
  startedAt: new Date().toISOString(),
  method: 'Temporarily stop local Mailpit, observe persisted retry, restore SMTP',
  completed: false,
  checks: [],
}

function check(name, actual, expected) {
  const passed = actual === expected
  report.checks.push({ name, actual, expected, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)
  if (!passed) throw new Error(`Check failed: ${name}`)
}

function docker(args) {
  try {
    return execFileSync('docker', args, {
      encoding: 'utf8',
      timeout: 20000,
      stdio: ['ignore', 'pipe', 'pipe'],
    }).trim()
  } catch {
    throw new Error('Local Docker operation failed.')
  }
}

function sql(statement) {
  return docker([
    'exec', 'legal-contract-analyzer-postgres-1',
    'psql', '-U', 'folio', '-d', 'folio',
    '-v', 'ON_ERROR_STOP=1', '-At', '-c', statement,
  ])
}

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
    signal: AbortSignal.timeout(15000),
  })

  for (const cookie of response.headers.getSetCookie()) {
    const pair = cookie.split(';', 1)[0]
    const position = pair.indexOf('=')

    if (position > 0) {
      cookies.set(pair.slice(0, position), pair.slice(position + 1))
    }
  }

  await response.text()
  return response.status
}

async function post(path, body) {
  const status = await request('/api/auth/csrf')
  const token = cookies.get('XSRF-TOKEN')

  if (status !== 200 || !token) {
    throw new Error('Could not prepare secure request.')
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

function readJob() {
  const output = sql(`
    SELECT row_to_json(job)
    FROM (
      SELECT state, attempts,
             next_attempt_at > now() AS retry_scheduled,
             lease_id IS NULL AND locked_until IS NULL AS lease_released,
             finished_at IS NOT NULL AS finished
      FROM password_recovery_jobs
      WHERE email = '${email}'
    ) AS job;
  `)

  return output ? JSON.parse(output) : null
}

async function waitForJob(predicate, seconds) {
  const deadline = Date.now() + seconds * 1000

  do {
    const job = readJob()

    if (job && predicate(job)) return job

    if (job && ['FAILED', 'EXPIRED'].includes(job.state)) {
      throw new Error('Retry job reached a terminal failure state.')
    }

    await delay(500)
  } while (Date.now() < deadline)

  throw new Error('Timed out waiting for expected job state.')
}

async function mail(path) {
  const response = await fetch(`http://127.0.0.1:8025${path}`, {
    redirect: 'manual',
    signal: AbortSignal.timeout(5000),
  })

  if (!response.ok) throw new Error('Local Mailpit API unavailable.')
  return response.json()
}

try {
  check(
    'Mailpit initially running',
    docker(['inspect', '--format', '{{.State.Running}}', 'folio-mailpit-local']),
    'true'
  )

  check(
    'No other active recovery jobs before SMTP interruption',
    sql(`SELECT count(*) FROM password_recovery_jobs
         WHERE state IN ('PENDING', 'PROCESSING');`),
    '0'
  )

  const registration = await post('/api/auth/register', {
    email,
    displayName: 'Mail Retry Test',
    password: randomBytes(24).toString('hex'),
  })
  check('Register dedicated retry account', registration, 201)

  docker(['stop', '--time', '5', 'folio-mailpit-local'])
  mailpitStopped = true

  check(
    'SMTP container stopped',
    docker(['inspect', '--format', '{{.State.Running}}', 'folio-mailpit-local']),
    'false'
  )

  const accepted = await post('/api/auth/forgot-password', { email })
  check('Request accepted while SMTP unavailable', accepted, 202)

  const failedAttempt = await waitForJob(
    job => job.state === 'PENDING' && job.attempts === 1,
    20
  )

  check('Failed delivery preserved as pending', failedAttempt.state, 'PENDING')
  check('First delivery attempt recorded', failedAttempt.attempts, 1)
  check('Retry scheduled for later', failedAttempt.retry_scheduled, true)
  check('Failed attempt released worker lease', failedAttempt.lease_released, true)

  check(
    'Failed email transaction left no reset token',
    sql(`
      SELECT count(*)
      FROM password_reset_tokens token
      JOIN app_users account ON account.id = token.user_id
      WHERE account.email = '${email}';
    `),
    '0'
  )

  docker(['start', 'folio-mailpit-local'])
  mailpitStopped = false

  const completed = await waitForJob(
    job => job.state === 'COMPLETED',
    75
  )

  check('Persisted job eventually completed', completed.state, 'COMPLETED')
  check('Successful delivery occurred on second attempt', completed.attempts, 2)
  check('Completed job released worker lease', completed.lease_released, true)
  check('Completion timestamp recorded', completed.finished, true)

  const listing = await mail('/api/v1/messages?limit=100')
  const item = listing.messages?.find(message =>
    message.To?.some(recipient => recipient.Address === email)
  )

  check('Retry email received', Boolean(item), true)

  const message = await mail(
    `/api/v1/message/${encodeURIComponent(item.ID)}`
  )

  check(
    'Retry email contains reset link',
    /#reset-password=[A-Za-z0-9_-]{43}(?![A-Za-z0-9_-])/.test(message.Text ?? ''),
    true
  )

  check(
    'Successful retry persisted one reset token',
    sql(`
      SELECT count(*)
      FROM password_reset_tokens token
      JOIN app_users account ON account.id = token.user_id
      WHERE account.email = '${email}';
    `),
    '1'
  )

  report.completed = true
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
} finally {
  if (mailpitStopped) {
    try {
      docker(['start', 'folio-mailpit-local'])
      console.log('Local Mailpit restored.')
    } catch {
      report.completed = false
      process.exitCode = 1
      console.error('Restore Mailpit manually: docker start folio-mailpit-local')
    }
  }

  report.finishedAt = new Date().toISOString()
  report.total = report.checks.length
  report.passed = report.checks.filter(item => item.passed).length

  const folder = join('testdata', 'security-evaluation')
  mkdirSync(folder, { recursive: true })

  const file = join(folder, `recovery-retry-${runId}.json`)
  writeFileSync(file, JSON.stringify(report, null, 2) + '\n')

  console.log(`Report saved: ${file}`)
  console.log(
    `Checks: ${report.total}; passed: ${report.passed}; completed: ${report.completed}`
  )
}