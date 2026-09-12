<template>
  <div class="designer-page">
    <div class="container">
      <!-- 顶部标题 + 操作（极简卡片头风格） -->
      <div class="page-head">
        <div class="head-left">
          <span class="eyebrow-pill">DESIGN STUDIO</span>
          <h1 class="page-title">{{ editingId ? '编辑设计' : 'DIY 设计台' }}</h1>
        </div>
        <div class="head-actions">
          <button class="icon-btn" :disabled="!canUndo" @click="undo" title="撤销">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
          </button>
          <button class="icon-btn" :disabled="!canRedo" @click="redo" title="重做">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          </button>
          <button class="btn-ghost-sm" @click="$router.push('/templates')">款式模板</button>
          <button class="btn-ghost-sm" @click="$router.push('/my-designs')">我的设计</button>
        </div>
      </div>

      <!-- 工作室核心：左预览 + 右素材面板（原功能结构保留） -->
      <div class="studio">
        <!-- ============== 左侧预览 ============== -->
        <div class="preview-wrap">
          <!-- 预览舞台（米色画布风） -->
          <div class="stage-head">
            <span class="stage-label">实时预览</span>

            <span class="active-finger" v-if="activeFinger >= 0">
              正在编辑 · 第 {{ activeFinger + 1 }} 指 · {{ FINGER_NAMES[activeFinger] }}
            </span>

            <div class="stage-head-right">
              <!-- 预览模式切换：手模 / 平铺 -->
              <div class="view-toggle">
                <button :class="{ on: viewMode === 'handModel' }" @click="viewMode = 'handModel'" title="手模视图">手模</button>
                <button :class="{ on: viewMode === 'hand' }" @click="viewMode = 'hand'" title="甲片平铺">平铺</button>
              </div>
            </div>
          </div>
          <div ref="previewRef" class="preview-stage"
               :style="{ minHeight: viewMode === 'hand' ? (360 * zoom / 100 + 'px') : (viewMode === 'handModel' ? '560px' : '520px') }">

            <!-- ========== 手模视图：位图手 + 逐指槽位（支持拖拽投放） ========== -->
            <div v-if="viewMode === 'handModel'" class="hand-wrap">
              <HandModel :active-finger="activeFinger" :fingers="fingers"
                         @select="activeFinger = $event"
                         @drop="onHandDrop">
                <template #default="{ finger, index, width, lenFit }">
                  <NailArt :shape="shapeById(finger.shapeId)"
                           :color="colorCodeOf(finger)"
                           :patterns="patternsOf(finger)"
                           :active="index === activeFinger"
                           :index="index"
                           :width="width"
                           :length="lenFit(finger.length)"
                           :outlined="false"
                           :overlays="finger.overlays"
                           :overlay-catalog="overlays"
                           :gradient="finger.gradient"
                           :cat-eye="finger.catEye"
                           :adjust="finger.adjust"
                           :french="finger.french"
                           :dots="finger.dots"
                           :pattern-style="finger.patternStyle"
                           :decorations="finger.decorations"
                           :pattern-pos="finger.patternPos"
                           @move-deco="p => onDecoMovedAt(index, p)"
                           @move-pattern="p => onPatMovedAt(index, p)" />
                </template>
              </HandModel>
            </div>

            <!-- ========== 甲片平铺预览：支持拖拽投放素材 ========== -->
            <div v-else-if="viewMode === 'hand'" class="nails" :style="{ transform: `scale(${zoom / 100})` }">
              <div v-for="(f, i) in fingers" :key="'p' + i"
                   class="nail-slot"
                   :class="{ active: i === activeFinger, over: dragOverIdx === i }"
                   role="button"
                   tabindex="0"
                   :aria-pressed="i === activeFinger"
                   :aria-label="`第 ${i + 1} 指 ${FINGER_NAMES[i]}，回车选中，空格放大精修`"
                   :title="`第 ${i + 1} 指 ${FINGER_NAMES[i]}`"
                   @click="activeFinger = i"
                   @keydown.enter.prevent="activeFinger = i"
                   @keydown.space.prevent="openZoom(i)"
                   @dblclick.stop.prevent="openZoom(i)"
                   @dragover.prevent="dragOverIdx = i"
                   @dragleave="dragOverIdx = -1"
                   @drop.prevent="dragOverIdx = -1; onNailDropAt(i, $event)">
                <NailArt
                  :shape="shapeById(f.shapeId)"
                  :color="colorCodeOf(f)"
                  :patterns="patternsOf(f)"
                  :active="i === activeFinger"
                  :index="i"
                  :width="widthByIndex[i]"
                  :length="f.length"
                  :overlays="f.overlays"
                  :overlay-catalog="overlays"
                  :gradient="f.gradient"
                  :cat-eye="f.catEye"
                  :adjust="f.adjust"
                  :french="f.french"
                  :dots="f.dots"
                  :pattern-style="f.patternStyle"
                  :decorations="f.decorations"
                  :pattern-pos="f.patternPos"
                  @move-deco="p => onDecoMovedAt(i, p)"
                  @move-pattern="p => onPatMovedAt(i, p)"
                />
              </div>
            </div>

            <!-- ========== 单甲精细编辑模式（借鉴 Luna：就地放大，非弹窗） ========== -->
            <div v-else class="single-mode">
              <div class="single-head">
                <span class="single-title">精细编辑 · 第 {{ activeFinger + 1 }} 指 · <b>{{ FINGER_NAMES[activeFinger] }}</b></span>
                <button class="back-btn" @click="viewMode = 'handModel'">⌄ 返回整组</button>
              </div>
              <div class="single-nail">
                <button class="single-nav" :disabled="activeFinger <= 0" @click="activeFinger--" title="上一指（←）">‹</button>
                <div class="single-canvas"
                     :class="{ over: singleOver }"
                     role="group"
                     :aria-label="`第 ${activeFinger + 1} 指 ${FINGER_NAMES[activeFinger]} 的放大预览，双击返回整组`"
                     @dragover.prevent="singleOver = true"
                     @dragleave="singleOver = false"
                     @drop.prevent="singleOver = false; onNailDropAt(activeFinger, $event)"
                     @dblclick.stop.prevent="viewMode = 'handModel'">
                  <NailArt
                    :shape="shapeById(fingers[activeFinger].shapeId)"
                    :color="colorCodeOf(fingers[activeFinger])"
                    :patterns="patternsOf(fingers[activeFinger])"
                    :active="false"
                    :index="activeFinger"
                    :width="ZOOMED_NAIL_W"
                    :length="fingers[activeFinger].length"
                    :overlays="fingers[activeFinger].overlays"
                    :overlay-catalog="overlays"
                    :gradient="fingers[activeFinger].gradient"
                    :cat-eye="fingers[activeFinger].catEye"
                    :adjust="fingers[activeFinger].adjust"
                    :french="fingers[activeFinger].french"
                    :dots="fingers[activeFinger].dots"
                    :pattern-style="fingers[activeFinger].patternStyle"
                    :decorations="fingers[activeFinger].decorations"
                    :pattern-pos="fingers[activeFinger].patternPos"
                    @move-deco="p => onDecoMovedAt(activeFinger, p)"
                    @move-pattern="p => onPatMovedAt(activeFinger, p)"
                  />
                </div>
                <button class="single-nav" :disabled="activeFinger >= 4" @click="activeFinger++" title="下一指（→）">›</button>
              </div>
              <div class="single-hint">双击大指甲 / ESC 返回整组 · ← → 切换手指 · 素材拖到哪就落在哪，落下后还能拖动微调</div>
            </div>

          </div>

          <!-- 预览下工具条 -->
          <div class="stage-toolbar">
            <div class="tools-left">
              <div class="zoom-pill-mini">
                <button class="zbtn" @click="zoomOut">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </button>
                <input type="range" :value="zoom" @input="onZoomInput" min="60" max="160" />
                <button class="zbtn" @click="zoomIn">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </button>
              </div>
              <button class="btn-ghost-sm" @click="syncAll">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                <span>{{ syncLabel }}</span>
              </button>
            </div>
            <div class="tools-right">
              <input class="text-input" v-model="designName" placeholder="设计名称（必填）" maxlength="50" />
              <button class="btn-primary-sm" :disabled="saving" @click="save">
                {{ saving ? '保存中…' : (editingId ? '保存修改' : '保存设计') }}
              </button>
            </div>
          </div>
          <p class="tip">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            点击指甲选中，<b>双击放大</b>；素材<b>拖到哪就落在哪</b>，图案落下后还能在甲面上拖动微调；平台标准价 <b>￥39.00 / 副</b>
          </p>
        </div>

        <!-- ============== 右侧素材面板 ============== -->
        <div class="material-panel">
          <!-- 分类 Tabs（极简无衬线风格） -->
          <div class="tabs-row">
            <button v-for="t in TABS" :key="t.key"
                    class="tab-btn"
                    :class="{ active: tab === t.key }"
                    @click="tab = t.key">
              <span v-html="t.icon"></span>
              <span class="tab-label">{{ t.zh }} · {{ t.en }}</span>
            </button>
          </div>

          <div class="tab-body">
            <!-- 甲型 -->
            <template v-if="tab === 'shape'">
              <p class="section-desc">为当前选中的第 <b>{{ activeFinger + 1 }} · {{ FINGER_NAMES[activeFinger] }}</b> 选择甲型：</p>
              <div class="shape-grid">
                <div v-for="m in shapes" :key="m.id" class="shape-card"
                     draggable="true" @dragstart="onDragStart($event, { kind: 'shape', id: m.id })"
                     role="button"
                     tabindex="0"
                     :aria-pressed="fingers[activeFinger].shapeId === m.id"
                     :aria-label="`甲型 ${m.name}`"
                     @keydown.enter.prevent="selectShape(m.id)"
                     @keydown.space.prevent="selectShape(m.id)"
                     :class="{ active: fingers[activeFinger].shapeId === m.id }"
                     @click="selectShape(m.id)">
                  <div class="shape-preview">
                    <NailArt :shape="m" :color="'#F5EADF'" :width="44" :length="fingers[activeFinger].length" />
                  </div>
                  <span class="shape-name">{{ m.name }}</span>
                </div>
                <empty-box v-if="!shapes.length" text="暂无甲型素材" />
              </div>

              <!-- 长度：独立于甲型的设计维度（标准即完整覆盖天然甲床，加长向指尖延伸） -->
              <div class="fx-block length-block">
                <div class="fx-head">
                  <div>
                    <span class="fx-title">长度</span>
                    <span class="fx-sub">标准已完整覆盖甲床 · 加长向指尖延伸</span>
                  </div>
                  <span class="len-value">{{ lengthLabel }}</span>
                </div>
                <div class="fx-body">
                  <div class="slider-row len-row">
                    <span class="fx-label">标准</span>
                    <input type="range" min="0.6" max="1.6" step="0.05"
                           v-model.number="fingers[activeFinger].length" @change="pushHistory" />
                    <span class="fx-label">加长</span>
                  </div>
                </div>
              </div>
            </template>

            <!-- 底胶：色卡 + 渐变 + 猫眼 + 色彩微调 -->
            <template v-if="tab === 'color'">
              <div class="color-meta-row">
                <p class="section-desc">HEX 自定义：</p>
                <div class="hex-row">
                  <input class="hex-input" v-model="hexInput" maxlength="7" @change="applyHex" placeholder="#C8813C" />
                  <span class="hex-swatch" :style="{ background: colorCodeOf(fingers[activeFinger]) }"></span>
                </div>
              </div>
              <!-- 拖动选色：横向色相 + 明度滑条 -->
              <div class="slider-colors">
                <div class="slider-row">
                  <span class="fx-label" id="hue-label">色相 <b>{{ Math.round(sliderHue) }}°</b></span>
                  <div class="hue-bar" role="slider" tabindex="0" aria-labelledby="hue-label"
                       :aria-valuemin="0" :aria-valuemax="360" :aria-valuenow="Math.round(sliderHue)"
                       @pointerdown.prevent="onBarDown($event, 'hue')"
                       @keydown.left.prevent="nudgeBar('hue', -4)"
                       @keydown.right.prevent="nudgeBar('hue', 4)"
                       @keydown.down.prevent="nudgeBar('hue', -4)"
                       @keydown.up.prevent="nudgeBar('hue', 4)">
                    <span class="bar-thumb" :style="{ left: (sliderHue / 360 * 100) + '%' }"></span>
                  </div>
                </div>
                <div class="slider-row">
                  <span class="fx-label" id="light-label">明度 <b>{{ Math.round(sliderLight) }}</b></span>
                  <div class="light-bar" role="slider" tabindex="0" aria-labelledby="light-label"
                       :style="lightBarStyle"
                       :aria-valuemin="0" :aria-valuemax="100" :aria-valuenow="Math.round(sliderLight)"
                       @pointerdown.prevent="onBarDown($event, 'light')"
                       @keydown.left.prevent="nudgeBar('light', -2)"
                       @keydown.right.prevent="nudgeBar('light', 2)"
                       @keydown.down.prevent="nudgeBar('light', -2)"
                       @keydown.up.prevent="nudgeBar('light', 2)">
                    <span class="bar-thumb" :style="{ left: sliderLight + '%', background: sliderColor }"></span>
                  </div>
                </div>
              </div>

              <!-- 色系筛选（借鉴 Luna：分类筛选） -->
              <div class="cat-row">
                <button v-for="c in COLOR_CATS" :key="c.value"
                        class="cat-chip"
                        :class="{ active: colorCategory === c.value }"
                        @click="colorCategory = c.value">
                  {{ c.label }}
                </button>
              </div>
              <p class="section-desc">精选色卡 · 共 <b>{{ filteredColors.length }}</b> 色（点击或拖到指甲应用）：</p>
              <div class="color-grid">
                <div v-for="m in filteredColors" :key="m.id"
                     class="color-cell"
                     draggable="true" @dragstart="onDragStart($event, { kind: 'color', id: m.id, code: m.colorCode })"
                     role="button"
                     tabindex="0"
                     :aria-pressed="fingers[activeFinger].colorId === m.id"
                     :aria-label="`色卡 ${m.name} ${m.colorCode}`"
                     @keydown.enter.prevent="selectColor(m.id, m.colorCode)"
                     @keydown.space.prevent="selectColor(m.id, m.colorCode)"
                     :class="{ active: fingers[activeFinger].colorId === m.id }"
                     :style="{ background: m.colorCode }"
                     :title="`${m.name} · ${m.colorCode}`"
                     @click="selectColor(m.id, m.colorCode)">
                  <i class="check">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                  </i>
                </div>
                <empty-box v-if="!colors.length" text="暂无色卡素材" />
              </div>

              <!-- ══ 效果层：OVERLAY 覆盖层栈（可多选叠加） ══ -->
              <div class="fx-block">
                <div class="fx-head">
                  <div>
                    <span class="fx-title">效果层</span>
                    <span class="fx-sub">珠光 / 闪耀 / 炫光 / 特殊材质 · 可叠加多层</span>
                  </div>
                  <span class="len-value" v-if="fingerOverlays.length">{{ fingerOverlays.length }} 层</span>
                </div>
                <div class="fx-body">
                  <div class="cat-row">
                    <button v-for="c in OVERLAY_CATS" :key="c.value" class="cat-chip"
                            :class="{ active: overlaySubtype === c.value }"
                            @click="overlaySubtype = c.value">{{ c.label }}</button>
                  </div>
                  <div class="overlay-grid">
                    <div v-for="m in filteredOverlays" :key="m.id"
                         class="overlay-card"
                         :class="{ active: fingerOverlays.some(o => o.id === m.id) }"
                         role="button" tabindex="0"
                         :aria-pressed="fingerOverlays.some(o => o.id === m.id)"
                         :aria-label="`效果层 ${m.name}`"
                         @click="toggleOverlay(m.id)"
                         @keydown.enter.prevent="toggleOverlay(m.id)">
                      <div class="overlay-preview">
                        <NailArt :shape="shapeById(active.shapeId)" :color="colorCodeOf(active)"
                                 :length="active.length" :outlined="false" :width="44"
                                 :overlays="[{ id: m.id, opacity: 1 }]" :overlay-catalog="overlays" />
                      </div>
                      <span class="overlay-name">{{ m.name }}</span>
                    </div>
                    <empty-box v-if="!filteredOverlays.length" text="暂无效果层素材" />
                  </div>
                  <template v-if="fingerOverlays.length">
                    <p class="section-desc ov-count">已叠加 <b>{{ fingerOverlays.length }}</b> 层（透明度可调）：</p>
                    <div class="ov-list">
                      <div v-for="o in fingerOverlays" :key="o.id" class="ov-item-row">
                        <span class="ov-name">{{ overlayName(o.id) }}</span>
                        <input type="range" min="0.15" max="1" step="0.05"
                               :value="o.opacity"
                               @input="setOverlayOpacity(o.id, Number($event.target.value))"
                               @change="pushHistory" />
                        <button class="ov-del" @click="removeOverlay(o.id)" title="移除该层">×</button>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- ══ 两色渐变 ══ -->
              <div class="fx-block" :class="{ off: !fingers[activeFinger].gradient.enabled }">
                <div class="fx-head">
                  <div>
                    <span class="fx-title">两色渐变</span>
                    <span class="fx-sub">将当前底色与另一种颜色柔和融合</span>
                  </div>
                  <button class="switch" :class="{ on: fingers[activeFinger].gradient.enabled }" @click="toggleGradient">
                    <i></i>
                  </button>
                </div>
                <template v-if="fingers[activeFinger].gradient.enabled">
                  <div class="fx-body">
                    <div class="grad-preview">
                      <span class="gp-c1" :style="{ background: colorCodeOf(fingers[activeFinger]) }"></span>
                      <span class="gp-arrow" @click="swapGradient" title="交换上下">⇅</span>
                      <span class="gp-c2" :style="{ background: fingers[activeFinger].gradient.color2 || '#FFFFFF' }"></span>
                    </div>
                    <div class="grad-pick">
                      <p class="fx-label">第二底色</p>
                      <div class="grad-colors">
                        <span v-for="m in colors.slice(0, 14)" :key="m.id"
                              class="gc-dot" :style="{ background: m.colorCode }"
                              :class="{ active: (fingers[activeFinger].gradient.color2 || '').toUpperCase() === m.colorCode.toUpperCase() }"
                              :title="m.name"
                              @click="pickGradient2(m.id, m.colorCode)"></span>
                        <input class="hex-input sm" v-model="gradientHex2" maxlength="7" @change="applyGradientHex" placeholder="#FFFFFF" />
                      </div>
                    </div>
                    <div class="slider-row">
                      <span class="fx-label">渐变位置 <b>{{ fingers[activeFinger].gradient.position }}%</b></span>
                      <input type="range" min="10" max="90" v-model.number="fingers[activeFinger].gradient.position" @change="pushHistory" />
                    </div>
                  </div>
                </template>
              </div>

              <!-- ══ 猫眼磁吸 ══ -->
              <div class="fx-block" :class="{ off: !fingers[activeFinger].catEye.enabled }">
                <div class="fx-head">
                  <div>
                    <span class="fx-title">猫眼磁吸</span>
                    <span class="fx-sub">拖动滑杆调整光带角度，模拟磁吸猫眼胶</span>
                  </div>
                  <button class="switch" :class="{ on: fingers[activeFinger].catEye.enabled }" @click="fingers[activeFinger].catEye.enabled = !fingers[activeFinger].catEye.enabled; pushHistory()">
                    <i></i>
                  </button>
                </div>
                <template v-if="fingers[activeFinger].catEye.enabled">
                  <div class="fx-body">
                    <div class="slider-row">
                      <span class="fx-label">光带角度 <b>{{ fingers[activeFinger].catEye.angle }}°</b></span>
                      <input type="range" min="-90" max="90" v-model.number="fingers[activeFinger].catEye.angle" @change="pushHistory" />
                    </div>
                    <div class="slider-row">
                      <span class="fx-label">光带宽度 <b>{{ fingers[activeFinger].catEye.width }}</b></span>
                      <input type="range" min="10" max="90" v-model.number="fingers[activeFinger].catEye.width" @change="pushHistory" />
                    </div>
                    <div class="slider-row">
                      <span class="fx-label">光感亮度 <b>{{ fingers[activeFinger].catEye.brightness }}</b></span>
                      <input type="range" min="10" max="100" v-model.number="fingers[activeFinger].catEye.brightness" @change="pushHistory" />
                    </div>
                  </div>
                </template>
              </div>

              <!-- ══ 色彩微调 ══ -->
              <div class="fx-block" :class="{ off: !adjustDirty }">
                <div class="fx-head">
                  <div>
                    <span class="fx-title">色彩微调</span>
                    <span class="fx-sub">在底色基础上调整，保留原色光泽观感</span>
                  </div>
                  <button class="btn-reset" v-if="adjustDirty" @click="resetAdjust">重置</button>
                </div>
                <div class="fx-body">
                  <div class="slider-row">
                    <span class="fx-label">色相 <b>{{ fingers[activeFinger].adjust.hue > 0 ? '+' : '' }}{{ fingers[activeFinger].adjust.hue }}</b></span>
                    <input type="range" min="-180" max="180" v-model.number="fingers[activeFinger].adjust.hue" @change="pushHistory" />
                  </div>
                  <div class="slider-row">
                    <span class="fx-label">饱和度 <b>{{ fingers[activeFinger].adjust.sat > 0 ? '+' : '' }}{{ fingers[activeFinger].adjust.sat }}</b></span>
                    <input type="range" min="-100" max="100" v-model.number="fingers[activeFinger].adjust.sat" @change="pushHistory" />
                  </div>
                  <div class="slider-row">
                    <span class="fx-label">明暗 <b>{{ fingers[activeFinger].adjust.light > 0 ? '+' : '' }}{{ fingers[activeFinger].adjust.light }}</b></span>
                    <input type="range" min="-100" max="100" v-model.number="fingers[activeFinger].adjust.light" @change="pushHistory" />
                  </div>
                </div>
              </div>
            </template>

            <!-- 图案：贴花 / 法式 / 波点豹纹 -->
            <template v-if="tab === 'pattern'">
              <!-- 子面板切换 -->
              <div class="subtabs">
                <button :class="{ active: patternSub === 'style' }"   @click="patternSub = 'style'">款式</button>
                <button :class="{ active: patternSub === 'sticker' }"  @click="patternSub = 'sticker'">贴花</button>
                <button :class="{ active: patternSub === 'french' }"   @click="patternSub = 'french'">法式</button>
                <button :class="{ active: patternSub === 'dots' }"     @click="patternSub = 'dots'">波点 / 豹纹</button>
              </div>

              <!-- ── 参数化款式（引擎渲染） ── -->
              <template v-if="patternSub === 'style'">
                <p class="section-desc">为 <b>第 {{ activeFinger + 1 }} · {{ FINGER_NAMES[activeFinger] }}</b> 选择款式（可叠加、可调参）：</p>
                <div class="style-grid">
                  <div v-for="m in stylePatterns" :key="m.id"
                       class="style-card"
                       :class="{ active: active.patternStyle?.code === m.code }"
                       draggable="true" @dragstart="onDragStart($event, { kind: 'style', code: m.code })"
                       role="button" tabindex="0"
                       :aria-pressed="active.patternStyle?.code === m.code"
                       @keydown.enter.prevent="toggleStyle(m.code)"
                       @click="toggleStyle(m.code)">
                    <div class="style-preview">
                      <NailArt :shape="shapeById(active.shapeId)" :color="colorCodeOf(active)"
                               :pattern-style="{ code: m.code, params: styleDefaults(m.code) }" :width="34" />
                    </div>
                    <span class="style-name">{{ m.name }}</span>
                  </div>
                  <empty-box v-if="!stylePatterns.length" text="暂无参数化款式" />
                </div>

                <!-- 当前款式参数调整 -->
                <div class="fx-block" v-if="active.patternStyle && activeStyleSchema.length">
                  <div class="fx-head">
                    <div>
                      <span class="fx-title">{{ activeStyleName }}</span>
                      <span class="fx-sub">调整参数实时预览</span>
                    </div>
                    <button class="btn-reset" @click="clearStyle">清除款式</button>
                  </div>
                  <div class="fx-body">
                    <div v-for="s in activeStyleSchema" :key="s.key" class="slider-row">
                      <template v-if="s.type === 'range'">
                        <span class="fx-label">{{ s.label }} <b>{{ active.patternStyle.params[s.key] }}</b></span>
                        <input type="range" :min="s.min" :max="s.max" :step="(String(s.default).includes('.') ? 0.1 : 1)"
                               :value="active.patternStyle.params[s.key]"
                               @input="updateStyleParam(s.key, Number($event.target.value))" />
                      </template>
                      <template v-else-if="s.type === 'color'">
                        <span class="fx-label">{{ s.label }}</span>
                        <div class="style-color-row">
                          <input type="color" :value="active.patternStyle.params[s.key]"
                                 @input="updateStyleParam(s.key, $event.target.value)" />
                          <input class="hex-input sm" :value="active.patternStyle.params[s.key]"
                                 maxlength="7" @change="updateStyleParam(s.key, $event.target.value)" />
                        </div>
                      </template>
                      <template v-else-if="s.type === 'select'">
                        <span class="fx-label">{{ s.label }}</span>
                        <select class="style-select" :value="active.patternStyle.params[s.key]"
                                @change="updateStyleParam(s.key, $event.target.value)">
                          <option v-for="o in s.options" :key="o" :value="o">{{ o }}</option>
                        </select>
                      </template>
                    </div>
                  </div>
                </div>
              </template>

              <!-- ── 贴花（素材库） ── -->
              <template v-if="patternSub === 'sticker'">
                <div class="cat-row">
                  <button v-for="c in patternCats" :key="c.value"
                          class="cat-chip"
                          :class="{ active: patternCategory === c.value }"
                          @click="patternCategory = c.value">
                    {{ c.label }}
                  </button>
                </div>
                <p class="section-desc">可选多个图案叠加到 <b>第 {{ activeFinger + 1 }} · {{ FINGER_NAMES[activeFinger] }}</b>：</p>
                <div class="pattern-grid">
                  <div v-for="m in filteredPatterns" :key="m.id"
                       class="pattern-card"
                       draggable="true" @dragstart="onDragStart($event, { kind: 'pattern', id: m.id })"
                       role="button"
                       tabindex="0"
                       :aria-pressed="fingers[activeFinger].patternIds.includes(m.id)"
                       :aria-label="`图案 ${m.name}`"
                       @keydown.enter.prevent="togglePattern(m.id)"
                       @keydown.space.prevent="togglePattern(m.id)"
                       :class="{ active: fingers[activeFinger].patternIds.includes(m.id) }"
                       @click="togglePattern(m.id)">
                    <div class="pattern-img">
                      <img v-if="m.imageUrl" :src="m.imageUrl" :alt="m.name" />
                      <span v-else class="pi-ph">{{ m.name.slice(0,2) }}</span>
                    </div>
                    <span class="pattern-name">{{ m.name }}</span>
                    <span v-if="m.category" class="pattern-cat">{{ categoryLabel(m.category) }}</span>
                  </div>
                  <empty-box v-if="!filteredPatterns.length" text="该分类暂无图案素材" />
                </div>
              </template>

              <!-- ── 法式甲 ── -->
              <template v-else-if="patternSub === 'french'">
                <div class="fx-block" :class="{ off: !fingers[activeFinger].french.enabled }">
                  <div class="fx-head">
                    <div>
                      <span class="fx-title">法式甲</span>
                      <span class="fx-sub">只覆盖指尖最下端，笑线中央向甲根方向上扬</span>
                    </div>
                    <button class="switch" :class="{ on: fingers[activeFinger].french.enabled }" @click="toggleFrench">
                      <i></i>
                    </button>
                  </div>
                  <template v-if="fingers[activeFinger].french.enabled">
                    <div class="fx-body">
                      <div class="slider-row">
                        <span class="fx-label">法式颜色</span>
                        <div class="french-colors">
                          <span v-for="c in FRENCH_COLORS" :key="c"
                                class="gc-dot lg" :style="{ background: c }"
                                :class="{ active: fingers[activeFinger].french.color === c }"
                                @click="fingers[activeFinger].french.color = c; pushHistory()"></span>
                        </div>
                      </div>
                      <div class="slider-row">
                        <span class="fx-label">法式高度 <b>{{ fingers[activeFinger].french.height }}</b></span>
                        <input type="range" min="10" max="40" v-model.number="fingers[activeFinger].french.height" @change="pushHistory" />
                      </div>
                      <div class="slider-row">
                        <span class="fx-label">法式弧度 <b>{{ fingers[activeFinger].french.curvature }}</b></span>
                        <input type="range" min="0" max="100" v-model.number="fingers[activeFinger].french.curvature" @change="pushHistory" />
                      </div>
                    </div>
                  </template>
                </div>
              </template>

              <!-- ── 波点 / 豹纹 ── -->
              <template v-else>
                <div class="fx-block" :class="{ off: !fingers[activeFinger].dots.enabled }">
                  <div class="fx-head">
                    <div>
                      <span class="fx-title">波点 / 豹纹</span>
                      <span class="fx-sub">智能配色会随底胶明暗自动变化</span>
                    </div>
                    <button class="switch" :class="{ on: fingers[activeFinger].dots.enabled }" @click="toggleDots">
                      <i></i>
                    </button>
                  </div>
                  <template v-if="fingers[activeFinger].dots.enabled">
                    <div class="fx-body">
                      <div class="kind-pick">
                        <button class="kind-btn" :class="{ active: fingers[activeFinger].dots.kind === 'dots' }"
                                @click="fingers[activeFinger].dots.kind = 'dots'; pushHistory()">● 波点</button>
                        <button class="kind-btn" :class="{ active: fingers[activeFinger].dots.kind === 'leopard' }"
                                @click="fingers[activeFinger].dots.kind = 'leopard'; pushHistory()">◎ 豹纹</button>
                      </div>
                      <div class="slider-row">
                        <span class="fx-label">密度 <b>{{ fingers[activeFinger].dots.density }}</b></span>
                        <input type="range" min="3" max="15" v-model.number="fingers[activeFinger].dots.density" @change="pushHistory" />
                      </div>
                      <div class="slider-row">
                        <span class="fx-label">大小 <b>{{ fingers[activeFinger].dots.size }}</b></span>
                        <input type="range" min="1" max="5" step="0.5" v-model.number="fingers[activeFinger].dots.size" @change="pushHistory" />
                      </div>
                      <div class="slider-row">
                        <span class="fx-label">自定义颜色</span>
                        <div class="french-colors">
                          <span class="gc-dot lg auto" :class="{ active: !fingers[activeFinger].dots.color }"
                                @click="fingers[activeFinger].dots.color = ''; pushHistory()" title="智能配色">✦</span>
                          <span v-for="c in ['#FFFFFF', '#1F1B1A', '#E53E8C', '#C8813C', '#8A6748']" :key="c"
                                class="gc-dot lg" :style="{ background: c }"
                                :class="{ active: fingers[activeFinger].dots.color === c }"
                                @click="fingers[activeFinger].dots.color = c; pushHistory()"></span>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </template>
            </template>

            <!-- [暂时下线-钻饰] 恢复时：取消本段注释 + 恢复 TABS 中的 ornament 项 + 恢复 NailArt 内 ⑥ 钻饰渲染块
            <template v-if="tab === 'ornament'">
              <p class="section-desc">点击钻饰添加到 <b>第 {{ activeFinger + 1 }} · {{ FINGER_NAMES[activeFinger] }}</b>，在预览区拖动调整位置：</p>
              <div class="cat-row">
                <button v-for="c in DECO_CATS" :key="c.value" class="cat-chip"
                        :class="{ active: decoCat === c.value }"
                        @click="decoCat = c.value">{{ c.label }}</button>
              </div>
              <div class="deco-tray">
                <button v-for="t in filteredDecals" :key="t.type"
                        class="deco-tray-item"
                        draggable="true" @dragstart="onDragStart($event, { kind: 'deco', type: t.type })"
                        @click="addDecoration(t.type)">
                  <svg width="34" height="34" viewBox="-8 -8 16 16">
                    <path v-for="(el, i) in gemPreviewEls(t.type)" :key="'tp' + i" v-bind="el" />
                  </svg>
                  <span>{{ t.name }}</span>
                </button>
              </div>

              <template v-if="fingers[activeFinger].decorations.length">
                <p class="section-desc deco-count">已放置 <b>{{ fingers[activeFinger].decorations.length }}</b> 件钻饰：</p>
                <div class="deco-list">
                  <div v-for="(d, k) in fingers[activeFinger].decorations" :key="d.id" class="deco-item-row">
                    <div class="deco-item-head">
                      <svg width="20" height="20" viewBox="-8 -8 16 16">
                        <path v-for="(el, i) in gemPreviewEls(d.type)" :key="'li' + i" v-bind="el" />
                      </svg>
                      <span class="deco-name">{{ (DECAL_TYPES.find(t => t.type === d.type) || {}).name || d.type }}</span>
                      <button class="deco-del" @click="removeDecoration(k)" title="删除">×</button>
                    </div>
                    <div class="slider-row">
                      <span class="fx-label">大小 <b>{{ d.size }}</b></span>
                      <input type="range" min="6" max="26" step="0.5" v-model.number="d.size" @change="pushHistory" />
                    </div>
                    <div class="slider-row">
                      <span class="fx-label">角度 <b>{{ d.angle }}°</b></span>
                      <input type="range" min="-180" max="180" v-model.number="d.angle" @change="pushHistory" />
                    </div>
                  </div>
                </div>
              </template>
              <p v-else class="deco-empty-tip">尚未放置钻饰，点击上方材料盘添加</p>
            </template>
            [暂时下线-钻饰] -->
          </div>
        </div>
      </div>
    </div>

    <!-- 保存完成浮层：完成即转化（继续编辑 / 加入购物车 / 立即结算） -->
    <div v-if="showSaved" class="saved-veil" @click.self="showSaved = false">
      <div class="saved-card">
        <div class="saved-badge">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
        </div>
        <h3 class="saved-title">设计已保存</h3>
        <img v-if="lastLocalPreview" :src="lastLocalPreview" class="saved-thumb" alt="设计预览" />
        <p class="saved-name">{{ designName }}</p>
        <p class="saved-sub">￥39.00 / 副 · 手工定制 · 可在"我的设计"中随时修改</p>
        <div class="saved-actions">
          <button class="sv-btn sv-ghost" @click="showSaved = false">继续编辑</button>
          <button class="sv-btn sv-line" :disabled="addingCart" @click="addSavedToCart(false)">
            {{ addingCart ? '加入中…' : '加入购物车' }}
          </button>
          <button class="sv-btn sv-primary" :disabled="addingCart" @click="addSavedToCart(true)">
            {{ addingCart ? '处理中…' : '立即结算' }}
          </button>
        </div>
        <router-link class="saved-link" to="/my-designs">查看我的设计 ›</router-link>
      </div>
    </div>

  </div>
