import fs from 'node:fs'
import path from 'node:path'

const trainPath = path.join(
  'data', 'external', 'cuad', 'train_separate_questions.json'
)

if (!fs.existsSync(trainPath)) {
  throw new Error(`CUAD training file missing: ${trainPath}`)
}

const dataset = JSON.parse(fs.readFileSync(trainPath, 'utf8'))
const labels = ['Cap On Liability', 'Uncapped Liability']
const MAX_EXAMPLES = 6
const MAX_PREVIEW = 900

console.log('CUAD LIABILITY TRAINING EXAMPLES')
console.log('Source: train_separate_questions.json')
console.log('Held-out test.json: not read')

for (const label of labels) {
  console.log(`\n${label}`)
  let shown = 0
  const seenTitles = new Set()

  outer:
  for (const contract of dataset.data) {
    if (seenTitles.has(contract.title)) continue

    for (const paragraph of contract.paragraphs) {
      for (const question of paragraph.qas) {
        if (!question.question.includes(`"${label}"`)) continue

        if (!Array.isArray(question.answers)
            || question.answers.length === 0) {
          continue
        }

        const answer = question.answers[0]
        const original = paragraph.context.slice(
          answer.answer_start,
          answer.answer_start + answer.text.length
        )

        if (original !== answer.text) {
          throw new Error(
            `Answer offset mismatch: ${contract.title}`
          )
        }

        const compact = answer.text.replace(/\s+/g, ' ').trim()
        const preview = compact.length > MAX_PREVIEW
          ? `${compact.slice(0, MAX_PREVIEW)}... [truncated]`
          : compact

        console.log(`- ${contract.title}`)
        console.log(`  UTF-16 offset: ${answer.answer_start}`)
        console.log(`  Labeled answer preview: ${preview}`)

        seenTitles.add(contract.title)
        shown++

        if (shown >= MAX_EXAMPLES) break outer
        break
      }
    }
  }

  console.log(`Examples shown: ${shown}`)
}