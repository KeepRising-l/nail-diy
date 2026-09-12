<template>
  <!-- 手模视图：透明底位图手（public/hands/hand-model-alpha.png，1278x1230，与原 hand-model.png 同一画面仅去白底）
       + NailArt 甲片逐指嵌入。每指锚点/宽度/角度/长度均从该图中对应天然甲床逐指实测（见 FINGERS 注释），
       甲根锚定在甲缘线、随指轴旋转、长度只向指尖延伸；不渲染任何指位文字，选中态无描边/外框 -->
  <div ref="rootRef" class="hand-model" role="group" aria-label="手模预览">
    <img class="hand-img" src="/hands/hand-model-alpha.png" alt="" aria-hidden="true" draggable="false" />

    <!-- 五指甲片槽位：由父组件渲染 NailArt（width=甲片画布宽，fit=该指长度适配系数） -->
    <div v-for="(f, i) in FINGERS" :key="'slot' + i"
         class="nail-slot"
         :class="{ active: f.idx === activeFinger, over: over === i }"
         :style="slotStyle(i)"
         role="button"
         tabindex="0"
         :aria-label="`选择${FINGER_NAMES[f.idx]}的甲片`"
         @click="$emit('select', f.idx)"
         @keydown.enter.prevent="$emit('select', f.idx)"
         @dragover.prevent="over = i"
         @dragleave="over = -1"
         @drop.prevent="$emit('drop', f.idx, $event)">
      <div class="slot-inner">
        <slot :finger="fingerAt(i)" :index="f.idx" :width="slotWidth(f)" :len-fit="slotLenFit(i)" />
      </div>
    </div>

    <!-- 甲缘遮罩：盖在甲片根部之上。肤色按各指实测取样，配合柔和模糊模拟甲缘皮肤对甲根的覆盖（嵌入感） -->
    <svg class="cuticle-mask" viewBox="0 0 1278 1230" aria-hidden="true">
      <defs>
        <filter id="hm-soft" x="-80%" y="-80%" width="260%" height="260%">
          <feGaussianBlur stdDeviation="2.4" />
        </filter>
      </defs>
      <template v-for="(f, i) in FINGERS" :key="'cm' + i">
        <!-- 肤色新月：跨在甲缘线上，尖端侧压住甲片根缘 -->
        <path :d="cuticlePath(f)" :transform="cuticleXform(f)" :fill="f.tone" filter="url(#hm-soft)" />
        <!-- 甲片从皮肤里长出来的那条浅浅的交接阴影 -->
        <path :d="foldLinePath(f)" :transform="cuticleXform(f)"
              fill="none" stroke="rgba(163, 102, 72, 0.20)" :stroke-width="f.s * 1.2" filter="url(#hm-soft)" />
      </template>
    </svg>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  activeFinger: { type: Number, default: 0 },
  fingers: { type: Array, default: () => [] }
})
const emit = defineEmits(['select', 'drop'])

const over = ref(-1)
const FINGER_NAMES = ['小指', '无名指', '中指', '食指', '大拇指']

/** 位图坐标系（与 hand-model-alpha.png 的 1278x1230 一致）。
 *  以下数据全部逐指从手图中实测：
 *  idx   —— fingers 数组索引
 *  x/y   —— 该指天然甲缘线中心（甲床最底缘，甲片根部的固定锚点）
 *  bedW  —— 天然甲床轮廓外缘宽度（px）；甲片最宽处 = bedW + 3，完整覆盖甲床、不明显超出
 *  bedL  —— 天然甲床甲缘到指尖的长度（px）；换算成标准→加长的长度映射系数（fitA/fitB）
 *  angle —— 甲床长轴与竖直方向的夹角（度，尖端偏向 +x 为正；甲片中心轴与指轴一致，拇指为大斜角）
 *  tone  —— 该指甲缘旁肤色取样（用于甲缘遮罩，融入手图） */
const HAND_VB = { w: 1278, h: 1230 }
const FINGERS = [
  { idx: 0, x: 315.8, y: 356,   bedW: 40, bedL: 79,  angle: -5.5, tone: '#F7D5C5' }, // 小指
  { idx: 3, x: 535.0, y: 206,   bedW: 50, bedL: 93,  angle: 9.5,  tone: '#F7D5C4' }, // 食指
  { idx: 2, x: 708.8, y: 150,   bedW: 58, bedL: 117, angle: 11.4, tone: '#F6D2C0' }, // 中指
  { idx: 1, x: 876.0, y: 262,   bedW: 57, bedL: 98,  angle: 18.9, tone: '#F7D6C5' }, // 无名指
  { idx: 4, x: 983.3, y: 714,   bedW: 70, bedL: 90,  angle: 38.8, tone: '#EBBFAC' }  // 大拇指
]

// NailArt 甲片画布几何约定：viewBox 宽 60，甲片横向占 x∈[8,52]（44 单位），甲根缘中心在 y≈7
const NAIL_ART_W = 60
const NAIL_LAT_W = 44
const ROOT_CENTER_Y = 7

