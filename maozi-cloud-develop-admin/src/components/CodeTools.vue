<template>
  <div>
    <div class="ct-hero">
      <div class="ct-deco ct-deco-1"></div>
      <div class="ct-deco ct-deco-2"></div>
      <div class="ct-hero-mark">🧰</div>
      <div class="ct-hero-left">
        <div class="ct-hero-name">🧰 编程工具</div>
        <div class="ct-hero-sub">开发常用小工具，数据均在本机处理</div>
        <div class="ct-hero-chips">
          <span class="ct-chip">🧩 JSON 格式化 · 支持注释</span>
          <span class="ct-chip">🕐 时间戳双向转换</span>
          <span class="ct-chip">🔐 密码 / UUID 生成 · 加密随机数</span>
          <span class="ct-chip">🧬 Base64 · 文本与图片</span>
          <span class="ct-chip">🔄 项目批量重命名 · 多线程</span>
        </div>
      </div>
    </div>

    <div class="tools-card">
      <div class="seg" role="tablist" aria-label="工具切换">
        <div class="seg-thumb" :style="segThumbStyle"></div>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'json' }" :aria-selected="activeTab === 'json'" @click="activeTab = 'json'">
          <span class="seg-ico blue"><span class="tt-emoji">🧩</span></span>
          <span>JSON 格式化</span>
        </button>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'ts' }" :aria-selected="activeTab === 'ts'" @click="activeTab = 'ts'">
          <span class="seg-ico amber"><span class="tt-emoji tt-emoji-clock">🕐</span></span>
          <span>时间戳转换</span>
        </button>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'pwd' }" :aria-selected="activeTab === 'pwd'" @click="activeTab = 'pwd'">
          <span class="seg-ico violet"><span class="tt-emoji">🔐</span></span>
          <span>密码生成</span>
        </button>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'b64' }" :aria-selected="activeTab === 'b64'" @click="activeTab = 'b64'">
          <span class="seg-ico teal"><span class="tt-emoji">🧬</span></span>
          <span>Base64</span>
        </button>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'rn' }" :aria-selected="activeTab === 'rn'" @click="activeTab = 'rn'">
          <span class="seg-ico orange"><span class="tt-emoji">🔄</span></span>
          <span>项目重命名</span>
        </button>
      </div>

      <!-- ===== JSON 格式化编辑器 ===== -->
      <div v-show="activeTab === 'json'" class="pane-box">
          <div class="json-toolbar">
            <el-button type="primary" @click="formatJson">格式化</el-button>
            <el-select v-model="indent" style="width: 128px">
              <el-option label="缩进 2 空格" value="2" />
              <el-option label="缩进 4 空格" value="4" />
              <el-option label="缩进 Tab" value="tab" />
            </el-select>
            <el-button @click="copyMinified">压缩复制</el-button>
            <el-button @click="copyEscaped">压缩转义并复制</el-button>
            <el-button @click="copyJson">复制</el-button>
            <el-button @click="clearJson">清空</el-button>
            <el-button text type="primary" @click="loadSample">示例</el-button>
          </div>

          <div class="json-editor">
            <!-- 行号栏：号码按 .ln 块实测 offsetTop 定位（自动换行的逻辑行占多行时贴块首行），
                 translateY 跟随滚动；当前光标行以行号变蓝加粗提示（无背景高亮条） -->
            <div class="json-gutter" aria-hidden="true">
              <div class="json-gutter-inner" :style="{ transform: `translateY(${-jsonScrollTop}px)` }">
                <div
                  v-for="n in jsonLineCount"
                  :key="n"
                  class="json-gutter-num"
                  :class="{ active: n === caretLine }"
                  :style="{ top: `${lineTops[n - 1] ?? JSON_PAD_TOP + (n - 1) * JSON_LINE_PITCH}px` }"
                >{{ n }}</div>
              </div>
            </div>
            <pre ref="highlightRef" class="json-highlight" aria-hidden="true"><code v-html="highlightedJson"></code></pre>
            <el-input
              ref="jsonInputRef"
              v-model="jsonText"
              type="textarea"
              :rows="16"
              spellcheck="false"
              class="json-area"
              placeholder="粘贴或输入 JSON，自动校验；手写支持 { [ &quot; 自动配对、回车缩进"
              @keydown="onJsonKeydown"
            />
          </div>

          <div class="json-status" :class="jsonText.trim() ? (jsonError ? 'bad' : 'ok') : 'idle'">
            <span class="status-dot"></span>
            <span class="status-text">
              <template v-if="!jsonText.trim()">等待输入…</template>
              <template v-else-if="jsonError">
                ✗ {{ jsonError.msg }}<template v-if="jsonError.line">（第 {{ jsonError.line }} 行 第 {{ jsonError.col }} 列）</template>
              </template>
              <template v-else>✓ 有效 JSON{{ hasComments ? '（含注释）' : '' }}</template>
            </span>
            <span v-if="jsonText.trim()" class="status-chips">
              <span class="st-chip">{{ jsonText.length }} 字符</span>
              <span class="st-chip">{{ jsonLineCount }} 行</span>
            </span>
          </div>
      </div>

      <!-- ===== 时间戳转换 ===== -->
      <div v-show="activeTab === 'ts'" class="pane-box">
          <!-- 当前时间 -->
          <div class="now-bar">
            <div class="now-deco now-deco-1"></div>
            <div class="now-deco now-deco-2"></div>
            <div class="now-left">
              <div class="now-time mono-cell">{{ fmtDateTime(now) }}</div>
              <div class="now-meta">{{ fmtDay(now) }} · 星期{{ weekdayOf(now) }} · {{ tzLabel }} · 实时</div>
            </div>
            <div class="now-ts">
              <div class="ts-chip" title="点击复制秒级时间戳" @click="copyText(String(secOf(now)))">
                <span class="chip-label">秒级</span>
                <span class="mono-cell">{{ secOf(now) }}</span>
              </div>
              <div class="ts-chip" title="点击复制毫秒级时间戳" @click="copyText(String(now.getTime()))">
                <span class="chip-label">毫秒</span>
                <span class="mono-cell">{{ now.getTime() }}</span>
              </div>
            </div>
          </div>

          <div class="ts-grid">
            <!-- 时间戳 → 时间 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico to-time">🕒</span>
                <span class="ts-card-title">时间戳 → 时间</span>
              </div>
              <div class="ts-input-row">
                <el-input
                  v-model="tsInput"
                  placeholder="输入 10 位秒级 / 13 位毫秒级时间戳"
                  class="mono-cell"
                  clearable
                />
                <el-button text type="primary" @click="tsInput = String(secOf(now))">此刻</el-button>
              </div>

              <template v-if="tsDate">
                <div class="ts-row" title="点击复制" @click="copyText(fmtDateTime(tsDate, true))">
                  <span class="ts-row-label">本地时间</span>
                  <span class="ts-row-value mono-cell">{{ fmtDateTime(tsDate, true) }}</span>
                </div>
                <div class="ts-row" title="点击复制" @click="copyText(tsDate.toISOString())">
                  <span class="ts-row-label">ISO 8601</span>
                  <span class="ts-row-value mono-cell">{{ tsDate.toISOString() }}</span>
                </div>
                <div class="ts-row" title="点击复制" @click="copyText(fmtUtc(tsDate, true))">
                  <span class="ts-row-label">UTC 时间</span>
                  <span class="ts-row-value mono-cell">{{ fmtUtc(tsDate, true) }}</span>
                </div>
                <div class="ts-row-inline">
                  <span>星期{{ weekdayOf(tsDate) }}</span>
                  <span class="ts-rel">{{ relativeTo(tsDate, now) }}</span>
                </div>
              </template>
              <div v-else-if="tsInput.trim()" class="ts-hint bad-hint">请输入纯数字时间戳</div>
              <div v-else class="ts-hint">输入后自动识别秒级 / 毫秒级</div>
            </div>

            <!-- 时间 → 时间戳 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico to-ts">📅</span>
                <span class="ts-card-title">时间 → 时间戳</span>
              </div>
              <el-date-picker
                v-model="pickDate"
                type="datetime"
                placeholder="选择日期时间"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm:ss.SSS"
                :shortcuts="dateShortcuts"
              />

              <template v-if="pickDate">
                <div class="ts-row" title="点击复制秒级时间戳" @click="copyText(String(secOf(pickDate)))">
                  <span class="ts-row-label">秒级时间戳</span>
                  <span class="ts-row-value mono-cell">{{ secOf(pickDate) }}</span>
                </div>
                <div class="ts-row" title="点击复制毫秒级时间戳" @click="copyText(String(pickDate.getTime()))">
                  <span class="ts-row-label">毫秒级时间戳</span>
                  <span class="ts-row-value mono-cell">{{ pickDate.getTime() }}</span>
                </div>
                <div class="ts-row" title="点击复制" @click="copyText(fmtDateTime(pickDate, true))">
                  <span class="ts-row-label">本地时间</span>
                  <span class="ts-row-value mono-cell">{{ fmtDateTime(pickDate, true) }}</span>
                </div>
              </template>
              <div v-else class="ts-hint" style="margin-top: 12px">请选择日期时间</div>
            </div>
          </div>
      </div>

      <!-- ===== 密码生成 ===== -->
      <div v-show="activeTab === 'pwd'" class="pane-box">
          <!-- 强度总览横幅：熵值与进度条实时反映当前配置 -->
          <div class="pwd-banner">
            <div class="pwd-banner-deco pwd-banner-deco-1"></div>
            <div class="pwd-banner-deco pwd-banner-deco-2"></div>
            <div class="pwd-banner-left">
              <div class="pwd-entropy-row">
                <span class="pwd-entropy-val mono-cell">{{ pwdEntropyBits.toFixed(0) }}</span>
                <span class="pwd-entropy-unit">bit 熵</span>
                <span class="pwd-badge" :class="pwdStrength.cls">{{ pwdStrength.label }}</span>
              </div>
              <div class="pwd-entropy-track">
                <div class="pwd-entropy-fill" :class="pwdStrength.cls" :style="{ width: pwdEntropyPct + '%' }"></div>
              </div>
              <div class="pwd-banner-meta">
                <template v-if="pwdFormat === 'uuid'">
                  UUID {{ pwdUuidVersion === 'v4' ? 'V4' : 'V7' }} · RFC 4122 · {{ pwdUuidVersion === 'v4' ? '122' : '74' }} bit 随机熵{{ pwdUuidVersion === 'v7' ? ' + 48 bit 时间戳' : '' }}
                </template>
                <template v-else>长度 {{ pwdLen ?? 16 }} · 字符池 {{ pwdPoolAll.length }} 个字符 · 每类必含</template>
              </div>
            </div>
            <div class="pwd-banner-shield">🛡️</div>
          </div>

          <div class="pwd-grid">
            <!-- 生成选项 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico pwd-ico-set">🔐</span>
                <span class="ts-card-title">生成选项</span>
              </div>

              <div class="pwd-field">
                <div class="pwd-field-label"><span>生成格式</span></div>
                <el-segmented v-model="pwdFormat" :options="pwdFormatOptions" class="pwd-count-seg" />
              </div>

              <div v-if="pwdFormat === 'password'" class="pwd-field">
                <div class="pwd-field-label">
                  <span>密码长度</span>
                  <el-input-number v-model="pwdLen" :min="4" :max="64" :value-on-clear="16" size="small" style="width: 104px" />
                </div>
                <el-slider v-model="pwdLen" :min="4" :max="64" :show-tooltip="false" :marks="pwdLenMarks" class="pwd-slider" />
              </div>

              <!-- UUID 选项：版本 + 输出格式 -->
              <template v-if="pwdFormat === 'uuid'">
                <div class="pwd-field">
                  <div class="pwd-field-label">
                    <span>UUID 版本</span>
                    <span class="pwd-field-tip">{{ pwdUuidVersion === 'v7' ? '时间戳前缀 · 按时间排序' : '全随机 · RFC 4122' }}</span>
                  </div>
                  <el-segmented v-model="pwdUuidVersion" :options="pwdUuidVersionOptions" class="pwd-count-seg" />
                </div>

                <div class="pwd-field">
                  <div class="pwd-field-label"><span>输出格式</span></div>
                  <div class="pwd-uuid-opts">
                    <el-switch v-model="pwdUuidUpper" size="small" active-text="大写" />
                    <el-switch v-model="pwdUuidHyphen" size="small" active-text="连字符" />
                    <el-segmented v-model="pwdUuidWrap" :options="pwdUuidWrapOptions" class="pwd-uuid-wrap" />
                  </div>
                </div>
              </template>

              <div v-if="pwdFormat === 'password'" class="pwd-field">
                <div class="pwd-field-label">
                  <span>字符类型</span>
                  <span class="pwd-field-tip">至少选一类</span>
                </div>
                <div class="pwd-type-grid">
                  <button
                    v-for="t in pwdTypes"
                    :key="t.key"
                    type="button"
                    class="pwd-type"
                    :class="{ on: t.model.value }"
                    :aria-pressed="t.model.value"
                    @click="t.model.value = !t.model.value"
                  >
                    <span class="pwd-type-ico mono-cell">{{ t.glyph }}</span>
                    <span class="pwd-type-text">
                      <span class="pwd-type-name">{{ t.label }}</span>
                      <span class="pwd-type-range mono-cell">{{ t.range }}</span>
                    </span>
                    <span class="pwd-type-check">✓</span>
                  </button>
                </div>
              </div>

              <div v-if="pwdFormat === 'password'" class="pwd-field">
                <div class="pwd-field-label">
                  <span>排除字符</span>
                  <el-button text type="primary" size="small" @click="pwdExclude = '0OoIl1|'">易混淆字符</el-button>
                </div>
                <el-input v-model="pwdExclude" placeholder="输入不希望出现的字符，可留空" clearable />
                <transition-group v-if="pwdExcludeChips.length" name="chip" tag="div" class="pwd-chips">
                  <span v-for="c in pwdExcludeChips" :key="c" class="pwd-chip mono-cell" title="点击移除" @click="removeExclude(c)">
                    {{ c }}<i>✕</i>
                  </span>
                </transition-group>
              </div>

              <div class="pwd-field">
                <div class="pwd-field-label"><span>生成数量</span></div>
                <el-segmented v-model="pwdCount" :options="pwdCountOptions" class="pwd-count-seg" />
              </div>

              <el-button type="primary" class="pwd-gen-btn" @click="genPasswords">
                <span class="pwd-gen-ico">{{ pwdFormat === 'uuid' ? '🆔' : '🎲' }}</span>
                <span>{{ pwdFormat === 'uuid' ? '生成 UUID' : '生成密码' }}</span>
              </el-button>
            </div>

            <!-- 生成结果 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico pwd-ico-res">📋</span>
                <span class="ts-card-title">生成结果</span>
                <span v-if="pwdResults.length" class="pwd-result-count">{{ pwdResults.length }} 条</span>
                <el-button v-if="pwdResults.length" text type="primary" size="small" class="pwd-head-btn" @click="genPasswords">🔁 重新生成</el-button>
                <el-button v-if="pwdResults.length" text type="primary" size="small" class="pwd-head-btn" @click="copyAllPasswords">复制全部</el-button>
              </div>

              <div v-if="pwdResults.length" class="pwd-list">
                <div
                  v-for="(p, i) in pwdResults"
                  :key="pwdBatch + '-' + i"
                  class="pwd-row"
                  :style="{ '--i': i }"
                  title="点击复制"
                  @click="copyText(p)"
                >
                  <span class="pwd-row-idx mono-cell">{{ i + 1 }}</span>
                  <!-- 字符分类着色：数字蓝 / 大写紫 / 小写绿 / 符号橙 -->
                  <span class="pwd-row-value mono-cell" v-html="coloredPwd(p)"></span>
                  <span class="pwd-row-copy">复制</span>
                </div>
              </div>

              <div v-else class="pwd-empty">
                <div class="pwd-empty-ico">🎲</div>
                <div class="pwd-empty-title">还没有密码</div>
                <div class="pwd-empty-sub">左侧选择字符类型，点击「生成密码」</div>
              </div>

              <div v-if="pwdResults.length" class="pwd-meta">
                <span v-if="pwdFormat === 'uuid'">🆔 RFC 4122 · 加密随机数{{ pwdUuidVersion === 'v7' ? ' · 毫秒时间戳前缀' : '' }}</span>
                <span v-else>🔐 加密随机数 · 每类必含 · 位置已打乱</span>
              </div>
            </div>
          </div>
      </div>

      <!-- ===== Base64 编解码 ===== -->
      <div v-show="activeTab === 'b64'" class="pane-box">
          <div class="ts-grid">
            <!-- 文本 Base64 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico b64-ico-text">🔤</span>
                <span class="ts-card-title">文本 Base64</span>
                <span class="b64-mode-tip">{{ b64Mode === 'encode' ? '文本 → Base64' : 'Base64 → 文本' }}</span>
              </div>

              <div class="b64-io-label">
                <span>输入</span>
                <span v-if="b64Input" class="mono-cell">{{ b64Input.length }} 字符</span>
              </div>
              <el-input
                v-model="b64Input"
                type="textarea"
                :rows="4"
                spellcheck="false"
                class="b64-area mono-cell"
                :placeholder="b64Mode === 'encode' ? '输入要编码的文本，支持中文与 Emoji' : '输入 Base64，自动忽略空白与 Data URL 前缀'"
              />

              <div class="b64-toolbar">
                <el-segmented v-model="b64Mode" :options="b64ModeOptions" class="b64-seg" @change="b64Run" />
                <el-switch v-model="b64UrlSafe" size="small" active-text="URL Safe" @change="b64Run" />
              </div>

              <div class="b64-io-label"><span>输出</span></div>
              <div class="b64-output mono-cell" :class="{ err: b64Error }">
                <template v-if="b64Error">✗ {{ b64Error }}</template>
                <template v-else>{{ b64Output || '—' }}</template>
              </div>

              <div class="b64-status">
                <span v-if="b64Error" class="bad">✗ 转换失败</span>
                <span v-else-if="b64Output" class="ok">✓ {{ b64Output.length }} 字符</span>
                <span v-else>选择方向立即转换</span>
                <span class="b64-status-btns">
                  <el-button v-if="b64Output && !b64Error" text type="primary" size="small" @click="copyText(b64Output)">复制结果</el-button>
                  <el-button text size="small" @click="b64Swap">⇅ 交换</el-button>
                  <el-button text size="small" @click="b64Clear">清空</el-button>
                </span>
              </div>
            </div>

            <!-- 图片 ⇄ Base64 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico b64-ico-img">🖼️</span>
                <span class="ts-card-title">图片 ⇄ Base64</span>
              </div>

              <!-- 拖拽 / 点击选择 / 粘贴截图 → Data URL -->
              <div
                class="img-dropzone"
                :class="{ over: imgDragging }"
                title="拖拽图片到此处，或点击选择文件"
                @click="imgPick"
                @dragenter.prevent="imgDragging = true"
                @dragover.prevent
                @dragleave.prevent="imgDragging = false"
                @drop.prevent="onImgDrop"
              >
                <input ref="imgFileRef" type="file" accept="image/*" hidden @change="onImgFile" />
                <div class="img-drop-hint">
                  <div class="img-drop-ico" :class="{ float: !imgUrl }">🖼️</div>
                  <div class="img-drop-title">拖拽图片到此处，或点击选择</div>
                  <div class="img-drop-sub">Ctrl+V 粘贴截图 · 拖入 Base64 文本可反向预览</div>
                  <div class="img-drop-formats">
                    <span>PNG</span><span>JPG</span><span>GIF</span><span>WebP</span><span>SVG</span>
                  </div>
                </div>
              </div>

              <transition name="img-fade">
                <div v-if="imgUrl" class="img-result">
                  <div class="img-preview"><img :src="imgUrl" alt="图片预览" /></div>
                  <div class="img-pills">
                    <span class="img-pill" :title="imgName">📄 {{ imgName }}</span>
                    <span class="img-pill">🏷️ {{ imgType }}</span>
                    <span class="img-pill">📦 {{ fmtSize(imgSize) }}</span>
                    <span class="img-pill hl">🔤 Base64 {{ fmtSize(imgUrl.length) }}</span>
                  </div>
                  <div class="img-actions">
                    <el-button type="primary" class="grad-btn" size="small" @click="copyText(imgUrl, '已复制 Data URL')">复制 Data URL</el-button>
                    <el-button size="small" @click="copyText(pureB64(), '已复制纯 Base64')">复制纯 Base64</el-button>
                    <el-button size="small" text type="danger" @click="imgReset">移除</el-button>
                  </div>
                </div>
              </transition>

              <!-- 反向：Base64 → 图片预览 -->
              <div class="img-decode">
                <div class="pwd-field-label"><span>Base64 → 图片预览</span></div>
                <div class="img-decode-row">
                  <el-input
                    v-model="imgDecodeInput"
                    placeholder="粘贴 Data URL 或纯 Base64，自动识别图片类型"
                    class="mono-cell"
                    clearable
                    @keyup.enter="imgPreviewDecode"
                  />
                  <el-button class="grad-plain-btn" @click="imgPreviewDecode">预览</el-button>
                </div>
                <div v-if="imgDecodeSrc" class="img-preview"><img :src="imgDecodeSrc" alt="解码预览" /></div>
                <div v-else-if="imgDecodeError" class="ts-hint bad-hint">{{ imgDecodeError }}</div>
              </div>
            </div>
          </div>
      </div>

      <!-- ===== 项目批量重命名 ===== -->
      <div v-show="activeTab === 'rn'" class="pane-box">
          <!-- 引擎横幅：状态徽章 + 配置实时总览 -->
          <div class="rn-banner">
            <div class="rn-banner-deco rn-banner-deco-1"></div>
            <div class="rn-banner-deco rn-banner-deco-2"></div>
            <div class="rn-banner-left">
              <div class="rn-banner-title-row">
                <span class="rn-banner-title">🛠️ 批量重命名引擎</span>
                <span class="rn-badge" :class="rnStatusBadge.cls">{{ rnStatusBadge.label }}</span>
              </div>
              <div class="rn-banner-sub">无限深度 · worker 线程池并行 · 字节级替换保持编码 · 含根目录自身</div>
              <div class="rn-banner-meta">
                规则 <b>{{ rnRuleCount }}</b> 组 · 排除 <b>{{ rnExcludeTags.length }}</b> 目录 · 线程 <b>{{ rnWorkerCount }}</b>
                <br />内容 <b>{{ rnContent ? '开' : '关' }}</b> · 文件名 <b>{{ rnRenameFiles ? '开' : '关' }}</b> · 目录名 <b>{{ rnRenameDirs ? '开' : '关' }}</b>
              </div>
            </div>
            <div class="rn-banner-mark">🔄</div>
          </div>

          <div class="ts-grid">
            <!-- 配置 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico rn-ico-set">🔄</span>
                <span class="ts-card-title">项目重命名</span>
              </div>

              <!-- 目录卡：整卡可点 -->
              <div class="rn-dir-card" :class="{ set: !!rnDir }" title="点击选择目录" @click="rnPickDir">
                <span class="rn-dir-ico">📂</span>
                <span class="rn-dir-info">
                  <span class="rn-dir-label">{{ rnDir ? '目标目录 · 含全部子层级' : '选择目录' }}</span>
                  <span class="rn-dir-path mono-cell">{{ rnDir || '点击选择要重命名的项目根目录' }}</span>
                </span>
                <span class="rn-dir-act">{{ rnDir ? '更换' : '浏览' }}</span>
              </div>

              <div class="pwd-field">
                <div class="pwd-field-label">
                  <span>替换规则</span>
                  <el-button text type="primary" size="small" @click="rnAddRule">＋ 添加规则</el-button>
                </div>
                <transition-group name="chip" tag="div" class="rn-rules">
                  <div v-for="(r, i) in rnRules" :key="r.id" class="rn-rule">
                    <span class="rn-rule-idx mono-cell" :class="{ ok: r.from.length > 0 && r.from !== r.to }">{{ i + 1 }}</span>
                    <el-input v-model="r.from" placeholder="旧字符" class="mono-cell" clearable />
                    <span class="rn-arrow">➜</span>
                    <el-input v-model="r.to" placeholder="新字符（空 = 删除）" class="mono-cell" clearable />
                    <el-button text type="danger" size="small" class="rn-rule-del" :disabled="rnRules.length <= 1" @click="rnRemoveRule(i)">✕</el-button>
                  </div>
                </transition-group>
              </div>

              <div class="pwd-field">
                <div class="pwd-field-label">
                  <span>执行选项</span>
                  <span class="pwd-field-tip">至少保留一项替换操作</span>
                </div>
                <div class="rn-opt-grid">
                  <div class="rn-opt" :class="{ on: rnContent }">
                    <span class="rn-opt-ico">📝</span>
                    <span class="rn-opt-text">
                      <span class="rn-opt-name">替换文件内容</span>
                      <span class="rn-opt-sub">字节级 · 保持原编码</span>
                    </span>
                    <el-switch v-model="rnContent" size="small" />
                  </div>
                  <div class="rn-opt" :class="{ on: rnContent && rnTextOnly, dim: !rnContent }">
                    <span class="rn-opt-ico">🛡️</span>
                    <span class="rn-opt-text">
                      <span class="rn-opt-name">仅文本文件</span>
                      <span class="rn-opt-sub">白名单 + 二进制检测 · META-INF 豁免</span>
                    </span>
                    <el-switch v-model="rnTextOnly" size="small" :disabled="!rnContent" />
                  </div>
                  <div class="rn-opt" :class="{ on: rnRenameFiles }">
                    <span class="rn-opt-ico">📄</span>
                    <span class="rn-opt-text">
                      <span class="rn-opt-name">替换文件名称</span>
                      <span class="rn-opt-sub">所有层级文件名</span>
                    </span>
                    <el-switch v-model="rnRenameFiles" size="small" />
                  </div>
                  <div class="rn-opt" :class="{ on: rnRenameDirs }">
                    <span class="rn-opt-ico">📁</span>
                    <span class="rn-opt-text">
                      <span class="rn-opt-name">替换目录名称</span>
                      <span class="rn-opt-sub">含根目录自身</span>
                    </span>
                    <el-switch v-model="rnRenameDirs" size="small" />
                  </div>
                </div>
              </div>

              <div class="pwd-field">
                <div class="pwd-field-label">
                  <span>排除目录</span>
                  <span class="pwd-field-tip">回车或逗号添加 · 点 chip 移除</span>
                </div>
                <el-input v-model="rnExcludeInput" placeholder="如 vendor，回车添加" @keydown.enter.prevent="addExcludeTag" @keydown="onExcludeKeydown" />
                <transition-group name="chip" tag="div" class="pwd-chips rn-exclude-chips">
                  <span v-for="t in rnExcludeTags" :key="t" class="pwd-chip mono-cell rn-exclude-chip" title="点击移除" @click="removeExcludeTag(t)">
                    {{ t }}<i>✕</i>
                  </span>
                </transition-group>
              </div>

              <el-button type="primary" class="pwd-gen-btn rn-gen-btn" :disabled="rnRunning" @click="rnStart">
                <span class="pwd-gen-ico">{{ rnRunning ? '⏳' : '🔄' }}</span>
                <span>{{ rnRunning ? '执行中…' : '开始重命名' }}</span>
              </el-button>
              <el-button v-if="rnRunning" text type="danger" class="rn-cancel-btn" @click="rnCancelRun">取消执行</el-button>
            </div>

            <!-- 进度 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico rn-ico-res">📊</span>
                <span class="ts-card-title">执行进度</span>
                <span v-if="rnRunning" class="rn-live"><i></i>{{ rnWorkerCount }} 线程并行</span>
              </div>

              <!-- 阶段步骤条：圆点 + 连接线 -->
              <div class="rn-steps">
                <template v-for="(s, i) in RN_STAGES" :key="s.key">
                  <div class="rn-step" :class="rnStageClass(s.key)">
                    <span class="rn-step-dot"></span>
                    <span class="rn-step-label">{{ s.label }}</span>
                  </div>
                  <span v-if="i < RN_STAGES.length - 1" class="rn-step-line" :class="rnLineClass(i)"></span>
                </template>
              </div>

              <!-- worker 池活动可视化（运行中） -->
              <div v-if="rnRunning" class="rn-workers">
                <span class="rn-workers-label">worker 池</span>
                <span
                  v-for="i in rnWorkerCount"
                  :key="i"
                  class="rn-worker"
                  :style="{ '--d': (i - 1) * 0.12 + 's' }"
                ></span>
              </div>

              <!-- 进度条（运行中扫光） -->
              <div class="rn-bar-row">
                <div class="rn-bar">
                  <div class="rn-bar-fill" :class="{ live: rnRunning }" :style="{ width: rnPct + '%' }"></div>
                </div>
                <span class="rn-pct mono-cell">{{ rnPct }}%</span>
              </div>
              <div class="rn-count mono-cell">{{ rnDone }} / {{ rnTotal || '—' }} 项</div>
              <div class="rn-current mono-cell" :title="rnCurrent">{{ rnCurrent || '等待执行' }}</div>

              <!-- 统计四宫格 -->
              <div v-if="rnStats" class="rn-stats">
                <div class="rn-stat s1">
                  <span class="rn-stat-ico">📝</span>
                  <div class="rn-stat-val mono-cell">{{ rnStats.contentChanged }}</div>
                  <div class="rn-stat-label">内容改动文件</div>
                </div>
                <div class="rn-stat s2">
                  <span class="rn-stat-ico">✨</span>
                  <div class="rn-stat-val mono-cell">{{ rnStats.replaceCount }}</div>
                  <div class="rn-stat-label">替换总处数</div>
                </div>
                <div class="rn-stat s3">
                  <span class="rn-stat-ico">📄</span>
                  <div class="rn-stat-val mono-cell">{{ rnStats.filesRenamed }}</div>
                  <div class="rn-stat-label">文件重命名</div>
                </div>
                <div class="rn-stat s4">
                  <span class="rn-stat-ico">📁</span>
                  <div class="rn-stat-val mono-cell">{{ rnStats.dirsRenamed }}</div>
                  <div class="rn-stat-label">目录重命名</div>
                </div>
              </div>

              <!-- 完成 / 错误摘要 -->
              <div v-if="rnStage === 'done'" class="rn-done">
                <div class="rn-done-line ok">
                  {{ rnStats?.cancelled ? '⏹ 已取消（已完成部分保留）' : '✓ 重命名完成' }}
                </div>
                <div v-if="rnStats" class="rn-done-sub">
                  扫描 {{ rnStats.scannedFiles }} 文件 · {{ rnStats.scannedDirs }} 目录
                  <template v-if="rnStats.skippedLarge"> · 跳过大文件 {{ rnStats.skippedLarge }}</template>
                </div>
                <div v-if="rnStats?.rootRenamed && rnNewRoot" class="rn-newroot">
                  <span class="rn-newroot-label">📂 根目录新路径</span>
                  <span class="rn-newroot-path mono-cell">{{ rnNewRoot }}</span>
                </div>
                <div v-if="rnStats?.errors.length" class="rn-errors">
                  <div class="rn-errors-title">⚠ {{ rnStats.errors.length }} 项跳过/失败：</div>
                  <div v-for="(e, i) in rnStats.errors.slice(0, 8)" :key="i" class="rn-error-line mono-cell">{{ e }}</div>
                  <div v-if="rnStats.errors.length > 8" class="rn-errors-more">… 共 {{ rnStats.errors.length }} 条</div>
                </div>
              </div>
              <div v-else-if="rnStage === 'error'" class="rn-done">
                <div class="rn-done-line bad">✗ {{ rnError || '执行失败' }}</div>
              </div>
              <div v-else-if="rnStage === 'idle'" class="pwd-empty rn-empty">
                <div class="pwd-empty-ico">🗂️</div>
                <div class="pwd-empty-title">尚未执行</div>
                <div class="pwd-empty-sub">配置目录与规则后点击「开始重命名」</div>
                <div class="rn-empty-tags">
                  <span>无限深度</span><span>多线程并行</span><span>实时进度</span>
                </div>
              </div>
            </div>
          </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 编程工具页：JSON 格式化编辑器（校验/压缩/行号错误定位）、时间戳互转、密码生成、Base64 编解码与项目批量重命名
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import type { RenameStats } from '../api'

