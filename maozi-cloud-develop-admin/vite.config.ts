import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 渲染进程（Vue 前端）构建配置
export default defineConfig({
  plugins: [vue()],
  // 打包后以相对路径加载资源，保证 file:// 协议下可用
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    strictPort: true
  }
})
