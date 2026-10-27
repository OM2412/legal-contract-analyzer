import { execFileSync } from 'node:child_process'
import { readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

const checkpointPath = process.argv[2]

if (!checkpointPath) {
  throw new Error('Pass the restart checkpoint JSON path.')
}

const checkpoint = JSON.parse(readFileSync(checkpointPath, 'utf8'))
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

if (
  !uuid.test(checkpoint.runId) ||
  !uuid.test(checkpoint.jobId) ||
  checkpoint.email !== `recovery-restart-${checkpoint.runId}@example.test`
) {
  throw new Error('Invalid restart-test checkpoint.')
}

const report = {
  runId: checkpoint.runId,
  startedAt: new Date().toISOString(),
  method: 'Pending database job processed after backend restart; no new reset request',
  completed: false,
  checks: [],
}

function check(name, actual, expected) {
  const passed = actual === expected
  report.checks.push({ name, actual, expected, passed })
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}: ${actual}`)

  if (!passed) throw new Error(`Check failed: ${name}`)
}

function readJob() {
  let output

  try {
    output = execFileSync('docker', [
      'exec',
      'legal-contract-analyzer-postgres-1',
      'psql',
      '-U', 'folio',
      '-d', 'folio',
      '-v', 'ON_ERROR_STOP=1',
      '-At',
      '-c',
      `SELECT row_to_json(job)
       FROM (
         SELECT email, state, attempts,
                finished_at IS NOT NULL AS finished,
                lease_id IS NULL AND locked_until IS NULL AS lease_released
         FROM password_recovery_jobs
         WHERE id = '${checkpoint.jobId}'
       ) AS job;`,
    ], {
      encoding: 'utf8',
      timeout: 5000,
      stdio: ['ignore', 'pipe', 'pipe'],
    }).trim()
  } catch {
    throw new Error('Could not read the local persisted job.')
  }

  if (!output) throw new Error('Persisted test job is missing.')
  return JSON.parse(output)
}

async function mail(path) {
  const response = await fetch(`http://127.0.0.1:8025${path}`, {
    redirect: 'manual',
    signal: AbortSignal.timeout(5000),
  })

  if (!response.ok) throw new Error('Could not read local Mailpit.')
  return response.json()
}

try {
  check('Checkpoint recorded pending job', checkpoint.stateBeforeRestart, 'PENDING')
  check('Checkpoint recorded zero attempts', checkpoint.attemptsBeforeRestart, 0)

  let job
  const deadline = Date.now() + 30000

  do {
    job = readJob()

    if (job.state === 'COMPLETED') break

    if (['FAILED', 'EXPIRED'].includes(job.state)) {
      throw new Error('Persisted job reached a terminal failure state.')
    }

    await delay(500)
  } while (Date.now() < deadline)

  check('Same persisted job completed', job.state, 'COMPLETED')
  check('Job belongs to dedicated test account', job.email === checkpoint.email, true)
  check('Job completed on first attempt', job.attempts, 1)
  check('Completion timestamp recorded', job.finished, true)
  check('Worker lease released', job.lease_released, true)

  const listing = await mail('/api/v1/messages?limit=100')
  const item = listing.messages?.find(message =>
    message.To?.some(recipient =>
      recipient.Address === checkpoint.email
    )
  )

  check('Reset email delivered after restart', Boolean(item), true)

  const message = await mail(
    `/api/v1/message/${encodeURIComponent(item.ID)}`
  )

  check(
    'Delivered email contains a reset link',
    /#reset-password=[A-Za-z0-9_-]{43}(?![A-Za-z0-9_-])/.test(message.Text ?? ''),
    true
  )

  report.completed = true
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
} finally {
  report.finishedAt = new Date().toISOString()
  report.total = report.checks.length
  report.passed = report.checks.filter(item => item.passed).length

  const file = join(
    'testdata',
    'security-evaluation',
    `recovery-restart-${checkpoint.runId}-report.json`
  )

  writeFileSync(file, JSON.stringify(report, null, 2) + '\n')

  console.log(`Report saved: ${file}`)
  console.log(
    `Checks: ${report.total}; passed: ${report.passed}; completed: ${report.completed}`
  )
}