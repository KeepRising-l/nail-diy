<template>
  <!--
    素材缩略图：按「有没有图」决定渲染方式，避免空 imageUrl 触发 el-image 的 error 插槽
    1) 图片素材（图案/贴纸/立体装饰）→ el-image
    2) 色卡 → 纯色块
    3) 甲型 → 用 engine/contours.js 现画 SVG 轮廓（与设计台同一份几何）
    4) 其余代码渲染素材（参数化款式/矢量装饰/甲油效果）→ 「代码渲染」标签
  -->
  <div class="thumb" :style="{ width: size + 'px', height: size + 'px' }">
    <el-image v-if="material.imageUrl" :src="material.imageUrl" fit="contain" class="thumb-img" />
    <div v-else-if="material.type === 'COLOR'" class="color-block"
         :style="{ background: material.colorCode || '#FFFFFF' }"
         :title="material.colorCode || '未设置颜色'" />
    <svg v-else-if="material.type === 'SHAPE'" class="shape-svg" viewBox="4 0 56 92"
         preserveAspectRatio="xMidYMid meet" role="img" :aria-label="`甲型 ${material.name}`">
      <path :d="shapePath" class="shape-fill" />
      <path :d="shapePath" class="shape-line" />
    </svg>
    <span v-else class="code-tag" :title="`${material.name} 为代码渲染素材，无预览图`">代码渲染</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { shapePathOf } from '../engine/contours'

const props = defineProps({
  /** 素材记录（material 表一行） */
  material: { type: Object, required: true },
  /** 缩略图边长（px） */
  size: { type: Number, default: 60 }
})

// 甲型统一按标准长度（系数 1）出图，保证 8 种甲型在列表里横向可比
const shapePath = computed(() => shapePathOf(props.material, 1))
</script>

<style scoped>
.thumb { display: flex; align-items: center; justify-content: center; }
.thumb-img { width: 100%; height: 100%; }
.color-block { width: 100%; height: 100%; border-radius: 6px; border: 1px solid var(--color-border); }
.shape-svg { width: 34px; height: 56px; }
.shape-fill { fill: var(--color-primary-light-9); }
.shape-line { fill: none; stroke: var(--color-primary); stroke-width: 2.2; stroke-linejoin: round; }
.code-tag {
  font-size: 11px;
  line-height: 1.4;
  text-align: center;
  padding: 3px 4px;
  color: var(--color-muted-foreground);
  background: var(--color-muted);
  border-radius: 6px;
}
</style>
