// Safe deployment smoke checks: no credentials, user data or contract uploads.
// Usage: node scripts/smoke-public.mjs https://your-deployment.example
const origin = process.argv[2]
if (!origin || !/^https:\/\//.test(origin)) {
  console.error('Usage: node scripts/smoke-public.mjs https://deployment-host')
  process.exit(2)
}

const base = new URL(origin)
const failures = []
const request = async (path, options = {}) => {
  const response = await fetch(new URL(path, base), {
    signal: AbortSignal.timeout(20000),
    redirect: 'manual',
    ...options,
  })
  return response
}

// A sleeping deployment can take longer than one request to start.
// Retry only readiness; later smoke/security failures must remain failures.
const readinessBudgetMs = 120000
async function waitForReady() {
  const deadline = Date.now() + readinessBudgetMs
  let lastDetail = 'no response'
  let attempt = 0
  while (Date.now() < deadline) {
    attempt++
    try {
      const response = await request('/api/health', {
        signal: AbortSignal.timeout(Math.min(20000, Math.max(1, deadline - Date.now()))),
      })
      let status
      if (response.status === 200) {
        try { status = await response.json() } catch {
          lastDetail = 'HTTP 200 without valid health JSON (startup page or invalid response)'
        }
        if (status?.status === 'UP' && status?.database === 'UP') {
          return { response, status }
        }
        if (status) lastDetail = 'Health JSON does not report application and database UP'
      } else {
        lastDetail = `HTTP ${response.status}`
        await response.body?.cancel()
        // Authentication/route errors are configuration problems, not startup.
        if (![408, 429, 500, 502, 503, 504].includes(response.status)) {
          throw new Error(`Non-retryable health response: ${lastDetail}`)
        }
      }
    } catch (error) {
      if (error.message.startsWith('Non-retryable')) throw error
      lastDetail = error.message
    }
    const remaining = deadline - Date.now()
    if (remaining <= 0) break
    console.log(`WAIT Readiness attempt ${attempt}: ${lastDetail}; retrying within 120-second startup budget`)
    await new Promise(resolve => setTimeout(resolve, Math.min(5000, remaining)))
  }
  throw new Error(`Application/database not ready within 120 seconds: ${lastDetail}`)
}

function check(label, pass, detail) {
  process.stdout.write(`${pass ? 'PASS' : 'FAIL'} ${label}: ${detail}\n`)
  if (!pass) failures.push(label)
}

try {
  const { response: health, status } = await waitForReady()
  check('Application and database ready', health.status === 200, `HTTP ${health.status}`)
  if (health.status === 200) {
    check('Health reports UP', status.status === 'UP', status.status ?? 'missing')
    check('Health response not cached',
      health.headers.get('cache-control')?.includes('no-store'),
      health.headers.get('cache-control') ?? 'missing')
  }

  const home = await request('/')
  const html = await home.text()
  check('Public entrypoint', home.status === 200, `HTTP ${home.status}`)

  const jsAsset = html.match(/src="(\/assets\/[^" ]+\.js)"/)?.[1]
  const cssAsset = html.match(/href="(\/assets\/[^" ]+\.css)"/)?.[1]
  check('Frontend asset references', Boolean(jsAsset && cssAsset),
    jsAsset && cssAsset ? 'JS and CSS assets present' : 'Missing built asset')

  if (jsAsset && cssAsset) {
    const [js, css] = await Promise.all([request(jsAsset), request(cssAsset)])
    check('JS asset reachable', js.status === 200, `HTTP ${js.status}`)
    check('CSS asset reachable', css.status === 200, `HTTP ${css.status}`)
  }

  const privateResponse = await request('/api/reviews')
  check('Unauthenticated archive blocked', privateResponse.status === 401,
    `HTTP ${privateResponse.status}`)

  const csrf = await request('/api/auth/csrf')
  check('CSRF endpoint available', csrf.ok, `HTTP ${csrf.status}`)
  check('CSRF response not cached',
    csrf.headers.get('cache-control')?.includes('no-store'),
    csrf.headers.get('cache-control') ?? 'missing cache-control')
  check('Anti-framing header',
    csrf.headers.get('x-frame-options') === 'DENY',
    csrf.headers.get('x-frame-options') ?? 'missing')
  check('Content-type sniffing blocked',
    csrf.headers.get('x-content-type-options') === 'nosniff',
    csrf.headers.get('x-content-type-options') ?? 'missing')
} catch (error) {
  check('Deployment reachable', false, error.message)
}

if (failures.length) {
  console.error(`${failures.length} smoke check(s) failed`)
  process.exitCode = 1
} else {
  console.log('Public deployment smoke checks passed.')
}
