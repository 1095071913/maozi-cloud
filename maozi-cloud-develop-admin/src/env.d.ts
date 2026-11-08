/** 让 TS 识别 .vue 单文件组件导入 */
declare module '*.vue' {
    import type {DefineComponent} from 'vue'
    const component: DefineComponent<object, object, unknown>
  export default component
}

interface Window {
  /** preload 经 contextBridge 注入的主进程能力 */
  api: import('./api').ElectronApi
}
