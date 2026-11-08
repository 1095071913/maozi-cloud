import {createApp} from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import './styles.css'

// 渲染进程入口：全局注册 Element Plus（中文语言包）后挂载根组件
createApp(App).use(ElementPlus, { locale: zhCn }).mount('#app')
