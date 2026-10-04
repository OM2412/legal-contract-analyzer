import { createHash } from 'node:crypto'
import { readFileSync, realpathSync, statSync } from 'node:fs'
import { isAbsolute, relative, resolve, sep } from 'node:path'
import process from 'node:process'

// Example:
// node scripts/locate-product-quote.mjs testdata/product-evaluation/heldout/PAIR-001-agreement.txt "Exact clause to annotate."
const [filename, quote] = process.argv.slice(2)
if (!filename || typeof quote !== 'string' || !quote.trim()) {
  throw new Error('Usage: node scripts/locate-product-quote.mjs relative-source.txt "Exact clause"')
}

const root = resolve(process.cwd())
const allowed = resolve(root, 'testdata/product-evaluation/heldout')
const path = resolve(root, filename)
const inside = (parent, child) => {
  const rel = relative(parent, child)
  return rel !== '' && rel !== '..' && !rel.startsWith(`..${sep}`)
}
if (isAbsolute(filename) || !filename.toLowerCase().endsWith('.txt')
    || !inside(allowed, path) || !inside(allowed, realpathSync(path))
    || !statSync(path).isFile()) {
  throw new Error('Source must be a .txt file inside testdata/product-evaluation/heldout')
}

const bytes = readFileSync(path)
const text = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(bytes)
const codePoints = Array.from(text)
const quotePoints = Array.from(quote)
const matches = []
let from = 0
while (from <= text.length - quote.length) {
  const utf16Start = text.indexOf(quote, from)
  if (utf16Start < 0) break
  const start = Array.from(text.slice(0, utf16Start)).length
  const end = start + quotePoints.length
  if (codePoints.slice(start, end).join('') === quote) {
    matches.push({ start, end })
  }
  from = utf16Start + 1
}

console.log(JSON.stringify({
  path: filename,
  sha256: createHash('sha256').update(bytes).digest('hex'),
  quote,
  matches,
  note: matches.length === 0
    ? 'Quote was not found verbatim. Copy it directly from the source.'
    : matches.length > 1
      ? 'Choose the intended occurrence by its offsets.'
      : 'Offsets count Unicode code points; end is exclusive.',
}, null, 2))
if (matches.length === 0) process.exitCode = 1