</template>

<script setup>
import { h, computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import html2canvas from 'html2canvas'
import request from '../utils/request'
import NailArt from '../components/NailArt.vue'
import HandModel from '../components/HandModel.vue'
import { hexToHsl, hslToHex } from '../utils/color'
import { decoPreview } from '../engine/decoGems'

// 简单的空状态组件（避免引入 el-empty 造成样式突兀）
const emptyBox = {
  props: ['text'],
  setup(props) {
    return () => h('div', { class: 'empty-box' }, [
      h('svg', { width: 36, height: 36, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.2 }, [
        h('circle', { cx: 12, cy: 12, r: 10 }),
        h('line', { x1: 12, y1: 8, x2: 12, y2: 12 }),
        h('line', { x1: 12, y1: 16, x2: 12.01, y2: 16 })
      ]),
      h('span', props.text)
    ])
  }
}

const route = useRoute()
const router = useRouter()

const TABS = [
  { key: 'shape',   zh: '甲型', en: 'SHAPE',   icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3c-3.5 0-6 2.5-6 6 0 5-1 7-1 9 0 2 2 3 7 3s7-1 7-3c0-2-1-4-1-9 0-3.5-2.5-6-6-6Z"/></svg>` },
  { key: 'color',   zh: '底胶', en: 'POLISH', icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2 6 5v6c0 5 3 8 6 10 3-2 6-5 6-10V5l-6-3Z"/></svg>` },
  { key: 'pattern', zh: '图案', en: 'DECOR', icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2 2 12l10 10 10-10L12 2Z"/><path d="m2 12 20 0"/><path d="M12 2v20"/></svg>` },
  // [暂时下线-钻饰] 恢复时取消下行注释即可
  // { key: 'ornament', zh: '钻饰', en: 'JEWEL', icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3h12l4 6-10 12L2 9l4-6Z"/><path d="M2 9h20"/><path d="m12 21-4-12 4-6 4 6-4 12"/></svg>` }
]
const FINGER_NAMES       = ['小指', '无名指', '中指', '食指', '大拇指']
const ZOOMED_NAIL_W = 210
const DECAL_TYPES = [
  // ROUND 圆钻（色变体）
  { type: 'round',           name: '水晶圆钻', cat: 'ROUND' },
  { type: 'round-pink',      name: '粉钻',     cat: 'ROUND' },
  { type: 'round-champagne', name: '香槟钻',   cat: 'ROUND' },
  { type: 'round-ab',        name: 'AB 彩钻',  cat: 'ROUND' },
  { type: 'round-smoke',     name: '烟灰钻',   cat: 'ROUND' },
  // FANCY 异形钻
  { type: 'drop',     name: '水滴钻', cat: 'FANCY' },
  { type: 'marquise', name: '马眼钻', cat: 'FANCY' },
  { type: 'oval',     name: '椭圆钻', cat: 'FANCY' },
  { type: 'heart',    name: '心形钻', cat: 'FANCY' },
  { type: 'star',     name: '星形钻', cat: 'FANCY' },
  { type: 'hex',      name: '六边钻', cat: 'FANCY' },
  { type: 'triangle', name: '三角钻', cat: 'FANCY' },
  // PEARL 珍珠
  { type: 'pearl',      name: '白珍珠', cat: 'PEARL' },
  { type: 'pearl-pink', name: '粉珍珠', cat: 'PEARL' },
  { type: 'pearl-noir', name: '黑珍珠', cat: 'PEARL' },
  // METAL 金属
  { type: 'bar',       name: '金属条',   cat: 'METAL' },
  { type: 'ring-gold', name: '金属圆环', cat: 'METAL' }
]
// 钻饰预览（与 NailArt 内渲染同引擎，见 engine/decoGems.js）
const DECO_CATS = [
  { value: 'ALL', label: '全部' },
  { value: 'ROUND', label: '圆钻' },
  { value: 'FANCY', label: '异形钻' },
  { value: 'PEARL', label: '珍珠' },
  { value: 'METAL', label: '金属' }
]
const decoCat = ref('ALL')
const filteredDecals = computed(() => decoCat.value === 'ALL'
  ? DECAL_TYPES
  : DECAL_TYPES.filter(t => t.cat === decoCat.value))
function gemPreviewEls(type) { return decoPreview(type) }
const FRENCH_COLORS = ['#FFFFFF', '#F6D7DE', '#F3E0C0', '#D9E4F2', '#1F1B1A', '#E53E8C']

// 单指完整配置（含所有编辑维度），旧数据缺字段时用此工厂补齐
function defaultFinger() {
  return {
    shapeId: null,
    length: 1,                        // 长度系数 0.6~1.6，1.0=标准（完整覆盖甲床+基础延伸），加长段顶端为 2.4× 甲床长
    colorId: null,
    overlays: [],                     // OVERLAY 层栈 [{ id, opacity }]，可多选叠加
    patternIds: [],
    gradient: { enabled: false, color2: '', position: 50 },
    catEye:   { enabled: false, angle: 25, width: 34, brightness: 72 },
    adjust:   { hue: 0, sat: 0, light: 0 },
    french:   { enabled: false, color: '#FFFFFF', height: 26, curvature: 72 },
    dots:     { enabled: false, kind: 'dots', density: 7, size: 2.5, color: '' },
    patternStyle: null,               // { code, params } 参数化款式（引擎渲染）
    decorations: [],
    patternPos: {}                  // { [图案id]: { x%, y% } } 拖放落点
  }
}

// -------- 素材 --------
const shapes = ref([])
const colors = ref([])
const patterns = ref([])
const stylePatterns = ref([])        // 参数化款式（PATTERN_STYLE）
const decors = ref([])               // 矢量装饰件（DECOR）
const overlays = ref([])             // 效果覆盖层（OVERLAY：珠光/闪耀/炫光/特殊）
const CATEGORY_LABELS = { BASIC: '基础', FRENCH: '法式', GRADIENT: '渐变', NATURE: '自然', GEO: '几何', SHINE: '闪钻', CUTE: '可爱' }
function categoryLabel(v) { return CATEGORY_LABELS[v] || v }
// 图案分类 chips 按实际素材动态生成，避免出现永远为空的死筛选
const patternCats = computed(() => {
  const present = [...new Set(patterns.value.map(m => m.category).filter(Boolean))]
  const order = Object.keys(CATEGORY_LABELS)
  present.sort((a, b) => order.indexOf(a) - order.indexOf(b))
  return [{ value: 'ALL', label: '全部' }, ...present.map(v => ({ value: v, label: categoryLabel(v) }))]
})
// 素材刷新后当前分类已不存在则回到全部
watch(patternCats, (cats) => {
  if (!cats.some(c => c.value === patternCategory.value)) patternCategory.value = 'ALL'
})

async function loadMaterials() {
  try {
    const [s, c, p, sp, d, ov] = await Promise.all([
      request.get('/materials', { params: { type: 'SHAPE' } }),
      request.get('/materials', { params: { type: 'COLOR' } }),
      request.get('/materials', { params: { type: 'PATTERN_IMG' } }),
      request.get('/materials', { params: { type: 'PATTERN_STYLE' } }),
      request.get('/materials', { params: { type: 'DECOR' } }),
      request.get('/materials', { params: { type: 'OVERLAY' } })
    ])
    shapes.value = s.data || []
    colors.value = c.data || []
    patterns.value = p.data || []
    stylePatterns.value = sp.data || []
    decors.value = d.data || []
    overlays.value = ov.data || []
  } catch (e) { /* */ }
}

// -------- 五指状态 --------
const fingers = ref(Array.from({ length: 5 }, () => defaultFinger()))
const activeFinger = ref(0)
const tab = ref('shape')
const patternSub = ref('sticker')   // 图案Tab子面板：贴花/法式/波点豹纹
const patternCategory = ref('ALL')
// 色卡色系筛选（借鉴 Luna 分类交互，复用 material.category 字段）
const colorCategory = ref('ALL')
const COLOR_CATS = [
  { value: 'ALL',     label: '全部' },
  { value: 'NUDE',    label: '裸粉' },
  { value: 'PINK',    label: '粉' },
  { value: 'RED',     label: '红橙' },
  { value: 'YELLOW',  label: '黄' },
  { value: 'GREEN',   label: '绿' },
  { value: 'TEAL',    label: '蓝绿' },
  { value: 'BLUE',    label: '蓝' },
  { value: 'PURPLE',  label: '紫' },
  { value: 'SILVER',  label: '银调' },
  { value: 'DARK',    label: '黑' },
  { value: 'GLITTER', label: '碎钻' }
]
const filteredColors = computed(() => colorCategory.value === 'ALL'
  ? colors.value
  : colors.value.filter(m => (m.category || '') === colorCategory.value))
const gradientHex2 = ref('')        // 渐变第二底色 HEX 输入
const zoom = ref(100)
const designName = ref('')
const hexInput = ref('')
const editingId = ref(null)
const saving = ref(false)
const widthByIndex = [48, 52, 56, 52, 48]
const dragOverIdx = ref(-1)         // 拖拽悬停的甲片高亮
const singleOver = ref(false)      // 单甲精修模式的拖拽悬停高亮

// -------- 拖拽投放：把右侧素材"推"到左侧指甲上 --------
let dragPayload = null
function onDragStart(e, payload) {
  dragPayload = payload
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('text/plain', payload.kind)  // Firefox 需要setData才会触发拖拽
}
function clearDrag() { dragPayload = null }
// 落点：相对甲面（.nail-unit）的百分比坐标，实现"拖到哪落在哪"
function dropPosFrom(el, ev) {
  const unit = el.querySelector('.nail-unit') || el
  const r = unit.getBoundingClientRect()
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v))
  return {
    x: clamp(((ev.clientX - r.left) / r.width) * 100, 6, 94),
    y: clamp(((ev.clientY - r.top) / r.height) * 100, 6, 94)
  }
}
function onNailDropAt(index, ev) {
  const p = dragPayload
  clearDrag()
  if (!p) return
  activeFinger.value = index
  const pos = dropPosFrom(ev.currentTarget, ev)
  if (p.kind === 'shape') {
    selectShape(p.id)
  } else if (p.kind === 'color') {
    selectColor(p.id, p.code)
  } else if (p.kind === 'pattern') {
    if (!active.value.patternIds.includes(p.id)) active.value.patternIds.push(p.id)
    active.value.patternPos[p.id] = { x: pos.x, y: pos.y }
    pushHistory()
  } else if (p.kind === 'deco') {
    addDecoration(p.type, pos)
  } else if (p.kind === 'style') {
    applyStyle(p.code)
  }
}
// 手模视图投放：槽位结构不同，复用同一套落点/投放逻辑
function onHandDrop(index, ev) {
  onNailDropAt(index, ev)
}

// -------- 拖动选色：色相 / 明度横向滑条（拖动圆点直接选色） --------
const SLIDER_SAT = 62                            // 滑条取色固定饱和度
const sliderHue = ref(20)                        // 0-360
const sliderLight = ref(62)                      // 0-100
const sliderColor = computed(() => hslToHex(sliderHue.value, SLIDER_SAT, sliderLight.value))
const lightBarStyle = computed(() => ({
  background: `linear-gradient(to right, #000000, ${hslToHex(sliderHue.value, SLIDER_SAT, 50)}, #FFFFFF)`
}))
let barDragging = false
function applySliderColor() {
  active.value.colorId = null
  hexInput.value = sliderColor.value
}
function onBarDown(e, kind) {
  barDragging = true
  const bar = e.currentTarget
  const apply = (ev) => {
    const rect = bar.getBoundingClientRect()
    const pct = Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width))
    if (kind === 'hue') sliderHue.value = pct * 360
    else sliderLight.value = pct * 100
    applySliderColor()
  }
  apply(e)
  const move = (ev) => apply(ev)
  const up = () => {
    barDragging = false
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
    pushHistory()
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}
// 点色卡 / 输 HEX / 撤销重做时，把滑条位置同步到当前颜色
watch(() => hexInput.value, (v) => {
  if (barDragging) return
  const { h, l } = hexToHsl(v || '#F5F0E6')
  sliderHue.value = h
  sliderLight.value = Math.round(l)
})
// 键盘微调滑条（←→/↑↓），保证无鼠标也能取色
function nudgeBar(kind, delta) {
  if (kind === 'hue') {
    sliderHue.value = Math.max(0, Math.min(360, sliderHue.value + delta))
  } else {
    sliderLight.value = Math.max(0, Math.min(100, sliderLight.value + delta))
  }
  applySliderColor()
  pushHistory()
}

// -------- 预览模式：手模 / 甲片平铺 / 单甲（就地切换，非弹窗） --------
const viewMode = ref('handModel')            // 'handModel' 手模 | 'hand' 甲片平铺 | 'single' 单甲精修
function openZoom(i) { activeFinger.value = i; viewMode.value = 'single' }
function onEscOrArrow(e) {
  if (viewMode.value !== 'single') return
  if (e.key === 'Escape') viewMode.value = 'handModel'
  else if (e.key === 'ArrowLeft' && activeFinger.value > 0) activeFinger.value--
  else if (e.key === 'ArrowRight' && activeFinger.value < 4) activeFinger.value++
}
onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onEscOrArrow)
  window.removeEventListener('dragend', clearDrag)
})