type ToolTab = 'json' | 'ts' | 'pwd' | 'b64' | 'rn'
const activeTab = ref<ToolTab>('json')

/** Tab 滑块按 Tab 数均分宽度、位移到当前项（位移动画由 .seg-thumb 的 transition 提供） */
const TOOL_TABS: ToolTab[] = ['json', 'ts', 'pwd', 'b64', 'rn']
const segThumbStyle = computed(() => ({
  width: `calc((100% - 8px) / ${TOOL_TABS.length})`,
  transform: `translateX(${Math.max(0, TOOL_TABS.indexOf(activeTab.value)) * 100}%)`
}))

/* ===================== JSON 格式化编辑器 ===================== */
const jsonText = ref('')
const indent = ref<'2' | '4' | 'tab'>('2')

interface JsonError {
  msg: string
  line?: number
  col?: number
}

/** 去除行注释与块注释（跳过字符串字面量，保留换行以维持行号），用于校验与压缩 */
function stripComments(text: string): string {
  let out = ''
  let i = 0
  const n = text.length
  while (i < n) {
    const c = text[i]
    if (c === '"') {
      out += c
      i++
      while (i < n) {
        out += text[i]
        if (text[i] === '\\' && i + 1 < n) {
          out += text[i + 1]
          i += 2
          continue
        }
        if (text[i] === '"') {
          i++
          break
        }
        i++
      }
      continue
    }
    if (c === '/' && text[i + 1] === '/') {
      while (i < n && text[i] !== '\n') i++
      continue
    }
    if (c === '/' && text[i + 1] === '*') {
      i += 2
      while (i < n && !(text[i] === '*' && text[i + 1] === '/')) {
        if (text[i] === '\n') out += '\n'
        i++
      }
      i += 2
      continue
    }
    out += c
    i++
  }
  return out
}

