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
 *  idx —— fingers 数组索引；x/y —— 本甲中心；w —— 甲片显示宽；angle —— 指轴旋转角（度，右偏为正） */
const HAND_VB = { w: 1278, h: 1230 }
const FINGERS = [
  { idx: 0, x: 331, y: 326, w: 70, angle: -14 }, // 小指
  { idx: 3, x: 543, y: 172, w: 78, angle: -8 },  // 食指
  { idx: 2, x: 722, y: 95,  w: 82, angle: -2 },  // 中指
  { idx: 1, x: 893, y: 219, w: 78, angle: 7 },   // 无名指
  { idx: 4, x: 1014, y: 674, w: 102, angle: 45 } // 大拇指
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

/** 槽位定位：中心锚在本甲中心，随指轴旋转 */
function slotStyle(i) {
  const f = FINGERS[i]
  return {
    left: (f.x / HAND_VB.w) * 100 + '%',
    top: (f.y / HAND_VB.h) * 100 + '%',
    width: slotWidth(f) + 'px',
    transform: `translate(-50%, -50%) rotate(${f.angle}deg)`
  }
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
.slot-num {
  position: absolute;
  bottom: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: var(--color-text-muted, #999);
  white-space: nowrap;
  pointer-events: none;
}
</style>