function zoomIn()  { zoom.value = Math.min(160, zoom.value + 10) }
function zoomOut() { zoom.value = Math.max(60, zoom.value - 10) }
function onZoomInput(e) { zoom.value = Number(e.target.value) }

const active = computed(() => fingers.value[activeFinger.value])

function shapeById(id) { return shapes.value.find(m => m.id === id) || null }
function colorCodeOf(f) {
  const c = colors.value.find(m => m.id === f.colorId)
  return c ? c.colorCode : (hexInput.value || '#F5F0E6')
}
function patternsOf(f) { return f.patternIds.map(id => patterns.value.find(m => m.id === id)).filter(Boolean) }
const filteredPatterns = computed(() => patternCategory.value === 'ALL'
  ? patterns.value
  : patterns.value.filter(m => m.category === patternCategory.value))

function selectShape(id) { if (active.value.shapeId !== id) { active.value.shapeId = id; pushHistory() } }
// -------- 效果层（OVERLAY 覆盖层栈）：多选叠加 / opacity / 删除 --------
const OVERLAY_CATS = [
  { value: 'ALL', label: '全部' },
  { value: 'GLOSS', label: '光泽' },
  { value: 'PEARL', label: '珠光' },
  { value: 'SHIMMER', label: '闪耀' },
  { value: 'EFFECT', label: '炫光' },
  { value: 'SPECIAL', label: '特殊' }
]
const overlaySubtype = ref('ALL')
const filteredOverlays = computed(() => overlaySubtype.value === 'ALL'
  ? overlays.value
  : overlays.value.filter(m => m.category === overlaySubtype.value))