// 逐指派生几何：px/单位换算 s、甲片画布宽、标准→加长 长度映射系数
for (const f of FINGERS) {
  f.s = (f.bedW + 3) / NAIL_LAT_W     // 甲片最宽处 = 甲床宽 + 3px：完整盖住天然甲床边缘，又不过分超出
  // 长度映射（分段）：L∈[0.6,1.0] 基础段（贴合→标准），L∈[1.0,1.6] 加长段（标准→极长，
  // 顶端延伸 = 原加长(1.37×甲床长) × 1.75 ≈ 2.40×甲床长）。根部锚点与宽度不随 L 变化。
  f.fitA = (1.16 * f.bedL / f.s + 3) / 76           // L=1.0（标准）时的 k
  f.fitB1 = 0.35 * f.bedL / (76 * f.s)              // 基础段斜率
  f.fitB2 = 2.0667 * f.bedL / (76 * f.s)            // 加长段斜率
}

/** 生成单指的长度映射函数（经槽位作用域传给 NailArt 的 length）。
 *  穿戴甲逻辑：L=0.6 贴合（尖端恰达指尖，仍完整覆盖甲床）→ L=1.0 标准（覆盖甲床 + 约 16% 基础延伸）
 *  → L=1.6 加长（2.40× 甲床长，即原加长再加四分之三）。只沿指轴向指尖伸长，根部不动。 */
function slotLenFit(i) {
  const f = FINGERS[i]
  return L => {
    const l = Math.min(1.6, Math.max(0.6, Number(L) || 1))
    const dBed = l <= 1
      ? 1.02 + 0.35 * (l - 0.6)                       // 相对甲床长的尖端距离系数
      : 1.16 + 2.0667 * (l - 1.0)
    const k = (dBed * f.bedL / f.s + 3) / 76
    return Math.min(2.8, Math.max(0.6, k))
  }
}

/** 单指数据（按视觉槽位 i 映射到 fingers 数组索引） */
function fingerAt(i) {
  const idx = FINGERS[i].idx
  return props.fingers[idx] || null
}

// 容器实测缩放：槽位/甲片尺寸按 1278 基准换算
const rootRef = ref(null)
const scale = ref(1)
let ro
onMounted(() => {
  ro = new ResizeObserver(entries => {
    const w = entries[0]?.contentRect?.width
    if (w) scale.value = w / HAND_VB.w
  })
  if (rootRef.value) ro.observe(rootRef.value)
})
onBeforeUnmount(() => ro?.disconnect())

/** 甲片画布宽（px）：画布 60 单位映射到 s·scale，甲片本体即与甲床同宽 */
function slotWidth(f) {
  return Math.round(f.s * NAIL_ART_W * scale.value * 10) / 10
}

/** 槽位定位：锚点沿指轴后移 ROOT_CENTER_Y 个单位（画布 px，百分比定位自动随容器缩放），
 *  使甲片根缘中心恰好落在甲缘线上；绕甲根旋转（transform-origin 50% 0），
 *  长度变化只向指尖方向延伸；
 *  +180°：NailArt 画布 y=0 为甲根、y 增大朝指尖，翻转后自由缘朝离开手腕的方向 */
function slotStyle(i) {
  const f = FINGERS[i]
  const rad = (f.angle * Math.PI) / 180
  const back = ROOT_CENTER_Y * f.s            // 画布 px，与 f.x/f.y 同一坐标系
  const ax = f.x - Math.sin(rad) * back       // 掌侧方向 = (-sinθ, +cosθ)
  const ay = f.y + Math.cos(rad) * back
  return {
    left: (ax / HAND_VB.w) * 100 + '%',
    top: (ay / HAND_VB.h) * 100 + '%',
    width: slotWidth(f) + 'px',
    transform: `translate(-50%, 0) rotate(${f.angle + 180}deg)`,
    transformOrigin: '50% 0'
  }
}

/** 甲缘遮罩几何（画布 px 单位）：以甲缘线中心为原点、rotate(angle+180) 后与甲片同向。
 *  局部 +y 朝指尖：甲片根缘中心在 y=0、根角在 y≈-3u，肤色新月从掌侧皮肤跨到甲面上 ~2u 处 */
function cuticleXform(f) {
  return `translate(${f.x} ${f.y}) rotate(${f.angle + 180})`
}
function cuticlePath(f) {
  const u = f.s
  const half = u * (NAIL_LAT_W / 2 + 0.2)  // 略宽于甲片最宽处，连根部两侧一起盖住
  const end = -2.5 * u                     // 新月两端（稍越过根角）
  const tipCtrl = 4.5 * u                  // 尖端侧控制点：中央压上甲面约 2u
  const palmCtrl = -9.5 * u                // 掌侧控制点：融入皮肤
  return `M ${-half} ${end} Q 0 ${tipCtrl} ${half} ${end} Q 0 ${palmCtrl} ${-half} ${end} Z`
}
/** 甲片破皮而出的交接线（细弧，画在肤色新月上） */
function foldLinePath(f) {
  const u = f.s
  const half = (u * (NAIL_LAT_W / 2) - 1) * 0.9
  return `M ${-half} ${-1.6 * u} Q 0 ${5.4 * u} ${half} ${-1.6 * u}`
}
</script>

<style scoped>
.hand-model {
  position: relative;
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  user-select: none;
}
.hand-img {
  display: block;
  width: 100%;
  height: auto;
  pointer-events: none;
}
.nail-slot {
  position: absolute;
  cursor: pointer;
  z-index: 2;
  background: transparent;
  border: none;
}
.nail-slot.active { z-index: 3; }
/* 拖拽悬停：极轻微提亮表示可投放，不改变甲片几何 */
.nail-slot.over .slot-inner { filter: brightness(1.07); }
.cuticle-mask {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 4;
  pointer-events: none;
}
</style>
