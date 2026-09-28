import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import {fileURLToPath, URL} from 'node:url'

export default defineConfig({
  // 生产经外层统一入口 nginx 以 /admin 子路径托管 (剥前缀转发到本容器):
  // 资源引用与路由 base 均自动取该值 (vue-router createWebHistory 默认读 BASE_URL),
  // 本地 dev 访问地址相应变为 http://localhost:999/admin/
  base: '/admin/',
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 999,
    host: true,
    proxy: {
      // Nacos 控制台走本服务同源代理：中间件管理内嵌打开时避免跨域 iframe 限制，
      // 并可将登录令牌写入同源 localStorage 实现自动登录（生产部署需在网关/Nginx 配置等效转发）
      '/nacos': {
        target: 'http://localhost:8848',
        changeOrigin: true
      },
      // Grafana 走同源代理并以子路径 /grafana 提供服务（容器已配 serve_from_sub_path），
      // 登录 Cookie 为第一方，自动登录稳定生效
      '/grafana': {
        target: 'http://localhost:3300',
        changeOrigin: true
      },
      // Snail-Job 控制台走同源代理：官方前端生产构建即以 /snail-job 子路径提供服务（hash 路由），
      // 自动登录需向其同源 localStorage 写入 token（生产部署需在网关/Nginx 配置等效转发，端口按实际调整）
      '/snail-job': {
        target: 'http://localhost:8800',
        changeOrigin: true
      }
    }
  }
})
