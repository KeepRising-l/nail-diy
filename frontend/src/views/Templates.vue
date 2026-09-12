<template>
  <div class="gallery-page">
    <div class="container">
      <!-- 页头 -->
      <header class="page-head">
        <div class="head-copy">
          <span class="eyebrow-pill">精选款式</span>
          <h1 class="page-title">款式灵感库</h1>
          <p class="page-desc">挑选心仪的款式一键套用，或者作为起点自由修改，打造你的专属款。</p>
        </div>
      </header>

      <!-- 风格标签（极简 pill 筛选，非 check-tag） -->
      <div class="filter-row">
        <button v-for="tag in tags" :key="tag"
                class="filter-pill"
                :class="{ active: query.styleTag === tag }"
                @click="switchTag(tag)">
          {{ tag === '' ? '全部' : tag }}
        </button>
      </div>

      <!-- 卡片网格：与首页款式风格统一 -->
      <div class="grid" v-loading="loading">
        <router-link
          v-for="t in list" :key="t.id"
          :to="`/designer?templateId=${t.id}`"
          class="card hoverable"
        >
          <div class="cover">
            <img v-if="t.coverUrl" :src="t.coverUrl" :alt="t.name" />
            <div v-else class="placeholder">{{ t.name }}</div>
            <span class="use-overlay">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              套用此款
            </span>
          </div>
          <div class="meta">
            <div class="meta-left">
              <span class="tag" v-if="t.styleTag">{{ t.styleTag }}</span>
              <h3 class="name">{{ t.name }}</h3>
            </div>
            <span class="price">¥{{ t.price }}</span>
          </div>
        </router-link>
      </div>
      <div v-if="!loading && list.length === 0" class="empty">
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M12 2 6 5v6c0 5 3 8 6 10 3-2 6-5 6-10V5l-6-3Z"/></svg>
        <p>暂无该分类款式，试试其它标签吧。</p>
      </div>

      <!-- 分页：极简 -->
      <div class="pagination" v-if="total > query.size">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="prev, pager, next"
          :pager-count="5"
          background
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const tags = ['', '可爱', '轻奢', '法式', '日系', '简约', '复古', '甜美']
const query = reactive({ styleTag: '', page: 1, size: 12 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/templates', { params: query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function switchTag(tag) {
  query.styleTag = tag
  query.page = 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.gallery-page { min-height: 100vh; padding: var(--space-2xl) 0 var(--space-3xl); }

/* 页头 */
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  gap: 24px;
  flex-wrap: wrap;
}
.page-title {
  font-size: 48px;
  letter-spacing: -1px;
  color: var(--color-foreground);
  margin: 16px 0 12px;
}
.page-desc {
  color: var(--color-muted-foreground);
  font-size: 15px;
  line-height: 1.8;
  max-width: 480px;
  margin: 0;
}

/* 筛选行 */
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: var(--space-md) 0 var(--space-lg);
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--space-xl);
}
.filter-pill {
  padding: 7px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all .15s ease;
}
.filter-pill:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light-5);
}
.filter-pill.active {
  background: var(--color-foreground);
  color: #fff;
  border-color: var(--color-foreground);
}

/* 卡片网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
}
.card {
  position: relative;
  background: #fff;
  border-radius: var(--radius-md);
  padding: 10px;
  display: block;
  overflow: hidden;
  transition: all .25s ease;
}
.cover {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  aspect-ratio: 4 / 5;
  background: var(--color-canvas);
}
.cover img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform .3s ease;
}
.card:hover .cover img { transform: scale(1.04); }
.placeholder {
  width: 100%; height: 100%;
  display: grid; place-items: center;
  color: var(--color-muted-foreground);
  font-size: 14px;
}
.use-overlay {
  position: absolute;
  top: 14px; right: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(255,255,255,0.96);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: var(--color-foreground);
  opacity: 0;
  transform: translateY(-4px);
  transition: all .2s;
  pointer-events: none;
}
.card:hover .use-overlay { opacity: 1; transform: translateY(0); }

.meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 4px 4px;
}
.meta-left { flex: 1; min-width: 0; }
.tag {
  display: inline-block;
  font-family: var(--font-ui);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--color-primary);
  background: var(--color-primary-light-9);
  padding: 2px 10px;
  border-radius: var(--radius-full);
  margin-bottom: 6px;
}
.name {
  font-family: var(--font-heading);
  font-size: 18px;
  color: var(--color-foreground);
  margin: 0;
  font-weight: 600;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.price {
  font-family: var(--font-ui);
  font-weight: 700;
  color: var(--color-foreground);
  font-size: 15px;
  padding-top: 4px;
  flex-shrink: 0;
}

.empty {
  padding: var(--space-2xl) 0;
  text-align: center;
  color: var(--color-muted-foreground);
}
.empty svg { margin-bottom: 12px; opacity: 0.35; }

/* 分页：重置 Element Plus 样式 */
.pagination {
  margin-top: var(--space-xl);
  display: flex;
  justify-content: center;
}
.pagination :deep(.el-pagination) {
  --el-pagination-hover-color: var(--color-primary);
  font-family: var(--font-ui);
}
.pagination :deep(.el-pager li) {
  border-radius: 50%;
  min-width: 36px;
  height: 36px;
  line-height: 36px;
  font-weight: 600;
  font-size: 13px;
  background: #fff;
  border: 1px solid var(--color-border);
  margin: 0 4px;
}
.pagination :deep(.el-pager li.is-active) {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
  box-shadow: 0 3px 10px rgba(229, 62, 140, 0.25);
}
.pagination :deep(.btn-prev),
.pagination :deep(.btn-next) {
  border-radius: 50%;
  width: 36px !important;
  height: 36px !important;
  background: #fff;
  border: 1px solid var(--color-border);
}

/* 响应式 */
@media (max-width: 1100px) { .grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px)  {
  .grid { grid-template-columns: repeat(2, 1fr); }
  .page-title { font-size: 34px; }
}
</style>
