-- ============================================================
-- form_auth.lua — 通用网页表单登录 + Cookie 会话 (应用级凭据, 自动注册)
-- ------------------------------------------------------------
-- 动机: HTTP Basic Auth 的登录框是浏览器原生 UI, 无法自定义样式;
-- 改为网页表单登录, 登录页/失败提示/登出页完全可定制。
-- 凭据按"应用"隔离: 每个受保护应用独立账号/密码/有效期,
-- 会话 Cookie 与登出吊销也按应用独立, 互不通用。
--
-- 【自动注册, 无需在本文件登记应用】
-- 注册 = 且仅 = 写两处 nginx 配置, Lua 零改动:
--   1. server 级凭据三行 (登录端点 /_auth/login 读取, 存在即注册):
--        set $form_auth_<应用key>_user          账号;
--        set $form_auth_<应用key>_password      密码原文;
--        set $form_auth_<应用key>_ttl           8h;   -- "8h"/"45m"/"30s"/"7d"/纯秒数
--   2. 受保护 location 内一行准入声明:
--        location /xx/ {
--            set $form_auth_app xx;              -- <应用key>, 必写
--            access_by_lua_block { require("form_auth").guard() }
--            ... proxy_pass ...
--        }
--   (应用 key 约定: 小写字母开头, 仅 [a-z0-9_], 用于凭据变量名/Cookie 名)
--
-- 流程:
--   1. access_by_lua guard(): 按本 location 声明的应用校验 Cookie 会话,
--      无效则 302 登录页 (带 app & ttl & redirect & rsig)
--   2. content_by_lua login(): 按表单 app 参数读该应用凭据校验 (常数时间
--      比较); 回跳目标凭 rsig 验签 (guard 用 SECRET 签发, 不可伪造,
--      替代静态前缀白名单), 无有效 rsig 时落到 DEFAULT_LANDING
--   3. content_by_lua logout(): 记录该应用登出吊销点 + 清除会话 Cookie
--
-- 会话 Cookie: 名 maozi_cloud_auth_<应用key>, Path=/, HttpOnly,
--              SameSite=Strict (防 CSRF/脚本读取)
--
-- ---------------------- 修改账号密码 ----------------------
--   1. 替换 nginx server 配置中该应用的 $form_auth_<key>_user/_password
--      (直接写原文, 特殊字符/空格建议加引号)
--   2. 建议同时更新下方 SESSION_SECRET: openssl rand -hex 32 (全部会话失效)
--   3. 重启 nginx 容器生效 (docker compose restart / up -d;
--      macOS 下单文件挂载被编辑器原子替换后容器内会失效, 勿用 -s reload)
-- ============================================================

local bit = require "bit"

-- ------------------------- 固定配置 -------------------------
-- 会话签名密钥 (openssl rand -hex 32), 泄露则可伪造会话, 务必保密
local SESSION_SECRET = "f377818ca4e2be984b93698a40e0bc1e29f99bf0e7075a66f80ad61be195ab48"

local COOKIE_PREFIX   = "maozi_cloud_auth_"
local LOGIN_URL       = "/_auth/login.html"
local DEFAULT_TTL     = 8 * 3600     -- 应用未配置/非法 ttl 时的回退有效期
local DEFAULT_LANDING = "/admin/"    -- 无有效回跳签名时的登录落点 (固定存在的路径)

local _M = {}

-- 应用 key 合法性: 小写字母开头, 仅 [a-z0-9_] (防伪造变量名拼接)
local function valid_key(key)
    return type(key) == "string" and key:match("^[a-z][a-z0-9_]*$") ~= nil
end

-- 读应用凭据变量 (nginx server 级 set $form_auth_<key>_<field>);
-- 三行凭据组存在 = 该应用已注册
local function cfg(key, field)
    return ngx.var["form_auth_" .. key .. "_" .. field]
end

-- 应用登录账号: 未配置返回 nil
local function auth_user(key)
    local v = cfg(key, "user")
    if type(v) == "string" and #v > 0 then return v end
    return nil
end

-- 应用登录密码(原文): 未配置/为空返回 nil (登录拒绝, 不回退)
local function password(key)
    local v = cfg(key, "password")
    if type(v) == "string" and #v > 0 then return v end
    return nil
end

-- 应用会话有效期: 支持 "8h"/"45m"/"30s"/"7d"/纯秒数, 非法回退 8h
local function session_ttl(key)
    local v = cfg(key, "ttl")
    if type(v) == "string" then
        local n, unit = v:match("^(%d+)([hmsd]?)$")
        if n then
            local ttl = tonumber(n) * ({ h = 3600, d = 86400, m = 60, s = 1, [""] = 1 })[unit]
            if ttl > 0 then return ttl end
        end
    end
    return DEFAULT_TTL
end

-- 二进制转 hex (ngx.hmac_sha1 返回二进制串)
local function to_hex(s)
    return (s:gsub(".", function(c) return string.format("%02x", c:byte()) end))
end

-- 会话签名: hmac_sha1(secret, user|issued|expiry) 的 hex
-- (token 携带签发/到期双时间戳, 与 TTL 解耦: 修改 TTL 不影响存量会话校验)
local function sign(user, issued, expiry)
    return to_hex(ngx.hmac_sha1(SESSION_SECRET, user .. "|" .. issued .. "|" .. expiry))
end

-- 回跳目标签名: guard 跳登录页时对 (app|redirect) 签发, login 验签。
-- 替代静态前缀白名单: 无 SECRET 无法伪造, 且签名绑定应用 key 不能跨应用挪用
local function redirect_sig(key, redirect)
    return to_hex(ngx.hmac_sha1(SESSION_SECRET, "r|" .. key .. "|" .. redirect))
