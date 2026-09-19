<template>
  <!-- 单枚甲片：SVG 分层渲染（底色→猫眼→法式→波点豹纹→贴花→钻饰→光泽→描边）；纯装饰，语义由宿主元素提供 -->
  <div class="nail-unit" :class="{ active, embedded: !outlined }" aria-hidden="true" @click="$emit('select')">
    <svg ref="svgRef" :viewBox="'0 0 60 ' + HEIGHT" class="nail-svg" :style="{ width: width + 'px' }">
      <defs>
        <clipPath :id="uid + '-clip'"><path :d="shapePath" /></clipPath>
        <!-- 钻饰切面裁剪（按基础形） -->
        <clipPath v-for="(p, key) in GEM_PATHS" :key="'dcp' + key" :id="uid + '-dc-' + key"><path :d="p" /></clipPath>

        <!-- 两色渐变底 -->
        <linearGradient v-if="isGradient" :id="uid + '-grad'" x1="0" y1="0" x2="0" y2="1">
          <stop :offset="gradStop1" :stop-color="base1Final" />
          <stop :offset="gradStop2" :stop-color="base2Final" />
        </linearGradient>

        <!-- 猫眼磁吸光带（旋转角度可调的高光带） -->
        <linearGradient v-if="catEyeOn" :id="uid + '-cat'"
                        x1="0" y1="0" x2="1" y2="0"
                        :gradientTransform="`rotate(${ce.angle} 30 42)`">
          <stop offset="0"    stop-color="#FFFFFF" stop-opacity="0" />
          <stop :offset="catA" stop-color="#E8EEF8" :stop-opacity="ceOpacity" />
          <stop offset="0.5"  stop-color="#FFFFFF" :stop-opacity="ceBright" />
          <stop :offset="catB" stop-color="#E8EEF8" :stop-opacity="ceOpacity" />
          <stop offset="1"    stop-color="#FFFFFF" stop-opacity="0" />
        </linearGradient>

        <!-- 凝胶顶胶高光：宽幅柔光（自上而下淡出）+ 细长高光条 -->
        <linearGradient :id="uid + '-sheen'" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0"   stop-color="#FFFFFF" stop-opacity="0.38" />
          <stop offset="0.5" stop-color="#FFFFFF" stop-opacity="0.09" />
          <stop offset="1"   stop-color="#FFFFFF" stop-opacity="0" />
        </linearGradient>
        <linearGradient :id="uid + '-shine'" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0"    stop-color="#FFFFFF" stop-opacity="0.25" />
          <stop offset="0.45" stop-color="#FFFFFF" stop-opacity="0.9" />
          <stop offset="1"    stop-color="#FFFFFF" stop-opacity="0.05" />
        </linearGradient>

        <!-- 钻饰材质：玻璃钻 / 玫瑰钻 / 金属 / 珍珠 -->
        <radialGradient :id="uid + '-gem'" cx="0.35" cy="0.28" r="0.95">
          <stop offset="0"    stop-color="#FFFFFF" />
          <stop offset="0.35" stop-color="#EAF2FC" />
          <stop offset="0.72" stop-color="#BDCFEA" />
          <stop offset="1"    stop-color="#8FA8D2" />
        </radialGradient>
        <radialGradient :id="uid + '-rose'" cx="0.35" cy="0.28" r="0.95">
          <stop offset="0"   stop-color="#FFFFFF" />
          <stop offset="0.4" stop-color="#FBE7EC" />
          <stop offset="1"   stop-color="#E5A9BC" />
        </radialGradient>
        <linearGradient :id="uid + '-gold'" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0"   stop-color="#F7E6B2" />
          <stop offset="0.5" stop-color="#DFBE72" />
          <stop offset="1"   stop-color="#B3924A" />
        </linearGradient>
        <radialGradient :id="uid + '-pearl'" cx="0.35" cy="0.3" r="0.95">
          <stop offset="0"    stop-color="#FFFFFF" />
          <stop offset="0.55" stop-color="#FAF3ED" />
          <stop offset="1"    stop-color="#DFC9BC" />
        </radialGradient>
      </defs>

      <g :clip-path="`url(#${uid}-clip)`">
        <!-- ① 底色（纯色 / 渐变） -->
        <path :d="shapePath" :fill="baseFill" />

        <!-- ② 猫眼光带（叠加提亮） -->
        <path v-if="catEyeOn" :d="shapePath" :fill="`url(#${uid}-cat)`" style="mix-blend-mode: screen" />

        <!-- ②.5 参数化款式（引擎生成：大理石/玳瑁/极光/曲线/几何/星星等，渲染在底色与法式之间）
             引擎按 60x84 标准甲面出坐标，这里按长度系数整体纵向缩放 -->
        <g :transform="`scale(1 ${kLen})`">
          <g v-for="(el, i) in styleEls" :key="'st' + i">
            <path v-if="el.tag === 'path'" v-bind="restAttrs(el)" />
            <circle v-else-if="el.tag === 'circle'" v-bind="restAttrs(el)" />
            <ellipse v-else-if="el.tag === 'ellipse'" v-bind="restAttrs(el)" />
          </g>
        </g>

        <!-- ③ 法式甲（指尖弧形带） -->
        <path v-if="frenchOn" :d="frenchPath" :fill="frenchFill" />

        <!-- ④ 波点 / 豹纹（随长度纵向缩放） -->
        <template v-if="dotsOn">
          <g v-if="dt.kind === 'dots'" :transform="`scale(1 ${kLen})`">
            <ellipse v-for="(p, i) in dotList" :key="'d' + i"
                     :cx="p.x" :cy="p.y" :rx="p.r" :ry="p.r * 0.92"
                     :fill="dotsFill" :opacity="0.9" />
          </g>
          <g v-else :transform="`scale(1 ${kLen})`"><!-- 豹纹：不规则环 + 内点 -->
            <g v-for="(p, i) in dotList" :key="'l' + i">
              <circle :cx="p.x" :cy="p.y" :r="p.r" fill="none"
                      :stroke="dotsFill" :stroke-width="p.r * 0.62" :opacity="0.85" />
              <circle :cx="p.x + p.r * 0.3" :cy="p.y - p.r * 0.25" :r="p.r * 0.3" :fill="dotsFill" />
            </g>
          </g>
        </template>

        <!-- ④.5 效果覆盖层（OVERLAY 层栈：珠光/闪耀/炫光/特殊等，可多选叠加）
             image_url 有值走透明 PNG 通道，否则参数化渲染；统一裁剪进轮廓、随长度缩放 -->
        <g v-for="ov in overlayRenders" :key="'ov' + ov.id" :opacity="ov.opacity">
          <template v-if="ov.image">
            <image :href="ov.image" x="-2" y="-2" :width="64" :height="HEIGHT + 4" preserveAspectRatio="none" />
          </template>
          <g v-else :transform="`scale(1 ${kLen})`">
            <path v-for="(el, i) in ov.els" v-bind="restAttrs(el)" :key="'ove' + i" />
            <circle v-for="(el, i) in ov.circles" v-bind="restAttrs(el)" :key="'ovc' + i" />
          </g>
        </g>

        <!-- ⑤ 贴花图案（素材库；落点由父组件记录，可在甲面上拖动微调） -->
        <g v-for="(p, j) in patterns" :key="'p' + j"
           class="pat-item"
           @pointerdown.stop.prevent="onPatDown($event, j)">
          <image :href="p.imageUrl"
                 :x="patXY(j).x" :y="patXY(j).y"
                 :width="PATTERN_U" :height="PATTERN_U" preserveAspectRatio="none" />
        </g>

        <!-- [暂时下线-钻饰] 恢复时取消本段注释（宿主 Designer 的 JEWEL 面板同步恢复）
        <g v-for="(dr, k) in decoRenders" :key="'deco' + dr.d.id"
           :transform="decoTransform(dr.d)"
           class="deco-item"
           @pointerdown.stop.prevent="onDecoDown($event, k)">
          <path :d="dr.gem.base" :fill="dr.gem.tint" fill-rule="evenodd" />
          <g :clip-path="`url(#${uid}-dc-${dr.baseKey})`">
            <path v-for="(el, i) in dr.gem.facets" v-bind="restAttrs(el)" :key="'fc' + i" />
          </g>
          <path v-for="(el, i) in dr.gem.finish" v-bind="restAttrs(el)" :key="'fi' + i" />
        </g>
        [暂时下线-钻饰] -->

        <!-- ⑦ 凝胶顶胶高光（随甲型裁剪，覆盖底色/款式/贴花之上） -->
        <path :d="sheenPath" :fill="`url(#${uid}-sheen)`" />
        <path :d="shinePath" fill="none" :stroke="`url(#${uid}-shine)`" stroke-width="4.4" stroke-linecap="round" />
      </g>

      <!-- 甲片描边（手模嵌入态不画描边，靠投影与甲床贴合表现边缘） -->
      <path v-if="outlined" :d="shapePath" fill="none" stroke="rgba(0,0,0,0.14)" stroke-width="1.5" />
    </svg>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { adjustColor, seededRandom, isDarkColor } from '../utils/color'
