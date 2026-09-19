<template>
  <div class="page">
    <!-- 平台总览卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 平台销售趋势 -->
    <el-card class="block">
      <template #header>
        <div class="card-header">
          <span>平台销售趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="0">全部</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendRef" class="chart" v-loading="trendLoading"></div>
    </el-card>

    <el-row :gutter="16">
      <!-- 用户角色分布 -->
      <el-col :span="10">
        <el-card class="block">
          <template #header>用户角色分布</template>
          <div ref="roleRef" class="chart"></div>
        </el-card>
      </el-col>
      <!-- 热门模板 TOP5 -->
      <el-col :span="14">
        <el-card class="block">
          <template #header>热门款式模板 TOP5（按收藏数）</template>
          <div ref="hotRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const overview = ref({
  totalUsers: 0, customerCount: 0, merchantCount: 0, totalOrders: 0,
  totalSales: 0, pendingUgc: 0, roleDist: [], hotTemplates: []
})
const days = ref(7)
const trendLoading = ref(false)

const trendRef = ref(null)
const roleRef = ref(null)
const hotRef = ref(null)
const charts = {}

const ROLE_LABEL = { USER: '顾客', MERCHANT: '商家', ADMIN: '管理员' }

const cards = computed(() => [
  { label: `平台用户（其中商家 ${overview.value.merchantCount} 家）`, value: overview.value.totalUsers },
  { label: '累计有效订单', value: overview.value.totalOrders },
  { label: '平台销售额（元）', value: Number(overview.value.totalSales).toFixed(2) },
  { label: '待审核贴纸', value: overview.value.pendingUgc }
])

function chart(el, name) {
  if (!charts[name]) charts[name] = echarts.init(el)
  return charts[name]
}

async function loadOverview() {
  const res = await request.get('/admin/dashboard/overview')
  overview.value = res.data
  renderRoles()
  renderHotTemplates()
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const res = await request.get('/admin/dashboard/trend', { params: { days: days.value } })
    const rows = res.data || []
    chart(trendRef.value, 'trend').setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额（元）', '订单量'] },
      grid: { left: 50, right: 50, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: rows.map(r => r.date) },
      yAxis: [
        { type: 'value', name: '销售额' },
        { type: 'value', name: '订单量', minInterval: 1 }
      ],
      series: [
        { name: '销售额（元）', type: 'line', smooth: true, areaStyle: { opacity: 0.15 },
          data: rows.map(r => r.amount) },
        { name: '订单量', type: 'line', smooth: true, yAxisIndex: 1, data: rows.map(r => r.orderCount) }
      ]
    })
  } finally {
    trendLoading.value = false
  }
}

function renderRoles() {
  const dist = (overview.value.roleDist || [])
    .map(r => ({ name: ROLE_LABEL[r.name] || r.name, value: r.count }))
  chart(roleRef.value, 'role').setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      itemStyle: { borderRadius: 4 },
      data: dist
    }]
  })
}

function renderHotTemplates() {
  const rows = overview.value.hotTemplates || []
  chart(hotRef.value, 'hot').setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 30, top: 20, bottom: 10, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: rows.map(r => r.name).reverse() },
    series: [{ type: 'bar', barWidth: 18, itemStyle: { color: '#C8813C', borderRadius: 4 },
               data: rows.map(r => r.count).reverse() }]
  })
}

function onResize() {
  Object.values(charts).forEach(c => c.resize())
}

onMounted(async () => {
  await nextTick()
  await Promise.all([loadOverview(), loadTrend()])
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach(c => c.dispose())
})
</script>

<style scoped>
.page { min-height: 100vh; }
.stat-card { text-align: center; }
.stat-value { font-size: 26px; font-weight: 700; color: #26221D; }
.stat-label { font-size: 13px; color: #8A8177; margin-top: 4px; }
.block { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart { height: 300px; }
</style>
