import fs from 'node:fs'
import path from 'node:path'

const [partsDir, outDir] = process.argv.slice(2)
if (!partsDir || !outDir) {
  console.error('usage: merge-parts.mjs <parts dir> <out dir>')
  process.exit(2)
}

const merged = { mod: null, protocol: null, files: [] }
const seen = new Set()
for (const part of fs.readdirSync(partsDir).sort()) {
  const partPath = path.join(partsDir, part)
  if (!fs.statSync(partPath).isDirectory()) continue
  for (const version of fs.readdirSync(partPath)) {
    const versionDir = path.join(partPath, version)
    const targetsFile = path.join(versionDir, 'targets.json')
    if (!fs.existsSync(targetsFile)) continue
    const targets = JSON.parse(fs.readFileSync(targetsFile, 'utf8'))
    if (merged.mod && merged.mod !== targets.mod) throw new Error(`Parts disagree on the mod version: ${merged.mod} and ${targets.mod}`)
    if (merged.protocol !== null && merged.protocol !== targets.protocol) throw new Error('Parts disagree on the protocol')
    merged.mod = targets.mod
    merged.protocol = targets.protocol
    const target = path.join(outDir, targets.mod)
    fs.mkdirSync(target, { recursive: true })
    for (const entry of targets.files) {
      const key = `${entry.loader}:${entry.file}`
      if (seen.has(key)) throw new Error(`Duplicate target ${key}`)
      seen.add(key)
      fs.copyFileSync(path.join(versionDir, entry.file), path.join(target, entry.file))
      merged.files.push(entry)
    }
  }
}
if (!merged.mod) throw new Error('No parts found')
merged.files.sort((a, b) => a.file.localeCompare(b.file) || a.loader.localeCompare(b.loader))
fs.writeFileSync(path.join(outDir, merged.mod, 'targets.json'), JSON.stringify(merged, null, 2) + '\n')
console.error(`Merged ${merged.files.length} targets for ${merged.mod}`)
console.log(`version=${merged.mod}`)
