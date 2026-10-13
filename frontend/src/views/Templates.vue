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
                :class="{ active: !favMode && query.styleTag === tag }"
                @click="switchTag(tag)">
          {{ tag === '' ? '全部' : tag }}
        </button>
        <button class="filter-pill fav-pill"
                :class="{ active: favMode }"
                @click="switchFav">
          <svg width="12" height="12" viewBox="0 0 24 24" :fill="favMode ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2.2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          我的收藏
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
            <button class="fav-btn" :class="{ on: isFav(t.id) }" title="收藏"
                    @click.stop.prevent="toggleFav(t)">
              <svg width="15" height="15" viewBox="0 0 24 24" :fill="isFav(t.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2.2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            </button>
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
        <p>{{ favMode ? '还没有收藏的款式，点亮卡片左上角的红心试试吧。' : '暂无该分类款式，试试其它标签吧。' }}</p>
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
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const tags = ['', '可爱', '轻奢', '法式', '日系', '简约', '复古', '甜美']
const query = reactive({ styleTag: '', page: 1, size: 12 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
// 收藏：favMode 时列表来自 /favorites，红心集合驱动卡片标记
const favMode = ref(false)
const favIds = ref(new Set())

function isFav(id) { return favIds.value.has(id) }

async function loadList() {
  loading.value = true
  try {
    const url = favMode.value ? '/favorites' : '/templates'
    const res = await request.get(url, { params: query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadFavIds() {
  // 游客不请求收藏接口：该接口需登录，401 会被 request 拦截器强制跳登录页
  if (!auth.token) {
    favIds.value = new Set()
    return
  }
  try {
    const res = await request.get('/favorites/ids')
    favIds.value = new Set(res.data || [])
  } catch (e) { favIds.value = new Set() }
}

async function toggleFav(t) {
  if (!auth.token) {
    ElMessage.warning('请先登录后再收藏')
    router.push({ path: '/login', query: { redirect: '/templates' } })
    return
  }
  try {
    const res = await request.post(`/favorites/${t.id}/toggle`)
    if (res.data) {
      favIds.value = new Set([...favIds.value, t.id])
      ElMessage.success(`已收藏「${t.name}」`)
    } else {
      const next = new Set(favIds.value)
      next.delete(t.id)
      favIds.value = next
      ElMessage.info('已取消收藏')
      if (favMode.value) {
        // 收藏页里取消后从列表移除
        list.value = list.value.filter(item => item.id !== t.id)
        total.value = Math.max(0, total.value - 1)
      }
    }
  } catch (e) { /* 401 已由 request 拦截器统一处理 */ }
}

function switchTag(tag) {
  favMode.value = false
  query.styleTag = tag
  query.page = 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadList()
}

function switchFav() {
  // 收藏列表来自需登录接口，游客先引导登录
  if (!favMode.value && !auth.token) {
    ElMessage.warning('请先登录后查看收藏')
    router.push({ path: '/login', query: { redirect: '/templates' } })
    return
  }
  favMode.value = !favMode.value
  query.styleTag = ''
  query.page = 1
  loadList()
}

onMounted(() => {
  loadList()
  loadFavIds()
})
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
.fav-pill { display: inline-flex; align-items: center; gap: 5px; }
.fav-pill.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

/* 收藏红心：封面左上角，收藏后常亮 */
.fav-btn {
  position: absolute;
  top: 14px; left: 14px;
  width: 30px; height: 30px;
  display: grid; place-items: center;
  background: rgba(255,255,255,0.94);
  border: none;
  border-radius: 50%;
  color: var(--color-muted-foreground);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.10);
  transition: all .15s ease;
  z-index: 1;
}
.fav-btn:hover { transform: scale(1.1); color: #E5527E; }
.fav-btn.on { color: #E5527E; }

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