const fingerOverlays = computed(() => active.value?.overlays || [])
function overlayName(id) { return overlays.value.find(m => m.id === id)?.name || '效果层' }
function toggleOverlay(id) {
  const arr = active.value.overlays
  const i = arr.findIndex(o => o.id === id)
  if (i >= 0) arr.splice(i, 1)
  else arr.push({ id, opacity: 1 })
  pushHistory()
}
function setOverlayOpacity(id, v) {
  const o = active.value.overlays.find(x => x.id === id)
  if (o) o.opacity = v
}
function removeOverlay(id) {
  const arr = active.value.overlays
  const i = arr.findIndex(o => o.id === id)
  if (i >= 0) { arr.splice(i, 1); pushHistory() }
}
// 长度档位文案（穿戴甲逻辑：贴合/标准/加长/极长——标准即完整覆盖甲床+基础延伸，默认 1.0 = 标准）
const LENGTH_LABELS = [[0.85, '贴合'], [1.15, '标准'], [1.45, '加长'], [9, '极长']]
const lengthLabel = computed(() => {
  const v = active.value?.length || 1
  return (LENGTH_LABELS.find(l => v < l[0]) || LENGTH_LABELS[3])[1]
})
function selectColor(id, code) { if (active.value.colorId !== id) { active.value.colorId = id; hexInput.value = code; pushHistory() } }
function applyHex() {
  const v = hexInput.value.trim()
  if (/^#[0-9a-fA-F]{6}$/.test(v)) { hexInput.value = v; active.value.colorId = null; pushHistory() }
  else hexInput.value = colorCodeOf(active.value) || '#F5F0E6'
}
// 点击添加（非拖拽）时的默认落点：错开摆放，避免多个图案/钻饰堆在同一处
const FREE_SLOTS = [
  { x: 35, y: 28 }, { x: 64, y: 50 }, { x: 30, y: 64 },
  { x: 68, y: 24 }, { x: 48, y: 78 }, { x: 22, y: 44 }
]
function togglePattern(id) {
  const arr = active.value.patternIds
  const i = arr.indexOf(id)
  if (i >= 0) {
    arr.splice(i, 1)
    delete active.value.patternPos[id]
  } else {
    arr.push(id)
    const slot = FREE_SLOTS[(arr.length - 1) % FREE_SLOTS.length]
    active.value.patternPos[id] = { x: slot.x, y: slot.y }
  }
  pushHistory()
}

// -------- 渐变 / 猫眼 / 色彩微调 / 法式 / 波点 / 钻饰 操作 --------
function toggleGradient() {
  active.value.gradient.enabled = !active.value.gradient.enabled
  if (active.value.gradient.enabled && !active.value.gradient.color2) {
    // 默认用比当前底色浅的白色
    active.value.gradient.color2 = '#FFFFFF'
  }
  pushHistory()
}
function pickGradient2(id, code) {
  active.value.gradient.color2 = code
  gradientHex2.value = code
  if (!active.value.gradient.enabled) active.value.gradient.enabled = true
  pushHistory()
}
function applyGradientHex() {
  const v = gradientHex2.value.trim()
  if (/^#[0-9a-fA-F]{6}$/.test(v)) {
    active.value.gradient.color2 = v.toUpperCase()
    if (!active.value.gradient.enabled) active.value.gradient.enabled = true
    pushHistory()
  } else {
    gradientHex2.value = active.value.gradient.color2
  }
}
function swapGradient() {
  const g = active.value.gradient
  const oldColor2 = g.color2 || '#FFFFFF'
  const base = colorCodeOf(active.value)
  // 交换：第二底色 ← 当前底色；底色 ← 原第二底色
  g.color2 = base
  const hit = colors.value.find(m => m.colorCode.toUpperCase() === oldColor2.toUpperCase())
  active.value.colorId = hit ? hit.id : null
  hexInput.value = oldColor2
  pushHistory()
}
function resetAdjust() {
  active.value.adjust = { hue: 0, sat: 0, light: 0 }
  pushHistory()
}
const adjustDirty = computed(() => {
  const a = active.value?.adjust
  return a && (a.hue !== 0 || a.sat !== 0 || a.light !== 0)
})
function toggleFrench() {
  active.value.french.enabled = !active.value.french.enabled
  pushHistory()
}
function toggleDots() {
  active.value.dots.enabled = !active.value.dots.enabled
  pushHistory()
}
function addDecoration(type, pos) {
  const id = Date.now() + Math.floor(Math.random() * 1000)
  const slot = pos || FREE_SLOTS[active.value.decorations.length % FREE_SLOTS.length]
  active.value.decorations.push({ id, type, x: slot.x, y: slot.y, size: 12, angle: 0 })
  pushHistory()
}
function removeDecoration(k) {
  active.value.decorations.splice(k, 1)
  pushHistory()
}
function onDecoMovedAt(fingerIdx, payload) {
  const f = fingers.value[fingerIdx]
  const d = f?.decorations[payload.index]
  if (d) { d.x = payload.x; d.y = payload.y }
}
// 贴花在甲面上拖动微调位置
function onPatMovedAt(fingerIdx, payload) {
  const f = fingers.value[fingerIdx]
  const id = f?.patternIds[payload.index]
  if (id != null) f.patternPos[id] = { x: payload.x, y: payload.y }
}
// -------- 参数化款式（引擎渲染，见 engine/patternStyles.js） --------
function parseStyleSchema(attrsStr) {
  try { return (JSON.parse(attrsStr || '{}').paramsSchema) || [] } catch (e) { return [] }
}
function styleDefaults(code) {
  const m = stylePatterns.value.find(x => x.code === code)
  const schema = parseStyleSchema(m?.attrs)
  const params = {}
  schema.forEach(s => { if (s.default !== undefined) params[s.key] = s.default })
  return params
}
const activeStyleSchema = computed(() => {
  const code = active.value?.patternStyle?.code
  const m = stylePatterns.value.find(x => x.code === code)
  return parseStyleSchema(m?.attrs)
})
const activeStyleName = computed(() => {
  const code = active.value?.patternStyle?.code
  return stylePatterns.value.find(x => x.code === code)?.name || ''
})
function applyStyle(code) {
  active.value.patternStyle = { code, params: styleDefaults(code) }
  pushHistory()
}
function toggleStyle(code) {
  if (active.value.patternStyle?.code === code) clearStyle()
  else applyStyle(code)
}
function updateStyleParam(key, val) {
  if (active.value.patternStyle) {
    active.value.patternStyle.params[key] = val
    pushHistory()
  }
}
function clearStyle() {
  active.value.patternStyle = null
  pushHistory()
}

const syncLabel = computed(() => '同步到全部手指')
function syncAll() {
  const src = JSON.parse(JSON.stringify(active.value))
  fingers.value.forEach(f => {
    f.shapeId = src.shapeId
    f.length = src.length
    f.overlays = JSON.parse(JSON.stringify(src.overlays || []))
    f.colorId = src.colorId
    f.patternIds = [...src.patternIds]
    f.gradient = JSON.parse(JSON.stringify(src.gradient))
    f.catEye   = JSON.parse(JSON.stringify(src.catEye))
    f.adjust   = JSON.parse(JSON.stringify(src.adjust))
    f.french   = JSON.parse(JSON.stringify(src.french))
    f.dots     = JSON.parse(JSON.stringify(src.dots))
    f.patternStyle = src.patternStyle ? JSON.parse(JSON.stringify(src.patternStyle)) : null
    f.decorations = src.decorations.map(d => ({ ...d, id: d.id + '-' + Math.random().toString(36).slice(2, 6) }))
    f.patternPos = JSON.parse(JSON.stringify(src.patternPos || {}))
  })
  pushHistory()
  ElMessage.success('已同步到全部五指')
}

// 撤销重做
const history = ref([])
const histIdx = ref(-1)
function snapshot() { return JSON.parse(JSON.stringify(fingers.value)) }
function pushHistory() {
  history.value = history.value.slice(0, histIdx.value + 1)
  history.value.push(snapshot())
  histIdx.value = history.value.length - 1
}
function applySnapshot(s) {
  fingers.value = JSON.parse(JSON.stringify(s))
  const c = colors.value.find(m => m.id === fingers.value[activeFinger.value]?.colorId)
  hexInput.value = c ? c.colorCode : ''
}
function undo() { if (histIdx.value > 0) { histIdx.value--; applySnapshot(history.value[histIdx.value]) } }
function redo() { if (histIdx.value < history.value.length - 1) { histIdx.value++; applySnapshot(history.value[histIdx.value]) } }
const canUndo = computed(() => histIdx.value > 0)
const canRedo = computed(() => histIdx.value < history.value.length - 1)

// 初始化
onMounted(async () => {
  window.addEventListener('keydown', onEscOrArrow)
  window.addEventListener('dragend', clearDrag)
  await loadMaterials()
  if (shapes.value.length) fingers.value.forEach(f => { f.shapeId = shapes.value[0].id })
  if (colors.value.length) { const first = colors.value[0]; fingers.value.forEach(f => { f.colorId = first.id }); hexInput.value = first.colorCode }

  const { templateId, designId } = route.query
  if (designId) {
    try {
      const res = await request.get(`/designs/${designId}`)
      applyParams(res.data.params)
      designName.value = res.data.name
      editingId.value = res.data.id
    } catch (e) { /* */ }
  } else if (templateId) {
    try {
      const res = await request.get(`/templates/${templateId}`)
      applyParams(res.data.designParams)
      designName.value = res.data.name ? `${res.data.name}（套用）` : ''
      ElMessage.success('已套用模板，可继续修改')
    } catch (e) { /* */ }
  }
  pushHistory()
})

function validId(list, id) { return list.some(m => m.id === id) }
// 旧设计数据缺新字段时用默认值补齐（深合并子对象）
function mergeFinger(target, fp) {
  const def = defaultFinger()
  target.shapeId = validId(shapes.value, fp.shapeId) ? fp.shapeId : target.shapeId
  target.length = Math.min(1.6, Math.max(0.6, Number(fp.length) || 1))
  if (validId(colors.value, fp.colorId)) target.colorId = fp.colorId
  // OVERLAY 层栈：只保留素材库中仍存在的层
  if (Array.isArray(fp.overlays)) {
    target.overlays = fp.overlays
      .filter(o => overlays.value.some(m => m.id === o.id))
      .map(o => ({ id: o.id, opacity: Math.max(0, Math.min(1, Number(o.opacity ?? 1))) }))
  }
  target.patternIds = (fp.patternIds || []).filter(id => validId(patterns.value, id))
  if (fp.gradient) target.gradient = { ...def.gradient, ...fp.gradient }
  if (fp.catEye)   target.catEye = { ...def.catEye, ...fp.catEye }
  if (fp.adjust)   target.adjust = { ...def.adjust, ...fp.adjust }
  if (fp.french)   target.french = { ...def.french, ...fp.french }
  if (fp.dots)     target.dots = { ...def.dots, ...fp.dots }
  if (fp.patternStyle) target.patternStyle = fp.patternStyle
  if (Array.isArray(fp.decorations)) target.decorations = fp.decorations
  // 贴花落点：只回填仍存在的图案 id（老数据无此字段则保持默认槽位）
  if (fp.patternPos && typeof fp.patternPos === 'object') {
    target.patternIds.forEach(id => { if (fp.patternPos[id]) target.patternPos[id] = fp.patternPos[id] })
  }
}
function applyParams(paramsStr) {
  try {
    const p = JSON.parse(paramsStr)
    if (Array.isArray(p.fingers)) {
      p.fingers.forEach((fp, i) => {
        if (i < 5) mergeFinger(fingers.value[i], fp || {})
      })
    } else {
      fingers.value.forEach(f => mergeFinger(f, p))
    }
    const c = colors.value.find(m => m.id === fingers.value[activeFinger.value].colorId)
    hexInput.value = c ? c.colorCode : ''
  } catch (e) { /* */ }
}

// 保存
const previewRef = ref(null)
async function capturePreview() {
  try {
    const canvas = await html2canvas(previewRef.value, { backgroundColor: '#ffffff', scale: 1 })
    // 同一次渲染顺便留下本地缩略图，供"已保存"完成浮层即时展示（不用等上传回链）
    lastLocalPreview.value = canvas.toDataURL('image/jpeg', 0.72)
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
    if (!blob) return ''
    const fd = new FormData()
    fd.append('file', blob, 'preview.png')
    const res = await request.post('/designs/upload/preview', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    return res.data
  } catch (e) { return '' }
}

// ---- 保存完成浮层：完成即转化，就地给出 继续编辑 / 加入购物车 / 立即结算 ----
const showSaved = ref(false)
const lastLocalPreview = ref('')
const addingCart = ref(false)
function addSavedToCart(goCheckout) {
  if (addingCart.value) return
  addingCart.value = true
  request.post('/cart', { designId: editingId.value, quantity: 1 })
    .then(() => {
      if (goCheckout) {
        router.push('/checkout')
      } else {
        ElMessage.success('已加入购物车，可在右上角购物车查看')
        showSaved.value = false
      }
    })
    .finally(() => { addingCart.value = false })
}

async function save() {
  if (!designName.value.trim()) return ElMessage.warning('请填写设计名称')
  if (!fingers.value.some(f => f.shapeId && f.colorId)) return ElMessage.warning('请为手指选择甲型和色卡')
  saving.value = true
  try {
    const body = {
      name: designName.value.trim(),
      params: JSON.stringify({ fingers: fingers.value, remark: '' }),
      previewUrl: ''
    }
    // 先入库再传预览图：避免后端校验不过时已上传的截图变成无法回收的孤儿文件
    let id = editingId.value
    if (id) {
      await request.put(`/designs/${id}`, body)
    } else {
      const res = await request.post('/designs', body)
      id = res.data
      editingId.value = id
    }
    const previewUrl = await capturePreview()
    if (previewUrl) {
      await request.put(`/designs/${id}`, { ...body, previewUrl })
    }
    showSaved.value = true
  } catch (e) {
    // request.js 已统一弹错误提示，此处吸掉避免未捕获异常
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
/* ============== 页面容器 ============== */
.designer-page { min-height: calc(100vh - 72px); padding: var(--space-xl) 0 var(--space-3xl); }

/* 页头 */
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: var(--space-xl);
}
.head-left { display: flex; flex-direction: column; gap: 12px; }
.page-title {
  font-size: 38px;
  letter-spacing: -0.5px;
  margin: 0;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.icon-btn {
  width: 38px; height: 38px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-foreground);
  display: grid; place-items: center;
  cursor: pointer;
  transition: all .15s;
}
.icon-btn:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary-light-5);
}
.icon-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.btn-ghost-sm {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 16px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-foreground);
  cursor: pointer;
  transition: all .15s;
}
.btn-ghost-sm:hover {
  border-color: var(--color-primary-light-5);
  color: var(--color-primary);
}
.btn-primary-sm {
  padding: 9px 20px;
  background: var(--color-solid);
  color: var(--color-on-solid);
  border: none;
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all .15s;
  box-shadow: 0 4px 14px var(--color-solid-shadow);
}
.btn-primary-sm:hover:not(:disabled) { background: var(--color-solid-hover); transform: translateY(-1px); }
.btn-primary-sm:disabled { opacity: 0.65; cursor: not-allowed; }

