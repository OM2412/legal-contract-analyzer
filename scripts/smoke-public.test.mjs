import { spawnSync } from 'node:child_process'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import assert from 'node:assert/strict'
const temp = mkdtempSync(join(tmpdir(), 'folio-smoke-'))
const preload = join(temp, 'mock.mjs')
writeFileSync(preload, `
const scenario = process.env.SCENARIO
let clock = 0, attempt = 0
Date.now = () => clock
globalThis.setTimeout = (fn, ms) => { clock += ms; queueMicrotask(fn) }
globalThis.fetch = async (url) => {
 const path = url.pathname
 const headers = {'cache-control':'no-store','x-frame-options':'DENY','x-content-type-options':'nosniff'}
 if(path === '/api/health') {
  attempt++
  // Reproduce a cold start that exceeds the previous 120-second budget.
  if(scenario === 'slow-start' && attempt <= 6) { clock += 20000; throw new DOMException('timed out', 'TimeoutError') }
  if(scenario === 'timeout' && attempt === 1) { clock += 20000; throw new DOMException('timed out', 'TimeoutError') }
  if(scenario === 'permanent-timeout') { clock += 20000; throw new DOMException('timed out', 'TimeoutError') }
  if(scenario === 'html' && attempt === 1) return new Response('<html>Starting</html>')
  if(scenario === '503' && attempt === 1) return new Response('', {status:503})
  if(scenario === '404') return new Response('', {status:404})
  return Response.json({status:'UP',database:scenario === 'db-down'?'DOWN':'UP'}, {headers})
 }
 if(path === '/') return new Response('<script src="/assets/app.js"></script><link href="/assets/app.css">')
 if(path === '/api/reviews') return new Response('',{status:scenario==='archive-open'?200:401})
 if(path === '/api/auth/csrf') {
  if(scenario === 'bad-header') delete headers['x-frame-options']
  return Response.json({}, {headers})
 }
 return new Response('asset')
}
`)
try {
 for (const scenario of ['ready','slow-start','timeout','html','503','permanent-timeout','db-down','404','archive-open','bad-header']) {
  const result=spawnSync(process.execPath,['--import',preload,'smoke-public.mjs','https://example.test'], {cwd:import.meta.dirname,env:{...process.env,SCENARIO:scenario},encoding:'utf8',timeout:5000})
  assert.ifError(result.error)
  const expected=['ready','slow-start','timeout','html','503'].includes(scenario)?0:1
  assert.equal(result.status,expected,scenario+'\n'+result.stdout+result.stderr)
  if(scenario==='permanent-timeout'||scenario==='db-down') assert.match(result.stdout,/not ready within 180 seconds/)
  if(scenario==='slow-start') assert.match(result.stdout,/Readiness reached after 150 seconds/)
  if(scenario==='404') assert.doesNotMatch(result.stdout,/WAIT/)
  console.log('PASS '+scenario)
 }
} finally { rmSync(temp,{recursive:true,force:true}) }
