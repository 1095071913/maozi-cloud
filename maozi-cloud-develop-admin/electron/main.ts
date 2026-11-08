import {app, BrowserWindow, shell} from 'electron'
import path from 'node:path'
import {registerPlatformHandlers} from './platform'
import {registerSysinfoHandlers} from './system/sysinfo'
import {registerBookmarkHandlers} from './bookmarks'
import {registerConfigHandlers} from './configs'
import {registerProjectHandlers} from './projects'
import {registerRenameHandlers} from './rename'

// 窗口创建前先注册各模块 IPC 处理器
registerPlatformHandlers()
registerSysinfoHandlers()
registerBookmarkHandlers()
registerConfigHandlers()
registerProjectHandlers()
registerRenameHandlers()

/** 创建主窗口：设置了 ELECTRON_START_URL（开发模式）时加载该地址，否则加载打包产物 dist/index.html */
function createWindow(): void {
  const win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    title: '开发者控制台',
    // macOS 无缝标题栏：去掉原生标题栏与重复的应用名，红绿灯悬浮在深色侧栏上；
    // 窗口拖拽由侧栏 / 页面头部的 drag 区域承担（Windows 不受影响，保留系统标题栏）
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 18, y: 20 },
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      // 未启用 sandbox，安全隔离由 contextIsolation + 禁用 nodeIntegration 保证
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  })

  // 配合 show: false，渲染就绪后再显示，避免白屏闪烁
  win.once('ready-to-show', () => win.show())

  // 锁定页面缩放（捏合与 Cmd/Ctrl +/-/0）：编程工具的 JSON 编辑器为"高亮层 + 透明输入层"叠加结构，
  // 非 100% 缩放下两层的行位置各自做设备像素取整，误差随行数累积（1.1 倍时约半行、1.5 倍时大半行），
  // 表现为光标/选区与文字错位。桌面工具无页面缩放需求，直接禁用
  win.webContents.setVisualZoomLevelLimits(1, 1)
  win.webContents.on('before-input-event', (event, input) => {
    if (input.type === 'keyDown' && (input.meta || input.control) &&
        ['+', '-', '=', '0', 'Add', 'Subtract', 'Equal', 'Digit0', 'NumpadAdd', 'NumpadSubtract'].includes(input.key)) {
      event.preventDefault()
    }
  })

  // 外部链接交给系统默认浏览器，不在应用内跳转
  win.webContents.setWindowOpenHandler(({ url }) => {
    void shell.openExternal(url)
    return { action: 'deny' }
  })

  if (process.env.ELECTRON_START_URL) {
    void win.loadURL(process.env.ELECTRON_START_URL)
  } else {
    void win.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

void app.whenReady().then(() => {
  createWindow()
  // macOS：点击 Dock 图标且没有窗口时重新创建
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// 关闭所有窗口即退出；macOS 按平台惯例保持应用运行
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
