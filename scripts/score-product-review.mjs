import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { spawnSync } from 'node:child_process'

// Usage: node scripts/score-product-review.mjs labels.json predictions-dir
// predictions-dir contains PAIR-ID.json responses saved from /api/review.
const [manifestName, predictionsName] = process.argv.slice(2)
if (!manifestName || !predictionsName) {
  throw new Error('Usage: node scripts/score-product-review.mjs labels.json predictions-dir')
}

const root = process.cwd()
const validation = spawnSync(process.execPath,
  [resolve(root, 'scripts/validate-product-labels.mjs'), manifestName],
  { cwd: root, encoding: 'utf8' })
if (validation.status !== 0) {
  throw new Error(`Label validation failed:\n${validation.stderr || validation.stdout}`)
}

const manifestBytes = readFileSync(resolve(root, manifestName))
const manifest = JSON.parse(manifestBytes.toString('utf8'))
const hash = (bytes) => createHash('sha256').update(bytes).digest('hex')
const metrics = {
  pairs: 0, documents: 0, goldObligations: 0, predictedObligations: 0,
  matched: 0, falsePositives: 0, misses: 0, exactEvidence: 0,
  fieldsChecked: 0, fieldErrors: {}, fieldsUnavailable: 0,
  statuses: {}, statusLabelsChecked: 0, statusErrors: 0,
}
const fieldNames = ['days', 'dayUnit', 'trigger', 'payer', 'payee', 'scope']
const problems = []

function fail(message) { throw new Error(message) }
function ratio(number, denominator) {
  return denominator === 0 ? null : Number((number / denominator).toFixed(3))
}
function sourceInfo(document) {
  const bytes = readFileSync(resolve(root, document.path))
  const text = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(bytes)
  return { codePoints: Array.from(text), version: `sha256:${hash(bytes)}` }
}
function checkEvidence(span, info, role, id) {
  if (!span || span.documentId !== `${role}-upload`
      || span.documentVersion !== info.version
      || !Number.isSafeInteger(span.start) || !Number.isSafeInteger(span.end)
      || span.start < 0 || span.end <= span.start
      || span.end > info.codePoints.length
      || info.codePoints.slice(span.start, span.end).join('') !== span.quote) {
    fail(`${id} ${role}: response contains ungrounded evidence`)
  }
}
function overlap(a, b) {
  const common = Math.max(0, Math.min(a.end, b.end) - Math.max(a.start, b.start))
  return common >= Math.ceil(Math.min(a.end - a.start, b.end - b.start) / 2)
}
function matchOneToOne(gold, predicted) {
  const owner = Array(predicted.length).fill(-1)
  function assign(goldIndex, visited) {
    const options = predicted.map((span, index) => ({
      index, common: Math.max(0,
        Math.min(gold[goldIndex].end, span.end)
          - Math.max(gold[goldIndex].start, span.start)),
    })).filter(({ index }) => overlap(gold[goldIndex], predicted[index]))
      .sort((a, b) => b.common - a.common)
    for (const { index } of options) {
      if (visited.has(index)) continue
      visited.add(index)
      if (owner[index] < 0 || assign(owner[index], visited)) {
        owner[index] = goldIndex
        return true
      }
    }
    return false
  }
  let matches = 0
  for (let i = 0; i < gold.length; i++) {
    if (assign(i, new Set())) matches++
  }
  return owner.map((goldIndex, predictedIndex) => ({ goldIndex, predictedIndex }))
    .filter(({ goldIndex }) => goldIndex >= 0)
}
function comparable(value) {
  return typeof value === 'string'
    ? value.trim().replace(/\s+/g, ' ').toLowerCase() : value
}
function scoreDocument(id, role, annotated, response) {
  const gold = annotated.paymentObligations
  const info = sourceInfo(annotated)
  const responseVersion = response[`${role}Version`]
  if (responseVersion !== info.version) {
    fail(`${id} ${role}: response version does not match the frozen source`)
  }
  const predicted = response[`${role}Matches`]
  if (!Array.isArray(predicted)) fail(`${id} ${role}: missing matches array`)
  for (const span of predicted) checkEvidence(span, info, role, id)
  const pairs = matchOneToOne(gold, predicted)
  metrics.documents++
  metrics.goldObligations += gold.length
  metrics.predictedObligations += predicted.length
  metrics.matched += pairs.length
  metrics.falsePositives += predicted.length - pairs.length
  metrics.misses += gold.length - pairs.length
  metrics.exactEvidence += pairs.filter(({ goldIndex, predictedIndex }) =>
    gold[goldIndex].quote === predicted[predictedIndex].quote).length
  if (gold.length !== predicted.length || pairs.length !== gold.length) {
    problems.push(`${id} ${role}: gold ${gold.length}, predicted ${predicted.length}, matched ${pairs.length}`)
  }

  // The API exposes structured fields only for one selected term.
  if (gold.length === 1 && predicted.length === 1 && pairs.length === 1) {
    const term = response[`${role}Term`]
    if (!term) {
      metrics.fieldsUnavailable++
      return
    }
    checkEvidence(term.evidence, info, role, id)
    if (term.evidence.start !== predicted[0].start
        || term.evidence.end !== predicted[0].end) {
      fail(`${id} ${role}: structured term and matches disagree`)
    }
    for (const field of fieldNames) {
      if (gold[0][field] === null) continue
      metrics.fieldsChecked++
      if (comparable(gold[0][field]) !== comparable(term[field])) {
        metrics.fieldErrors[field] = (metrics.fieldErrors[field] ?? 0) + 1
        problems.push(`${id} ${role}: ${field} expected ${JSON.stringify(gold[0][field])}, got ${JSON.stringify(term[field])}`)
      }
    }
  }
}

for (const entry of manifest.cases) {
  if (!/^[A-Za-z0-9_-]+$/.test(entry.id)) {
    fail(`Case ID cannot be used as a prediction filename: ${entry.id}`)
  }
  const path = resolve(root, predictionsName, `${entry.id}.json`)
  const response = JSON.parse(readFileSync(path, 'utf8'))
  if (!response || typeof response !== 'object') fail(`${entry.id}: invalid response`)
  scoreDocument(entry.id, 'agreement', entry.agreement, response)
  scoreDocument(entry.id, 'sow', entry.sow, response)
  metrics.pairs++
  if (typeof response.reviewStatus !== 'string') fail(`${entry.id}: missing reviewStatus`)
  metrics.statuses[response.reviewStatus] = (metrics.statuses[response.reviewStatus] ?? 0) + 1
  if (entry.expectedReviewStatus !== undefined) {
    metrics.statusLabelsChecked++
    if (entry.expectedReviewStatus !== response.reviewStatus) {
      metrics.statusErrors++
      problems.push(`${entry.id}: review status expected ${entry.expectedReviewStatus}, got ${response.reviewStatus}`)
    }
  }
}

console.log(JSON.stringify({
  manifestSha256: hash(manifestBytes),
  ...metrics,
  precision: ratio(metrics.matched, metrics.predictedObligations),
  recall: ratio(metrics.matched, metrics.goldObligations),
  f1: ratio(2 * metrics.matched,
    metrics.predictedObligations + metrics.goldObligations),
  note: 'One-to-one candidate evidence overlap (at least half the shorter span), not legal classification accuracy. Field checks cover aligned single-term documents only.',
  problems,
}, null, 2))
