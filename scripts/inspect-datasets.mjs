import { readFileSync } from 'node:fs'
import { join } from 'node:path'

function readJson(relativePath) {
  return JSON.parse(
    readFileSync(join(process.cwd(), relativePath), 'utf8')
  )
}

function inspectCuad() {
  const dataset = readJson(
    'data/external/cuad/train_separate_questions.json'
  )

  const documents = dataset.data ?? []
  const titles = new Set()
  const categories = new Map()
  let paragraphs = 0
  let questions = 0

  for (const document of documents) {
    if (document.title) titles.add(document.title)

    for (const paragraph of document.paragraphs ?? []) {
      paragraphs++

      for (const qa of paragraph.qas ?? []) {
        questions++

        const name = qa.question ?? '(missing question)'
        const current = categories.get(name) ?? {
          questions: 0,
          withAnswer: 0,
        }

        current.questions++

        if (Array.isArray(qa.answers)
            && qa.answers.length > 0) {
          current.withAnswer++
        }

        categories.set(name, current)
      }
    }
  }

  console.log('\nCUAD TRAIN INVENTORY')
  console.log('Top-level keys:', Object.keys(dataset).join(', '))
  console.log('Data entries:', documents.length)
  console.log('Distinct titles:', titles.size)
  console.log('Paragraphs:', paragraphs)
  console.log('Question instances:', questions)
  console.log('Distinct question labels:', categories.size)

  for (const [name, counts] of [...categories]
    .sort(([left], [right]) => left.localeCompare(right))) {
    console.log(
      `${name} | instances=${counts.questions}` +
      ` | answered=${counts.withAnswer}`
    )
  }
}

function inspectContractNli() {
  const dataset = readJson(
    'data/external/contractnli/train.json'
  )

  const documents = dataset.documents ?? []
  const labels = dataset.labels ?? {}
  const choices = new Map()

  for (const document of documents) {
    const annotations =
      document.annotation_sets?.[0]?.annotations ?? {}

    for (const annotation of Object.values(annotations)) {
      const choice = annotation.choice ?? '(missing)'
      choices.set(choice, (choices.get(choice) ?? 0) + 1)
    }
  }

  console.log('\nCONTRACTNLI TRAIN INVENTORY')
  console.log('Top-level keys:', Object.keys(dataset).join(', '))
  console.log('Documents:', documents.length)
  console.log('Hypothesis labels:', Object.keys(labels).length)
  console.log('Choice counts:')

  for (const [choice, count] of choices) {
    console.log(`  ${choice}: ${count}`)
  }

  console.log('Hypothesis IDs and short descriptions:')

  for (const [id, label] of Object.entries(labels)) {
    console.log(
      `  ${id}: ${label.short_description ?? '(no description)'}`
    )
  }
}

inspectCuad()
inspectContractNli()