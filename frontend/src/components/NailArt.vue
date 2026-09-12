<template>
  <!-- 单枚甲片：SVG 分层渲染（底色→猫眼→法式→波点豹纹→贴花→钻饰→光泽→描边）；纯装饰，语义由宿主元素提供 -->
  <div class="nail-unit" :class="{ active }" aria-hidden="true" @click="$emit('select')">
    <svg ref="svgRef" :viewBox="'0 0 60 ' + HEIGHT" class="nail-svg" :style="{ width: width + 'px' }">
      <defs>
        <clipPath :id="uid + '-clip'"><path :d="shapePath" /></clipPath>

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
      </defs>

      <g :clip-path="`url(#${uid}-clip)`">
        <!-- ① 底色（纯色 / 渐变） -->
        <path :d="shapePath" :fill="baseFill" />

        <!-- ② 猫眼光带（叠加提亮） -->
        <path v-if="catEyeOn" :d="shapePath" :fill="`url(#${uid}-cat)`" style="mix-blend-mode: screen" />

        <!-- ②.5 参数化款式（引擎生成：大理石/玳瑁/极光/曲线/几何/星星等，渲染在底色与法式之间） -->
        <g v-for="(el, i) in styleEls" :key="'st' + i">
          <path v-if="el.tag === 'path'" v-bind="restAttrs(el)" />
          <circle v-else-if="el.tag === 'circle'" v-bind="restAttrs(el)" />
          <ellipse v-else-if="el.tag === 'ellipse'" v-bind="restAttrs(el)" />
        </g>

        <!-- ③ 法式甲（指尖弧形带） -->
        <path v-if="frenchOn" :d="frenchPath" :fill="frenchFill" />

        <!-- ④ 波点 / 豹纹 -->
        <template v-if="dotsOn">
          <g v-if="dt.kind === 'dots'">
            <ellipse v-for="(p, i) in dotList" :key="'d' + i"
                     :cx="p.x" :cy="p.y" :rx="p.r" :ry="p.r * 0.92"
                     :fill="dotsFill" :opacity="0.9" />
          </g>
          <g v-else><!-- 豹纹：不规则环 + 内点 -->
            <g v-for="(p, i) in dotList" :key="'l' + i">
              <circle :cx="p.x" :cy="p.y" :r="p.r" fill="none"
                      :stroke="dotsFill" :stroke-width="p.r * 0.62" :opacity="0.85" />
              <circle :cx="p.x + p.r * 0.3" :cy="p.y - p.r * 0.25" :r="p.r * 0.3" :fill="dotsFill" />
            </g>
          </g>
        </template>

        <!-- ⑤ 贴花图案（素材库；落点由父组件记录，可在甲面上拖动微调） -->
        <g v-for="(p, j) in patterns" :key="'p' + j"
           class="pat-item"
           @pointerdown.stop.prevent="onPatDown($event, j)">
          <image :href="p.imageUrl"
                 :x="patXY(j).x" :y="patXY(j).y"
                 :width="18" :height="18" preserveAspectRatio="none" />
        </g>

        <!-- ⑥ 钻饰装饰（可拖动） -->
        <g v-for="(d, k) in decorations" :key="'deco' + d.id"
           :transform="decoTransform(d)"
           class="deco-item"
           @pointerdown.stop.prevent="onDecoDown($event, k)">
          <path :d="DECO_PATHS[d.type] || DECO_PATHS.star" :fill="decoFill(d)" />
          <circle v-if="d.type !== 'bar'" :cx="-1.6" :cy="-1.8" r="1.1" fill="rgba(255,255,255,0.95)" />
        </g>
      </g>

      <!-- ⑦ 光泽高光 + 描边 -->
      <path :d="glossPath" fill="rgba(255,255,255,0.16)" />
      <path :d="shapePath" fill="none" stroke="rgba(0,0,0,0.12)" stroke-width="1.5" />
    </svg>
    <span v-if="active" class="nail-ring"></span>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { adjustColor, seededRandom, isDarkColor } from '../utils/color'
import { genPattern } from '../engine/patternStyles'

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
  patternStyle: { type: Object, default: null }  // { code, params } 参数化款式（见 engine/patternStyles.js）
})
const emit = defineEmits(['select', 'moveDeco', 'movePattern'])

const HEIGHT = 84
// 组件实例唯一 id（SVG defs 防串色）
const uid = 'na' + Math.random().toString(36).slice(2, 9)
const svgRef = ref(null)

