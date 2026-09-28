-- ============================================================
-- dynamic_balancer.lua —— OpenResty 运行时动态 DNS 负载均衡模块
-- ------------------------------------------------------------
-- 背景: nginx 静态 upstream 在配置加载期一次性解析域名:
--   1) nginx 先于后端容器启动时, 域名尚未注册到 Docker DNS,
--      解析失败导致 nginx 启动失败 / upstream 不可用;
--   2) 后端容器重启换 IP 后, 旧解析结果被永久缓存, 持续 502/503。
-- 方案 (balancer_by_lua* 阶段禁用 cosocket, DNS 查询只能放在 access 阶段):
--   access_by_lua   -> 按节点实时向 Docker 内嵌 DNS (127.0.0.11) 解析,
--                      结果带 TTL 缓存, 写入 ngx.ctx.dyn_peers;
--   balancer_by_lua -> 只消费 ngx.ctx.dyn_peers, proxy_next_upstream
--                      每次重试重新进入并按 attempt 切换节点 (A 失败切 B)。
--   失败冷却 (等价静态 upstream 的 max_fails=1 fail_timeout): 节点连接失败后
--   冷却 FAIL_COOLDOWN 秒, 冷却节点【降级】到 peers 列表尾部作兜底而非剔除:
--   正常请求仍优先走健康节点, 不会先撞坏节点 (超时型故障每次要耗 5s
--   connect_timeout, RT 不可接受); 一旦健康节点失败, 冷却节点立即接管,
--   消除「冷却残影」—— 容器已恢复但冷却未到期的窗口内不再是零可用节点。
--   同时消除「DNS 缓存残影」: 连接失败时同步失效该域名 DNS 缓存, 容器重启
--   换 IP 后下一请求即拿到新 IP (新 IP 无冷却记录, 直接恢复为健康节点);
--   解析失败的旧值回退有时间上限 (DNS_STALE_TTL), 长期停止的节点 (如部署
--   结束后已删除的 backup) 在窗口后彻底退出 peers, 不再被周期性僵尸探测。
--   后端容器任意时机启动 / 重启均可自动恢复。
-- ============================================================

local balancer     = require "ngx.balancer"
local dns_resolver = require "resty.dns.resolver"
local lrucache     = require "resty.lrucache"

local _M = { _VERSION = '1.0' }

-- Docker 内嵌 DNS (用户自定义网络中的容器域名由其负责解析)
local NAMESERVER    = '127.0.0.11'
-- 解析缓存秒数, 与 nginx.conf 中 resolver valid 保持一致;
-- 后端容器重启换 IP 后, 最长该时间窗口内自动恢复 (调小恢复更快, DNS 查询更频繁)
local DNS_CACHE_TTL = 10
-- 解析失败时旧值回退的时限 (秒): 仅当缓存过期未超过该时长才回退旧 IP,
-- 兜底容器短暂重启 / DNS 抖动窗口; 超过则视为节点已死亡, 彻底退出 peers
local DNS_STALE_TTL = 30
-- 单次 DNS 查询超时 (ms) 与重传次数
local DNS_TIMEOUT   = 2000
local DNS_RETRANS   = 2

-- 节点连接失败后的冷却秒数 (等价 max_fails=1 fail_timeout=10s):
-- 冷却期内该节点降级到 peers 尾部 (不作首选但仍可兜底接管), 到期后自动恢复
local FAIL_COOLDOWN = 10

-- 解析失败时供 balancer 指向的占位地址 (本机拒绝连接, 使本次尝试立即失败)
local BLACKHOLE_HOST = '127.0.0.1'
local BLACKHOLE_PORT = 1

-- worker 级 IP 缓存: { [host] = { ip = "x.x.x.x", expire = 过期时间戳 } }
local ip_cache = lrucache.new(128)

-- 失败冷却存储, 优先用跨 worker 共享的 shm (nginx.conf 中 lua_shared_dict),
-- 未配置时退化为 worker 级 lrucache (行为等价 nginx 默认静态 upstream 的每 worker 统计):
-- { ["ip:port"] = 冷却到期时间戳 }
local fail_store = ngx.shared.dyn_balancer_fail or lrucache.new(128)

local function peer_key(ip, port)
    return ip .. ':' .. port
end

-- 解析失败时的旧值回退: 仅当缓存过期未超过 DNS_STALE_TTL 秒才返回旧 IP
-- (兜底容器短暂重启 / DNS 抖动窗口); 超过则视为节点已死亡返回 nil,
-- 使其彻底退出 peers, 不再以僵尸 IP 周期性消耗探测请求
local function stale_fallback(hit, now, err)
    if hit and (hit.expire + DNS_STALE_TTL) > now then
        return hit.ip
    end
    return nil, err
end

-- 解析 host, 成功返回 ip; 查询失败时若有旧值则回退旧值 (有时限, 见 stale_fallback)
-- 仅可在允许 cosocket 的阶段调用 (如 access_by_lua)
local function resolve(host)
    local now = ngx.now()
    local hit = ip_cache:get(host)
    if hit and hit.expire > now then
        return hit.ip
    end

    local r, err = dns_resolver:new({
        nameservers = { NAMESERVER },
        timeout     = DNS_TIMEOUT,
        retrans     = DNS_RETRANS,
    })
    if not r then
        return stale_fallback(hit, now, 'dns resolver new() failed: ' .. tostring(err))
    end

    local answers, qerr = r:query(host, { qtype = dns_resolver.TYPE_A })
    if not answers then
        -- 域名暂未注册 (后端容器还没起来) 等情况; 不缓存失败结果, 下次请求即重试
        return stale_fallback(hit, now, tostring(qerr))
    end

    local ip
    for _, ans in ipairs(answers) do
        if ans.address then
            ip = ans.address
            break
        end
    end

    if not ip then
        return stale_fallback(hit, now, 'no A record for ' .. host)
    end

    ip_cache:set(host, { ip = ip, expire = now + DNS_CACHE_TTL })
    return ip
