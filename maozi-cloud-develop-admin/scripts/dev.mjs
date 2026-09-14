/**
 * 开发模式：先编译主进程/preload（保证最新代码），再启动 vite dev server，
 * 就绪后以 ELECTRON_START_URL 拉起 electron
 */
import {spawn} from 'node:child_process'
import {build} from 'esbuild'

const isWin = process.platform === 'win32'
const npmCmd = isWin ? 'npm.cmd' : 'npm'
const npxCmd = isWin ? 'npx.cmd' : 'npx'

// 主进程/preload 不走 vite，dev 启动前必须重新编译，否则 electron 跑的是旧代码
const shared = {
  bundle: true,
  platform: 'node',
  target: 'node20',
  format: 'cjs',
  external: ['electron']
}
console.log('[dev] 编译 main/preload ...')
await build({ ...shared, entryPoints: ['electron/main.ts'], outfile: 'dist-electron/main.js' })
await build({ ...shared, entryPoints: ['electron/preload.ts'], outfile: 'dist-electron/preload.js' })
console.log('[dev] main/preload 已更新 -> dist-electron/')

const vite = spawn(npxCmd, ['vite', '--strictPort'], {
  stdio: ['ignore', 'pipe', 'inherit'],
  shell: isWin
})

let electron = null
let started = false

function startElectron() {
  if (started) return
  started = true
  electron = spawn(npxCmd, ['electron', '.'], {
    stdio: 'inherit',
    shell: isWin,
    env: { ...process.env, ELECTRON_START_URL: 'http://localhost:5173' }
  })
  electron.on('exit', () => {
    vite.kill()
    process.exit(0)
  })
}

vite.stdout.on('data', (chunk) => {
  const text = chunk.toString()
  process.stdout.write(`[vite] ${text}`)
  if (text.includes('Local:') || text.includes('ready in')) {
    setTimeout(startElectron, 300)
  }
})

vite.on('exit', (code) => {
  if (!started) {
    console.error('vite 启动失败，退出码', code)
    process.exit(1)
  }
})

function cleanup() {
  vite.kill()
  if (electron) electron.kill()
}
process.on('SIGINT', () => {
  cleanup()
  process.exit(0)
})
process.on('SIGTERM', () => {
  cleanup()
  process.exit(0)
})
