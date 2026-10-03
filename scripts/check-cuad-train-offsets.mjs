import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const file = join(
  process.cwd(),
  'data/external/cuad/train_separate_questions.json'
)

const dataset = JSON.parse(readFileSync(file, 'utf8'))

const selectedLabels = [
  'Termination For Convenience',
  'Cap On Liability',
  'Uncapped Liability',
  'Ip Ownership Assignment',
  'Post-Termination Services',
  'Notice Period To Terminate Renewal',
]

const counts = new Map(
  selectedLabels.map((label) => [
    label,
    {
      questions: 0,
      answeredQuestions: 0,
      answerSpans: 0,
      exactUtf16: 0,
      exactCodePoints: 0,
      invalid: 0,
      mismatched: 0,
    },
  ])
)

const mismatchExamples = []

function labelFromQuestion(question) {
  return question?.match(/related to "([^"]+)"/)?.[1] ?? null
}

for (const document of dataset.data ?? []) {
  for (const paragraph of document.paragraphs ?? []) {
    const context = paragraph.context

    if (typeof context !== 'string') {
      throw new Error('CUAD paragraph has no text context')
    }

    for (const qa of paragraph.qas ?? []) {
      const label = labelFromQuestion(qa.question)
      const count = counts.get(label)

      if (!count) continue

      count.questions++

      const answers = Array.isArray(qa.answers)
        ? qa.answers
        : []

      if (answers.length > 0) {
        count.answeredQuestions++
      }

      for (const answer of answers) {
        count.answerSpans++

        const start = answer.answer_start
        const quote = answer.text

        if (!Number.isSafeInteger(start)
            || start < 0
            || typeof quote !== 'string'
            || quote.length === 0) {
          count.invalid++
          continue
        }

        if (context.slice(start, start + quote.length) === quote) {
          count.exactUtf16++
          continue
        }

        // Python-style character offsets can differ from JS UTF-16
        // offsets when earlier text contains non-BMP characters.
        const characters = Array.from(context)
        const quoteLength = Array.from(quote).length
        const byCodePoints = characters
          .slice(start, start + quoteLength)
          .join('')

        if (byCodePoints === quote) {
          count.exactCodePoints++
          continue
        }

        count.mismatched++

        if (mismatchExamples.length < 5) {
          mismatchExamples.push({
            label,
            questionId: qa.id ?? '(no ID)',
            offset: start,
            quoteLength,
          })
        }
      }
    }
  }
}

console.log('CUAD TRAIN OFFSET CHECK')
console.log('Source: train_separate_questions.json')
console.log('Held-out test.json: not read\n')

for (const [label, result] of counts) {
  console.log(label)
  console.log(`  question instances: ${result.questions}`)
  console.log(`  answered questions: ${result.answeredQuestions}`)
  console.log(`  answer spans: ${result.answerSpans}`)
  console.log(`  exact UTF-16 offsets: ${result.exactUtf16}`)
  console.log(
    `  exact code-point offsets: ${result.exactCodePoints}`
  )
  console.log(`  invalid answer records: ${result.invalid}`)
  console.log(`  mismatched spans: ${result.mismatched}`)
}

if (mismatchExamples.length > 0) {
  console.log('\nFIRST MISMATCH METADATA (no contract text):')
  console.log(JSON.stringify(mismatchExamples, null, 2))
}