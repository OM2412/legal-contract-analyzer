// Prepare blind, private annotation packets. Never runs Folio or sends text to AI.
// node scripts/prepare-real-evaluation.mjs status
// node scripts/prepare-real-evaluation.mjs prepare SEC-... data/external/.../msa.txt data/external/.../sow.txt
import { createHash } from 'node:crypto'
import { readFile, realpath, mkdir, writeFile } from 'node:fs/promises'
import { resolve, relative, sep, join } from 'node:path'

const workspace = process.cwd()
const externalRoot = resolve(workspace, 'data/external')
const manifest = JSON.parse(await readFile('testdata/product-evaluation/real-candidates.json', 'utf8'))
const [command, id, agreementArg, sowArg] = process.argv.slice(2)

if (command === 'status') {
  for (const item of manifest.sources) {
    console.log(`${item.id}: pairing=${item.pairingVerified ? 'verified' : 'pending'}, source=${item.completeSourceTextPrepared ? 'ready' : 'pending'}, reuse=${item.reusePermissionVerified ? 'reviewed' : 'pending'}, independent-gold=${item.independentLabelsFrozen ? 'frozen' : 'pending'}`)
  }
  console.log('No real-world accuracy score is available until source rights and independent annotation are complete.')
  process.exit(0)
}

if (command !== 'prepare' || !id || !agreementArg || !sowArg) {
  console.error('Usage: node scripts/prepare-real-evaluation.mjs status | prepare <candidate-id> <private-msa.txt> <private-sow.txt>')
  process.exit(2)
}

const candidate = manifest.sources.find(source => source.id === id)
if (!candidate) throw new Error('Unknown candidate ID')
if (process.env.FOLIO_REUSE_REVIEWED !== 'yes') {
  throw new Error('Review source reuse rights first; then set FOLIO_REUSE_REVIEWED=yes for private preparation')
}

const inside = (parent, child) => {
  const r = relative(parent, child)
  return r !== '' && r !== '..' && !r.startsWith(`..${sep}`)
}

async function sourceInfo(input) {
  const absolute = await realpath(resolve(workspace, input))
  if (!inside(externalRoot, absolute) || !absolute.endsWith('.txt')) {
    throw new Error('Source files must be existing private .txt files under data/external/')
  }
  const bytes = await readFile(absolute)
  if (bytes.length > 1024 * 1024) throw new Error('Source exceeds 1 MiB text upload limit')
  const text = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(bytes)
  if (!text.trim() || text.includes('\0')) throw new Error('Invalid UTF-8 text source')
  return {
    path: relative(workspace, absolute),
    sha256: createHash('sha256').update(bytes).digest('hex'),
    codePoints: Array.from(text).length,
    paymentObligations: null, // Null means UNANNOTATED; never interpret as a negative example.
  }
}

const agreement = await sourceInfo(agreementArg)
const sow = await sourceInfo(sowArg)
if (agreement.path === sow.path) throw new Error('Agreement and SOW must be different files')

const directory = join(externalRoot, 'folio-evaluation', id)
await mkdir(directory, { recursive: true })
for (const reviewer of ['A', 'B']) {
  const packet = {
    schemaVersion: 1,
    status: 'AWAITING_INDEPENDENT_ANNOTATION',
    caseId: id,
    reviewer,
    sourceUrl: candidate.url,
    agreement,
    sow,
    expectedReviewStatus: null,
    notes: 'Annotate without seeing Folio predictions. Replace null paymentObligations with reviewed arrays, including [] only for confirmed negatives.',
  }
  await writeFile(join(directory, `reviewer-${reviewer.toLowerCase()}.draft.json`),
    `${JSON.stringify(packet, null, 2)}\n`, { flag: 'wx' })
}
console.log(`Prepared private independent-review drafts in ${relative(workspace, directory)}`)
console.log('No source text copied or sent to an AI provider. No annotations or accuracy metrics invented.')
