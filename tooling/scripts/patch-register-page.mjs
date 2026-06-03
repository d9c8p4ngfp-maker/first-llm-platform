import fs from 'node:fs'

const registerPath = 'D:/first/first-gateway-web/src/pages/auth/RegisterPage.tsx'
let src = fs.readFileSync(registerPath, 'utf8')

if (!src.includes('TOKEN_KEY')) {
  src = src.replace(
    "import { useAuthStore } from '@/stores/auth'",
    "import { useAuthStore } from '@/stores/auth'\nimport { TOKEN_KEY } from '@/lib/utils'"
  )
}

if (!src.includes('localStorage.removeItem(TOKEN_KEY)')) {
  src = src.replace(
    `  useEffect(() => {
    authApi.registerEnabled().then((res) => setEnabled(res.enabled)).catch(() => setEnabled(true))
  }, [])`,
    `  useEffect(() => {
    localStorage.removeItem(TOKEN_KEY)
    useAuthStore.getState().clearSession()
    authApi.registerEnabled().then((res) => setEnabled(res.enabled)).catch(() => setEnabled(true))
  }, [])`
  )
}

src = src.replace(
  `function mapRegisterError(message: string): string {
  if (!message || message === '??????') {
    return '????????????????????'
  }`,
  `function mapRegisterError(message: string, status?: number): string {
  if (status === 403) {
    return '????????????????????????'
  }
  if (!message || message === '??????') {
    return '????????????????????'
  }`
)

src = src.replace(
  'const raw = extractApiError(err, \'\')\n      const message = mapRegisterError(raw)',
  `const status = (err as { response?: { status?: number } })?.response?.status
      const raw = extractApiError(err, '')
      const message = mapRegisterError(raw, status)`
)

fs.writeFileSync(registerPath, src, 'utf8')
console.log('RegisterPage updated')