import { genPattern } from '../engine/patternStyles'
import { genOverlay } from '../engine/polishOverlays'
import { decoGem, GEM_PATHS } from '../engine/decoGems'
import { clampLength, contourHeight, shapePathOf } from '../engine/contours'

const props = defineProps({
  shape: { type: Object, default: null },   // { name } 甲型素材
  color: { type: String, default: '#F5F0E6' }, // 底色1
  patterns: { type: Array, default: () => [] },
  active: { type: Boolean, default: false },
  index: { type: Number, default: 0 },
  width: { type: Number, default: 56 },
  // ---- 新增编辑能力 ----
  gradient: { type: Object, default: null },    // { enabled, color2, position(0-100) }
  catEye: { type: Object, default: null },      // { enabled, angle(-90~90), width(5-90), brightness(0-100) }
  adjust: { type: Object, default: null },      // { hue(-180~180), sat(-100~100), light(-100~100) }
  french: { type: Object, default: null },      // { enabled, color, height(10-40), curvature(0-100) }
  dots: { type: Object, default: null },        // { enabled, kind:'dots'|'leopard', density(3-15), size(1-5), color }
  decorations: { type: Array, default: () => [] }, // [{ id, type, x%, y%, size, angle }]
  patternPos: { type: Object, default: null },   // { [图案id]: { x%, y% } } 拖放落点（未记录的回退默认槽位）
  patternStyle: { type: Object, default: null }, // { code, params } 参数化款式（见 engine/patternStyles.js）
  length: { type: Number, default: 1 },          // 长度系数 0.6~2.8，独立于甲型的设计维度
  outlined: { type: Boolean, default: true },    // 是否绘制描边（手模嵌入态传 false，改用柔和投影）
  overlays: { type: Array, default: () => [] },      // [{ id, opacity }] OVERLAY 层栈，可多选叠加
  overlayCatalog: { type: Array, default: () => [] } // OVERLAY 素材目录（id → code/image_url 解析）
})
const emit = defineEmits(['select', 'moveDeco', 'movePattern'])
// 组件实例唯一 id（SVG defs 防串色）
const uid = 'na' + Math.random().toString(36).slice(2, 9)
const svgRef = ref(null)

