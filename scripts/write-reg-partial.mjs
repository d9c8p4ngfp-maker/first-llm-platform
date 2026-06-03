import fs from 'node:fs'
const p = 'D:/first/first-gateway-web/src/pages/auth/RegisterPage.tsx'
const lines = [
"import { useEffect, useState } from 'react'",
"import { Link, useNavigate } from '@tanstack/react-router'",
"import { Button } from '@/components/ui/button'",
"import { Input } from '@/components/ui/input'",
"import { Label } from '@/components/ui/label'",
"import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'",
"import { useAuthStore } from '@/stores/auth'",
"import { TOKEN_KEY } from '@/lib/utils'",
"import * as authApi from '@/api/auth'",
"import { extractApiError } from '@/lib/api-error'",
"",
"function mapRegisterError(message: string, status?: number): string {",
"  if (status === 403) {",
"    return '\u8bbf\u95ee\u88ab\u62d2\u7edd\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5'",
"  }",
"  if (!message || message === '\u7cfb\u7edf\u5185\u90e8\u9519\u8bef') {",
"    return '\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u6216\u90ae\u7bb1\u662f\u5426\u5df2\u88ab\u5360\u7528'",
"  }",
"  const map: Record<string, string> = {",
"    'username already exists': '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528',",
"    'email already exists': '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c',",
"    '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528': '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528',",
"    '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c': '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c',",
"    'username length must be 3-50 characters': '\u7528\u6237\u540d\u957f\u5ea6\u4e3a 3-50 \u4e2a\u5b57\u7b26',",
"    'password must be at least 6 characters': '\u5bc6\u7801\u81f3\u5c11 6 \u4f4d',",
"  }",
"  return map[message] || message",
"}",
]
fs.writeFileSync(p, lines.join('\n') + '\n', 'utf8')
console.log('partial write ok')