/* ============== Studio 主体 ============== */
.studio {
  display: grid;
  grid-template-columns: minmax(380px, 420px) 1fr;
  gap: var(--space-xl);
  align-items: flex-start;
}

/* ============== 左侧预览 ============== */
.preview-wrap {
  position: sticky;
  top: calc(72px + 24px);
  background: #fff;
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}
.stage-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.stage-label {
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--color-muted-foreground);
  text-transform: uppercase;
}
.active-finger {
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  background: var(--color-primary-light-9);
  color: var(--color-primary);
  border-radius: var(--radius-full);
}

.preview-stage {
  background: linear-gradient(180deg, var(--color-canvas) 0%, var(--color-canvas-deep) 100%);
  border-radius: var(--radius-sm);
  padding: 24px 12px;
  min-height: 300px;
  display: grid; place-items: center;
  position: relative;
  overflow: hidden;
}

.nails {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 14px;
  transform-origin: center;
  transition: transform .2s ease;
}
.nail-slot {
  position: relative;
  cursor: pointer;
  padding: 6px;
  border-radius: 12px;
  transition: background .15s ease, box-shadow .15s ease;
}
.nail-slot:hover { background: rgba(255,255,255,0.35); }
.nail-slot.active { background: rgba(255,255,255,0.55); }
/* 拖拽悬停高亮：可投放到该甲片 */
.nail-slot.over { box-shadow: 0 0 0 3px rgba(229, 62, 140, 0.55); }


