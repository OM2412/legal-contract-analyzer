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

function check(label, pass, detail) {
  process.stdout.write(`${pass ? 'PASS' : 'FAIL'} ${label}: ${detail}\n`)
  if (!pass) failures.push(label)
}

try {
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
