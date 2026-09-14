# 个人开发管理（maozi-cloud-develop-admin）

一个面向开发者的本机开发环境管理工具（桌面应用）：

- **环境变量管理**：可视化添加 / 编辑 / 删除 shell 配置文件（`~/.zshrc`、`~/.zprofile`、`~/.bash_profile` 等）中的 `export` 变量；支持 `launchctl setenv` 让变量立即对图形应用生效
- **Hosts 管理**：可视化编辑 `/etc/hosts`，支持多域名、行内备注、启用 / 禁用（注释切换），保存时自动备份并刷新 DNS 缓存
- **系统信息仪表盘**：设备名称 / CPU / GPU / 内存 / 磁盘配置识别；CPU（总量 + 每核）、GPU、内存、磁盘实时占用监控（1 秒刷新）；内网 IP（网卡列表）与公网 IP（含归属地）探测

当前支持 **macOS**（Apple Silicon / Intel），平台层已抽象，Windows 支持预留（hosts 逻辑已实现，见 `electron/platform/win32.ts`）。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 桌面框架 | Electron + TypeScript |
| 前端界面 | Vue 3 + Element Plus + Vite |
| 打包分发 | electron-builder（dmg / nsis） |

## 目录结构

```
maozi-cloud-develop-admin/
├── electron/                 # Electron 主进程（系统操作都在这里）
│   ├── main.ts               # 窗口创建与应用生命周期
│   ├── preload.ts            # contextBridge 安全桥接（渲染进程唯一入口）
│   ├── platform/             # ★ 平台抽象层（跨平台扩展点）
│   │   ├── types.ts          # 平台无关接口与数据类型
│   │   ├── shared.ts         # hosts 解析 / 序列化（平台无关）
│   │   ├── darwin.ts         # macOS 实现（osascript 提权、launchctl）
│   │   ├── win32.ts          # Windows 实现（预留，hosts 已可用）
│   │   └── index.ts          # 按平台选择实现 + IPC 注册
│   └── system/
│       └── sysinfo.ts        # 系统信息与实时监控（CPU/GPU/内存/磁盘/IP）
├── src/                      # 渲染进程（Vue 前端）
│   ├── App.vue               # 侧边栏布局
│   ├── api.ts                # IPC 类型化封装
│   └── components/
│       ├── EnvManager.vue    # 环境变量页
│       ├── HostsManager.vue  # Hosts 管理页
│       ├── SystemInfo.vue    # 系统信息仪表盘页
│       └── RingProgress.vue  # 环形进度组件
├── build/                    # 图标资源（icon.icns / icon.png）
├── scripts/                  # dev / 主进程构建脚本
├── electron-builder.yml      # 打包配置
└── package.json
```

## 环境要求

- **Node.js >= 18**（建议 20+），自带 npm
- 打包 macOS 应用需要在 macOS 上执行；打包 Windows 安装包建议在 Windows 机器（或 CI）上执行

## 开发调试

```bash
npm install        # 安装依赖
npm run dev        # 启动开发模式（vite 热更新 + electron 窗口）
```

## 从源码打包

### macOS（.app / .dmg）

```bash
npm install

# 打包当前架构（Apple Silicon 机器出 arm64 版）
npm run dist:mac

# 或指定架构
npm run dist:mac:arm64      # Apple Silicon（M 系列）
npm run dist:mac:x64        # Intel
npm run dist:mac:universal  # 通用二进制（体积翻倍，两种机器都能跑）
```

产物输出在 `release/` 目录：

```
release/
├── maozi-cloud-develop-admin-1.0.0-arm64.dmg   # 分发用安装镜像
├── maozi-cloud-develop-admin-1.0.0-arm64.zip   # 绿色压缩包
└── mac-arm64/maozi-cloud-develop-admin.app     # 直接可用的应用
```

`dmg` 双击拖入「应用程序」即完成安装；`displayName` 在 Finder / 启动台中显示为「个人开发管理」。

**首次打开提示“无法验证开发者”怎么办？**

本项目默认未做代码签名（`electron-builder.yml` 中 `mac.identity: null`），首次打开会被 Gatekeeper 拦截，任选其一：

```bash
# 方式一：解除隔离属性（推荐，对单个应用执行）
xattr -cr /Applications/maozi-cloud-develop-admin.app
```

- 方式二：在「应用程序」中**右键 → 打开 → 再点打开」**
- 方式三：系统设置 → 隐私与安全性 → 底部点「仍要打开」

**正式分发（可选）**：若拥有 Apple Developer 账号（$99/年），修改 `electron-builder.yml`：

```yaml
mac:
  identity: "你的证书名（如 Apple Development: xxx）"   # 删除 identity: null
  # notarize: true   # 需配置 APPLE_ID / APPLE_APP_SPECIFIC_PASSWORD / APPLE_TEAM_ID 环境变量
```

签名 + 公证后，其他用户打开不会再有安全提示。

### Windows（预留）

```bash
npm run dist:win    # 在 Windows 机器上执行，产出 NSIS 安装包（release/*.exe）
```

> macOS 上交叉打包 Windows 的 NSIS 依赖 wine，不推荐。建议在 Windows 机器或 CI（见下）上执行。
> Windows 的 hosts 读写已实现；环境变量管理待实现（方案见 `electron/platform/win32.ts` 顶部注释：注册表 `HKCU\Environment` + `WM_SETTINGCHANGE` 广播）。

### 依赖下载慢？

`npm install` 或打包时若 Electron 二进制下载失败，使用国内镜像：

```bash
export ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/
export ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/
npm install
```

也可以直接取消 `electron-builder.yml` 末尾 `electronDownload.mirror` 的注释。

## 安全与稳定性设计

- **提权方式**：修改 hosts 通过 `osascript ... with administrator privileges` 弹出系统授权框，命令只拼接程序生成的安全路径，不执行用户输入的任意 shell
- **自动备份**：每次写入前，`/etc/hosts` → `/etc/hosts.maozi.bak`，shell 配置 → `~/.zshrc.maozi.bak` 等
- **托管区块**：应用新增的环境变量写入 `# >>> maozi-cloud-develop-admin (managed) >>>` 区块，删除干净、不碰你手写的其他行
- **渲染进程隔离**：`contextIsolation` 开启、`nodeIntegration` 关闭，前端只能通过 preload 暴露的白名单 IPC 操作系统

## 新增平台（如 Linux）步骤

1. 新建 `electron/platform/linux.ts`，实现 `Platform` 接口（`types.ts`）
2. 在 `platform/index.ts` 的 `selectPlatform()` 中挂载
3. `electron-builder.yml` 增加 `linux` target 配置

界面与 IPC 层完全复用，无需改动。