end

-- 组装 A/Backup 双节点列表, 命名规则: <service>-<ENVIRONMENT>-<VERSION> 与 -backup 后缀
function _M.nodes(service, port)
    local env = os.getenv('ENVIRONMENT') or 'dev'
    local ver = os.getenv('VERSION') or 'main'
    local base = service .. '-' .. env .. '-' .. ver
    return {
        { host = base,               port = port },  -- A 节点
        { host = base .. '-backup',  port = port },  -- B 节点 (backup)
    }
end

-- 单节点服务完整域名: <service>-<ENVIRONMENT>-<VERSION>
function _M.host(service)
    local env = os.getenv('ENVIRONMENT') or 'dev'
    local ver = os.getenv('VERSION') or 'main'
    return service .. '-' .. env .. '-' .. ver
end

-- access_by_lua_block 入口, nodes 为 _M.nodes() 的返回值;
-- 逐节点解析 DNS (带 TTL 缓存), 健康节点在前, 处于失败冷却期的节点降级到
-- 尾部按冷却到期时间升序兜底 (而非剔除), 结果写入 ngx.ctx 供 balancer 消费。
-- 降级而非剔除的原因: 冷却登记时刻与容器真实恢复时刻不同步 (如容器重启
-- 过程中探测失败, 冷却却持续到容器 healthy 之后), 剔除会使该窗口内健康
-- 节点一失败就没有任何可尝试的节点; 降级则冷却节点仍可作为第二候选接管,
-- 同时正常请求依然优先走健康节点, 不必为坏节点付出连接超时代价
function _M.resolve_nodes(nodes)
    local now = ngx.now()
    local up, cooled = {}, {}
    for _, node in ipairs(nodes) do
        local ip, err = resolve(node.host)
        if not ip then
            -- 该节点域名暂无法解析且超出旧值回退时限 (后端容器未启动/已停止), 跳过
            ngx.log(ngx.WARN, '[dynamic_balancer] dns resolve failed, host=', node.host,
                    ', err=', err)
        else
            local peer = { host = node.host, ip = ip, port = node.port }
            local expire = fail_store:get(peer_key(ip, node.port))
            if expire and expire > now then
                cooled[#cooled + 1] = { peer = peer, expire = expire }
            else
                up[#up + 1] = peer
            end
        end
    end

    -- 冷却节点按到期时间升序追加到尾部: 全部节点都在冷却时, 最先到期的
    -- 优先被兜底尝试 (与原「保留最先到期节点」语义一致, 且其余节点可依次接续)
    if #cooled > 0 then
        table.sort(cooled, function(a, b) return a.expire < b.expire end)
        for _, c in ipairs(cooled) do
            up[#up + 1] = c.peer
        end
    end

    ngx.ctx.dyn_peers = up
    ngx.ctx.dyn_balancer_attempt = 0
end

-- balancer_by_lua_block 入口; proxy_next_upstream 每次重试都会重新进入本函数,
-- attempt 递增用于切换节点 (A -> B)
function _M.balance()
    local ctx = ngx.ctx
    local peers = ctx.dyn_peers
    local attempt = (ctx.dyn_balancer_attempt or 0) + 1
    ctx.dyn_balancer_attempt = attempt

    -- 本函数被重新进入即说明前一个节点尝试失败, 登记失败冷却,
    -- 使后续请求在 resolve_nodes 阶段降级该节点 (等价 fail_timeout);
    -- 同时失效其 DNS 缓存, 消除「DNS 缓存残影」: 若容器已重启换 IP,
    -- 下一请求立即重新解析, 新 IP 无冷却记录, 直接恢复为健康节点。
    -- 注: 最后一次尝试的失败无重试回调可记录, 不入冷却, 兜底节点恢复零延迟
    if attempt > 1 and peers then
        local prev = peers[attempt - 1]
        local state = balancer.get_last_failure()
        if prev and state then
            fail_store:set(peer_key(prev.ip, prev.port), ngx.now() + FAIL_COOLDOWN)
            ip_cache:delete(prev.host)
            ngx.log(ngx.WARN, '[dynamic_balancer] peer failed and cooled down, host=',
                    prev.host, ', ip=', prev.ip, ', state=', state,
                    ', cooldown=', FAIL_COOLDOWN .. 's')
        end
    end

    -- 关键: upstream 内只有一个占位 server, nginx 按静态 server 数量判断
    -- "是否还有节点可重试", 不显式声明 extra tries 的话连接失败后不会触发
    -- proxy_next_upstream 重试 (表现为 A 失败直接 502, 不切 B);
    -- 总尝试次数仍受 proxy_next_upstream_tries 上限约束
    local extra = peers and (#peers - attempt) or 0
    if extra > 0 then
        local ok, err = balancer.set_more_tries(extra)
        if not ok then
            ngx.log(ngx.WARN, '[dynamic_balancer] set_more_tries failed, extra=', extra,
                    ', err=', err)
        end
    end

    local peer = peers and peers[attempt]
    if not peer then
        -- 无可用节点或全部节点已尝试失败, 指向占位地址令本次立即失败,
        -- 由 proxy_next_upstream_tries 结束重试并向客户端返回 502/503
        balancer.set_current_peer(BLACKHOLE_HOST, BLACKHOLE_PORT)
        return
    end

    local ok, err = balancer.set_current_peer(peer.ip, peer.port)
    if not ok then
        ngx.log(ngx.ERR, '[dynamic_balancer] set_current_peer failed, ip=', peer.ip,
                ', port=', peer.port, ', err=', err)
    end
end

return _M
