import { readFileSync } from 'node:fs'

const path = process.argv[2] || 'testdata/product-evaluation/real-candidates.json'
const manifest = JSON.parse(readFileSync(path, 'utf8'))
if (manifest.schemaVersion !== 1 || manifest.status !== 'SOURCE_DISCOVERY_ONLY_NOT_LABELED') {
  throw new Error('This manifest must be identified as unlabeled source discovery')
}
if (!Array.isArray(manifest.sources) || manifest.sources.length === 0) {
  throw new Error('A nonempty candidate source list is required')
}
const ids = new Set()
for (const source of manifest.sources) {
  if (typeof source.id !== 'string' || !/^[A-Z0-9-]+$/.test(source.id)
      || ids.has(source.id)) throw new Error('Missing or duplicate source ID')
  ids.add(source.id)
  const url = new URL(source.url)
  if (url.protocol !== 'https:' || url.hostname !== 'www.sec.gov'
      || !url.pathname.startsWith('/Archives/edgar/data/')) {
    throw new Error(`${source.id}: expected SEC EDGAR archive URL`)
  }
  for (const key of ['publisher', 'type', 'msaLocator', 'sowLocator', 'notes']) {
    if (typeof source[key] !== 'string' || !source[key].trim()) {
      throw new Error(`${source.id}: ${key} is required`)
    }
  }
  for (const key of ['pairingVerified', 'completeSourceTextPrepared',
    'reusePermissionVerified', 'independentLabelsFrozen']) {
    if (typeof source[key] !== 'boolean') throw new Error(`${source.id}: invalid ${key}`)
  }
  if (source.independentLabelsFrozen && (!source.completeSourceTextPrepared
      || !source.reusePermissionVerified)) {
    throw new Error(`${source.id}: labels cannot be frozen before source and permission review`)
  }
}
console.log(`Validated ${manifest.sources.length} real-filing candidates; 0 gold labels or real-world accuracy claims.`)
