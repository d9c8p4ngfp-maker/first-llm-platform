import fs from 'node:fs'
import path from 'node:path'

function unescapeJsString(inner) {
  return inner
    .replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)))
    .replace(/\\n/g, '\n')
    .replace(/\\'/g, "'")
    .replace(/\\"/g, '"')
    .replace(/\\\\/g, '\\')
}

function decodeUnicodeEscapes(source) {
  let out = source.replace(/\{'((?:\\.|[^'\\])*)'\}/g, (_, inner) => {
    if (!inner.includes('\\u')) return `{'${inner}'}`
    return unescapeJsString(inner)
  })
  out = out.replace(/'((?:\\.|[^'\\])*)'/g, (match, inner) => {
    if (!inner.includes('\\u')) return match
    return `'${unescapeJsString(inner)}'`
  })
  out = out.replace(/"((?:\\.|[^"\\])*)"/g, (match, inner) => {
    if (!inner.includes('\\u')) return match
    return `"${unescapeJsString(inner)}"`
  })
  return out
}

const root = 'D:/first/first-gateway-web/src'
const files = []
function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name)
    const stat = fs.statSync(full)
    if (stat.isDirectory()) walk(full)
    else if (/\.(tsx?|jsx?)$/.test(name)) files.push(full)
  }
}
walk(root)

let changed = 0
for (const file of files) {
  const raw = fs.readFileSync(file, 'utf8')
  if (!raw.includes('\\u')) continue
  const next = decodeUnicodeEscapes(raw)
  if (next !== raw) {
    fs.writeFileSync(file, next, 'utf8')
    changed++
    console.log('fixed:', file.replace(/\\/g, '/').split('first-gateway-web/')[1])
  }
}
console.log('total fixed:', changed)