// ---------- 甲型轮廓 ----------
const PATHS = {
  square: 'M16 6 H44 C50 6 54 10 54 16 V56 C54 68 44 78 30 78 C16 78 6 68 6 56 V16 C6 10 10 6 16 6 Z',
  oval: 'M30 2 C18 2 10 12 10 24 C10 38 8 50 10 58 C12 68 20 78 30 78 C40 78 48 68 50 58 C52 50 50 38 50 24 C50 12 42 2 30 2 Z',
  almond: 'M30 2 C20 2 12 12 12 24 C11 40 10 52 14 60 C18 70 24 76 30 76 C36 76 42 70 46 60 C50 52 49 40 48 24 C48 12 40 2 30 2 Z',
  stiletto: 'M30 0 C22 2 14 12 13 26 C12 44 10 54 14 62 C18 72 24 78 30 78 C36 78 42 72 46 62 C50 54 48 44 47 26 C46 12 38 2 30 0 Z',
  short: 'M30 8 C18 8 10 18 10 28 C10 42 8 52 10 58 C12 68 20 76 30 76 C40 76 48 68 50 58 C52 52 50 42 50 28 C50 18 42 8 30 8 Z',
  // ---- 扩充甲型（对齐 Luna 风格的长短系列） ----
  xsquare: 'M17 2 H43 C50 2 54 7 54 15 V58 C54 70 44 79 30 79 C16 79 6 70 6 58 V15 C6 7 10 2 17 2 Z',
  xstiletto: 'M30 0 C24 1 15 10 14 24 C13 44 11 56 15 64 C19 73 25 79 30 79 C35 79 41 73 45 64 C49 56 47 44 46 24 C45 10 36 1 30 0 Z',
  ballerina: 'M9 4 H51 L45 52 C44 66 38 76 30 76 C22 76 16 66 15 52 Z',
  shortsq: 'M14 16 H46 C50 16 53 19 53 24 V54 C53 66 44 74 30 74 C16 74 7 66 7 54 V24 C7 19 10 16 14 16 Z',
  round: 'M30 10 C43 10 52 20 52 34 C52 52 44 66 30 70 C16 66 8 52 8 34 C8 20 17 10 30 10 Z',
  // ---- 二期扩充：少见甲型 ----
  edge: 'M14 4 L26 4 L30 14 L34 4 L46 4 C50 4 52 8 52 14 V60 C52 72 44 78 30 78 C16 78 8 72 8 60 V14 C8 8 10 4 14 4 Z',
  flare: 'M20 40 C20 18 24 6 30 6 C36 6 40 18 40 40 L56 66 C58 74 46 79 30 79 C14 79 2 74 4 66 Z',
  russian: 'M15 0 H45 C50 0 53 4 53 10 V62 C53 74 43 81 30 81 C17 81 7 74 7 62 V10 C7 4 10 0 15 0 Z',
  lipstick: 'M14 4 H40 C46 4 50 8 50 14 L54 44 C56 60 44 70 28 72 C14 74 6 66 8 52 Z',
  moval: 'M30 6 C20 6 12 15 12 26 C12 40 10 52 12 60 C14 69 21 77 30 77 C39 77 46 69 48 60 C50 52 48 40 48 26 C48 15 40 6 30 6 Z',
  default: 'M30 4 C18 4 10 14 10 26 C10 42 10 56 10 62 C10 72 18 78 30 78 C42 78 50 72 50 62 C50 56 50 42 50 26 C50 14 42 4 30 4 Z'
}
const shapePath = computed(() => {
  // 新版：素材 code 直接绑定渲染器 path（素材库重构后）
  const code = props.shape?.code || ''
  if (code && PATHS[code]) return PATHS[code]
  // 旧数据回退：按名称关键字匹配
  const name = props.shape?.name || ''
  let key = 'default'
  // 匹配顺序敏感：长前缀系列 → 方/椭/杏/尖 → 圆 → 短圆
  if (name.includes('超长方')) key = 'xsquare'
  else if (name.includes('超长尖')) key = 'xstiletto'
  else if (name.includes('俄罗斯')) key = 'russian'
  else if (name.includes('中长')) key = 'moval'
  else if (name.includes('芭蕾') || name.includes('梯形')) key = 'ballerina'
  else if (name.includes('折角')) key = 'edge'
  else if (name.includes('喇叭')) key = 'flare'
  else if (name.includes('唇形')) key = 'lipstick'
  else if (name.includes('短方')) key = 'shortsq'
  else if (name.includes('圆形')) key = 'round'
  else if (name.includes('方圆') || name.includes('方形')) key = 'square'
  else if (name.includes('椭圆')) key = 'oval'
  else if (name.includes('杏仁')) key = 'almond'
  else if (name.includes('尖')) key = 'stiletto'
  else if (name.includes('短圆')) key = 'short'
  return PATHS[key]
})

