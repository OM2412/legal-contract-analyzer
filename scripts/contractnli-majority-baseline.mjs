import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import process from 'node:process'

// Train-only per-hypothesis majority baseline evaluated on dev.
// No model tuning, text inspection, evidence prediction, or test.json read.
const dataDir = process.argv[2] ?? join(
  process.cwd(), 'data', 'external', 'contractnli'
)
const choices = ['Entailment', 'Contradiction', 'NotMentioned']

function readSplit(split) {
  const data = JSON.parse(readFileSync(join(dataDir, `${split}.json`), 'utf8'))
  if (!data || !Array.isArray(data.documents) ||
      !data.labels || typeof data.labels !== 'object') {
    throw new Error(`Invalid ${split} dataset structure`)
  }
  const keys = Object.keys(data.labels).sort()
  if (keys.length !== 17) throw new Error(`${split}: expected 17 hypotheses`)

  const rows = []
  const ids = new Set()
  for (const document of data.documents) {
    if (document.id === undefined || document.id === null ||
        ids.has(String(document.id))) {
      throw new Error(`${split}: missing or duplicate document ID`)
    }
    ids.add(String(document.id))
    const sets = document.annotation_sets
    if (!Array.isArray(sets) || sets.length !== 1 ||
        !sets[0]?.annotations) {
      throw new Error(`${split}: missing annotation set`)
    }
    const annotations = sets[0].annotations
    if (Object.keys(annotations).length !== keys.length) {
      throw new Error(`${split}: incomplete annotations`)
    }
    for (const key of keys) {
      const actual = annotations[key]?.choice
      if (!choices.includes(actual)) {
        throw new Error(`${split}: unknown choice for ${key}`)
      }
      rows.push({ key, actual })
    }
  }
  if (rows.length === 0) throw new Error(`${split}: no annotations`)
  return { data, keys, rows, ids, documents: data.documents.length }
}

function ratio(numerator, denominator) {
  return denominator === 0 ? 0 : numerator / denominator
}

const train = readSplit('train')
const dev = readSplit('dev')
if (train.keys.join('|') !== dev.keys.join('|')) {
  throw new Error('Train/dev hypothesis keys differ')
}
for (const key of train.keys) {
  if (train.data.labels[key].hypothesis !== dev.data.labels[key].hypothesis) {
    throw new Error(`Train/dev hypothesis differs for ${key}`)
  }
}
for (const id of train.ids) {
  if (dev.ids.has(id)) throw new Error(`Train/dev document ID overlap: ${id}`)
}

const trainCounts = Object.fromEntries(
  train.keys.map((key) => [
    key,
    Object.fromEntries(choices.map((choice) => [choice, 0])),
  ])
)
for (const row of train.rows) trainCounts[row.key][row.actual]++

// In a tie, use the fixed order in `choices`; dev labels never set the rule.
const predictionByHypothesis = Object.fromEntries(
  train.keys.map((key) => [
    key,
    choices.reduce((best, choice) =>
      trainCounts[key][choice] > trainCounts[key][best] ? choice : best
    ),
  ])
)

const matrix = Object.fromEntries(choices.map((actual) => [
  actual,
  Object.fromEntries(choices.map((predicted) => [predicted, 0])),
]))
const correctByHypothesis = Object.fromEntries(
  train.keys.map((key) => [key, 0])
)
for (const row of dev.rows) {
  const predicted = predictionByHypothesis[row.key]
  matrix[row.actual][predicted]++
  if (predicted === row.actual) correctByHypothesis[row.key]++
}

const metrics = choices.map((choice) => {
  const tp = matrix[choice][choice]
  const actual = choices.reduce((sum, predicted) =>
    sum + matrix[choice][predicted], 0)
  const predicted = choices.reduce((sum, label) =>
    sum + matrix[label][choice], 0)
  const precision = ratio(tp, predicted)
  const recall = ratio(tp, actual)
  const f1 = ratio(2 * precision * recall, precision + recall)
  return { choice, actual, predicted, precision, recall, f1 }
})

const correct = choices.reduce((sum, choice) =>
  sum + matrix[choice][choice], 0)
const macroF1 = metrics.reduce((sum, item) => sum + item.f1, 0) /
  choices.length
const format = (value) => value.toFixed(3)

console.log('CONTRACTNLI PER-HYPOTHESIS MAJORITY BASELINE')
console.log('Source: train.json -> dev.json')
console.log('test.json: not opened')
console.log(`Train documents/instances: ${train.documents}/${train.rows.length}`)
console.log(`Dev documents/instances: ${dev.documents}/${dev.rows.length}`)
console.log('Tie rule: Entailment, then Contradiction, then NotMentioned')
console.log('\nCONFUSION MATRIX (rows actual, columns predicted)')
console.log(`Actual \\ Predicted | ${choices.join(' | ')}`)
for (const actual of choices) {
  console.log(`${actual} | ${choices.map((pred) => matrix[actual][pred]).join(' | ')}`)
}
console.log('\nPER-CLASS METRICS')
for (const item of metrics) {
  console.log(`${item.choice} | support ${item.actual} | predicted ${item.predicted}` +
    ` | P ${format(item.precision)} | R ${format(item.recall)} | F1 ${format(item.f1)}`)
}
console.log(`Accuracy: ${correct}/${dev.rows.length} = ${format(ratio(correct, dev.rows.length))}`)
console.log(`Macro F1: ${format(macroF1)}`)
console.log('\nPER-HYPOTHESIS TRAIN MAJORITY / DEV CORRECT')
for (const key of train.keys) {
  console.log(`${key} | ${predictionByHypothesis[key]}` +
    ` | ${correctByHypothesis[key]}/${dev.documents}`)
}
console.log('\nNo evidence spans were predicted; evidence quality was not scored.')
console.log('This NDA inference baseline is not payment review or legal risk accuracy.')
