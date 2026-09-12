<template>
  <!-- 矢量手模：肤色参数化，五指甲片槽位由父组件通过作用域插槽填充（NailArt） -->
  <div class="hand-model" :class="'skin-' + skinTone" role="group" aria-label="手模预览">
    <svg class="hand-bg" viewBox="0 0 320 540" width="100%" aria-hidden="true">
      <!-- 肤色浅层渐变（扁平插画风，仅用肤色做明暗） -->
      <defs>
        <linearGradient id="hm-shade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" :stop-color="skinLight" />
          <stop offset="1" :stop-color="skinDeep" />
        </linearGradient>
      </defs>

      <!-- 五根手指（胶囊形，自甲根向下延伸，顶端被甲片覆盖） -->
      <g>
        <rect v-for="(f, i) in FINGERS" :key="'fi' + i"
              :x="f.x" :y="f.rootY" :width="f.w" :height="f.len" :rx="f.w / 2"
              fill="url(#hm-shade)" />
      </g>

      <!-- 手掌（覆盖手指根部，形成整体） -->
      <path :d="palmPath" fill="url(#hm-shade)" />

      <!-- 手腕衔接 -->
      <rect x="120" y="480" width="80" height="60" rx="18" fill="url(#hm-shade)" />
    </svg>

    <!-- 五指甲片槽位：由父组件渲染 NailArt，位置/角度由 FINGERS 常量控制 -->
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
      <slot :finger="fingerAt(i)" :index="f.idx" :width="f.w" />
      <span class="slot-num">{{ FINGER_NAMES[f.idx] }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  skinTone: { type: String, default: 'medium' }, // fair / medium / tan / deep
  activeFinger: { type: Number, default: 0 },
  fingers: { type: Array, default: () => [] }
})
const emit = defineEmits(['select', 'drop'])

const over = ref(-1)

const FINGER_NAMES = ['小指', '无名指', '中指', '食指', '大拇指']

// 五指几何（320x540 viewBox 坐标系）：甲根中心 x、甲根 y、甲片宽、手指长度、旋转角度
// 视觉从左到右：大拇指(侧) → 食指 → 中指 → 无名指 → 小指，对应 fingers 数组倒序索引
const FINGERS = [
  { idx: 4, x: 42,  rootY: 210, w: 52, len: 130, rotate: -16 }, // 大拇指
  { idx: 3, x: 110, rootY: 150, w: 44, len: 185, rotate: -5 },  // 食指
  { idx: 2, x: 164, rootY: 120, w: 44, len: 215, rotate: 0 },   // 中指（最长）
  { idx: 1, x: 218, rootY: 150, w: 44, len: 185, rotate: 5 },   // 无名指
  { idx: 0, x: 268, rootY: 195, w: 38, len: 140, rotate: 13 }   // 小指
]

const SKIN = {
  fair:   { light: '#F7E0CC', deep: '#E4BE9E' },
  medium: { light: '#E8C6A2', deep: '#C99B6F' },
  tan:    { light: '#C8935C', deep: '#A4713B' },
  deep:   { light: '#93613A', deep: '#6F4424' }
}
const skin = computed(() => SKIN[props.skinTone] || SKIN.medium)
const skinLight = computed(() => skin.value.light)
const skinDeep = computed(() => skin.value.deep)

// 手掌轮廓：包裹五根手指根部，向下收窄到手腕
const palmPath = 'M 60 300 '
  + 'C 40 340 48 400 70 450 '
  + 'C 90 495 120 505 160 505 '
  + 'C 200 505 230 495 250 450 '
  + 'C 272 400 280 340 260 300 '
  + 'C 250 330 220 350 160 350 '
  + 'C 100 350 70 330 60 300 Z'

/** 单指数据（按视觉槽位 i 映射到 fingers 数组索引） */
function fingerAt(i) {
  const idx = FINGERS[i].idx
  return props.fingers[idx] || null
}

/** 甲片槽位定位：NailArt 宽 w、高 w/60*84，底部对齐甲根，整体旋转 */
function slotStyle(i) {
  const f = FINGERS[i]
  const w = f.w
  const h = (w / 60) * 84
  const left = (f.x - w / 2) / 320 * 100
  const top = (f.rootY - h) / 540 * 100
  return {
    left: left + '%',
    top: top + '%',
    width: w + 'px',
    transform: `rotate(${f.rotate}deg)`
  }
}
</script>

<style scoped>
.hand-model {
  position: relative;
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
  aspect-ratio: 320 / 540;
}
.hand-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.nail-slot {
  position: absolute;
  cursor: pointer;
  transform-origin: 50% 100%;
  transition: transform .16s ease;
  z-index: 2;
}
.nail-slot:hover { transform: translateY(-2px); }
.nail-slot.active { z-index: 3; }
.nail-slot.over { box-shadow: 0 0 0 3px rgba(229, 62, 140, 0.55); }
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