/* 工具条 */
.stage-toolbar {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.tools-left, .tools-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.zoom-pill-mini {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--color-muted);
  padding: 4px 6px;
  border-radius: var(--radius-full);
}
.zbtn {
  width: 28px; height: 28px;
  border: none; background: transparent;
  border-radius: 50%;
  display: grid; place-items: center;
  cursor: pointer;
  color: var(--color-foreground);
  transition: background .15s;
}
.zbtn:hover { background: #fff; }
.zoom-pill-mini input[type="range"] {
  -webkit-appearance: none; appearance: none;
  width: 90px; height: 3px;
  background: var(--color-border);
  border-radius: 3px;
  outline: none;
}
.zoom-pill-mini input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px; height: 14px;
  background: var(--color-foreground);
  border-radius: 50%;
  cursor: pointer;
}
.text-input {
  padding: 8px 14px;
  width: 180px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-family: var(--font-ui);
  font-size: 13px;
  outline: none;
  transition: border-color .15s;
  background: #fff;
}
.text-input:focus { border-color: var(--color-primary); }

.tip {
  margin-top: 14px;
  padding: 10px 14px;
  background: var(--color-muted);
  border-radius: var(--radius-sm);
  color: var(--color-muted-foreground);
  font-size: 12.5px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tip b { color: var(--color-foreground); font-weight: 700; }

/* ============== 右侧素材面板 ============== */
.material-panel {
  background: #fff;
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}
/* Tabs 行 */
.tabs-row {
  display: flex;
  gap: 6px;
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--space-lg);
}
.tab-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 10px;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
  position: relative;
}
.tab-btn:hover { color: var(--color-foreground); background: var(--color-muted); }
.tab-btn.active {
  background: var(--color-primary-light-9);
  color: var(--color-primary);
  font-weight: 700;
}
.tab-label {
  font-family: var(--font-ui);
  font-size: 12px;
  letter-spacing: 0.3px;
  line-height: 1;
}

