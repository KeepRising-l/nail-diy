<template>
  <div class="page">
    <!-- 统计卡片：今日/累计 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :class="card.tone">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="block">
      <!-- 待履约订单：可直达订单中心 -->
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card stat-link" @click="$router.push('/merchant/orders')">
          <div class="stat-value warn">{{ overview.pendingFulfillment }}</div>
          <div class="stat-label">待履约订单（已支付待接单/制作/发货）</div>
        </el-card>
      </el-col>
      <!-- 平均评分 + 履约占比 -->
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ Number(overview.avgRating).toFixed(1) }}</div>
          <div class="stat-label">
            平均评分 · 自提 {{ overview.selfPickupCount }} 单 / 邮寄 {{ overview.expressCount }} 单
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card class="block">
      <template #header>快捷入口</template>
      <div class="quick-links">
        <el-button v-for="q in quickLinks" :key="q.path" size="large" plain @click="$router.push(q.path)">
          {{ q.label }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import request from '../../utils/request'

const overview = ref({ totalOrders: 0, totalSales: 0, pendingFulfillment: 0, avgRating: 0,
                       selfPickupCount: 0, expressCount: 0, ratingDist: [] })
const trend = ref([])

const today = computed(() => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
})
const todayPoint = computed(() => trend.value.find(r => r.date === today.value) || { amount: 0, orderCount: 0 })

const cards = computed(() => [
  { label: '今日订单数', value: todayPoint.value.orderCount },
  { label: '今日营业额（元）', value: Number(todayPoint.value.amount).toFixed(2) },
  { label: '累计订单数', value: overview.value.totalOrders },
  { label: '累计销售额（元）', value: Number(overview.value.totalSales).toFixed(2) }
])

const quickLinks = [
  { label: '订单中心', path: '/merchant/orders' },
  { label: '素材管理', path: '/merchant/materials' },
  { label: '模板管理', path: '/merchant/templates' },
  { label: '评价管理', path: '/merchant/reviews' },
  { label: '公告管理', path: '/merchant/notices' },
  { label: '门店管理', path: '/merchant/stores' },
  { label: '销售统计', path: '/merchant/analytics' }
]

onMounted(async () => {
  const [o, t] = await Promise.all([
    request.get('/merchant/analytics/overview'),
    request.get('/merchant/analytics/trend', { params: { days: 7 } })
  ])
  overview.value = o.data
  trend.value = t.data || []
})
</script>

<style scoped>
.page { min-height: 100vh; }
.block { margin-top: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 26px; font-weight: 700; color: #26221D; }
.stat-value.warn { color: #e6a23c; }
.stat-label { font-size: 13px; color: #8A8177; margin-top: 4px; }
.stat-link { cursor: pointer; transition: transform .15s; }
.stat-link:hover { transform: translateY(-2px); }
.quick-links { display: flex; flex-wrap: wrap; gap: 12px; }
</style>
