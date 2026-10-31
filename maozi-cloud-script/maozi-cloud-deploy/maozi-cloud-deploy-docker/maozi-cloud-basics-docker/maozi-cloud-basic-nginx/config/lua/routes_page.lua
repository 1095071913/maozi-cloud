-- ============================================================
-- routes_page.lua — 根路径 "/" 服务导航页 (扫描路由片段自动生成)
-- ------------------------------------------------------------
-- 动机: routes/*.nginx.conf.template 是路由的唯一事实来源, 导航页不再
-- 维护第二份清单 —— 新增/删除片段文件后, 页面自动同步, 零额外登记。
--
-- 时机: 每个 worker 首次命中 "/" 时扫描一次并缓存于模块级变量
-- (io.popen/io.open 为阻塞调用, 仅执行这一次, 之后全走缓存);
-- 片段增删本就必须重启 nginx 容器 (location 是加载期编译的),
-- 新 worker 自然重新扫描, 页面与路由的生效时机天然一致。
--
-- 每个片段文件提取:
--   入口URI   单段前缀 location (如 /nacos/、/arthas/); 多段路径
--             (如 /arthas/console) 属子路由不单列; 无单段前缀时
--             回退取最短 location 路径
--   外链入口  片段内注释指令 "# @entry <绝对URL>" 显式指定卡片链接,
--             覆盖 location 提取结果 —— 用于无法挂子路径的服务
--             (前端为根路径绝对引用, 如 Sentinel Dashboard), 由
--             compose 暴露的独立端口直连; 此类片段可不写任何 location
--   标题/简介 文件头首个非装饰注释行 (# === / # ---- 之外):
--             首个英文 "(" 之前为标题, 首对括号内为简介,
--             无注释头时回退用文件名 (新片段照此约定写首行即可)
--   需登录    存在 set $form_auth_<应用key>_user 凭据组
--             (见 form_auth.lua) -> 点击后 guard 302 到登录页
--
-- 页面本身公开 (与登录页同层级), 只含路由入口, 无地址/凭据等敏感信息。
-- ============================================================

local ROUTES_DIR   = "/etc/nginx/conf.d/routes"
local FRAGMENT_PAT = "%.nginx%.conf%.template$"

local cache  -- 模块级缓存: 扫描结果 (每个 worker 一份, 随进程存活)

local _M = {}

-- HTML 转义: 标题/简介取自注释文本, 防止意外注入页面标记
local function esc(s)
    return (s:gsub('[&<>"\']', {
        ["&"] = "&amp;", ["<"] = "&lt;", [">"] = "&gt;",
        ['"'] = "&quot;", ["'"] = "&#39;",
    }))
end

-- 文件头首个非装饰注释行 -> 标题 (首个 "(" 前) + 简介 (首对括号内)
local function extract_title(content, fallback)
    for line in content:gmatch("[^\r\n]+") do
        local text = line:match("^%s*#%s*(.-)%s*$")
        -- 跳过空行与 "# ===" / "# ----" 装饰线
        if text and text ~= "" and not text:match("^[=%-]+$") then
            local title = text:match("^(.-)%s*%(") or text
            local desc  = text:match("%((.-)%)")
            return title, desc
        end
    end
    return fallback, nil
end

-- 提取页面入口 URI: 优先单段前缀 location, 兜底最短 location 路径
local function extract_entry(content)
    local entry, fallback
    for path in content:gmatch("location%s+=?%s*([^%s{]+)%s*{") do
        -- 只认以 "/" 开头的普通 location, 跳过 /_ 开头的内部端点
        if path:sub(1, 1) == "/" and path:sub(1, 2) ~= "/_" then
            if path:match("^/[^/]+/$") then
                -- 单段前缀 (如 /nacos/) = 应用主入口
                if not entry or #path < #entry then entry = path end
            elseif not fallback or #path < #fallback then
                fallback = path
            end
        end
    end
    return entry or fallback
end

-- 扫描路由片段目录 (阻塞调用, 仅每 worker 首次 render 时执行一次)
local function scan()
    local items = {}

    local p = io.popen("/bin/ls -1 " .. ROUTES_DIR .. " 2>/dev/null", "r")
    if not p then return items end
    local names = {}
    for name in p:lines() do
        if name:match(FRAGMENT_PAT) then names[#names + 1] = name end
    end
    p:close()

    table.sort(names)  -- 与主配置 include 的引入顺序保持一致
    for _, name in ipairs(names) do
        local f = io.open(ROUTES_DIR .. "/" .. name, "r")
        if f then
            local content = f:read("*a")
            f:close()
            -- "# @entry <url>": 外链入口指令, 优先于 location 提取
            -- (用于前端为根路径绝对引用、无法挂子路径的服务);
            -- 仅认 "/..." 或 http(s):// 开头的合法链接, 注释散文里
            -- 出现的疑似指令字样一律忽略, 防止误匹配
            local external = content:match("#%s*@entry%s+(%S+)")
            if external and not (external:match("^https?://") or external:sub(1, 1) == "/") then
                external = nil
            end
            local entry = external or extract_entry(content)
            if entry then
                local title, desc = extract_title(content, name:gsub(FRAGMENT_PAT, ""))
                items[#items + 1] = {
                    file      = name,
                    title     = title,
                    desc      = desc,
                    href      = entry,
                    protected = content:match("set%s+%$form_auth_[a-z0-9_]+_user") ~= nil,
                }
            end
        end
    end
    return items
end

-- ---------------------- 页面渲染 ----------------------

local function card(item)
    local letter = item.title:match("%a") or "#"
    local desc   = item.desc
        and ('<div class="d">' .. esc(item.desc) .. '</div>')
        or  '<div class="d"></div>'
    local lock = item.protected
        and '<span class="lock">需登录</span>'
        or  '<span class="lock open">免登录</span>'
    return
        '<a class="card" href="' .. esc(item.href) .. '">' ..
            '<div class="icon">' .. esc(letter) .. '</div>' ..
            '<div class="t">' .. esc(item.title) .. '</div>' ..
            desc ..
            '<div class="meta"><code>' .. esc(item.href) .. '</code>' .. lock .. '</div>' ..
        '</a>'
end

local function page(items)
    local body
    if #items == 0 then
        body = '<div class="empty">未发现路由片段<br>' ..
               '在 routes/ 新增 *.nginx.conf.template 并重启容器后, 入口自动出现在此处</div>'
    else
        local cards = {}
        for _, it in ipairs(items) do cards[#cards + 1] = card(it) end
        body = table.concat(cards)
    end

    return [[<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="robots" content="noindex, nofollow">
<title>服务导航 · maozi-cloud-nginx</title>
<style>
  :root {
    --bg-0: #070b18; --bg-1: #0b1226;
    --card: rgba(17, 26, 48, 0.72); --card-border: rgba(120, 160, 255, 0.16);
    --text-1: #eef2ff; --text-2: #9aa7c7; --text-3: #5f6c8f;
    --accent-1: #5b8cff; --accent-2: #8b5cf6; --accent-3: #22d3ee;
    --ok: #34d399; --ok-bg: rgba(52, 211, 153, 0.10); --ok-border: rgba(52, 211, 153, 0.32);
  }
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
                 "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    background: var(--bg-0); color: var(--text-1);
    min-height: 100vh; padding: 56px 24px 40px;
    -webkit-font-smoothing: antialiased;
  }
  .bg {
    position: fixed; inset: 0; z-index: 0;
    background:
      radial-gradient(1200px 800px at 80% -10%, rgba(91, 140, 255, 0.16), transparent 60%),
      radial-gradient(1000px 700px at -10% 110%, rgba(139, 92, 246, 0.14), transparent 60%),
      linear-gradient(160deg, var(--bg-1) 0%, var(--bg-0) 55%, #05070f 100%);
  }
  .bg::after {
    content: ""; position: absolute; inset: -50%;
    background-image:
      linear-gradient(rgba(120, 160, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(120, 160, 255, 0.05) 1px, transparent 1px);
    background-size: 56px 56px;
    transform: perspective(600px) rotateX(38deg) translateY(18%);
    mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 75%);
    -webkit-mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 75%);
  }
  .wrap { position: relative; z-index: 1; max-width: 960px; margin: 0 auto; }
  header { text-align: center; margin-bottom: 38px; }
  .logo {
    width: 64px; height: 64px; margin: 0 auto 16px; border-radius: 18px;
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(145deg, rgba(91, 140, 255, 0.16), rgba(139, 92, 246, 0.10));
    border: 1px solid rgba(120, 160, 255, 0.25);
    box-shadow: 0 12px 32px rgba(91, 140, 255, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.10);
  }
  .logo svg { width: 32px; height: 32px; display: block; }
  h1 { font-size: 23px; font-weight: 700; letter-spacing: 2px; margin-bottom: 8px; }
  .sub { color: var(--text-2); font-size: 13.5px; }
  .grid {
    display: grid; gap: 18px;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
  .card {
    display: flex; flex-direction: column; gap: 6px;
    padding: 26px 24px 22px; border-radius: 20px;
    background: var(--card); border: 1px solid var(--card-border);
    text-decoration: none; color: var(--text-1);
    backdrop-filter: blur(22px) saturate(140%);
    -webkit-backdrop-filter: blur(22px) saturate(140%);
    box-shadow: 0 20px 48px rgba(2, 6, 20, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.06);
    transition: transform 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
    animation: card-in 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
  }
  .card:hover {
    transform: translateY(-4px);
    border-color: rgba(91, 140, 255, 0.45);
    box-shadow: 0 26px 60px rgba(2, 6, 20, 0.65);
  }
  @keyframes card-in {
    from { opacity: 0; transform: translateY(18px); }
    to   { opacity: 1; transform: translateY(0); }
  }
  .icon {
    width: 46px; height: 46px; border-radius: 14px;
    display: flex; align-items: center; justify-content: center;
    font-size: 20px; font-weight: 700; color: #fff;
    background: linear-gradient(145deg, var(--accent-1), var(--accent-2));
    box-shadow: 0 10px 24px rgba(91, 140, 255, 0.28);
    margin-bottom: 8px;
  }
  .t { font-size: 16.5px; font-weight: 600; }
  .d { color: var(--text-2); font-size: 12.5px; line-height: 1.6; min-height: 20px; }
  .meta {
    display: flex; align-items: center; justify-content: space-between;
    gap: 8px; margin-top: 10px;
  }
  code {
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 12px; color: var(--accent-3);
    background: rgba(34, 211, 238, 0.08);
    border: 1px solid rgba(34, 211, 238, 0.22);
    padding: 3px 8px; border-radius: 8px;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
  .lock {
    flex: none; font-size: 11.5px; color: #fbbf24;
    background: rgba(251, 191, 36, 0.10);
    border: 1px solid rgba(251, 191, 36, 0.30);
    padding: 3px 9px; border-radius: 999px;
  }
  .lock.open { color: var(--ok); background: var(--ok-bg); border-color: var(--ok-border); }
  .empty {
    grid-column: 1 / -1; text-align: center; color: var(--text-2);
    border: 1px dashed rgba(120, 160, 255, 0.25); border-radius: 16px;
    padding: 44px 20px; font-size: 14px; line-height: 2;
  }
  footer { text-align: center; color: var(--text-3); font-size: 12px; margin-top: 42px; line-height: 1.8; }
</style>
</head>
<body>
<div class="bg"></div>
<div class="wrap">
  <header>
    <div class="logo">
      <svg viewBox="0 0 24 24" fill="none" stroke="url(#g)" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <defs><linearGradient id="g" x1="0" y1="0" x2="24" y2="24">
          <stop offset="0" stop-color="#5b8cff"/><stop offset="1" stop-color="#8b5cf6"/>
        </linearGradient></defs>
        <circle cx="12" cy="12" r="9"/>
        <path d="M12 3v4M12 17v4M3 12h4M17 12h4M12 10.5A1.5 1.5 0 1 0 12 13.5 1.5 1.5 0 1 0 12 10.5z"/>
      </svg>
    </div>
    <h1>maozi-cloud-nginx</h1>
    <div class="sub">服务导航 · 共 ]] .. #items .. [[ 个入口 · 由 routes/ 路由片段自动生成</div>
  </header>
  <div class="grid">]] .. body .. [[</div>
  <footer>新增 routes/*.nginx.conf.template 片段并重启 nginx 容器后, 入口自动出现</footer>
</div>
</body>
</html>]]
end

-- content 阶段: 渲染 "/" 导航页 (由 all.nginx.conf.template 的
-- location = / 调用)
function _M.render()
    if not cache then cache = scan() end
    ngx.header.content_type = "text/html; charset=utf-8"
    ngx.print(page(cache))
end

return _M