// ---------- 甲型轮廓 × 长度（两个独立设计维度） ----------
// 甲型轮廓几何统一在 engine/contours.js：设计台与运营端素材预览共用同一份，形状不会走样
const f1 = v => Number(v.toFixed(1))
const kLen = computed(() => clampLength(props.length))
const HEIGHT = computed(() => contourHeight(kLen.value))
const shapePath = computed(() => shapePathOf(props.shape, kLen.value))

// ---------- 贴花位置 ----------
const PATTERN_U = 28                              // 贴花渲染尺寸（素材自带留白，有效视觉≈甲宽 40%）
const patternPos = [
  { x: 21, y: 26 }, { x: 30, y: 40 }, { x: 18, y: 48 }
]
// 拖放落点优先（百分比 → SVG 坐标，图案中心对准落点）；未记录的老数据回退默认槽位
function patXY(j) {
  const p = props.patterns[j]
  const pos = p ? props.patternPos?.[p.id] : null
  if (pos) {
    return {
      x: Math.round((pos.x / 100) * 60 - PATTERN_U / 2),
      y: Math.round((pos.y / 100) * HEIGHT.value - PATTERN_U / 2)
    }
  }
  return patternPos[j % patternPos.length]
}

// ---------- ① 底色 / 渐变 ----------
const adj = () => props.adjust || { hue: 0, sat: 0, light: 0 }
const base1Final = computed(() => adjustColor(props.color, adj()))
const isGradient = computed(() => !!props.gradient?.enabled && !!props.gradient.color2)
const base2Final = computed(() => adjustColor(props.gradient?.color2 || '#FFFFFF', adj()))
const baseFill = computed(() => isGradient.value ? `url(#${uid}-grad)` : base1Final.value)
// 渐变位置：过渡带围绕 position，带宽约 70%
const gradStop1 = computed(() => Math.max(0, ((props.gradient?.position ?? 50) / 100) - 0.35).toFixed(2))
const gradStop2 = computed(() => Math.min(1, ((props.gradient?.position ?? 50) / 100) + 0.35).toFixed(2))