end

-- 常数时间字符串比较 (防时序侧信道逐字节猜测)
local function ct_equal(a, b)
    if #a ~= #b then return false end
    local diff = 0
    for i = 1, #a do
        diff = bit.bor(diff, bit.bxor(string.byte(a, i), string.byte(b, i)))
    end
    return diff == 0
end

-- 校验指定应用的 Cookie 会话, 有效返回用户名, 否则返回 nil
local function valid_session(key)
    local cookie = ngx.var["cookie_" .. COOKIE_PREFIX .. key]
    if not cookie then return nil end

    -- 格式: user|issued|expiry|sig(hex)
    local user, issued, expiry, sig = cookie:match("^([^|]+)|(%d+)|(%d+)|([0-9a-f]+)$")
    if not user then return nil end
    if not ct_equal(sig, sign(user, issued, expiry)) then return nil end
    if tonumber(expiry) < ngx.time() then return nil end

    -- 账号须与该应用当前配置一致 (改账号后旧账号的存量会话自动失效)
    local cfg_user = auth_user(key)
    if not cfg_user or user ~= cfg_user then return nil end

    -- 登出吊销: 签发时间早于吊销点的全部失效 (token 自带 issued, 与 TTL 无关)
    -- (shared dict 在 nginx.conf 中声明; 单实例内存态, 重启清零)
    local shared = ngx.shared.form_auth_state
    if shared then
        local revoked_after = tonumber(shared:get("revoked:" .. key .. ":" .. user)) or 0
        if tonumber(issued) < revoked_after then return nil end
    end

    return user
end

-- access 阶段守卫: 挂在受保护 location 下
-- 应用 = 本 location 内 set $form_auth_app <key>; 的声明 (必写, 即"注册")
function _M.guard()
    local key = ngx.var.form_auth_app

    -- 挂了 guard 却没声明应用属配置错误, 显式报错而非静默放行/误跳转
    if not valid_key(key) then
        ngx.log(ngx.ERR, "form_auth: location 缺少有效声明 set $form_auth_app <key>; uri=", ngx.var.uri)
        return ngx.exit(500)
    end

    if valid_session(key) then return end

    -- 原始地址含 query (request_uri); location 内 rewrite 不影响本读取
    local redirect = ngx.var.request_uri or ngx.var.uri

    return ngx.redirect(LOGIN_URL ..
        "?app="      .. key ..
        "&ttl="      .. session_ttl(key) ..
        "&redirect=" .. ngx.escape_uri(redirect) ..
        "&rsig="     .. redirect_sig(key, redirect))
end

-- content 阶段: POST /_auth/login 处理表单提交 (按 app 参数取应用凭据)
function _M.login()
    if ngx.req.get_method() ~= "POST" then
        return ngx.redirect(LOGIN_URL)
    end

    ngx.req.read_body()
    local args = ngx.req.get_post_args()

    -- 应用 key 取表单声明, 必须已注册 (凭据组存在)
    local key = args.app
    if not valid_key(key) then
        return ngx.redirect(LOGIN_URL .. "?error=1")
    end

    local user   = args.username or ""
    local pass   = args.password or ""

    -- 提交密码与该应用配置原文做常数时间比较; 配置缺失/为空时直接判失败
    local cfg_user = auth_user(key)
    local cfg_pass = password(key)

    local pass_ok = cfg_pass ~= nil and ct_equal(pass, cfg_pass)
    local user_ok = cfg_user ~= nil and user == cfg_user

    -- 回跳目标: 仅接受 guard 签发的 rsig 证明 (防伪造/防跨应用挪用);
    -- 手工打开登录页等无签名场景落到 DEFAULT_LANDING
    local target = DEFAULT_LANDING
    if type(args.redirect) == "string" and #args.redirect > 0
       and args.redirect:sub(1, 1) == "/" and args.redirect:sub(1, 2) ~= "//"
       and type(args.rsig) == "string"
       and ct_equal(args.rsig, redirect_sig(key, args.redirect)) then
        target = args.redirect
    end

    if not user_ok or not pass_ok then
        -- 失败回到登录页, 由页面展示错误提示; app/redirect/rsig/ttl 保留下次回跳
        return ngx.redirect(LOGIN_URL ..
            "?error=1" ..
            "&app="      .. key ..
            "&ttl="      .. session_ttl(key) ..
            "&redirect=" .. ngx.escape_uri(target) ..
            "&rsig="     .. redirect_sig(key, target))
    end

    local ttl    = session_ttl(key)
    local issued = ngx.time()
    local expiry = issued + ttl
    local token  = cfg_user .. "|" .. issued .. "|" .. expiry .. "|" ..
                   sign(cfg_user, issued, expiry)

    ngx.header["Set-Cookie"] = COOKIE_PREFIX .. key .. "=" .. token ..
        "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" .. ttl

    return ngx.redirect(target)
end

-- content 阶段: GET /_auth/logout?app=<key> 清除该应用会话 (app 必填)
function _M.logout()
    local args = ngx.req.get_uri_args()
    local key  = args.app

    if valid_key(key) then
        -- 服务端吊销: 令该应用当前用户所有已签发 token 立即失效
        local u = auth_user(key)
        local shared = ngx.shared.form_auth_state
        if shared and u then
            shared:set("revoked:" .. key .. ":" .. u, ngx.time())
        end

        ngx.header["Set-Cookie"] = COOKIE_PREFIX .. key ..
            "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"
    end

    return ngx.redirect(LOGIN_URL .. "?logout=1")
end

return _M