/* Tab 通用 */
.section-desc {
  font-family: var(--font-ui);
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin: 0 0 12px;
}
.section-desc b { color: var(--color-primary); }

/* 甲型卡片 */
.shape-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.shape-card {
  padding: 14px 10px;
  background: var(--color-muted);
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  text-align: center;
  cursor: pointer;
  transition: all .2s;
}
.shape-card:hover { border-color: var(--color-primary-light-5); }
.shape-card.active {
  background: var(--color-primary-light-9);
  border-color: var(--color-primary);
}
.shape-preview {
  display: grid; place-items: center;
  padding: 4px;
  margin-bottom: 8px;
}
.shape-name {
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-foreground);
}
.shape-card.active .shape-name { color: var(--color-primary); }

/* 颜色面板 */
.color-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
}
.color-meta-row .section-desc { margin: 0; }
.hex-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.hex-input {
  width: 108px;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-family: var(--font-ui);
  font-size: 13px;
  outline: none;
  transition: border-color .15s;
}
.hex-input:focus { border-color: var(--color-primary); }
.hex-swatch {
  width: 32px; height: 32px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  box-shadow: inset 0 0 0 2px #fff;
}

/* 拖动选色滑条 */
.slider-colors {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}
.hue-bar, .light-bar {
  position: relative;
  height: 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  cursor: pointer;
  touch-action: none;
}
.hue-bar {
  background: linear-gradient(to right, #FF0000, #FFFF00, #00FF00, #00FFFF, #0000FF, #FF00FF, #FF0000);
}
.bar-thumb {
  position: absolute;
  top: 50%;
  width: 20px; height: 20px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid rgba(255,255,255,0.9);
  box-shadow: 0 1px 4px rgba(0,0,0,0.35), inset 0 0 0 1px rgba(0,0,0,0.15);
  transform: translate(-50%, -50%);
  pointer-events: none;
}
/* 键盘操作时的可见焦点（色卡/甲型/图案/甲片/滑条） */
.nail-slot:focus-visible,
.shape-card:focus-visible,
.color-cell:focus-visible,
.pattern-card:focus-visible,
.hue-bar:focus-visible,
.light-bar:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
.color-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(52px, 1fr));
  gap: 10px;
  max-height: 296px;
  overflow-y: auto;
  padding-right: 4px;
}
.color-cell {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all .15s;
  position: relative;
}
.color-cell:hover { transform: scale(1.06); }
.color-cell.active {
  transform: scale(1.08);
  box-shadow: 0 0 0 3px #fff, 0 0 0 5px var(--color-primary);
}
.color-cell .check {
  position: absolute;
  inset: 0;
  display: grid; place-items: center;
  opacity: 0;
  color: #fff;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.4));
}
.color-cell.active .check { opacity: 1; }

/* 图案面板 */
.cat-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.cat-chip {
  padding: 5px 14px;
  border: 1px solid var(--color-border);
  background: transparent;
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
}
.cat-chip:hover { color: var(--color-primary); border-color: var(--color-primary-light-5); }
.cat-chip.active {
  background: var(--color-foreground);
  color: #fff;
  border-color: var(--color-foreground);
}
.pattern-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}
.pattern-card {
  padding: 10px;
  background: var(--color-muted);
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all .2s;
  text-align: center;
}
.pattern-card:hover { border-color: var(--color-primary-light-5); }
.pattern-card.active {
  background: var(--color-primary-light-9);
  border-color: var(--color-primary);
}
.pattern-img {
  width: 56px; height: 56px;
  margin: 0 auto 8px;
  border-radius: 10px;
  background: #fff;
  display: grid; place-items: center;
  overflow: hidden;
}
.pattern-img img { width: 100%; height: 100%; object-fit: contain; padding: 6px; }
.pi-ph { color: var(--color-muted-foreground); font-family: var(--font-heading); font-size: 18px; }
.pattern-name {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-foreground);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pattern-cat {
  display: inline-block;
  font-size: 10px;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  background: #fff;
  color: var(--color-muted-foreground);
  font-family: var(--font-ui);
  letter-spacing: 0.3px;
}
.pattern-card.active .pattern-name { color: var(--color-primary); }

/* ========== 图案子面板切换 ========== */
.subtabs {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
  background: var(--color-muted);
  padding: 4px;
  border-radius: var(--radius-full);
}
.subtabs button {
  flex: 1;
  padding: 8px 10px;
  border: none;
  background: transparent;
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
}
.subtabs button.active {
  background: #fff;
  color: var(--color-foreground);
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

/* ========== 特效区块（渐变/猫眼/微调/法式/波点） ========== */
.fx-block {
  margin-top: 18px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  transition: border-color .15s, opacity .15s;
}
.fx-block.off { opacity: 0.62; }
.fx-block:not(.off) { border-color: var(--color-primary-light-5); }
.fx-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.fx-title {
  display: block;
  font-family: var(--font-ui);
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-foreground);
}
.fx-sub {
  display: block;
  font-size: 11.5px;
  color: var(--color-muted-foreground);
  margin-top: 3px;
}
.fx-body { margin-top: 14px; display: flex; flex-direction: column; gap: 12px; }

