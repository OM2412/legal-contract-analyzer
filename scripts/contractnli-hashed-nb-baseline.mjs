import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import process from 'node:process'

// Per-hypothesis Bernoulli Naive Bayes over hashed document words.
// Fit on train.json, evaluate on dev.json; test.json is never opened.
const dataDir = process.argv[2] ?? join(
  process.cwd(), 'data', 'external', 'contractnli'
)
const choices = ['Entailment', 'Contradiction', 'NotMentioned']
const bucketCount = 4096
const smoothing = 1

function readSplit(name) {
  const data = JSON.parse(
    readFileSync(join(dataDir, `${name}.json`), 'utf8')
  )

  if (!Array.isArray(data?.documents) || !data.labels) {
    throw new Error(`${name}: invalid ContractNLI structure`)
  }

  const keys = Object.keys(data.labels).sort()

  if (keys.length !== 17) {
    throw new Error(`${name}: expected 17 hypotheses`)
  }

  const ids = new Set()

  for (const doc of data.documents) {
    if (
      doc.id === undefined ||
      ids.has(String(doc.id)) ||
      typeof doc.text !== 'string' ||
      !doc.text.trim() ||
      !Array.isArray(doc.annotation_sets) ||
      doc.annotation_sets.length !== 1
    ) {
      throw new Error(`${name}: invalid or duplicate document`)
    }

    ids.add(String(doc.id))

    const annotations = doc.annotation_sets[0]?.annotations

    if (
      !annotations ||
      Object.keys(annotations).length !== keys.length ||
      keys.some(
        (key) => !choices.includes(annotations[key]?.choice)
      )
    ) {
      throw new Error(`${name}: incomplete hypothesis choices`)
    }
  }

  return { data, keys, ids }
}

function hashWord(word) {
  // FNV-1a: deterministic across Node versions and operating systems.
  let hash = 2166136261

  for (let i = 0; i < word.length; i++) {
    hash ^= word.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }

  return (hash >>> 0) % bucketCount
}

function features(text) {
  const words = text.toLowerCase().match(/[a-z]{3,}/g) ?? []
  const buckets = new Set(words.map(hashWord))

  if (buckets.size === 0) {
    throw new Error('Document has no word features')
  }

  return buckets
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
  if (
    train.data.labels[key].hypothesis !==
    dev.data.labels[key].hypothesis
  ) {
    throw new Error(
      `Train/dev hypothesis wording differs: ${key}`
    )
  }
}

for (const id of train.ids) {
  if (dev.ids.has(id)) {
    throw new Error(`Train/dev document ID overlap: ${id}`)
  }
}

const models = new Map(
  train.keys.map((key) => [
    key,
    {
      classCounts: new Uint32Array(choices.length),
      featureCounts: choices.map(
        () => new Uint32Array(bucketCount)
      ),
    },
  ])
)

for (const document of train.data.documents) {
  const present = features(document.text)
  const annotations = document.annotation_sets[0].annotations

  for (const key of train.keys) {
    const model = models.get(key)
    const labelIndex = choices.indexOf(annotations[key].choice)

    model.classCounts[labelIndex]++

    for (const bucket of present) {
      model.featureCounts[labelIndex][bucket]++
    }
  }
}

// Precompute absent-feature scores, then add present-feature deltas.
for (const model of models.values()) {
  model.baseScores = new Float64Array(choices.length)
  model.deltas = choices.map(
    () => new Float64Array(bucketCount)
  )

  for (
    let classIndex = 0;
    classIndex < choices.length;
    classIndex++
  ) {
    const classSize = model.classCounts[classIndex]

    let base = Math.log(
      (classSize + smoothing) /
      (
        train.data.documents.length +
        choices.length * smoothing
      )
    )

    for (let bucket = 0; bucket < bucketCount; bucket++) {
      const probability = (
        model.featureCounts[classIndex][bucket] +
        smoothing
      ) / (classSize + 2 * smoothing)

      base += Math.log1p(-probability)

      model.deltas[classIndex][bucket] =
        Math.log(probability) -
        Math.log1p(-probability)
    }

    model.baseScores[classIndex] = base
  }
}

