import fs from 'node:fs'

const [targetsFile, scope = 'nodes'] = process.argv.slice(2)
if (!targetsFile) {
  console.error('usage: runtime-matrix.mjs <targets.json> [nodes|all]')
  process.exit(2)
}

const targets = JSON.parse(fs.readFileSync(targetsFile, 'utf8'))

function javaFor(mc) {
  const [major, minor, patch = 0] = mc.split('.').map(Number)
  if (major >= 26) return 25
  if (minor > 20 || (minor === 20 && patch >= 5)) return 21
  if (minor >= 18) return 17
  if (minor === 17) return 16
  return 8
}

async function json(url) {
  const response = await fetch(url, { headers: { 'User-Agent': 'grubie-companion-ci' } })
  if (!response.ok) throw new Error(`${url}: ${response.status}`)
  return response.json()
}

const forgePromos = (await json('https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json')).promos
const neoVersions = (await json('https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge')).versions
const fabricGame = new Set((await json('https://meta.fabricmc.net/v2/versions/game')).map((entry) => entry.version))

function available(loader, mc) {
  if (loader === 'fabric') return fabricGame.has(mc)
  if (loader === 'forge') return Boolean(forgePromos[`${mc}-latest`] || forgePromos[`${mc}-recommended`])
  if (loader === 'neoforge') {
    if (mc === '1.20.1') return false
    const parts = mc.split('.')
    const prefix = parts[0] === '1' ? `${parts[1]}.${parts[2] ?? '0'}.` : `${mc}.`
    return neoVersions.some((version) => version.startsWith(prefix) && !version.includes('beta'))
  }
  return false
}

const include = []
for (const entry of targets.files) {
  const node = entry.file.replace(/^grubie-companion-[^+]+\+/, '').replace(/\.jar$/, '')
  const nodeMc = node.slice(node.indexOf('-') + 1)
  const releases = scope === 'all' ? entry.minecraft : entry.minecraft.filter((mc) => mc === nodeMc || entry.minecraft.length === 1)
  for (const mc of releases.length ? releases : [entry.minecraft[entry.minecraft.length - 1]]) {
    if (!available(entry.loader, mc)) continue
    include.push({ mc, loader: entry.loader, java: javaFor(mc), file: entry.file })
  }
}
console.error(`${include.length} runtime runs`)
console.log(`matrix=${JSON.stringify({ include })}`)