// ---------- ② 猫眼光带 ----------
const catEyeOn = computed(() => !!props.catEye?.enabled)
const ce = computed(() => ({ angle: 0, width: 34, brightness: 72, ...props.catEye }))
const ceBright = computed(() => (0.35 + (ce.value.brightness / 100) * 0.65).toFixed(2))
const ceOpacity = computed(() => (ceBright.value * 0.75).toFixed(2))
const halfW = computed(() => Math.max(0.08, (ce.value.width / 100) / 2 * 0.9))
const catA = computed(() => Math.max(0, 0.5 - halfW.value).toFixed(2))
const catB = computed(() => Math.min(1, 0.5 + halfW.value).toFixed(2))

// ---------- ②.5 参数化款式（引擎） ----------
const styleEls = computed(() => {
  const code = props.patternStyle?.code
  if (!code) return []
  const params = props.patternStyle?.params || {}
  return genPattern(code, { baseColor: base1Final.value, seed: props.index + 1, ...params })
})
// 去掉元素描述里的 tag 字段，余下全部透传为 SVG 属性
function restAttrs(el) {
  const { tag, ...attrs } = el
  return attrs
}

// ---------- ④.5 效果覆盖层（OVERLAY 层栈） ----------
// image_url 有值 → 透明 PNG 通道（最终形态）；否则按 code 参数化渲染（过渡期）
const overlayRenders = computed(() => {
  return (props.overlays || []).map(o => {
    const mat = (props.overlayCatalog || []).find(m => m.id === o.id)
    if (!mat) return null
    const base = { id: o.id, opacity: Math.max(0, Math.min(1, Number(o.opacity ?? 1))) }
    if (mat.imageUrl) return { ...base, image: mat.imageUrl }
    const els = genOverlay(mat.code, { seed: props.index + 1 })
    return { ...base, els: els.filter(e => e.tag === 'path'), circles: els.filter(e => e.tag === 'circle') }
  }).filter(Boolean)
})

// ---------- ③ 法式甲（贴着自由缘/指尖，随长度伸缩） ----------
const frenchOn = computed(() => !!props.french?.enabled)
const fr = computed(() => ({ color: '#FFFFFF', height: 26, curvature: 72, ...props.french }))
const frenchFill = computed(() => fr.value.color || '#FFFFFF')
const frenchPath = computed(() => {
  const tipY = 76 * kLen.value + 4
  const topY = tipY - Math.max(8, Math.min(40, fr.value.height))
  const lift = (fr.value.curvature / 100) * 16   // 笑线中央上扬
  return `M -2 ${f1(topY)} Q 30 ${f1(topY - lift)} 62 ${f1(topY)} L 62 ${f1(tipY + 6)} L -2 ${f1(tipY + 6)} Z`
})