// ---------- 贴花位置 ----------
const patternPos = [
  { x: 21, y: 26 }, { x: 30, y: 40 }, { x: 18, y: 48 }
]
// 拖放落点优先（百分比 → SVG 坐标，图案中心对准落点）；未记录的老数据回退默认槽位
function patXY(j) {
  const p = props.patterns[j]
  const pos = p ? props.patternPos?.[p.id] : null
  if (pos) {
    return {
      x: Math.round((pos.x / 100) * 60 - 9),
      y: Math.round((pos.y / 100) * HEIGHT - 9)
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

// ---------- ③ 法式甲 ----------
const frenchOn = computed(() => !!props.french?.enabled)
const fr = computed(() => ({ color: '#FFFFFF', height: 26, curvature: 72, ...props.french }))
const frenchFill = computed(() => fr.value.color || '#FFFFFF')
const frenchPath = computed(() => {
  const topY = 78 - Math.max(8, Math.min(40, fr.value.height))
  const lift = (fr.value.curvature / 100) * 16   // 笑线中央上扬
  return `M -2 ${topY} Q 30 ${topY - lift} 62 ${topY} L 62 86 L -2 86 Z`
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

// ---------- ⑥ 钻饰 ----------
// 六种钻饰 path（以原点为中心，基准尺寸约 ±6）
function starPts() {
  const pts = []
  for (let i = 0; i < 10; i++) {
    const ang = -Math.PI / 2 + (i * Math.PI) / 5
    const r = i % 2 === 0 ? 6 : 2.6
    pts.push(`${(Math.cos(ang) * r).toFixed(1)},${(Math.sin(ang) * r).toFixed(1)}`)
  }
  return 'M' + pts.join(' L') + ' Z'
}
const DECO_PATHS = {
  star: starPts(),
  drop: 'M0,-6 C3,-2 5.5,1 5.5,3.2 A5.5,5.5 0 1 1 -5.5,3.2 C-5.5,1 -3,-2 0,-6 Z',
  hex: 'M-5.2,-3 L0,-6 L5.2,-3 L5.2,3 L0,6 L-5.2,3 Z',
  bar: 'M-6,-2.5 L6,-2.5 L6,2.5 L-6,2.5 Z',
  round: 'M0,-5.6 A5.6,5.6 0 1 1 -0.01,-5.6 Z',
  heart: 'M0,5.5 C-6,1 -6.5,-2.5 -3.6,-4.4 C-1.8,-5.6 0,-4.4 0,-2.8 C0,-4.4 1.8,-5.6 3.6,-4.4 C6.5,-2.5 6,1 0,5.5 Z',
  // ---- 二期扩充 ----
  oval: 'M0,-6.5 A4.4,6.5 0 1 1 -0.01,-6.5 Z',
  triangle: 'M0,-6 L5.8,4.5 L-5.8,4.5 Z',
  marquise: 'M0,-7 C2.6,-4 3.4,-1 2.2,2.4 L0,7 L-2.2,2.4 C-3.4,-1 -2.6,-4 0,-7 Z',
  pearl: 'M0,-5.2 A5.2,5.2 0 1 1 -0.01,-5.2 Z'
}
function decoFill(d) {
  const map = {
    star: '#EAF0FA', drop: '#DFE9F7', hex: '#E6EDF9',
    bar: '#D9E4F2', round: '#EDF2FB', heart: '#F6D7DE',
    oval: '#E3ECF8', triangle: '#DDE7F5', marquise: '#E8F0FA',
    pearl: '#F6EFEA'
  }
  return map[d.type] || '#EAF0FA'
}
function decoTransform(d) {
  const s = (d.size || 12) / 12
  return `translate(${(d.x / 100) * 60} ${(d.y / 100) * HEIGHT}) rotate(${d.angle || 0}) scale(${s})`
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

// ---------- ⑦ 光泽 ----------
const glossPath = 'M14 14 C20 8 30 6 38 10 C32 16 24 22 18 30 C13 24 12 18 14 14 Z'
</script>

<style scoped>
.nail-unit { position: relative; cursor: pointer; }
.nail-svg { display: block; transition: transform .18s ease; }
.nail-unit:hover .nail-svg { transform: translateY(-3px); }
.nail-ring {
  position: absolute; inset: -4px; border: 2px solid var(--color-primary);
  border-radius: 20px; pointer-events: none;
}
.deco-item { cursor: grab; }
.deco-item:active { cursor: grabbing; }
.pat-item { cursor: grab; }
.pat-item:active { cursor: grabbing; }
</style>
