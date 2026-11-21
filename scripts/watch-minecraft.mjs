import fs from 'node:fs'

const SETTINGS = 'settings.gradle'
const PROPERTIES = 'stonecutter.properties.toml'
const FLOOR = '1.16.5'
const write = process.argv.includes('--write')

async function json(url) {
  const response = await fetch(url, { headers: { 'User-Agent': 'grubie-companion-ci' } })
  if (!response.ok) throw new Error(`${url}: ${response.status}`)
  return response.json()
}

function parts(version) {
  return version.split('.').map(Number)
}

function compare(a, b) {
  const x = parts(a)
  const y = parts(b)
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    const d = (x[i] ?? 0) - (y[i] ?? 0)
    if (d !== 0) return d
  }
  return 0
}

function lineOf(version) {
  const [major, minor] = parts(version)
  return `${major}.${minor}`
}

function nextLine(version) {
  const [major, minor] = parts(version)
  return `${major}.${minor + 1}`
}

const settings = fs.readFileSync(SETTINGS, 'utf8')
let properties = fs.readFileSync(PROPERTIES, 'utf8')

const matrix = new Map()
for (const match of settings.matchAll(/'([0-9.]+)'\s*:\s*\[([^\]]*)\]/g)) {
  matrix.set(match[1], [...match[2].matchAll(/'([a-z]+)'/g)].map((m) => m[1]))
}
const covered = new Map()
for (const match of properties.matchAll(/\["([0-9.]+)"\]\s*\nmod\.releases = "([^"]*)"/g)) {
  for (const release of match[2].split(',')) covered.set(release.trim(), match[1])
}

const manifest = await json('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
const releases = manifest.versions
  .filter((entry) => entry.type === 'release' && compare(entry.id, FLOOR) >= 0)
  .map((entry) => entry.id)
  .sort(compare)
const missing = releases.filter((release) => !covered.has(release))
if (missing.length === 0) {
  console.log(JSON.stringify({ changed: false, latest: manifest.latest.release }))
} else {
  await update()
}

async function update() {

const fabricGame = new Set((await json('https://meta.fabricmc.net/v2/versions/game')).filter((v) => v.stable).map((v) => v.version))
const forgePromos = (await json('https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json')).promos
const neoVersions = (await json('https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge')).versions

function neoFor(mc) {
  const p = mc.split('.')
  const prefix = p[0] === '1' ? `${p[1]}.${p[2] ?? '0'}.` : `${mc}.`
  const stable = neoVersions.filter((v) => v.startsWith(prefix) && !v.includes('beta') && !v.includes('alpha'))
  return stable.length ? stable[stable.length - 1] : null
}

function forgeFor(mc) {
  const build = forgePromos[`${mc}-recommended`] || forgePromos[`${mc}-latest`]
  return build ? `${mc}-${build}` : null
}

const nodes = [...matrix.keys()].sort(compare)
const changes = []
for (const release of missing) {
  const node = nodes.filter((n) => lineOf(n) === lineOf(release) && compare(n, release) <= 0).pop()
  if (node) {
    const block = new RegExp(`(\\["${node.replace(/\./g, '\\.')}"\\]\\s*\\nmod\\.releases = ")([^"]*)(")`)
    properties = properties.replace(block, (_, a, list, c) => `${a}${list},${release}${c}`)
    changes.push({ kind: 'patch', node, release })
    continue
  }
  const loaders = []
  if (fabricGame.has(release)) loaders.push('fabric')
  const forge = forgeFor(release)
  if (forge) loaders.push('forge')
  const neo = neoFor(release)
  if (neo) loaders.push('neoforge')
  if (loaders.length === 0) {
    changes.push({ kind: 'waiting', release, reason: 'no loader supports it yet' })
    continue
  }
  const upper = nextLine(release)
  let block = `["${release}"]\nmod.releases = "${release}"\n\n`
  if (loaders.includes('fabric')) block += `[fabric."${release}"]\nmod.mc_compat = ">=${release} <${upper}"\n\n`
  if (loaders.includes('forge')) block += `[forge."${release}"]\nmod.mc_compat = "[${release},${upper})"\ndeps.forge = "${forge}"\n\n`
  if (loaders.includes('neoforge')) block += `[neoforge."${release}"]\nmod.mc_compat = "[${release},${upper})"\ndeps.neoforge = "${neo}"\n\n`
  properties = properties.trimEnd() + '\n\n' + block.trimEnd() + '\n'
  matrix.set(release, loaders)
  nodes.push(release)
  changes.push({ kind: 'line', node: release, release, loaders })
}

const built = changes.filter((c) => c.kind !== 'waiting')
if (write && built.length) {
  const lines = [...matrix.entries()].sort((a, b) => compare(a[0], b[0])).map(([mc, loaders]) => {
    const key = `'${mc}'`.padEnd(9)
    return `        ${key}: [${loaders.map((l) => `'${l}'`).join(', ')}],`
  })
  const updated = settings.replace(/def matrix = \[[\s\S]*?\n\]/, `def matrix = [\n${lines.join('\n')}\n]`)
  fs.writeFileSync(SETTINGS, updated)
  fs.writeFileSync(PROPERTIES, properties)
  for (const change of built) {
    if (change.kind !== 'line' || !change.loaders.includes('forge')) continue
    fs.mkdirSync(`versions/${change.node}-forge`, { recursive: true })
    fs.writeFileSync(`versions/${change.node}-forge/gradle.properties`, 'loom.platform=forge\n')
  }
}

const only = [...new Set(built.map((c) => c.node))].join(',')
console.log(JSON.stringify({ changed: built.length > 0, latest: manifest.latest.release, changes, only }))
}