/* 开关 */
.switch {
  width: 42px; height: 24px;
  border-radius: var(--radius-full);
  border: none;
  background: var(--color-border);
  position: relative;
  cursor: pointer;
  transition: background .18s;
  flex-shrink: 0;
}
.switch i {
  position: absolute;
  top: 3px; left: 3px;
  width: 18px; height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  transition: transform .18s;
}
.switch.on { background: var(--color-primary); }
.switch.on i { transform: translateX(18px); }

.btn-reset {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  background: #fff;
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
  flex-shrink: 0;
}
.btn-reset:hover { color: var(--color-primary); border-color: var(--color-primary-light-5); }

/* 滑杆行 */
.slider-row { display: flex; flex-direction: column; gap: 6px; }
.fx-label {
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-foreground);
  display: flex;
  justify-content: space-between;
}
.fx-label b { color: var(--color-primary); font-weight: 700; }
.slider-row input[type="range"] {
  -webkit-appearance: none; appearance: none;
  width: 100%; height: 3px;
  background: var(--color-border);
  border-radius: 3px;
  outline: none;
  cursor: pointer;
}
.slider-row input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 15px; height: 15px;
  background: var(--color-foreground);
  border-radius: 50%;
  cursor: pointer;
  border: 2.5px solid #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.25);
}

/* 渐变预览与第二底色 */
.grad-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px;
  background: var(--color-muted);
  border-radius: var(--radius-sm);
}
.gp-c1, .gp-c2 {
  width: 64px; height: 30px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  box-shadow: inset 0 0 0 2px #fff;
}
.gp-arrow {
  width: 26px; height: 26px;
  display: grid; place-items: center;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  cursor: pointer;
  color: var(--color-muted-foreground);
  font-size: 13px;
  transition: all .15s;
}
.gp-arrow:hover { color: var(--color-primary); border-color: var(--color-primary-light-5); }
.grad-colors { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.gc-dot {
  width: 22px; height: 22px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: transform .12s;
  box-shadow: inset 0 0 0 1.5px #fff;
}
.gc-dot:hover { transform: scale(1.12); }
.gc-dot.active { box-shadow: 0 0 0 2px #fff, 0 0 0 4px var(--color-primary); }
.gc-dot.lg { width: 26px; height: 26px; }
.gc-dot.auto {
  background: #fff;
  display: grid; place-items: center;
  font-size: 11px;
  color: var(--color-primary);
}
.hex-input.sm { width: 92px; padding: 6px 10px; font-size: 12px; }

/* 波点类型 */
.kind-pick { display: flex; gap: 8px; }
.kind-btn {
  flex: 1;
  padding: 9px 10px;
  border: 1.5px solid var(--color-border);
  background: #fff;
  border-radius: var(--radius-sm);
  font-family: var(--font-ui);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
}
.kind-btn.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light-9);
}
.french-colors { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* ========== 钻饰面板 ========== */
.deco-tray {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
}
.deco-tray-item {
  padding: 14px 8px 10px;
  background: var(--color-muted);
  border: 1.5px solid transparent;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all .15s;
  font-family: var(--font-ui);
}
.deco-tray-item span {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-foreground);
}
.deco-tray-item:hover {
  border-color: var(--color-primary-light-5);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.07);
}
.deco-count { margin-top: 18px; }
.deco-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}
.deco-item-row {
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.deco-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.deco-name {
  flex: 1;
  font-family: var(--font-ui);
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-foreground);
}
.deco-del {
  width: 24px; height: 24px;
  border: none;
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  display: grid; place-items: center;
  transition: all .15s;
}
.deco-del:hover { background: #FDE8F1; color: var(--color-primary); }
.deco-empty-tip {
  margin-top: 16px;
  padding: 18px;
  text-align: center;
  font-size: 12.5px;
  color: var(--color-muted-foreground);
  background: var(--color-muted);
  border-radius: var(--radius-sm);
}

/* 空状态 */
.empty-box {
  grid-column: 1 / -1;
  padding: 36px 0;
  text-align: center;
  color: var(--color-muted-foreground);
  font-family: var(--font-ui);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  opacity: 0.8;
}

/* ========== 单甲精修模式（就地放大，借鉴 Luna） ========== */
.single-mode {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  padding: 8px 0;
}
.single-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.single-title {
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-foreground);
}
.single-title b { color: var(--color-primary); }
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
}
.back-btn:hover { color: var(--color-primary); border-color: var(--color-primary-light-5); }
.single-nail {
  display: flex;
  align-items: center;
  gap: 14px;
}
.single-canvas {
  display: grid;
  place-items: center;
  min-height: 300px;
  padding: 16px;
  background: rgba(255,255,255,0.45);
  border-radius: 16px;
  cursor: zoom-out;
  transition: box-shadow .15s ease;
}
/* 单甲模式拖拽悬停高亮：可投放到当前大指甲 */
.single-canvas.over { box-shadow: 0 0 0 3px rgba(229, 62, 140, 0.55); }
.single-nav {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-foreground);
  font-size: 20px;
  line-height: 1;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all .15s;
}
.single-nav:hover:not(:disabled) { color: var(--color-primary); border-color: var(--color-primary-light-5); }
.single-nav:disabled { opacity: .3; cursor: not-allowed; }
.single-hint {
  font-family: var(--font-ui);
  font-size: 11.5px;
  color: var(--color-muted-foreground);
}

/* 响应式：Studio 主布局 */
@media (max-width: 1000px) {
  .studio { grid-template-columns: 1fr; }
  .preview-wrap { position: relative; top: 0; }
}

/* ========== 效果层（OVERLAY 覆盖层栈） ========== */
.overlay-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.overlay-card {
  background: #F7F1E8;
  border: 2px solid transparent;
  border-radius: 12px;
  padding: 8px 4px 6px;
  text-align: center;
  cursor: pointer;
  transition: all .15s;
}
.overlay-card:hover { border-color: var(--color-primary-light-5); }
.overlay-card.active { border-color: var(--color-primary); background: #FBE9F1; }
.overlay-preview { display: grid; place-items: center; min-height: 64px; }
.overlay-name {
  display: block;
  margin-top: 2px;
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-foreground);
}
.ov-count { margin-top: 12px; }
.ov-list { display: flex; flex-direction: column; gap: 6px; }
.ov-item-row {
  display: grid;
  grid-template-columns: 92px 1fr 30px;
  align-items: center;
  gap: 10px;
  background: var(--color-muted, #F5F0E6);
  border-radius: 10px;
  padding: 6px 10px;
}
.ov-name { font-family: var(--font-ui); font-size: 12px; font-weight: 600; }
.ov-del {
  width: 24px; height: 24px;
  border: none; border-radius: 50%;
  background: #fff; color: var(--color-muted-foreground);
  font-size: 15px; line-height: 1;
  cursor: pointer;
}
.ov-del:hover { color: #D64545; }

/* ========== 长度控制（独立于甲型的设计维度） ========== */
.length-block { margin-top: 14px; }
.len-value {
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  background: var(--color-primary-light-9);
  color: var(--color-primary);
  border-radius: var(--radius-full);
}
.len-row { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; }

/* ========== 视图切换 + 手模容器 ========== */
.stage-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.view-toggle {
  display: inline-flex;
  background: var(--color-muted);
  padding: 3px;
  border-radius: var(--radius-full);
}
.view-toggle button {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s;
}
.view-toggle button.on {
  background: #fff;
  color: var(--color-foreground);
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.hand-wrap {
  display: flex;
  justify-content: center;
  width: 100%;
  padding: 8px 0;
}

/* ========== 参数化款式面板 ========== */
.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
  margin-bottom: 6px;
}
.style-card {
  padding: 10px;
  background: var(--color-muted);
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all .2s;
  text-align: center;
}
.style-card:hover { border-color: var(--color-primary-light-5); }
.style-card.active {
  background: var(--color-primary-light-9);
  border-color: var(--color-primary);
}
.style-preview {
  height: 64px;
  display: grid;
  place-items: center;
  margin-bottom: 6px;
  overflow: hidden;
}
.style-name {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-foreground);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.style-card.active .style-name { color: var(--color-primary); }
.style-color-row { display: flex; align-items: center; gap: 8px; }
.style-color-row input[type="color"] {
  width: 36px; height: 30px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  padding: 2px;
  cursor: pointer;
}
.style-select {
  padding: 7px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-family: var(--font-ui);
  font-size: 12.5px;
  color: var(--color-foreground);
  background: #fff;
  outline: none;
}

/* ========== 保存完成浮层 ========== */
.saved-veil {
  position: fixed; inset: 0; z-index: 60;
  background: rgba(60, 45, 35, 0.42);
  backdrop-filter: blur(2px);
  display: grid; place-items: center;
}
.saved-card {
  width: 340px;
  background: var(--color-canvas, #F7EFE2);
  border-radius: 16px;
  padding: 26px 26px 18px;
  text-align: center;
  box-shadow: 0 18px 48px rgba(60, 40, 25, 0.25);
}
.saved-badge {
  width: 44px; height: 44px; margin: 0 auto 10px;
  border-radius: 50%;
  display: grid; place-items: center;
  color: #fff;
  background: var(--color-primary);
}
.saved-title { margin: 0 0 12px; font-size: 18px; }
.saved-thumb {
  width: 100%; height: 190px; object-fit: cover;
  border-radius: 12px; border: 1px solid rgba(160, 120, 80, 0.18);
  background: #fff;
}
.saved-name { margin: 10px 0 2px; font-weight: 700; }
.saved-sub { margin: 0 0 14px; font-size: 12px; color: #8A8177; }
.saved-actions { display: flex; gap: 8px; }
.sv-btn {
  flex: 1; padding: 9px 0;
  border-radius: 10px; border: 1px solid transparent;
  font-size: 13px; cursor: pointer;
}
.sv-ghost { background: transparent; border-color: rgba(160, 120, 80, 0.35); color: #7A6A58; }
.sv-ghost:hover { background: rgba(160, 120, 80, 0.08); }
.sv-line { background: #fff; border-color: var(--color-primary); color: var(--color-primary); }
.sv-primary { background: var(--color-primary); color: #fff; }
.sv-primary:hover { filter: brightness(1.05); }
.sv-btn:disabled { opacity: 0.6; cursor: default; }
.saved-link { display: inline-block; margin-top: 12px; font-size: 12px; color: #8A8177; }
.saved-link:hover { color: var(--color-primary); }
</style>