/** 校验（剥注释后 JSON.parse）：把报错 position 换算成行/列（行号按剥离注释后的文本计算） */
const jsonError = computed<JsonError | null>(() => {
  const text = jsonText.value
  if (!text.trim()) return null
  const stripped = stripComments(text)
  try {
    JSON.parse(stripped)
    return null
  } catch (err) {
    const msg = (err as Error).message
    const posMatch = msg.match(/position (\d+)/)
    if (posMatch) {
      const pos = Number(posMatch[1])
      const before = stripped.slice(0, pos)
      return { msg, line: before.split('\n').length, col: pos - before.lastIndexOf('\n') }
    }
    return { msg }
  }
})

const hasComments = computed(() => {
  const text = jsonText.value
  return text.trim() ? stripComments(text) !== text : false
})

const jsonLineCount = computed(() => jsonText.value.split('\n').length)

/* 行号栏状态：滚动跟随 + 光标所在行高亮 */
/**
 * 统一固定行高 17px（整数）：行号栏、当前行高亮、透明 textarea、着色 pre 四者共用，
 * 自动换行的逻辑行块高必为它的整数倍。不能用 line-height: normal——含中文的行会命中
 * CJK 回退字体（自然行高约 18.5px，大于等宽字体的 15px），该行被撑高并推移其后所有
 * 行，行号从首个中文行起整体错位；也不能用非整数 px——textarea 内部逐行整数量化，
 * 约 0.3px/行的取整差会累积成文字与光标错位。行号/当前行高亮的实际定位以 .ln 块的
 * 实测 offsetTop/offsetHeight 为准（见 lineTops），下面两个常量仅作测量前兜底
 */
const JSON_LINE_PITCH = 17
const JSON_PAD_TOP = 10
const jsonScrollTop = ref(0)
const caretPos = ref(0)
let jsonSelChange: (() => void) | null = null
const caretLine = computed(() => jsonText.value.slice(0, caretPos.value).split('\n').length)

const jsonStats = computed(
  () => `${jsonText.value.length} 字符 · ${jsonText.value.split('\n').length} 行`
)

/* ===== 语法高亮：注释绿 / key 红 / 字符串值蓝 / 数值橙 / true·false·null 紫 ===== */
const jsonInputRef = ref()
const highlightRef = ref<HTMLElement>()

const highlightedJson = computed(() => {
  const text = jsonText.value
  const n = text.length
  let out = '<div class="ln">'
  let i = 0
  const escHtml = (s: string): string =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // token 文本可能含换行（跨行块注释、未闭合字符串）：换行处断开 .ln 块，
  // 让每个逻辑行独占一个块——自动换行时块随内容变高，行号按块实测位置对齐
  const emit = (cls: string | null, raw: string): void => {
    const parts = raw.split('\n')
    for (let p = 0; p < parts.length; p++) {
      if (p > 0) out += '</div><div class="ln">'
      if (parts[p]) out += cls ? `<span class="tok-${cls}">${escHtml(parts[p])}</span>` : escHtml(parts[p])
    }
  }

  while (i < n) {
    const c = text[i]

    if (c === '\n') {
      emit(null, '\n')
      i++
      continue
    }

    if (c === '/' && (text[i + 1] === '/' || text[i + 1] === '*')) {
      const isLine = text[i + 1] === '/'
      let end: number
      if (isLine) {
        end = text.indexOf('\n', i)
        if (end < 0) end = n
      } else {
        end = text.indexOf('*/', i + 2)
        end = end < 0 ? n : end + 2
      }
      emit('comment', text.slice(i, end))
      i = end
      continue
    }

    if (c === '"') {
      let j = i + 1
      while (j < n) {
        if (text[j] === '\\') {
          j += 2
          continue
        }
        if (text[j] === '"') {
          j++
          break
        }
        j++
      }
      // 字符串后（跳过空白）跟着冒号即为 key
      let k = j
      while (k < n && /\s/.test(text[k])) k++
      emit(text[k] === ':' ? 'key' : 'string', text.slice(i, j))
      i = j
      continue
    }

    if (/[0-9]/.test(c) || (c === '-' && /[0-9]/.test(text[i + 1] ?? ''))) {
      let j = i + 1
      while (j < n && /[0-9.eE]/.test(text[j])) j++
      emit('number', text.slice(i, j))
      i = j
      continue
    }

    if (/[a-z]/.test(c)) {
      let j = i
      while (j < n && /[a-z]/.test(text[j])) j++
      const word = text.slice(i, j)
      emit(word === 'true' || word === 'false' || word === 'null' ? 'literal' : null, word)
      i = j
      continue
    }

    emit(null, c)
    i++
  }
  // 末行以块收尾：末行为空时 .ln 的 min-height 保证与 textarea 空行等高
  return `${out}</div>`
})

/* 行号定位：按 .ln 块的实测 offsetTop。自动换行时某逻辑行占多个可视行（块高 =
   N×17px），行号只对齐块首行；内容或宽度变化后重测（当前行不画背景高亮条，
   仅行号变蓝加粗提示） */
const lineTops = ref<number[]>([])
let jsonRo: ResizeObserver | null = null

function measureLines(): void {
  const pre = highlightRef.value
  if (!pre) return
  const tops: number[] = []
  pre.querySelectorAll<HTMLElement>(':scope > code > .ln').forEach(el => {
    tops.push(el.offsetTop)
  })
  lineTops.value = tops
}

watch(highlightedJson, async () => {
  await nextTick()
  measureLines()
})

/** textarea 滚动时同步高亮层（在挂载后绑定到原生 textarea） */
onMounted(() => {
  const inst = jsonInputRef.value as
    | { textarea?: HTMLTextAreaElement; $el?: HTMLElement }
    | undefined
  const ta = inst?.textarea ?? inst?.$el?.querySelector('textarea')
  ta?.addEventListener('scroll', () => {
    if (highlightRef.value && ta) highlightRef.value.scrollTop = ta.scrollTop
    jsonScrollTop.value = ta?.scrollTop ?? 0
  })
  // 光标行号高亮：selectionchange 在 textarea 聚焦时随光标/选区变化触发
  jsonSelChange = () => {
    if (ta && document.activeElement === ta) caretPos.value = ta.selectionStart
  }
  document.addEventListener('selectionchange', jsonSelChange)
  // 编辑器宽度变化会改变换行点（块高随之变化）→ 重测行块位置
  if (highlightRef.value && typeof ResizeObserver !== 'undefined') {
    jsonRo = new ResizeObserver(() => measureLines())
    jsonRo.observe(highlightRef.value)
  }
  measureLines()
})
onMounted(() => {
  timer = setInterval(() => (now.value = new Date()), 1000)
})

/**
 * 重新排版 JSONC（保留注释）：括号层级换行缩进、冒号后补空格、逗号后换行；
 * 行尾注释与内容隔两个空格，块注释内部行跟随缩进
 */
function prettyJsonc(text: string, unit: string): string {
  let out = ''
  let depth = 0
  let pendingBreak = false
  let i = 0
  const n = text.length
  const indent = (): string => unit.repeat(Math.max(0, depth))
  const place = (): void => {
    if (pendingBreak) {
      out += `\n${indent()}`
      pendingBreak = false
    }
  }

  while (i < n) {
    const c = text[i]

    if (c === '\n' || c === '\r' || c === ' ' || c === '\t') {
      i++
      continue
    }

    if (c === '{' || c === '[') {
      const close = c === '{' ? '}' : ']'
      let j = i + 1
      while (j < n && (text[j] === ' ' || text[j] === '\t' || text[j] === '\n' || text[j] === '\r')) j++
      place()
      if (text[j] === close) {
        out += c + close
        i = j + 1
        continue
      }
      out += c
      depth++
      pendingBreak = true
      i++
      continue
    }

    if (c === '}' || c === ']') {
      depth--
      pendingBreak = true
      place()
      out += c
      i++
      continue
    }

    if (c === ',') {
      out += ','
      pendingBreak = true
      i++
      continue
    }

    if (c === ':') {
      out += ': '
      i++
      continue
    }

    if (c === '/' && (text[i + 1] === '/' || text[i + 1] === '*')) {
      const isLine = text[i + 1] === '/'
      let end: number
      if (isLine) {
        end = text.indexOf('\n', i)
        if (end < 0) end = n
      } else {
        end = text.indexOf('*/', i + 2)
        end = end < 0 ? n : end + 2
      }
      let comment = text.slice(i, end)
      comment = isLine ? comment.replace(/\r$/, '') : comment.split('\n').join(`\n${indent()}`)
      if (pendingBreak) {
        out += `\n${indent()}${comment}`
      } else {
        out += `  ${comment}`
      }
      pendingBreak = true
      i = end
      continue
    }

    place()
    if (c === '"') {
      let j = i + 1
      while (j < n) {
        if (text[j] === '\\') {
          j += 2
          continue
        }
        if (text[j] === '"') {
          j++
          break
        }
        j++
      }
      out += text.slice(i, j)
      i = j
      continue
    }
    let j = i
    while (j < n && !'{}[]:,\n\r\t /'.includes(text[j])) j++
    out += text.slice(i, j)
    i = j
  }

  return out
}

function formatJson(): void {
  if (!requireValid()) return
  jsonText.value = prettyJsonc(jsonText.value, indentUnitStr())
}

