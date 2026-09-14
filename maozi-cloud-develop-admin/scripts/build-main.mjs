/**
 * 用 esbuild 将主进程 / preload 编译为 CJS（electron 不支持 ESM 入口）
 */
import {build} from 'esbuild'

const shared = {
  bundle: true,
  platform: 'node',
  target: 'node20',
  format: 'cjs',
  external: ['electron']
}

await build({
  ...shared,
  entryPoints: ['electron/main.ts'],
  outfile: 'dist-electron/main.js'
})

await build({
  ...shared,
  entryPoints: ['electron/preload.ts'],
  outfile: 'dist-electron/preload.js'
})

console.log('main/preload 构建完成 -> dist-electron/')
