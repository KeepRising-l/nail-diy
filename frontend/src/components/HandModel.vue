<template>
  <!-- 手模视图：位图手部视觉（public/hands/hand-model.png，1278x1230）+ NailArt 甲片槽位对齐指尖。
       槽位锚点为各指本甲中心（HAND_VB 坐标系），随指轴旋转；尺寸随容器实测缩放，任何宽度下不错位 -->
  <div ref="rootRef" class="hand-model" role="group" aria-label="手模预览">
    <img class="hand-img" src="/hands/hand-model.png" alt="" aria-hidden="true" draggable="false" />

    <!-- 五指甲片槽位：由父组件渲染 NailArt -->
    <div v-for="(f, i) in FINGERS" :key="'slot' + i"
         class="nail-slot"
         :class="{ active: f.idx === activeFinger, over: over === i }"
         :style="slotStyle(i)"
         role="button"
         tabindex="0"
         :aria-label="`第 ${f.idx + 1} 指 ${FINGER_NAMES[f.idx]}`"
         @click="$emit('select', f.idx)"
         @keydown.enter.prevent="$emit('select', f.idx)"
         @dragover.prevent="over = i"
         @dragleave="over = -1"
         @drop.prevent="$emit('drop', f.idx, $event)">
      <div class="slot-inner">
        <slot :finger="fingerAt(i)" :index="f.idx" :width="slotWidth(f)" />
        <span class="slot-num">{{ FINGER_NAMES[f.idx] }}</span>
      </div>
    </div>

    <!-- 甲缘皮肤遮罩：盖在甲片根部之上，模拟甲根/甲沟的皮肤覆盖，产生"嵌入"感 -->
    <svg class="cuticle-mask" viewBox="0 0 1278 1230" aria-hidden="true">
      <defs>
        <filter id="hm-soft" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur stdDeviation="3" />
        </filter>
      </defs>
      <path v-for="(f, i) in FINGERS" :key="'cm' + i"
            :d="cuticlePath(f)"
            :transform="`translate(${f.x} ${f.y}) rotate(${f.angle + 180})`"
            fill="#F2C9B8" filter="url(#hm-soft)" />
    </svg>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  activeFinger: { type: Number, default: 0 },
  fingers: { type: Array, default: () => [] }
})
const emit = defineEmits(['select', 'drop'])

const over = ref(-1)
const FINGER_NAMES = ['小指', '无名指', '中指', '食指', '大拇指']

/** 位图坐标系（与 hand-model.png 的 1278x1230 一致）。
 *  idx —— fingers 数组索引；x/y —— 甲根中心（甲半月处，长度变化的固定锚点）；
 *  w —— 甲片画布宽；angle —— 指轴旋转角（度，右偏为正，逐指实测） */
const HAND_VB = { w: 1278, h: 1230 }
const FINGERS = [
  { idx: 0, x: 368, y: 420, w: 88,  angle: -13 }, // 小指
  { idx: 3, x: 537, y: 255, w: 98,  angle: -12 }, // 食指
  { idx: 2, x: 665, y: 262, w: 106, angle: -2 },  // 中指
  { idx: 1, x: 802, y: 295, w: 95,  angle: 14 },  // 无名指
  { idx: 4, x: 955, y: 760, w: 122, angle: 28 }   // 大拇指
]

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

function slotWidth(f) {
  return Math.round(f.w * scale.value * 10) / 10
}

/** 槽位定位：甲根线中心锚在 (x,y)，绕甲根旋转（transform-origin 50% 0）。
 *  长度变化只向指尖方向延伸，甲根位置不动；
 *  +180°：NailArt 画布 y=0 为甲根、y 增大朝指尖，翻转后自由缘朝离开手腕的方向 */
function slotStyle(i) {
  const f = FINGERS[i]
  return {
    left: (f.x / HAND_VB.w) * 100 + '%',
    top: (f.y / HAND_VB.h) * 100 + '%',
    width: slotWidth(f) + 'px',
    transform: `translate(-50%, 0) rotate(${f.angle + 180}deg)`,
    transformOrigin: '50% 0'
  }
}

/** 甲缘遮罩：以甲根为中心的新月形皮肤色块（局部坐标 +y 朝指尖），盖住甲片根缘 */
function cuticlePath(f) {
  const half = f.w * 0.47
  const dip = f.w * 0.17   // 外缘（朝指尖）下探深度
  const lift = f.w * 0.06  // 内缘（朝腕侧）上凸高度
  return `M ${-half} -2 Q 0 ${dip} ${half} -2 Q 0 ${-lift} ${-half} -2 Z`
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
}
.slot-inner {
  position: relative;
  transition: transform .16s ease;
}
.nail-slot:hover .slot-inner { transform: translateY(-2px); }
.nail-slot.active { z-index: 3; }
.nail-slot.over .slot-inner { filter: drop-shadow(0 0 3px rgba(229, 62, 140, 0.9)); }
.cuticle-mask {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 4;
  pointer-events: none;
}
.slot-num {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: var(--color-text-muted, #999);
  white-space: nowrap;
  pointer-events: none;
}
</style>