function predict(model, present) {
  let bestIndex = 0
  let bestScore = -Infinity

  for (
    let classIndex = 0;
    classIndex < choices.length;
    classIndex++
  ) {
    let score = model.baseScores[classIndex]

    for (const bucket of present) {
      score += model.deltas[classIndex][bucket]
    }

    if (score > bestScore) {
      bestIndex = classIndex
      bestScore = score
    }
  }

  return choices[bestIndex]
}

const matrix = Object.fromEntries(
  choices.map((actual) => [
    actual,
    Object.fromEntries(
      choices.map((predicted) => [predicted, 0])
    ),
  ])
)

const correctByHypothesis = Object.fromEntries(
  train.keys.map((key) => [key, 0])
)

for (const document of dev.data.documents) {
  const present = features(document.text)
  const annotations = document.annotation_sets[0].annotations

  for (const key of train.keys) {
    const actual = annotations[key].choice
    const predicted = predict(models.get(key), present)

    matrix[actual][predicted]++

    if (actual === predicted) {
      correctByHypothesis[key]++
    }
  }
}

let correct = 0
let macroF1 = 0

const metrics = choices.map((choice) => {
  const truePositive = matrix[choice][choice]
  correct += truePositive

  const support = choices.reduce(
    (sum, predicted) =>
      sum + matrix[choice][predicted],
    0
  )

  const predictedCount = choices.reduce(
    (sum, actual) =>
      sum + matrix[actual][choice],
    0
  )

  const precision = ratio(truePositive, predictedCount)
  const recall = ratio(truePositive, support)
  const f1 = ratio(
    2 * precision * recall,
    precision + recall
  )

  macroF1 += f1 / choices.length

  return {
    choice,
    support,
    predictedCount,
    precision,
    recall,
    f1,
  }
})

const format = (value) => value.toFixed(3)
const total = dev.data.documents.length * train.keys.length

console.log(
  'CONTRACTNLI HASHED BERNOULLI NB DEV BASELINE'
)
console.log(
  'Fit: train.json | Evaluation: dev.json | test.json: not opened'
)
console.log(
  `Documents: train ${train.data.documents.length}, ` +
  `dev ${dev.data.documents.length}`
)
console.log(
  `Hypotheses: ${train.keys.length} | Dev instances: ${total}`
)
console.log(
  `Features: ${bucketCount} hashed word-presence buckets, ` +
  `Laplace alpha ${smoothing}`
)

console.log(
  'CONFUSION MATRIX (rows actual, columns predicted)'
)
console.log(
  `Actual \\ Predicted | ${choices.join(' | ')}`
)

for (const actual of choices) {
  console.log(
    `${actual} | ` +
    choices.map(
      (predicted) => matrix[actual][predicted]
    ).join(' | ')
  )
}

console.log('PER-CLASS METRICS')

for (const item of metrics) {
  console.log(
    `${item.choice} | support ${item.support}` +
    ` | predicted ${item.predictedCount}` +
    ` | P ${format(item.precision)}` +
    ` | R ${format(item.recall)}` +
    ` | F1 ${format(item.f1)}`
  )
}

console.log(
  `Accuracy: ${correct}/${total} = ` +
  format(ratio(correct, total))
)
console.log(`Macro F1: ${format(macroF1)}`)

console.log('PER-HYPOTHESIS DEV CORRECT')

for (const key of train.keys) {
  console.log(
    `${key} | ` +
    `${correctByHypothesis[key]}/` +
    dev.data.documents.length
  )
}

console.log(
  'No evidence spans were predicted; evidence quality was not scored.'
)
console.log(
  'This NDA inference result is not payment review or legal risk accuracy.'
)