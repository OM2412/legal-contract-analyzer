import { createHash } from 'node:crypto'
import { readFileSync, realpathSync, statSync } from 'node:fs'
import { isAbsolute, relative, resolve, sep } from 'node:path'
import process from 'node:process'

// Usage: node scripts/validate-product-labels.mjs path/to/labels.json
// Labels must be frozen before running the review service on these cases.
const root = resolve(process.cwd())
const allowedRoot = resolve(root, 'testdata/product-evaluation/heldout')
const labelPath = process.argv[2]

function fail(message) {
  throw new Error(message)
}

function object(value, where) {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    fail(`${where} must be an object`)
  }
  return value
}

function nonempty(value, where) {
  if (typeof value !== 'string' || value.trim() === '') {
    fail(`${where} must be a nonempty string`)
  }
  return value
}

function sha256(bytes) {
  return createHash('sha256').update(bytes).digest('hex')
}

function oneOf(value, choices, where) {
  if (!choices.includes(value)) {
    fail(`${where} must be one of: ${choices.join(', ')}`)
  }
}

function inside(parent, child) {
  const rel = relative(parent, child)
  return rel !== '' && rel !== '..' && !rel.startsWith(`..${sep}`)
}

function validateDocument(document, where) {
  object(document, where)
  const name = nonempty(document.path, `${where}.path`)
  if (isAbsolute(name) || !name.toLowerCase().endsWith('.txt')) {
    fail(`${where}.path must be a relative UTF-8 .txt path`)
  }
  const path = resolve(root, name)
  if (!inside(allowedRoot, path) || !inside(allowedRoot, realpathSync(path))) {
    fail(`${where}.path must stay inside testdata/product-evaluation/heldout`)
  }
  if (!statSync(path).isFile()) fail(`${where}.path must name a file`)
  const bytes = readFileSync(path)
  const decoded = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(bytes)
  if (!decoded.trim() || decoded.includes('\0')) {
    fail(`${where}.path is blank or contains a null character`)
  }
  const expected = nonempty(document.sha256, `${where}.sha256`).toLowerCase()
  if (!/^[a-f0-9]{64}$/.test(expected) || sha256(bytes) !== expected) {
    fail(`${where}.sha256 differs from the source file`)
  }
  if (!Array.isArray(document.paymentObligations)) {
    fail(`${where}.paymentObligations must be an array (use [] for none)`)
  }
  const codePoints = Array.from(decoded)
  for (const [index, label] of document.paymentObligations.entries()) {
    const field = `${where}.paymentObligations[${index}]`
    object(label, field)
    if (!Number.isSafeInteger(label.start) || !Number.isSafeInteger(label.end)
        || label.start < 0 || label.end <= label.start
        || label.end > codePoints.length) {
      fail(`${field} has invalid code-point offsets`)
    }
    const quote = nonempty(label.quote, `${field}.quote`)
    if (codePoints.slice(label.start, label.end).join('') !== quote) {
      fail(`${field}.quote does not match source text at its offsets`)
    }
    if (label.days !== null && (!Number.isSafeInteger(label.days)
        || label.days < 0)) {
      fail(`${field}.days must be a nonnegative integer or null`)
    }
    oneOf(label.dayUnit, ['CALENDAR_DAYS', 'BUSINESS_DAYS', 'UNKNOWN'],
      `${field}.dayUnit`)
    oneOf(label.trigger,
      ['INVOICE_RECEIPT', 'INVOICE_DATE', 'FINAL_ACCEPTANCE', 'OTHER', 'UNKNOWN'],
      `${field}.trigger`)
    for (const key of ['payer', 'payee', 'scope']) {
      if (label[key] !== null) nonempty(label[key], `${field}.${key}`)
    }
  }
  return document.paymentObligations.length
}

if (!labelPath) {
  fail('Pass a label JSON file: node scripts/validate-product-labels.mjs path/to/labels.json')
}
const manifestBytes = readFileSync(resolve(root, labelPath))
const manifest = object(JSON.parse(manifestBytes.toString('utf8')), 'manifest')
if (manifest.schemaVersion !== 1 || manifest.split !== 'heldout') {
  fail('manifest requires schemaVersion 1 and split "heldout"')
}
if (!Array.isArray(manifest.cases) || manifest.cases.length === 0) {
  fail('manifest.cases must contain at least one Agreement/SOW pair')
}
const ids = new Set()
let obligations = 0
for (const [index, entry] of manifest.cases.entries()) {
  const where = `cases[${index}]`
  object(entry, where)
  const id = nonempty(entry.id, `${where}.id`)
  if (ids.has(id)) fail(`Duplicate case ID: ${id}`)
  ids.add(id)
  const agreement = object(entry.agreement, `${where}.agreement`)
  const sow = object(entry.sow, `${where}.sow`)
  if (agreement.path === sow.path) fail(`${where} repeats the same source file`)
  obligations += validateDocument(agreement, `${where}.agreement`)
  obligations += validateDocument(sow, `${where}.sow`)
}
console.log(`Validated ${manifest.cases.length} pairs, ${manifest.cases.length * 2} documents, ${obligations} annotated payment obligations.`)
console.log(`Manifest SHA-256: ${sha256(manifestBytes)}`)
console.log('No review predictions or accuracy metrics were computed.')