/** 压缩结果（注释会被去除，压缩后的 JSON 无法携带注释） */
function minifiedJson(): string {
  return JSON.stringify(JSON.parse(stripComments(jsonText.value)))
}

async function copyMinified(): Promise<void> {
  if (!requireValid()) return
  await copyText(minifiedJson(), '已复制压缩 JSON（注释已去除）')
}

/** 压缩并整体转义为字符串字面量（引号、反斜杠、换行转义），可直接粘贴进代码 */
async function copyEscaped(): Promise<void> {
  if (!requireValid()) return
  await copyText(JSON.stringify(minifiedJson()), '已复制压缩转义 JSON')
}

function requireValid(): boolean {
  if (!jsonText.value.trim()) {
    ElMessage.warning('请先输入 JSON 内容')
    return false
  }
  if (jsonError.value) {
    ElMessage.error(`JSON 无效：${jsonError.value.msg}`)
    return false
  }
  return true
}

async function copyJson(): Promise<void> {
  if (!requireValid()) return
  await copyText(jsonText.value)
}

function clearJson(): void {
  jsonText.value = ''
}

function loadSample(): void {
  jsonText.value = `{
  // 项目信息（支持注释，格式化后注释保留）
  "name": "maozi-cloud",
  "version": "1.0.0",
  "features": ["微服务", "分布式", "一站式解决方案"],
  "author": { "name": "maozi", "url": "https://github.com/1095071913/maozi-cloud" },
  "openSource": true,
  "stars": 1024 // 星标数
}`
}

/** ===== 编辑器增强：配对/缩进（execCommand 插入以保留撤销栈并同步 v-model） ===== */
function insertAtCursor(ta: HTMLTextAreaElement, text: string): void {
  if (!document.execCommand('insertText', false, text)) {
    const start = ta.selectionStart
    ta.value = ta.value.slice(0, start) + text + ta.value.slice(ta.selectionEnd)
    ta.dispatchEvent(new Event('input', { bubbles: true }))
    // 兜底路径必须手动归位：给 value 赋值会把光标重置到整段文本末尾
    const cursor = start + text.length
    ta.setSelectionRange(cursor, cursor)
  }
}

function indentUnitStr(): string {
  return indent.value === 'tab' ? '\t' : ' '.repeat(Number(indent.value))
}

/** 从 pos 向前找最内层未闭合的开括号（'{' 或 '['），跳过字符串字面量（处理转义引号） */
function enclosingOpener(value: string, pos: number): string {
  let depth = 0
  for (let i = pos - 1; i >= 0; i--) {
    const c = value[i]
    if (c === '"') {
      // 统计引号前的连续反斜杠，奇数为转义引号，不是字符串边界
      let bs = 0
      let j = i - 1
      while (j >= 0 && value[j] === '\\') {
        bs++
        j--
      }
      if (bs % 2 === 1) continue
      // 未转义引号：整段跳过（向前找到它的配对开引号）
      let k = i - 1
      while (k >= 0) {
        let bs2 = 0
        let m = k - 1
        while (m >= 0 && value[m] === '\\') {
          bs2++
          m--
        }
        if (value[k] === '"' && bs2 % 2 === 0) break
        k--
      }
      i = k
      continue
    }
    if (c === '}' || c === ']') {
      depth++
      continue
    }
    if (c === '{' || c === '[') {
      if (depth === 0) return c
      depth--
    }
  }
  return ''
}

function onJsonKeydown(e: KeyboardEvent): void {
  // 中文输入法组合中、或按住修饰键的快捷键不做拦截
  if (e.isComposing || e.ctrlKey || e.metaKey || e.altKey) return
  const ta = e.target as HTMLTextAreaElement
  if (!ta || ta.tagName !== 'TEXTAREA') return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const value = ta.value
  const hasSel = start !== end

  // 1. 跨越闭合符：光标紧贴 } ] " 且无选中时，再输入同类符号直接跳过而不是重复插入
  //    （" 既是配对符又是闭合符，此判断必须先于自动配对，否则会被配对分支拦截）
  if (!hasSel && (e.key === '}' || e.key === ']' || e.key === '"') && value[start] === e.key) {
    e.preventDefault()
    ta.setSelectionRange(start + 1, start + 1)
    return
  }

  // 2. 自动配对：输入 { [ " 生成闭合符，光标落在中间；有选中内容时改为包裹选区
  const closeOf: Record<string, string> = { '{': '}', '[': ']', '"': '"' }
  if (closeOf[e.key]) {
    e.preventDefault()
    const sel = value.slice(start, end)
    insertAtCursor(ta, e.key + sel + closeOf[e.key])
    ta.setSelectionRange(start + 1, start + 1 + sel.length)
    return
  }

  // 3. 冒号跳出引号：光标紧贴闭合引号时按 :，先跳出引号再补 ': '（配合 key 自动引号）
  if (e.key === ':' && !hasSel && value[start] === '"') {
    e.preventDefault()
    ta.setSelectionRange(start + 1, start + 1)
    insertAtCursor(ta, ': ')
    return
  }

  // 4. key 自动加引号：在对象成员起始位（前面是 { 或 , 且所在容器是对象）直接输入裸字符时，
  //    自动包上双引号，光标落在引号内继续输入；数组中不拦截（可能是 true/false/null）
  if (
    !hasSel &&
    e.key.length === 1 &&
    /[0-9A-Za-z_$\u4e00-\u9fa5]/.test(e.key) &&
    value[start] !== '"'
  ) {
    let p = start - 1
    while (p >= 0 && /\s/.test(value[p])) p--
    const prevCh = p >= 0 ? value[p] : ''
    if ((prevCh === '{' || prevCh === ',') && enclosingOpener(value, start) === '{') {
      e.preventDefault()
      insertAtCursor(ta, `"${e.key}"`)
      ta.setSelectionRange(start + 2, start + 2)
      return
    }
  }

  const unit = indentUnitStr()

  // 5. Tab：插入缩进（跟随上方缩进设置），而不是把焦点移出编辑框
  if (e.key === 'Tab') {
    e.preventDefault()
    insertAtCursor(ta, unit)
    return
  }

  // 6. 智能退格：空引号对整体删除；行首缩进按单位退格（2/4 空格一次删一个单位），
  //    不足一个单位则删到行首；Tab 缩进用浏览器默认（一次删一个 Tab）
  if (e.key === 'Backspace' && !hasSel) {
    // 光标在 "" 中间：一次删掉一对引号
    if (value[start - 1] === '"' && value[start] === '"') {
      e.preventDefault()
      ta.setSelectionRange(start - 1, start + 1)
      if (!document.execCommand('delete')) {
        ta.value = value.slice(0, start - 1) + value.slice(start + 1)
        ta.dispatchEvent(new Event('input', { bubbles: true }))
        ta.setSelectionRange(start - 1, start - 1)
      }
      return
    }
    const lineStart = value.lastIndexOf('\n', start - 1) + 1
    const before = value.slice(lineStart, start)
    if (before.length > 0 && /^[ \t]+$/.test(before) && indentUnitStr() !== '\t') {
      const delLen = Math.min(indentUnitStr().length, before.length)
      e.preventDefault()
      ta.setSelectionRange(start - delLen, start)
      if (!document.execCommand('delete')) {
        ta.value = value.slice(0, start - delLen) + value.slice(start)
        ta.dispatchEvent(new Event('input', { bubbles: true }))
        ta.setSelectionRange(start - delLen, start - delLen)
      }
      return
    }
  }

  // 7. 回车自动缩进：配对之间三行展开，{ [ 之后加一层缩进，其余继承当前行缩进
  if (e.key === 'Enter' && !hasSel) {
    const before = value.slice(0, start)
    const lineIndent = (before.slice(before.lastIndexOf('\n') + 1).match(/^[ \t]*/) ?? [''])[0]
    const prev = before.slice(-1)
    const next = value[start] ?? ''
    e.preventDefault()

    // 末项回车自动补逗号：光标在值末尾、其后（跳过空白）就是闭合括号时，
    // 先补 ',' 再换行；闭合括号与光标同行时顺带推到独立行。
    // 行内含 // 注释时跳过（逗号会被注释吞掉导致语法错误）
    let p = start - 1
    while (p >= 0 && /\s/.test(value[p])) p--
    const prevCh = p >= 0 ? value[p] : ''
    let q = start
    while (q < value.length && /\s/.test(value[q])) q++
    const nextCh = value[q] ?? ''
    const lineText = before.slice(before.lastIndexOf('\n') + 1)
    const afterValue = prevCh !== '' && !'{[,:'.includes(prevCh)
    if (afterValue && (nextCh === '}' || nextCh === ']') && !lineText.includes('//')) {
      if (/[\n\r]/.test(value.slice(start, q))) {
        // 闭合括号已在独立行：补逗号后光标与同级条目对齐（当前行缩进，不加一层）
        insertAtCursor(ta, `,\n${lineIndent}`)
      } else {
        // 闭合括号与光标同行：三行展开（值行、光标行、闭合行），光标行深一层
        insertAtCursor(ta, `,\n${lineIndent}${unit}\n${lineIndent}`)
        const cursor = start + 2 + lineIndent.length + unit.length
        ta.setSelectionRange(cursor, cursor)
      }
      return
    }

    if ((prev === '{' && next === '}') || (prev === '[' && next === ']')) {
      insertAtCursor(ta, `\n${lineIndent}${unit}\n${lineIndent}`)
      const cursor = start + 1 + lineIndent.length + unit.length
      ta.setSelectionRange(cursor, cursor)
    } else if (prev === '{' || prev === '[') {
      insertAtCursor(ta, `\n${lineIndent}${unit}`)
    } else {
      insertAtCursor(ta, `\n${lineIndent}`)
    }
  }
}

/* ===================== 时间戳转换 ===================== */
const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null

const tzLabel = computed(() => {
  const off = -new Date().getTimezoneOffset() / 60
  return `UTC${off >= 0 ? '+' : ''}${off}`
})

const tsInput = ref('')
const pickDate = ref<Date | null>(new Date())

const dateShortcuts = [
  { text: '此刻', value: () => new Date() },
  {
    text: '今天 00:00:00',
    value: () => {
      const d = new Date()
      d.setHours(0, 0, 0, 0)
      return d
    }
  },
  {
    text: '昨天 00:00:00',
    value: () => {
      const d = new Date()
      d.setDate(d.getDate() - 1)
      d.setHours(0, 0, 0, 0)
      return d
    }
  }
]

/** 纯数字输入自动识别：13 位以上按毫秒（超长截到毫秒），否则按秒 */
const tsDate = computed<Date | null>(() => {
  const raw = tsInput.value.trim()
  if (!/^\d{1,16}$/.test(raw)) return null
  const num = raw.length > 13 ? Number(raw.slice(0, 13)) : Number(raw)
  const ms = raw.length >= 13 ? num : num * 1000
  const d = new Date(ms)
  return isNaN(d.getTime()) ? null : d
})

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/** withMs：时间戳 → 时间卡片里精确到毫秒（其余场合秒级即可，避免无意义的 .000 抖动） */
function fmtDateTime(d: Date, withMs = false): string {
  const base = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  return withMs ? `${base}.${String(d.getMilliseconds()).padStart(3, '0')}` : base
}

