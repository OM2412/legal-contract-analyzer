import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import process from 'node:process'

// Reads only the released train and development splits.
// The test split remains untouched until the baseline is frozen.
const dataDir = process.argv[2] ?? join(
  process.cwd(), 'data', 'external', 'contractnli'
)
const choices = ['Entailment', 'Contradiction', 'NotMentioned']

function requireObject(value, where) {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`Expected object at ${where}`)
  }
  return value
}

function requireArray(value, where) {
  if (!Array.isArray(value)) throw new Error(`Expected array at ${where}`)
  return value
}

function readSplit(name) {
  const dataset = requireObject(
    JSON.parse(readFileSync(join(dataDir, `${name}.json`), 'utf8')),
    name
  )
  const labels = requireObject(dataset.labels, `${name}.labels`)
  const keys = Object.keys(labels).sort()
  if (keys.length !== 17) {
    throw new Error(`${name}: expected 17 hypothesis keys, got ${keys.length}`)
  }

  for (const key of keys) {
    const label = requireObject(labels[key], `${name}.labels.${key}`)
    if (typeof label.hypothesis !== 'string' || !label.hypothesis.trim()) {
      throw new Error(`${name}: missing hypothesis text for ${key}`)
    }
  }

  const counts = Object.fromEntries(
    keys.map((key) => [key, Object.fromEntries(choices.map((c) => [c, 0]))])
  )
  const documents = requireArray(dataset.documents, `${name}.documents`)
  const ids = new Set()
  let evidenceReferences = 0
  let positiveWithoutEvidence = 0
  let candidateSpans = 0

  for (const document of documents) {
    const item = requireObject(document, `${name}.document`)
    if (typeof item.text !== 'string' || !item.text.trim()) {
      throw new Error(`${name}: document has no text`)
    }
    if (item.id === undefined || item.id === null) {
      throw new Error(`${name}: document has no ID`)
    }
    const id = String(item.id)
    if (ids.has(id)) throw new Error(`${name}: duplicate document ID ${id}`)
    ids.add(id)

    const spans = requireArray(item.spans, `${name}.${id}.spans`)
    candidateSpans += spans.length
    for (const pair of spans) {
      if (!Array.isArray(pair) || pair.length !== 2 ||
          !Number.isSafeInteger(pair[0]) || !Number.isSafeInteger(pair[1]) ||
          pair[0] < 0 || pair[1] <= pair[0] || pair[1] > item.text.length) {
        throw new Error(`${name}: invalid candidate span in ${id}`)
      }
    }

    const annotationSets = requireArray(
      item.annotation_sets, `${name}.${id}.annotation_sets`
    )
    if (annotationSets.length !== 1) {
      throw new Error(`${name}: expected one annotation set for ${id}`)
    }
    const annotations = requireObject(
      annotationSets[0].annotations, `${name}.${id}.annotations`
    )
    if (Object.keys(annotations).length !== keys.length ||
        keys.some((key) => !(key in annotations))) {
      throw new Error(`${name}: incomplete hypothesis annotations in ${id}`)
    }

    for (const key of keys) {
      const annotation = requireObject(
        annotations[key], `${name}.${id}.${key}`
      )
      if (!choices.includes(annotation.choice)) {
        throw new Error(`${name}: unknown choice for ${id}/${key}`)
      }
      const refs = requireArray(annotation.spans, `${name}.${id}.${key}.spans`)
      for (const ref of refs) {
        if (!Number.isSafeInteger(ref) || ref < 0 || ref >= spans.length) {
          throw new Error(`${name}: invalid evidence index for ${id}/${key}`)
        }
      }
      if (annotation.choice === 'NotMentioned' && refs.length !== 0) {
        throw new Error(`${name}: NotMentioned has evidence in ${id}/${key}`)
      }
      if (annotation.choice !== 'NotMentioned' && refs.length === 0) {
        positiveWithoutEvidence++
      }
      evidenceReferences += refs.length
      counts[key][annotation.choice]++
    }
  }

  return {
    name, labels, keys, counts, ids,
    documents: documents.length, candidateSpans,
    evidenceReferences, positiveWithoutEvidence,
  }
}

const train = readSplit('train')
const dev = readSplit('dev')
if (train.keys.join('|') !== dev.keys.join('|')) {
  throw new Error('Train/dev hypothesis keys differ')
}
for (const key of train.keys) {
  if (train.labels[key].hypothesis !== dev.labels[key].hypothesis) {
    throw new Error(`Train/dev hypothesis wording differs for ${key}`)
  }
}
for (const id of train.ids) {
  if (dev.ids.has(id)) throw new Error(`Train/dev document ID overlap: ${id}`)
}

console.log('CONTRACTNLI TRAIN/DEV INVENTORY')
console.log('test.json: not opened')
for (const split of [train, dev]) {
  const totals = Object.fromEntries(choices.map((choice) => [
    choice,
    split.keys.reduce((sum, key) => sum + split.counts[key][choice], 0),
  ]))
  console.log(`\n${split.name.toUpperCase()}`)
  console.log(`Documents: ${split.documents}`)
  console.log(`Hypothesis instances: ${split.documents * split.keys.length}`)
  console.log(`Candidate spans: ${split.candidateSpans}`)
  console.log(`Evidence index references: ${split.evidenceReferences}`)
  console.log(`Entailment: ${totals.Entailment}`)
  console.log(`Contradiction: ${totals.Contradiction}`)
  console.log(`NotMentioned: ${totals.NotMentioned}`)
  console.log(`Entailment/Contradiction without evidence: ${split.positiveWithoutEvidence}`)
}
console.log('\nPER-HYPOTHESIS COUNTS (E / C / N)')
for (const key of train.keys) {
  const shortName = train.labels[key].short_description ?? key
  const format = (split) => choices.map((choice) => split.counts[key][choice]).join(' / ')
  console.log(`${key} | ${shortName} | train ${format(train)} | dev ${format(dev)}`)
}
console.log('\nNo predictions or accuracy metrics were computed.')
