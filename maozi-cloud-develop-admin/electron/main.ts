import { app, BrowserWindow, shell } from 'electron'
import path from 'node:path'
import { registerPlatformHandlers } from './platform'
import { registerSysinfoHandlers } from './system/sysinfo'
import { registerBookmarkHandlers } from './bookmarks'
import { registerConfigHandlers } from './configs'
import { registerProjectHandlers } from './projects'

// 禁用沙箱要求下的安全默认值由 BrowserWindow 配置保证
registerPlatformHandlers()
registerSysinfoHandlers()
registerBookmarkHandlers()
registerConfigHandlers()
registerProjectHandlers()

function createWindow(): void {
  const win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    title: '个人开发管理',
    // macOS 无缝标题栏：去掉原生标题栏与重复的应用名，红绿灯悬浮在深色侧栏上；
    // 窗口拖拽由侧栏 / 页面头部的 drag 区域承担（Windows 不受影响，保留系统标题栏）
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 18, y: 20 },
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  })

  win.once('ready-to-show', () => win.show())

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
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