// ---------- ④ 波点 / 豹纹 ----------
const dotsOn = computed(() => !!props.dots?.enabled)
const dt = computed(() => ({ kind: 'dots', density: 7, size: 2.5, color: '', ...props.dots }))
// 智能配色：未指定颜色时按底色明暗自动取反差色
const dotsFill = computed(() => {
  if (dt.value.color) return dt.value.color
  return isDarkColor(base1Final.value) ? 'rgba(255,255,255,0.85)' : 'rgba(60,45,35,0.7)'
})
const dotList = computed(() => {
  const { kind, density, size } = dt.value
  const n = Math.round(density)
  const rnd = seededRandom(props.index * 97 + n * 13 + (kind === 'leopard' ? 5 : 1))
  const list = []
  for (let i = 0; i < n; i++) {
    const x = 6 + rnd() * 48
    const y = 10 + rnd() * 62
    const r = kind === 'leopard'
      ? size * (0.9 + rnd() * 0.7)
      : size * (0.85 + rnd() * 0.4)
    list.push({ x, y, r })
  }
  return list
})

// ---------- ⑥ 钻饰（切面水晶渲染，见 engine/decoGems.js） ----------
const decoRenders = computed(() => (props.decorations || []).map(d => {
  const baseKey = GEM_PATHS[d.type] ? d.type : d.type.split('-')[0]
  return { d, gem: decoGem(d.type), baseKey }
}))
function decoTransform(d) {
  const s = (d.size || 12) / 12
  return `translate(${(d.x / 100) * 60} ${(d.y / 100) * HEIGHT.value}) rotate(${d.angle || 0}) scale(${s})`
}

// 拖动装饰：SVG 坐标 → 百分比，emit 给父组件
function onDecoDown(e, k) {
  const svg = svgRef.value
  if (!svg) return
  const rect = svg.getBoundingClientRect()
  const move = (ev) => {
    const x = ((ev.clientX - rect.left) / rect.width) * 100
    const y = ((ev.clientY - rect.top) / rect.height) * 100
    emit('moveDeco', {
      index: k,
      x: Math.max(8, Math.min(92, x)),
      y: Math.max(8, Math.min(92, y))
    })
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

// 拖动贴花：与钻饰同款交互，落点以百分比回传父组件
function onPatDown(e, j) {
  const svg = svgRef.value
  if (!svg) return
  const rect = svg.getBoundingClientRect()
  const move = (ev) => {
    const x = ((ev.clientX - rect.left) / rect.width) * 100
    const y = ((ev.clientY - rect.top) / rect.height) * 100
    emit('movePattern', {
      index: j,
      x: Math.max(5, Math.min(95, x)),
      y: Math.max(5, Math.min(95, y))
    })
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

// ---------- ⑦ 顶胶高光（随长度伸缩） ----------
// 宽幅柔光自甲根向指尖淡出；细长高光条偏左贯穿甲面（凝胶亮面质感，参照实拍甲片高光）
const sheenPath = computed(() => {
  const tipY = 76 * kLen.value + 4
  const y2 = Math.min(tipY - 4, 12 + 46 * kLen.value)
  const w = 6 + 24 * kLen.value
  return `M 13 6 Q 30 1 47 6 Q 49 ${f1(w)} 42 ${f1(y2)} Q 30 ${f1(y2 + 5)} 18 ${f1(y2)} Q 11 ${f1(w)} 13 6 Z`
})
const shinePath = computed(() => {
  const y2 = Math.min(76 * kLen.value - 2, 11 + 34 * kLen.value)
  return `M 24 11 C ${f1(24 - 2 * kLen.value)} ${f1(11 + 10 * kLen.value)}, 21.5 ${f1(11 + 22 * kLen.value)}, 23.5 ${f1(y2)}`
})
</script>

<style scoped>
.nail-unit { position: relative; cursor: pointer; }
/* 嵌入态（手模）：无描边、无选中框、无悬停位移，仅极柔和的自然投影贴合甲床 */
.nail-unit.embedded .nail-svg { filter: drop-shadow(0 2px 3px rgba(120, 80, 50, 0.18)); }
.nail-unit.embedded:hover .nail-svg { transform: none; }
.nail-svg { display: block; transition: transform .18s ease; }
.nail-unit:hover .nail-svg { transform: translateY(-3px); }
.deco-item { cursor: grab; }
.deco-item:active { cursor: grabbing; }
.pat-item { cursor: grab; }
.pat-item:active { cursor: grabbing; }
</style>