function fmtDay(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function fmtUtc(d: Date, withMs = false): string {
  const iso = d.toISOString()
  const base = iso.replace('T', ' ').slice(0, 19)
  return withMs ? `${base}.${iso.slice(20, 23)} UTC` : `${base} UTC`
}

function weekdayOf(d: Date): string {
  return '日一二三四五六'[d.getDay()]
}

function secOf(d: Date): number {
  return Math.floor(d.getTime() / 1000)
}

function relativeTo(d: Date, base: Date): string {
  const diff = (base.getTime() - d.getTime()) / 1000
  const abs = Math.abs(diff)
  const suffix = diff >= 0 ? '前' : '后'
  if (abs < 60) return `${Math.round(abs)} 秒${suffix}`
  if (abs < 3600) return `${Math.round(abs / 60)} 分钟${suffix}`
  if (abs < 86400) return `${Math.round(abs / 3600)} 小时${suffix}`
  if (abs < 86400 * 30) return `${Math.round(abs / 86400)} 天${suffix}`
  if (abs < 86400 * 365) return `${Math.round(abs / 86400 / 30)} 个月${suffix}`
  return `${(abs / 86400 / 365).toFixed(1)} 年${suffix}`
}

/* ===================== 密码生成 ===================== */
const PWD_DIGITS = '0123456789'
const PWD_UPPER = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
const PWD_LOWER = 'abcdefghijklmnopqrstuvwxyz'
const PWD_SYMBOLS = '!@#$%^&*()-_=+[]{}<>?,.;:~'

const pwdLen = ref(16)
const pwdCount = ref(5)
const pwdUseDigits = ref(true)
const pwdUseUpper = ref(true)
const pwdUseLower = ref(true)
const pwdUseSymbol = ref(false)
const pwdExclude = ref('')
const pwdResults = ref<string[]>([])
/** 批次号：每次生成更新，作为结果行 key 的一部分触发入场动画重播 */
const pwdBatch = ref(0)

/** 生成格式：随机密码 / UUID；UUID 选项（版本、大小写、连字符、包装） */
const pwdFormat = ref<'password' | 'uuid'>('password')
const pwdFormatOptions = [
  { label: '🔑 随机密码', value: 'password' },
  { label: '🆔 UUID', value: 'uuid' }
]
const pwdUuidVersion = ref<'v4' | 'v7'>('v4')
const pwdUuidVersionOptions = [
  { label: 'V4 全随机', value: 'v4' },
  { label: 'V7 时间有序', value: 'v7' }
]
const pwdUuidUpper = ref(false)
const pwdUuidHyphen = ref(true)
const pwdUuidWrap = ref<'none' | 'brace' | 'urn'>('none')
const pwdUuidWrapOptions = [
  { label: '标准', value: 'none' },
  { label: '{ }', value: 'brace' },
  { label: 'urn:', value: 'urn' }
]

/** 字符类型卡（v-for 渲染，model 直接绑到各开关 ref） */
const pwdTypes = [
  { key: 'digits', label: '数字', glyph: '123', range: '0-9', model: pwdUseDigits },
  { key: 'upper', label: '大写字母', glyph: 'ABC', range: 'A-Z', model: pwdUseUpper },
  { key: 'lower', label: '小写字母', glyph: 'abc', range: 'a-z', model: pwdUseLower },
  { key: 'symbol', label: '特殊符号', glyph: '#$&', range: '!@#…', model: pwdUseSymbol }
]

const pwdLenMarks: Record<number, string> = { 8: '8', 16: '16', 32: '32', 64: '64' }
const pwdCountOptions = [1, 5, 10, 20, 50, 100]

/** 排除字符去重后的 chip 列表（空白不参与排除也不展示） */
const pwdExcludeChips = computed(() => [...new Set([...pwdExclude.value])].filter(c => c.trim()))

function removeExclude(c: string): void {
  pwdExclude.value = [...pwdExclude.value].filter(x => x !== c).join('')
}

/** 均匀随机整数 [0, max)：32 位加密随机数先做拒绝采样再取模，避免 modulo 偏差 */
function randInt(max: number): number {
  const limit = Math.floor(0x100000000 / max) * max
  const buf = new Uint32Array(1)
  do {
    crypto.getRandomValues(buf)
  } while (buf[0] >= limit)
  return buf[0] % max
}

/** 所选类型各自过滤排除字符后的字符池（被排除清空的类型自动剔除，不参与“必含”保证） */
const pwdPools = computed(() => {
  const ex = new Set(pwdExclude.value)
  const picked = [
    pwdUseDigits.value ? PWD_DIGITS : '',
    pwdUseUpper.value ? PWD_UPPER : '',
    pwdUseLower.value ? PWD_LOWER : '',
    pwdUseSymbol.value ? PWD_SYMBOLS : ''
  ]
  return picked.map(s => [...s].filter(c => !ex.has(c)).join('')).filter(s => s.length > 0)
})

const pwdPoolAll = computed(() => pwdPools.value.join(''))

/** 单条密码：先从每个所选类型各取一个保证齐备，再从全池补足长度，最后洗牌打散位置 */
function genOnePassword(len: number): string {
  const pools = pwdPools.value
  const all = pwdPoolAll.value
  const chars: string[] = []
  if (len >= pools.length) {
    for (const s of pools) chars.push(s[randInt(s.length)])
  }
  while (chars.length < len) chars.push(all[randInt(all.length)])
  // Fisher–Yates 洗牌：必含字符不集中在开头
  for (let i = chars.length - 1; i > 0; i--) {
    const j = randInt(i + 1)
    ;[chars[i], chars[j]] = [chars[j], chars[i]]
  }
  return chars.join('')
}

function genPasswords(): void {
  const count = pwdCount.value ?? 5
  pwdBatch.value = Date.now()
  // UUID 分支：格式校验只针对随机密码
  if (pwdFormat.value === 'uuid') {
    pwdResults.value = Array.from({ length: count }, () => genOneUuid())
    return
  }
  if (!pwdUseDigits.value && !pwdUseUpper.value && !pwdUseLower.value && !pwdUseSymbol.value) {
    ElMessage.warning('请至少选择一种字符类型')
    return
  }
  if (!pwdPoolAll.value) {
    ElMessage.error('排除字符覆盖了所选全部类型，已无可用字符')
    return
  }
  const len = pwdLen.value ?? 16
  pwdResults.value = Array.from({ length: count }, () => genOnePassword(len))
}

/** UUID 字节 → 十六进制，按输出选项组合（连字符 → 大写 → 包装） */
function formatUuid(b: Uint8Array): string {
  const hex = [...b].map(x => x.toString(16).padStart(2, '0')).join('')
  let s = pwdUuidHyphen.value
    ? `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
    : hex
  if (pwdUuidUpper.value) s = s.toUpperCase()
  if (pwdUuidWrap.value === 'brace') s = `{${s}}`
  else if (pwdUuidWrap.value === 'urn') s = `urn:uuid:${s}`
  return s
}

/**
 * 单条 UUID（加密随机数）：
 * V4 全随机；V7 前 48bit 为毫秒时间戳（BigInt 移位避开 32 位截断），
 * 天然按生成时间排序，版本/变体位按 RFC 4122 设置
 */
function genOneUuid(): string {
  const b = crypto.getRandomValues(new Uint8Array(16))
  if (pwdUuidVersion.value === 'v7') {
    const ts = BigInt(Date.now())
    b[0] = Number((ts >> 40n) & 0xffn)
    b[1] = Number((ts >> 32n) & 0xffn)
    b[2] = Number((ts >> 24n) & 0xffn)
    b[3] = Number((ts >> 16n) & 0xffn)
    b[4] = Number((ts >> 8n) & 0xffn)
    b[5] = Number(ts & 0xffn)
    b[6] = (b[6] & 0x0f) | 0x70
  } else {
    b[6] = (b[6] & 0x0f) | 0x40
  }
  b[8] = (b[8] & 0x3f) | 0x80
  return formatUuid(b)
}

/** 熵（bit）：随机密码 = 长度 × log2(字符池)；UUID 取随机位（V4=122 / V7=74，时间戳不计入） */
const pwdEntropyBits = computed(() => {
  if (pwdFormat.value === 'uuid') {
    return pwdUuidVersion.value === 'v4' ? 122 : 74
  }
  const size = Math.max(2, pwdPoolAll.value.length)
  return (pwdLen.value ?? 16) * Math.log2(size)
})

/** 熵进度条宽度：0–128bit 映射 0–100% */
const pwdEntropyPct = computed(() => Math.min(100, (pwdEntropyBits.value / 128) * 100))

/** 密码按字符类别着色展示（v-html + :deep 命中，同 JSON 高亮做法） */
function coloredPwd(p: string): string {
  const esc = (s: string): string => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return [...p]
    .map(c => {
      const cls = /[0-9]/.test(c) ? 'n' : /[A-Z]/.test(c) ? 'u' : /[a-z]/.test(c) ? 'l' : 's'
      return `<span class="pc-${cls}">${esc(c)}</span>`
    })
    .join('')
}

const pwdStrength = computed((): { label: string; cls: string } => {
  const bits = pwdEntropyBits.value
  if (bits < 40) return { label: '弱', cls: 's1' }
  if (bits < 60) return { label: '中', cls: 's2' }
  if (bits < 90) return { label: '强', cls: 's3' }
  return { label: '极强', cls: 's4' }
})

async function copyAllPasswords(): Promise<void> {
  await copyText(pwdResults.value.join('\n'), `已复制 ${pwdResults.value.length} 条密码`)
}

/* ===================== Base64 编解码 ===================== */
const b64Input = ref('')
const b64Output = ref('')
const b64Error = ref('')
const b64UrlSafe = ref(false)
const b64Mode = ref<'encode' | 'decode'>('encode')
const b64ModeOptions = [
  { label: '编码 →', value: 'encode' },
  { label: '← 解码', value: 'decode' }
]

/** 文本编解码：按 UTF-8 字节转换（支持中文）；urlSafe 输出 -_ 无填充，解码时还原 */
function b64Do(mode: 'encode' | 'decode'): void {
  b64Error.value = ''
  const input = b64Input.value
  // 空输入静默清空输出（不当作错误，避免切换方向时闪红）
  if (!input.trim()) {
    b64Output.value = ''
    return
  }
  try {
    if (mode === 'encode') {
      const bytes = new TextEncoder().encode(input)
      let bin = ''
      // 逐字节拼接避免大输入时 spread 爆栈
      for (const b of bytes) bin += String.fromCharCode(b)
      let out = btoa(bin)
      if (b64UrlSafe.value) out = out.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
      b64Output.value = out
    } else {
      let s = input.replace(/\s/g, '')
      // 粘贴 Data URL 时自动剥离前缀，只解码 Base64 部分
      const m = s.match(/^data:[^,]*?,/)
      if (m) s = s.slice(m[0].length)
      if (b64UrlSafe.value) s = s.replace(/-/g, '+').replace(/_/g, '/')
      while (s.length % 4) s += '='
      const bin = atob(s)
      b64Output.value = new TextDecoder().decode(Uint8Array.from(bin, c => c.charCodeAt(0)))
    }
  } catch {
    b64Output.value = ''
    b64Error.value = mode === 'encode' ? '编码失败' : '不是有效的 Base64 字符串'
  }
}

/** 方向切换 / URL Safe 开关变化时立即按当前模式转换 */
function b64Run(): void {
  b64Do(b64Mode.value)
}

/** 输出 ↔ 输入互换并反向执行：编码结果换到输入自动解码回来（反之亦然） */
function b64Swap(): void {
  if (!b64Output.value) return
  b64Input.value = b64Output.value
  b64Mode.value = b64Mode.value === 'encode' ? 'decode' : 'encode'
  b64Run()
}

function b64Clear(): void {
  b64Input.value = ''
  b64Output.value = ''
  b64Error.value = ''
}

/* ===== 图片 ⇄ Base64 ===== */
const imgFileRef = ref<HTMLInputElement>()
const imgDragging = ref(false)
const imgUrl = ref('')
const imgName = ref('')
const imgType = ref('')
const imgSize = ref(0)
const imgDecodeInput = ref('')
const imgDecodeSrc = ref('')
const imgDecodeError = ref('')

function fmtSize(n: number): string {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

function loadImgFile(file: File): void {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning('图片大于 8MB，Base64 体积会很大，复制可能较慢')
  }
  const reader = new FileReader()
  reader.onload = () => {
    imgUrl.value = String(reader.result ?? '')
    imgName.value = file.name || '剪贴板图片'
    imgType.value = file.type
    imgSize.value = file.size
  }
  reader.onerror = () => ElMessage.error('图片读取失败')
  reader.readAsDataURL(file)
}

function imgPick(): void {
  imgFileRef.value?.click()
}

function onImgFile(e: Event): void {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) loadImgFile(f)
  // 清空 value 允许再次选择同一文件
  ;(e.target as HTMLInputElement).value = ''
}

/** 拖入图片文件直接转 Base64；拖入文本（Data URL / Base64）走反向预览 */
function onImgDrop(e: DragEvent): void {
  imgDragging.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) {
    loadImgFile(f)
    return
  }
  const text = e.dataTransfer?.getData('text')?.trim()
  if (text) {
    imgDecodeInput.value = text
    imgPreviewDecode()
  }
}

/** 全局粘贴截图：Base64 Tab 激活且剪贴板含图片时直接加载 */
function onGlobalPaste(e: ClipboardEvent): void {
  if (activeTab.value !== 'b64') return
  const items = e.clipboardData?.items
  if (!items) return
  // DataTransferItemList 不可迭代，按索引遍历
  for (let i = 0; i < items.length; i++) {
    const it = items[i]
    if (it.type.startsWith('image/')) {
      const f = it.getAsFile()
      if (f) {
        e.preventDefault()
        loadImgFile(f)
      }
      return
    }
  }
}

function imgReset(): void {
  imgUrl.value = ''
  imgName.value = ''
  imgType.value = ''
  imgSize.value = 0
}

/** Data URL 里逗号后的纯 Base64 部分 */
function pureB64(): string {
  return imgUrl.value.slice(imgUrl.value.indexOf(',') + 1)
}

/** 纯 Base64 按魔数猜 MIME（jpeg/png/gif/webp/svg），默认按 png 尝试 */
function guessImageMime(b64: string): string {
  if (b64.startsWith('/9j/')) return 'image/jpeg'
  if (b64.startsWith('iVBOR')) return 'image/png'
  if (b64.startsWith('R0lGOD')) return 'image/gif'
  if (b64.startsWith('UklGR')) return 'image/webp'
  if (b64.startsWith('PHN2Z')) return 'image/svg+xml'
  return 'image/png'
}

/** Base64 → 图片：先离屏加载校验，成功后才显示预览 */
function imgPreviewDecode(): void {
  const raw = imgDecodeInput.value.trim()
  imgDecodeError.value = ''
  imgDecodeSrc.value = ''
  if (!raw) {
    imgDecodeError.value = '请先粘贴 Data URL 或纯 Base64'
    return
  }
  let src = raw
  if (!raw.startsWith('data:')) src = `data:${guessImageMime(raw.replace(/\s/g, ''))};base64,${raw.replace(/\s/g, '')}`
  const img = new Image()
  img.onload = () => (imgDecodeSrc.value = src)
  img.onerror = () => (imgDecodeError.value = '无法解析为图片，请检查 Base64 内容')
  img.src = src
}

/* ===================== 项目批量重命名 ===================== */
/** 执行管线阶段（主进程推送，渲染层只做展示） */
const RN_STAGES = [
  { key: 'scan', label: '扫描' },
  { key: 'content', label: '内容替换' },
  { key: 'rename', label: '重命名' },
  { key: 'done', label: '完成' }
] as const
const RN_STAGE_ORDER: string[] = ['scan', 'content', 'rename', 'done']

const rnDir = ref('')
/** 规则行带自增 id：transition-group 的稳定 key（增删有动画） */
let rnRuleSeq = 1
const rnRules = ref<Array<{ id: number; from: string; to: string }>>([{ id: rnRuleSeq++, from: '', to: '' }])
const rnContent = ref(true)
const rnTextOnly = ref(true)
/** 名称替换开关：文件名 / 目录名（目录含根目录自身），可独立关闭只做内容或只做改名 */
const rnRenameFiles = ref(true)
const rnRenameDirs = ref(true)
/** 排除目录 chips 化：tags 为生效集合，input 为待添加内容 */
const rnExcludeInput = ref('')
const rnExcludeTags = ref([
  'node_modules', '.git', '.svn', 'dist', 'build', 'target', 'out', '.idea', '.vscode', '.gradle'
])

const rnRunning = ref(false)
const rnStage = ref<'idle' | 'scan' | 'content' | 'rename' | 'done' | 'error'>('idle')
const rnDone = ref(0)
const rnTotal = ref(0)
const rnCurrent = ref('')
const rnStats = ref<RenameStats | null>(null)
const rnNewRoot = ref('')
const rnError = ref('')

/** worker 池规模（与主进程 N = min(8, cpus) 一致），用于活动可视化 */
const rnWorkerCount = Math.max(1, Math.min(8, navigator.hardwareConcurrency || 4))

const rnPct = computed(() => (rnTotal.value > 0 ? Math.min(100, Math.round((rnDone.value / rnTotal.value) * 100)) : 0))

/** 有效规则数（旧字符非空且新旧不同，与主进程过滤一致），横幅实时显示 */
const rnRuleCount = computed(() => rnRules.value.filter(r => r.from.length > 0 && r.from !== r.to).length)

/** 横幅状态徽章：待命灰 / 运行蓝脉冲 / 完成绿 / 取消黄 / 失败红 */
const RN_STAGE_NAMES: Record<string, string> = { scan: '扫描中', content: '替换内容', rename: '重命名中' }
const rnStatusBadge = computed((): { label: string; cls: string } => {
  if (rnRunning.value) return { label: RN_STAGE_NAMES[rnStage.value] ?? '运行中', cls: 'run' }
  if (rnStage.value === 'done') return rnStats.value?.cancelled ? { label: '已取消', cls: 'warn' } : { label: '已完成', cls: 'ok' }
  if (rnStage.value === 'error') return { label: '失败', cls: 'bad' }
  return { label: '待命', cls: 'idle' }
})

/** 阶段样式：进行中橙色脉冲、已完成绿色、全部完成全绿 */
function rnStageClass(key: string): string {
  if (rnStage.value === 'done') return 'done'
  const cur = RN_STAGE_ORDER.indexOf(rnStage.value)
  if (cur < 0) return ''
  const idx = RN_STAGE_ORDER.indexOf(key)
  return idx < cur ? 'done' : idx === cur ? 'on' : ''
}

/** 步骤间连接线：该线之前的阶段已开始/完成即变绿 */
function rnLineClass(i: number): string {
  if (rnStage.value === 'done') return 'done'
  const cur = RN_STAGE_ORDER.indexOf(rnStage.value)
  return cur > i ? 'done' : ''
}

async function rnPickDir(): Promise<void> {
  const res = await api.projects.pickDir()
  if (res.ok && res.data) rnDir.value = res.data
}

function rnAddRule(): void {
  rnRules.value.push({ id: ++rnRuleSeq, from: '', to: '' })
}

function rnRemoveRule(i: number): void {
  if (rnRules.value.length > 1) rnRules.value.splice(i, 1)
}

/** 有效规则（旧字符非空且新旧不同）——与主进程过滤逻辑一致 */
function rnValidRules(): Array<{ from: string; to: string }> {
  return rnRules.value.filter(r => r.from.length > 0 && r.from !== r.to).map(r => ({ from: r.from, to: r.to }))
}

/** 排除目录 chips：回车 / 中英文逗号提交，去重入列 */
function addExcludeTag(): void {
  const parts = rnExcludeInput.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
  for (const t of parts) {
    if (!rnExcludeTags.value.includes(t)) rnExcludeTags.value.push(t)
  }
  rnExcludeInput.value = ''
}

function onExcludeKeydown(e: KeyboardEvent): void {
  if (e.key === ',' || e.key === '，') {
    e.preventDefault()
    addExcludeTag()
  }
}

function removeExcludeTag(t: string): void {
  rnExcludeTags.value = rnExcludeTags.value.filter(x => x !== t)
}

async function rnStart(): Promise<void> {
  if (rnRunning.value) return
  if (!rnDir.value.trim()) {
    ElMessage.warning('请先选择目标目录')
    return
  }
  const rules = rnValidRules()
  if (!rules.length) {
    ElMessage.warning('请至少填写一组有效规则（旧字符非空且新旧不同）')
    return
  }
  if (!rnContent.value && !rnRenameFiles.value && !rnRenameDirs.value) {
    ElMessage.warning('请至少保留一项替换操作（文件内容 / 文件名称 / 目录名称）')
    return
  }
  // 批量改写不可撤销：执行前必须确认
  try {
    await ElMessageBox.confirm(
      `将对「${rnDir.value}」及其全部子层级（含根目录名）执行替换：${rules
        .map(r => `"${r.from}" → "${r.to}"`)
        .join('，')}。命中处将被永久改写，操作不可撤销，建议先提交 git 或做好备份。`,
      '确认批量重命名',
      { confirmButtonText: '确认执行', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  rnRunning.value = true
  rnStage.value = 'scan'
  rnDone.value = 0
  rnTotal.value = 0
  rnCurrent.value = ''
  rnStats.value = null
  rnNewRoot.value = ''
  rnError.value = ''
  const res = await api.rename.run({
    root: rnDir.value.trim(),
    rules,
    options: {
      includeContent: rnContent.value,
      renameFiles: rnRenameFiles.value,
      renameDirs: rnRenameDirs.value,
      textOnly: rnTextOnly.value,
      excludeDirs: [...rnExcludeTags.value]
    }
  })
  rnRunning.value = false
  if (res.ok && res.data) {
    rnStats.value = res.data.stats
    rnNewRoot.value = res.data.newRoot
    if (rnStage.value !== 'error') rnStage.value = 'done'
    ElMessage.success(res.data.stats.cancelled ? '已取消，已完成部分保留' : '重命名完成')
  } else {
    rnStage.value = 'error'
    rnError.value = res.error ?? '执行失败'
    ElMessage.error(rnError.value)
  }
}

async function rnCancelRun(): Promise<void> {
  await api.rename.cancel()
  ElMessage.info('正在取消…')
}

/** 进度事件 → 界面状态（80ms 节流在主进程完成） */
let rnOffProgress: (() => void) | null = null
onMounted(() => {
  rnOffProgress = api.rename.onRenameProgress(p => {
    if (p.stage === 'error') {
      rnStage.value = 'error'
      rnError.value = p.current || '执行失败'
      return
    }
    rnStage.value = p.stage === 'done' ? 'done' : p.stage
    rnDone.value = p.done
    rnTotal.value = p.total
    rnCurrent.value = p.current
    if (p.stats) rnStats.value = p.stats
  })
})

async function copyText(text: string, msg = '已复制到剪贴板'): Promise<void> {
  await api.util.copy(text)
  ElMessage.success(msg)
}

onMounted(() => {
  timer = setInterval(() => (now.value = new Date()), 1000)
})

// 截图直接 Ctrl+V 进 Base64 Tab（监听随组件卸载移除）
onMounted(() => {
  document.addEventListener('paste', onGlobalPaste)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (jsonSelChange) document.removeEventListener('selectionchange', jsonSelChange)
  if (jsonRo) jsonRo.disconnect()
  document.removeEventListener('paste', onGlobalPaste)
  rnOffProgress?.()
})
</script>

<style scoped>
/* ===== Hero ===== */
.ct-hero {
  -webkit-app-region: drag;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 32px;
  border-radius: 18px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
  margin-bottom: 14px;
}

.ct-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.ct-deco-1 {
  width: 300px;
  height: 300px;
  right: -70px;
  top: -160px;
}

.ct-deco-2 {
  width: 190px;
  height: 190px;
  right: 150px;
  bottom: -120px;
  border-color: rgba(255, 255, 255, 0.07);
}

.ct-hero-left {
  position: relative;
  min-width: 0;
}

.ct-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ct-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.ct-hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.ct-hero-mark {
  position: absolute;
  right: 44px;
  top: 50%;
  transform: translateY(-50%) rotate(-8deg);
  font-size: 86px;
  opacity: 0.14;
  filter: grayscale(0.2);
  pointer-events: none;
  user-select: none;
}

.ct-chip {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(6px);
}

.tools-card {
  /* Electron 防击穿：hero 的 drag 拖拽区按合成层几何生效且不受 z-index 影响，
     显式 no-drag 隔离（同 styles.css 的 .page-header button 处理），保证工具卡内全部可点 */
  -webkit-app-region: no-drag;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  padding: 6px 22px 20px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
}

/* 切换 Tab 时内容轻入 */
.pane-box {
  animation: ct-pane-in 0.28s ease both;
}

@keyframes ct-pane-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 滑块分段导航 ===== */
.seg {
  position: relative;
  display: flex;
  width: fit-content;
  /* 窄窗口钳制到父级宽度：按钮按 flex 收缩（见 .seg-item），
     避免 fit-content 采纳 max-content 后整体溢出视口、末位 Tab 只剩图标可点 */
  max-width: 100%;
  margin-bottom: 22px;
  padding: 4px;
  border-radius: 14px;
  background: linear-gradient(180deg, #e8edf4, #eff3f8);
  box-shadow: inset 0 1px 3px rgba(15, 23, 42, 0.08), inset 0 0 0 1px rgba(15, 23, 42, 0.04);
}

.seg-thumb {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  /* 纯装饰滑块：绝不参与命中测试（transform 移动的合成层在个别 Electron 版本会拦截点击） */
  pointer-events: none;
  -webkit-app-region: no-drag;
  /* 宽度与位移按 Tab 数量计算（见脚本 segThumbStyle），此处只负责位移动画 */
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.14), 0 1px 2px rgba(15, 23, 42, 0.06),
    inset 0 0 0 1px rgba(15, 23, 42, 0.04);
  transition: transform 0.42s cubic-bezier(0.3, 1.25, 0.45, 1);
}

.seg-item {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  /* 5 个 Tab 基准宽 152px（width 参与 fit-content 容器内在尺寸计算）；
     窄窗口下 seg 被 max-width 钳制时按钮等比收缩（同 width 同 shrink → 等宽，滑块均分仍对齐），
     min-width:0 解除内容宽度钳制，收缩后文字隐藏不折行 */
  width: 152px;
  flex-shrink: 1;
  min-width: 0;
  height: 38px;
  padding: 0;
  overflow: hidden;
  white-space: nowrap;
  border: none;
  border-radius: 10px;
  background: none;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #64748b;
  cursor: pointer;
  outline: none;
  transition: color 0.25s ease;
  /* Electron 防击穿：Tab 按钮显式 no-drag（drag 区按合成层几何吞点击，不受 z-index 影响） */
  -webkit-app-region: no-drag;
}

.seg-item:hover {
  color: #334155;
}

.seg-item.active {
  color: #0f172a;
}

.seg-item:focus-visible {
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.22);
}

.seg-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  transition: transform 0.32s cubic-bezier(0.3, 1.25, 0.45, 1), box-shadow 0.3s ease, background 0.3s ease;
}

.seg-ico.blue {
  background: linear-gradient(135deg, #e3edfd, #d3e3fb);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.16);
}

.seg-ico.amber {
  background: linear-gradient(135deg, #fdf3d9, #fbecc2);
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, 0.18);
}

.seg-ico.violet {
  background: linear-gradient(135deg, #ede9fe, #ddd6fe);
  box-shadow: inset 0 0 0 1px rgba(124, 58, 237, 0.16);
}

.seg-ico.teal {
  background: linear-gradient(135deg, #ccfbf1, #99f6e4);
  box-shadow: inset 0 0 0 1px rgba(13, 148, 136, 0.16);
}

.seg-ico.orange {
  background: linear-gradient(135deg, #ffedd5, #fed7aa);
  box-shadow: inset 0 0 0 1px rgba(234, 88, 12, 0.16);
}

.seg-item.active .seg-ico {
  transform: translateY(-1px) scale(1.06);
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.1), inset 0 0 0 1px rgba(15, 23, 42, 0.05);
}

.seg-item.active .seg-ico.blue {
  background: linear-gradient(135deg, #eaf2fe, #dbe9fd);
}

.seg-item.active .seg-ico.amber {
  background: linear-gradient(135deg, #fef6e3, #fcf0cd);
}

.seg-item.active .seg-ico.violet {
  background: linear-gradient(135deg, #f3efff, #e9e2fd);
}

.seg-item.active .seg-ico.teal {
  background: linear-gradient(135deg, #effefa, #d5f7f0);
}

.seg-item.active .seg-ico.orange {
  background: linear-gradient(135deg, #fff4e6, #ffe8cc);
}

.tt-emoji {
  font-size: 13px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 🕐 字形在 emoji 字体里偏上偏大，单独做光学对齐 */
.tt-emoji-clock {
  font-size: 12.5px;
  transform: translateY(0.5px);
}

/* ===== JSON ===== */
.json-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 12px;
}

.json-toolbar :deep(.el-button) {
  border-radius: 9px;
  font-weight: 600;
}

.json-toolbar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  border: none;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.32);
}

.json-toolbar :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(37, 99, 235, 0.42);
}

.json-toolbar :deep(.el-select .el-select__wrapper) {
  border-radius: 9px;
}

/* 高亮编辑器：着色 pre 在底层，透明 textarea 叠加在上层，两层度量必须一致。
   --json-lh 为四层共用的固定行高，取整数 px 的原因见脚本里 JSON_LINE_PITCH 注释 */
.json-editor {
  --json-lh: 17px;
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.json-editor:focus-within {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.25);
}

/* 行号栏：透明背景无分隔线（与正文区融为一体）；文字层左移 44px 腾出行号位
   （两层 padding 必须同步改，保持对齐） */
.json-gutter {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 44px;
  z-index: 2;
  overflow: hidden;
  pointer-events: none;
  user-select: none;
}

.json-gutter-inner {
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #b6c0cd;
  will-change: transform;
}

/* 号码按 .ln 块实测 top 绝对定位（见脚本 lineTops）：自动换行时逻辑行占多行，
   号码只随块首行，不再按固定行距堆叠 */
.json-gutter-num {
  position: absolute;
  right: 10px;
  height: var(--json-lh, 17px);
  line-height: var(--json-lh, 17px);
  text-align: right;
}

.json-gutter-num.active {
  color: #2563eb;
  font-weight: 600;
}

.json-highlight {
  position: absolute;
  inset: 0;
  margin: 0;
  padding: 10px 12px 10px 56px;
  /* hidden 不出滚动条：scrollTop 由滚动同步代码赋值，跟随 textarea */
  overflow: hidden;
  pointer-events: none;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  /* 行高与 textarea 一致（整数 px），normal 会被含中文的行撑高导致与行号错位 */
  line-height: var(--json-lh, 17px);
  /* 与 textarea 同为自动换行：超长行折行显示（不出横向滚动条）；换行相关属性
     （white-space/overflow-wrap/tab-size）与 scrollbar-gutter 必须两层完全一致，
     否则换行点分叉、文字与光标错位 */
  white-space: pre-wrap;
  overflow-wrap: break-word;
  scrollbar-gutter: stable;
  tab-size: 4;
  color: #1f2d3d;
}

/* 每个逻辑行一个块：自动换行时块高为 N×17px（行高固定整数保证整除），行号/当前行
   高亮按块实测位置定位（脚本 measureLines） */
.json-highlight :deep(.ln) {
  min-height: var(--json-lh, 17px);
}

.json-highlight code {
  font: inherit;
}

/* token 配色（span 由 v-html 生成，无 scoped 属性，需 :deep 命中） */
.json-highlight :deep(.tok-comment) {
  color: #16a34a;
}

.json-highlight :deep(.tok-key) {
  color: #dc2626;
}

.json-highlight :deep(.tok-string) {
  color: #2563eb;
}

.json-highlight :deep(.tok-number) {
  color: #ea580c;
}

.json-highlight :deep(.tok-literal) {
  color: #7c3aed;
}

.json-area :deep(.el-textarea__inner) {
  position: relative;
  z-index: 1;
  display: block;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  /* 行高取整数 px：normal 会被含中文的行撑高、非整数会逐行取整漂移，均导致与行号错位 */
  line-height: var(--json-lh, 17px);
  padding: 10px 12px 10px 56px;
  border: none;
  box-shadow: none;
  background: transparent;
  color: transparent;
  caret-color: #1f2d3d;
  resize: none;
  /* 不换行的问题已改回自动换行（见 .json-highlight 注释），此处与着色层保持一致 */
  white-space: pre-wrap;
  overflow-wrap: break-word;
  /* 恒定预留滚动条位：宽度不随垂直滚动条出现/消失跳动，换行点与着色层保持一致 */
  scrollbar-gutter: stable;
  tab-size: 4;
}

.json-area :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.json-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 7px 12px;
  border-radius: 9px;
  font-size: 12.5px;
}

.json-status .status-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.json-status .status-chips {
  margin-left: auto;
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.json-status .st-chip {
  padding: 1px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(0, 0, 0, 0.05);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: inherit;
  opacity: 0.85;
}

.json-status .status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.json-status.idle {
  background: transparent;
  color: #94a3b8;
}

.json-status.idle .status-dot {
  background: #cbd5e1;
}

.json-status.ok {
  background: #f0fdf4;
  color: #059669;
}

.json-status.ok .status-dot {
  background: #10b981;
}

.json-status.bad {
  background: #fef2f2;
  color: #dc2626;
}

.json-status.bad .status-dot {
  background: #ef4444;
}

/* ===== 时间戳 ===== */
.now-bar {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
}

.now-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.now-deco-1 {
  width: 200px;
  height: 200px;
  right: -50px;
  top: -110px;
}

.now-deco-2 {
  width: 120px;
  height: 120px;
  right: 100px;
  bottom: -80px;
  border-color: rgba(255, 255, 255, 0.07);
}

.now-left {
  position: relative;
}

.now-time {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
}

.now-meta {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.now-ts {
  position: relative;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ts-chip {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 9px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: background 0.2s;
}

.ts-chip:hover {
  background: rgba(255, 255, 255, 0.22);
}

.chip-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
}

.ts-chip .mono-cell {
  font-size: 14px;
}

.ts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.ts-card {
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 14px 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.ts-card:hover {
  transform: translateY(-2px);
  border-color: #c7d7ee;
  box-shadow: 0 8px 22px rgba(16, 24, 40, 0.08);
}

.ts-card-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 12px;
}

.ts-card-ico {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.1);
}

.ts-card-ico.to-time {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}

.ts-card-ico.to-ts {
  background: linear-gradient(135deg, #ede9fe, #ddd6fe);
}

.ts-card-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.ts-input-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.ts-input-row .el-input {
  flex: 1;
}

.ts-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.ts-row:hover {
  border-color: #93c5fd;
}

.ts-row-label {
  font-size: 12px;
  color: #8a94a6;
  flex-shrink: 0;
}

.ts-row-value {
  font-size: 13px;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ts-row-inline {
  display: flex;
  justify-content: space-between;
  padding: 2px 12px 0;
  font-size: 12px;
  color: #64748b;
}

.ts-rel {
  color: #2563eb;
}

.ts-hint {
  margin-top: 4px;
  font-size: 12.5px;
  color: #94a3b8;
}

.bad-hint {
  color: #dc2626;
}

/* ===== 密码生成 ===== */
.pwd-ico-set {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
}

.pwd-ico-res {
  background: linear-gradient(135deg, #e0f2fe, #bae6fd);
}

/* 强度横幅：深蓝渐变 + 熵值进度条（同 now-bar 风格） */
.pwd-banner {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
}

.pwd-banner-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.pwd-banner-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.pwd-banner-deco-2 {
  width: 130px;
  height: 130px;
  right: 110px;
  bottom: -70px;
  border-color: rgba(255, 255, 255, 0.07);
}

.pwd-banner-left {
  position: relative;
  min-width: 0;
  flex: 1;
}

.pwd-entropy-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.pwd-entropy-val {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
}

.pwd-entropy-unit {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

/* 强度徽章：色随级别（弱红/中黄/强绿/极强蓝） */
.pwd-badge {
  padding: 2px 12px;
  margin-left: 6px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  border: 1px solid;
}

.pwd-badge.s1 {
  color: #fca5a5;
  background: rgba(248, 113, 113, 0.16);
  border-color: rgba(248, 113, 113, 0.4);
}

.pwd-badge.s2 {
  color: #fcd34d;
  background: rgba(251, 191, 36, 0.16);
  border-color: rgba(251, 191, 36, 0.4);
}

.pwd-badge.s3 {
  color: #6ee7b7;
  background: rgba(52, 211, 153, 0.16);
  border-color: rgba(52, 211, 153, 0.4);
}

.pwd-badge.s4 {
  color: #93c5fd;
  background: rgba(125, 211, 252, 0.16);
  border-color: rgba(125, 211, 252, 0.4);
}

/* 熵进度条：填充色随强度，扫光动画提示“实时” */
.pwd-entropy-track {
  position: relative;
  overflow: hidden;
  height: 8px;
  margin-top: 10px;
  max-width: 420px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.pwd-entropy-fill {
  position: relative;
  height: 100%;
  min-width: 8px;
  border-radius: 999px;
  transition: width 0.45s cubic-bezier(0.3, 1.1, 0.4, 1), background 0.3s;
}

.pwd-entropy-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: pwd-shine 2.4s linear infinite;
}

.pwd-entropy-fill.s1 {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.pwd-entropy-fill.s2 {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.pwd-entropy-fill.s3 {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.pwd-entropy-fill.s4 {
  background: linear-gradient(90deg, #3b82f6, #7dd3fc);
}

@keyframes pwd-shine {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(100%);
  }
}

.pwd-banner-meta {
  margin-top: 9px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  font-variant-numeric: tabular-nums;
}

.pwd-banner-shield {
  position: relative;
  font-size: 54px;
  opacity: 0.85;
  filter: drop-shadow(0 6px 14px rgba(0, 0, 0, 0.3));
  transform: rotate(-8deg);
  user-select: none;
}

.pwd-grid {
  display: grid;
  grid-template-columns: minmax(0, 11fr) minmax(0, 13fr);
  gap: 14px;
  margin-top: 14px;
}

.pwd-field {
  margin-bottom: 13px;
}

.pwd-field-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: #64748b;
}

.pwd-field-tip {
  font-size: 11px;
  color: #b0b9c8;
  font-weight: 400;
}

/* 滑块美化：渐变轨道 + 圆形把手 + 刻度小字 */
.pwd-slider {
  margin: 8px 8px 2px 2px;
}

.pwd-slider :deep(.el-slider__runway) {
  height: 6px;
  border-radius: 999px;
  background: #e8eef6;
}

.pwd-slider :deep(.el-slider__bar) {
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #2563eb);
}

.pwd-slider :deep(.el-slider__button) {
  width: 16px;
  height: 16px;
  border: 3px solid #2563eb;
  background: #fff;
  box-shadow: 0 1px 5px rgba(37, 99, 235, 0.35);
}

.pwd-slider :deep(.el-slider__marks-text) {
  font-size: 10px;
  color: #b0b9c8;
  margin-top: 5px;
}

/* 字符类型卡：选中渐变描边 + 图标反白 + 对勾徽章弹出 */
.pwd-type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.pwd-type {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  border: 1.5px solid #e6edf5;
  border-radius: 11px;
  background: #fbfdff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s, background 0.18s, transform 0.18s, box-shadow 0.18s;
}

.pwd-type:hover {
  border-color: #bfdbfe;
  transform: translateY(-1px);
}

.pwd-type.on {
  border-color: #2563eb;
  background: linear-gradient(135deg, #eff6ff, #e2eeff);
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.12), inset 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.pwd-type-ico {
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: #eef2f7;
  color: #64748b;
  transition: all 0.18s;
}

.pwd-type.on .pwd-type-ico {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.35);
}

.pwd-type-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.pwd-type-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.pwd-type-range {
  font-size: 10.5px;
  color: #94a3b8;
}

.pwd-type-check {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  font-size: 10px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(37, 99, 235, 0.4);
  opacity: 0;
  transform: scale(0.3);
  transition: all 0.22s cubic-bezier(0.3, 1.6, 0.5, 1);
}

.pwd-type.on .pwd-type-check {
  opacity: 1;
  transform: scale(1);
}

/* 排除字符 chips：点击移除，增删带动画 */
.pwd-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.pwd-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border-radius: 7px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
}

.pwd-chip:hover {
  background: #fee2e2;
  transform: translateY(-1px);
}

.pwd-chip i {
  font-style: normal;
  font-size: 10px;
  opacity: 0.6;
  padding: 0 2px;
}

.chip-enter-active,
.chip-leave-active {
  transition: all 0.18s ease;
}

.chip-enter-from,
.chip-leave-to {
  opacity: 0;
  transform: scale(0.6);
}

.chip-move {
  transition: transform 0.18s ease;
}

/* 生成数量分段选择 */
.pwd-count-seg {
  width: 100%;
}

.pwd-count-seg :deep(.el-segmented) {
  width: 100%;
  border-radius: 9px;
}

.pwd-count-seg :deep(.el-segmented__item) {
  font-variant-numeric: tabular-nums;
}

.pwd-count-seg :deep(.el-segmented__item.is-selected) {
  color: #1d4ed8;
  font-weight: 700;
}

/* UUID 输出格式：大写/连字符开关 + 包装分段 */
.pwd-uuid-opts {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.pwd-uuid-opts :deep(.el-switch__label) {
  font-size: 12px;
}

.pwd-uuid-wrap {
  flex: 1;
  min-width: 170px;
}

.pwd-uuid-wrap :deep(.el-segmented__item.is-selected) {
  color: #1d4ed8;
  font-weight: 700;
}

/* 生成按钮：渐变主按钮，hover 上浮按压回弹 */
.pwd-gen-btn {
  width: 100%;
  height: 38px;
  margin-top: 2px;
  border-radius: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  border: none;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
  transition: transform 0.18s, box-shadow 0.18s;
}

.pwd-gen-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.45);
  background: linear-gradient(135deg, #2563eb, #1e3fae);
}

.pwd-gen-btn:active {
  transform: translateY(0) scale(0.99);
}

.pwd-gen-ico {
  font-size: 15px;
  margin-right: 2px;
}

/* 结果区：条数徽章 + 头部按钮 */
.pwd-result-count {
  padding: 1px 9px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.pwd-head-btn {
  margin-left: auto;
  padding: 0 6px;
}

.pwd-head-btn + .pwd-head-btn {
  margin-left: 0;
}

/* 结果列表：逐条错落入场，hover 右移浮起“复制” */
.pwd-list {
  max-height: 436px;
  overflow: auto;
}

.pwd-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, transform 0.15s;
  animation: pwd-row-in 0.32s cubic-bezier(0.2, 0.9, 0.3, 1) both;
  animation-delay: calc(var(--i, 0) * 26ms);
}

@keyframes pwd-row-in {
  from {
    opacity: 0;
    transform: translateX(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.pwd-row:hover {
  border-color: #93c5fd;
  background: #f8faff;
  transform: translateX(2px);
}

.pwd-row-idx {
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.pwd-row-value {
  flex: 1;
  min-width: 0;
  letter-spacing: 0.6px;
  line-height: 1.5;
}

/* 密码字符分类着色（span 由 v-html 生成，:deep 命中） */
.pwd-row-value :deep(.pc-n) {
  color: #2563eb;
}

.pwd-row-value :deep(.pc-u) {
  color: #7c3aed;
}

.pwd-row-value :deep(.pc-l) {
  color: #059669;
}

.pwd-row-value :deep(.pc-s) {
  color: #ea580c;
}

.pwd-row-copy {
  flex: none;
  font-size: 11px;
  color: #2563eb;
  opacity: 0;
  transition: opacity 0.15s;
}

.pwd-row:hover .pwd-row-copy {
  opacity: 1;
}

/* 空状态 */
.pwd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 46px 0 40px;
  color: #94a3b8;
}

.pwd-empty-ico {
  font-size: 42px;
  opacity: 0.55;
  filter: saturate(0.85);
  animation: pwd-float 3s ease-in-out infinite;
}

@keyframes pwd-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-7px);
  }
}

.pwd-empty-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.pwd-empty-sub {
  font-size: 12px;
}

.pwd-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 4px 0;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 980px) {
  .pwd-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== Base64 ===== */
.b64-ico-text {
  background: linear-gradient(135deg, #ccfbf1, #99f6e4);
}

.b64-ico-img {
  background: linear-gradient(135deg, #ffe4e6, #fecdd3);
}

.b64-mode-tip {
  margin-left: auto;
  padding: 1px 10px;
  border-radius: 999px;
  background: #f0fdfa;
  border: 1px solid #ccfbf1;
  color: #0f766e;
  font-size: 11px;
  font-weight: 600;
}

.b64-io-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 2px 6px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #94a3b8;
}

.b64-area :deep(.el-textarea__inner) {
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 12.5px;
  line-height: 17px;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.b64-area :deep(.el-textarea__inner:focus) {
  border-color: #14b8a6;
  box-shadow: 0 0 0 1px rgba(20, 184, 166, 0.22);
}

/* 工具条：方向分段（切换即转换）+ URL Safe 开关 */
.b64-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin: 10px 0;
}

.b64-seg {
  flex: 1;
  min-width: 150px;
}

.b64-seg :deep(.el-segmented__item.is-selected) {
  color: #0f766e;
  font-weight: 700;
}

.b64-toolbar :deep(.el-switch__label) {
  font-size: 12px;
}

/* 只读结果区：可选中复制，长文自动换行滚动；错误态红底 */
.b64-output {
  min-height: 86px;
  max-height: 220px;
  overflow: auto;
  padding: 10px 12px;
  border: 1px solid #e6edf5;
  border-radius: 10px;
  background: #f8fafc;
  color: #1f2d3d;
  white-space: pre-wrap;
  word-break: break-all;
  user-select: text;
  transition: border-color 0.2s, background 0.2s;
}

.b64-output.err {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.b64-status {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 26px;
  margin-top: 8px;
  font-size: 12.5px;
  color: #94a3b8;
}

.b64-status .ok {
  color: #059669;
}

.b64-status .bad {
  color: #dc2626;
}

.b64-status-btns {
  margin-left: auto;
  display: flex;
  align-items: center;
}

/* 拖拽区：悬停浅青；拖入渐变流光边框 + 图标上浮（提示层 pointer-events:none 防抖动） */
.img-dropzone {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 128px;
  border: 1.5px dashed #cbd5e1;
  border-radius: 14px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.img-dropzone:hover {
  border-color: #5eead4;
  background: #f0fdfa;
}

.img-dropzone.over {
  border-color: transparent;
  background:
    linear-gradient(#f0fdfa, #f0fdfa) padding-box,
    linear-gradient(120deg, #14b8a6, #3b82f6, #14b8a6) border-box;
  background-size: 100% 100%, 250% 100%;
  box-shadow: 0 8px 24px rgba(20, 184, 166, 0.18);
  animation: img-border-flow 1.4s linear infinite;
}

@keyframes img-border-flow {
  to {
    background-position: 0 0, 250% 0;
  }
}

.img-drop-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 12px;
  text-align: center;
  font-size: 12.5px;
  color: #64748b;
  pointer-events: none;
}

.img-drop-ico {
  font-size: 30px;
  transition: transform 0.2s;
}

/* 无图时图标缓慢上下浮动；拖入时停止浮动并放大 */
.img-drop-ico.float {
  animation: pwd-float 2.8s ease-in-out infinite;
}

.img-dropzone.over .img-drop-ico {
  transform: scale(1.18);
  animation: none;
}

.img-drop-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.img-drop-sub {
  font-size: 11.5px;
  color: #94a3b8;
}

.img-drop-formats {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}

.img-drop-formats span {
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #0f766e;
  background: #ccfbf1;
  border: 1px solid #99f6e4;
}

/* 预览：棋盘格透明底 + 图片投影 */
.img-result {
  margin-top: 2px;
}

.img-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 88px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background:
    linear-gradient(45deg, #f8fafc 25%, transparent 25%, transparent 75%, #f8fafc 75%),
    linear-gradient(45deg, #f8fafc 25%, transparent 25%, transparent 75%, #f8fafc 75%);
  background-size: 16px 16px;
  background-position: 0 0, 8px 8px;
  background-color: #fff;
}

.img-preview img {
  max-width: 100%;
  max-height: 180px;
  border-radius: 7px;
  box-shadow: 0 2px 10px rgba(16, 24, 40, 0.1);
}

/* 信息 pills：名称 / 类型 / 大小 / Base64 体积 */
.img-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.img-pill {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11.5px;
}

.img-pill.hl {
  background: #e0f2fe;
  color: #0369a1;
  font-weight: 600;
}

.img-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 11px;
}

/* 渐变主按钮（蓝紫主视觉，密码生成按钮同款） */
.grad-btn {
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
  border: none !important;
  border-radius: 9px !important;
  font-weight: 600;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.3);
  transition: transform 0.18s, box-shadow 0.18s;
}

.grad-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(37, 99, 235, 0.42);
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
}

/* teal 描边按钮（呼应 Base64 Tab 主色） */
.grad-plain-btn {
  border-radius: 9px;
  font-weight: 600;
  color: #0f766e;
  border-color: #99f6e4;
  background: #f0fdfa;
}

.grad-plain-btn:hover {
  color: #0f766e;
  border-color: #5eead4;
  background: #ccfbf1;
}

.img-decode {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #eef2f7;
}

.img-decode-row {
  display: flex;
  gap: 6px;
}

.img-decode-row .el-input {
  flex: 1;
}

/* 图片结果 / 预览淡入 */
.img-fade-enter-active,
.img-fade-leave-active {
  transition: all 0.25s ease;
}

.img-fade-enter-from,
.img-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* ===== 项目批量重命名 ===== */
.rn-ico-set {
  background: linear-gradient(135deg, #ffedd5, #fed7aa);
}

.rn-ico-res {
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
}

/* 引擎横幅：深琥珀渐变 + 状态徽章 + 配置总览（右侧 🔄 缓旋点题） */
.rn-banner {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, #431407 0%, #9a3412 58%, #ea580c 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(154, 52, 18, 0.3);
}

.rn-banner-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.rn-banner-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.rn-banner-deco-2 {
  width: 130px;
  height: 130px;
  right: 120px;
  bottom: -70px;
  border-color: rgba(255, 255, 255, 0.08);
}

.rn-banner-left {
  position: relative;
  min-width: 0;
  flex: 1;
}

.rn-banner-title-row {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.rn-banner-title {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.rn-banner-sub {
  margin-top: 7px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.rn-banner-meta {
  margin-top: 11px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
  font-variant-numeric: tabular-nums;
}

.rn-banner-meta b {
  color: #fed7aa;
  font-weight: 700;
}

/* 状态徽章：待命灰 / 运行蓝脉冲 / 完成绿 / 取消黄 / 失败红 */
.rn-badge {
  padding: 2px 12px;
  margin-left: 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 1px;
  border: 1px solid;
}

.rn-badge.idle {
  color: #f1f5f9;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.32);
}

.rn-badge.run {
  color: #bfdbfe;
  background: rgba(96, 165, 250, 0.18);
  border-color: rgba(147, 197, 253, 0.45);
  animation: rn-pulse 1.6s ease infinite;
}

.rn-badge.ok {
  color: #bbf7d0;
  background: rgba(52, 211, 153, 0.16);
  border-color: rgba(110, 231, 183, 0.4);
}

.rn-badge.warn {
  color: #fde68a;
  background: rgba(251, 191, 36, 0.16);
  border-color: rgba(252, 211, 77, 0.4);
}

.rn-badge.bad {
  color: #fecaca;
  background: rgba(248, 113, 113, 0.16);
  border-color: rgba(252, 165, 165, 0.4);
}

.rn-banner-mark {
  position: relative;
  font-size: 58px;
  margin-right: 26px;
  opacity: 0.85;
  filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.45)) drop-shadow(0 0 2px rgba(255, 237, 213, 0.5));
  user-select: none;
  animation: rn-spin 9s linear infinite;
}

@keyframes rn-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 目录选择卡：整卡可点，未选虚线 / 已选实线浅橙底 */
.rn-dir-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 13px;
  border: 1.5px dashed #fcd9b6;
  border-radius: 12px;
  background: linear-gradient(135deg, #fffaf5, #fff7ed);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, transform 0.2s, box-shadow 0.2s;
}

.rn-dir-card:hover {
  border-color: #fb923c;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(234, 88, 12, 0.12);
}

.rn-dir-card.set {
  border-style: solid;
  border-color: #fdba74;
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
}

.rn-dir-ico {
  flex: none;
  font-size: 22px;
}

.rn-dir-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rn-dir-label {
  font-size: 10.5px;
  color: #c2410c;
  font-weight: 700;
  letter-spacing: 1px;
}

.rn-dir-path {
  font-size: 12px;
  color: #7c2d12;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rn-dir-act {
  flex: none;
  padding: 4px 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #fdba74;
  color: #c2410c;
  font-size: 12.5px;
  font-weight: 600;
}

/* 规则行：序号有效转绿，箭头 hover 变橙 */
.rn-rules {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rn-rule {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rn-rule-idx {
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 11px;
  transition: all 0.25s ease;
}

.rn-rule-idx.ok {
  background: #d1fae5;
  color: #059669;
  font-weight: 700;
}

.rn-rule .el-input {
  flex: 1;
  min-width: 0;
}

.rn-arrow {
  flex: none;
  color: #cbd5e1;
  font-size: 13px;
  transition: color 0.2s ease, transform 0.2s ease;
}

.rn-rule:hover .rn-arrow {
  color: #f97316;
  transform: translateX(1px);
}

.rn-rule-del {
  flex: none;
  padding: 0 4px;
}

/* 执行选项卡：选中橙色描边，禁用降透明度 */
.rn-opt-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.rn-opt {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  border: 1.5px solid #e6edf5;
  border-radius: 11px;
  background: #fbfdff;
  transition: all 0.18s ease;
}

.rn-opt.on {
  border-color: #fb923c;
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  box-shadow: 0 3px 10px rgba(234, 88, 12, 0.1);
}

.rn-opt.dim {
  opacity: 0.55;
}

.rn-opt-ico {
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  background: #fff;
  border: 1px solid #fde68a;
}

.rn-opt-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.rn-opt-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.rn-opt-sub {
  font-size: 10.5px;
  color: #94a3b8;
}

/* 排除目录 chips：橙系（区别于密码排除的红系） */
.rn-exclude-chips {
  margin-top: 8px;
}

.rn-exclude-chip {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #c2410c;
}

.rn-exclude-chip:hover {
  background: #ffedd5;
}

/* 开始按钮：橙色渐变主按钮 */
.rn-gen-btn {
  background: linear-gradient(135deg, #f97316, #c2410c) !important;
  box-shadow: 0 4px 12px rgba(234, 88, 12, 0.35) !important;
}

.rn-gen-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #f97316, #c2410c) !important;
  box-shadow: 0 6px 16px rgba(234, 88, 12, 0.45) !important;
}

.rn-cancel-btn {
  width: 100%;
  margin: 8px 0 0;
  height: 30px;
}

/* 运行中标记 */
.rn-live {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #059669;
  font-variant-numeric: tabular-nums;
}

.rn-live i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  animation: rn-pulse 1.8s ease infinite;
}

@keyframes rn-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

/* 阶段步骤条：圆点 + 连接线（进行橙脉冲 / 完成绿 / 待灰） */
.rn-steps {
  display: flex;
  align-items: center;
  margin: 2px 0 14px;
}

.rn-step {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: none;
}

.rn-step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e2e8f0;
  transition: all 0.25s ease;
}

.rn-step-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  white-space: nowrap;
}

.rn-step.on .rn-step-dot {
  background: #f97316;
  box-shadow: 0 0 0 4px rgba(249, 115, 22, 0.15);
  animation: rn-pulse 1.2s ease infinite;
}

.rn-step.on .rn-step-label {
  color: #c2410c;
}

.rn-step.done .rn-step-dot {
  background: #059669;
}

.rn-step.done .rn-step-label {
  color: #059669;
}

.rn-step-line {
  flex: 1;
  min-width: 12px;
  height: 2.5px;
  margin: 0 8px;
  border-radius: 2px;
  background: #e8eef6;
  transition: background 0.3s ease;
}

.rn-step-line.done {
  background: #6ee7b7;
}

/* worker 池活动可视化：方块波浪脉冲，数量 = 线程数 */
.rn-workers {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  margin-bottom: 12px;
  min-height: 20px;
}

.rn-workers-label {
  font-size: 11px;
  color: #94a3b8;
  margin-right: 6px;
  align-self: center;
}

.rn-worker {
  width: 13px;
  height: 19px;
  border-radius: 4px;
  background: linear-gradient(180deg, #fdba74, #f97316);
  transform-origin: bottom;
  animation: rn-worker-b 1.1s ease-in-out infinite;
  animation-delay: var(--d, 0s);
}

@keyframes rn-worker-b {
  0%,
  100% {
    transform: scaleY(0.5);
    opacity: 0.4;
  }
  50% {
    transform: scaleY(1);
    opacity: 1;
  }
}

/* 进度条：橙渐变填充，运行中扫光 */
.rn-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rn-bar {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.rn-bar-fill {
  position: relative;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #fb923c, #ea580c);
  transition: width 0.25s ease;
}

.rn-bar-fill.live {
  overflow: hidden;
}

.rn-bar-fill.live::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  animation: rn-shine 1.8s linear infinite;
}

@keyframes rn-shine {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(100%);
  }
}

.rn-pct {
  flex: none;
  width: 44px;
  text-align: right;
  font-size: 13px;
  font-weight: 700;
  color: #c2410c;
  font-variant-numeric: tabular-nums;
}

.rn-count {
  margin-top: 6px;
  font-size: 11.5px;
  color: #8a94a6;
  font-variant-numeric: tabular-nums;
}

/* 当前路径：终端风深底（❯ 前缀由 ::before 提供） */
.rn-current {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  min-height: 20px;
  padding: 6px 10px;
  border-radius: 8px;
  background: #0f172a;
  color: #7dd3fc;
  font-size: 11.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rn-current::before {
  content: '❯';
  flex: none;
  color: #34d399;
  font-weight: 700;
}

/* 统计四宫格：图标 + 主题色数值（蓝紫绿橙） */
.rn-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 14px;
  animation: ct-pane-in 0.3s ease both;
}

.rn-stat {
  padding: 11px 6px 10px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  text-align: center;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.rn-stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(16, 24, 40, 0.07);
}

.rn-stat-ico {
  font-size: 15px;
}

.rn-stat-val {
  margin-top: 3px;
  font-size: 19px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.rn-stat-label {
  margin-top: 2px;
  font-size: 11px;
  color: #8a94a6;
}

.rn-stat.s1 .rn-stat-val {
  color: #2563eb;
}

.rn-stat.s2 .rn-stat-val {
  color: #7c3aed;
}

.rn-stat.s3 .rn-stat-val {
  color: #059669;
}

.rn-stat.s4 .rn-stat-val {
  color: #ea580c;
}

/* 完成 / 错误摘要 */
.rn-done {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  animation: ct-pane-in 0.3s ease both;
}

.rn-done-line {
  font-size: 13px;
  font-weight: 700;
}

.rn-done-line.ok {
  color: #059669;
}

.rn-done-line.bad {
  color: #dc2626;
}

.rn-done-sub {
  margin-top: 5px;
  font-size: 12px;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

/* 根目录新路径 pill */
.rn-newroot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.rn-newroot-label {
  flex: none;
  font-size: 11.5px;
  color: #047857;
  font-weight: 700;
}

.rn-newroot-path {
  padding: 3px 10px;
  border-radius: 7px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #047857;
  font-size: 11.5px;
  word-break: break-all;
}

.rn-errors {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}

.rn-errors-title {
  font-size: 12px;
  font-weight: 600;
  color: #d97706;
}

.rn-error-line {
  margin-top: 5px;
  font-size: 11px;
  color: #8a94a6;
  word-break: break-all;
}

.rn-errors-more {
  margin-top: 5px;
  font-size: 11px;
  color: #b0b9c8;
}

/* 空状态：图标放大加重视觉分量 */
.rn-empty .pwd-empty-ico {
  font-size: 50px;
  opacity: 0.65;
}

/* 空状态补充标签 */
.rn-empty-tags {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.rn-empty-tags span {
  padding: 2px 10px;
  border-radius: 999px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #c2410c;
  font-size: 11px;
  font-weight: 600;
}

@media (max-width: 1080px) {
  .rn-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .rn-opt-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .ts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
