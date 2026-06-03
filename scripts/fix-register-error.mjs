import fs from 'node:fs'

fs.writeFileSync('D:/first/first-gateway-web/src/lib/api-error.ts', `export function extractApiError(err: unknown, fallback = '\u8bf7\u6c42\u5931\u8d25'): string {
  if (typeof err !== 'object' || err === null) {
    return fallback
  }
  const axiosErr = err as {
    response?: { data?: { error?: { message?: string } } }
    message?: string
    code?: string
  }
  const apiMessage = axiosErr.response?.data?.error?.message
  if (apiMessage) {
    return apiMessage
  }
  if (axiosErr.code === 'ERR_NETWORK') {
    return '\u65e0\u6cd5\u8fde\u63a5\u540e\u7aef\uff0c\u8bf7\u786e\u8ba4\u670d\u52a1\u5df2\u542f\u52a8\uff080808 \u7aef\u53e3\uff09'
  }
  if (axiosErr.message) {
    return axiosErr.message
  }
  return fallback
}
`, 'utf8')

const registerPath = 'D:/first/first-gateway-web/src/pages/auth/RegisterPage.tsx'
let src = fs.readFileSync(registerPath, 'utf8')
if (src.charCodeAt(1) === 0) {
  // utf16le
  const buf = fs.readFileSync(registerPath)
  src = buf.toString('utf16le')
}
if (!src.includes("from '@/lib/api-error'")) {
  src = src.replace("import * as authApi from '@/api/auth'", "import * as authApi from '@/api/auth'\nimport { extractApiError } from '@/lib/api-error'")
}
src = src.replace(
  /catch \(err: unknown\) \{[\s\S]*?\} finally \{\s*setLoading\(false\)/,
  `catch (err: unknown) {
      const raw = extractApiError(err, '')
      const message = mapRegisterError(raw)
      setError(message || '\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u6216\u90ae\u7bb1\u662f\u5426\u5df2\u88ab\u5360\u7528')
    } finally {
      setLoading(false)`
)
fs.writeFileSync(registerPath, src, 'utf8')
console.log('fixed api-error and RegisterPage')
