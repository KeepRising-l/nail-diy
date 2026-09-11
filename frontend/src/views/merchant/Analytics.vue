<template>
  <div class="page">
    <!-- ANA-REQ-1：销售总览卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ANA-REQ-2：销售趋势 -->
    <el-card class="block">
      <template #header>
        <div class="card-header">
          <span>销售趋势</span>
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
      <!-- ANA-REQ-4：履约方式占比 -->
      <el-col :span="10">
        <el-card class="block">
          <template #header>履约方式占比</template>
          <div ref="deliveryRef" class="chart"></div>
        </el-card>
      </el-col>
      <!-- ANA-REQ-5：评价分布 -->
      <el-col :span="14">
        <el-card class="block">
          <template #header>评价 1-5 星分布</template>
          <div ref="ratingRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ANA-REQ-3：设计偏好 TOP5 -->
    <el-card class="block">
      <template #header>设计偏好 TOP5（按购买数量）</template>
      <el-row :gutter="16">
        <el-col :span="8"><div ref="shapeRef" class="chart"></div></el-col>
        <el-col :span="8"><div ref="colorRef" class="chart"></div></el-col>
        <el-col :span="8"><div ref="patternRef" class="chart"></div></el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const overview = ref({ totalOrders: 0, totalSales: 0, pendingFulfillment: 0, avgRating: 0,
                       selfPickupCount: 0, expressCount: 0, ratingDist: [] })
const days = ref(7)
const trendLoading = ref(false)

const trendRef = ref(null)
const deliveryRef = ref(null)
const ratingRef = ref(null)
const shapeRef = ref(null)
const colorRef = ref(null)
const patternRef = ref(null)
const charts = {}

const cards = computed(() => [
  { label: '累计订单数', value: overview.value.totalOrders },
  { label: '累计销售额（元）', value: Number(overview.value.totalSales).toFixed(2) },
  { label: '待履约订单', value: overview.value.pendingFulfillment },
  { label: '平均评分', value: Number(overview.value.avgRating).toFixed(1) }
])

function chart(el, name) {
  if (!charts[name]) charts[name] = echarts.init(el)
  return charts[name]
}

async function loadOverview() {
  const res = await request.get('/merchant/analytics/overview')
  overview.value = res.data
  renderDelivery()
  renderRating()
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const res = await request.get('/merchant/analytics/trend', { params: { days: days.value } })
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

function renderDelivery() {
  const o = overview.value
  chart(deliveryRef.value, 'delivery').setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      data: [
        { name: '到店自提', value: o.selfPickupCount },
        { name: '商家邮寄', value: o.expressCount }
      ]
    }]
  })
}

function renderRating() {
  const dist = overview.value.ratingDist || []
  chart(ratingRef.value, 'rating').setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dist.map(d => d.name + ' 星') },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', barWidth: 36, itemStyle: { color: '#e6a23c', borderRadius: 6 },
               data: dist.map(d => d.count) }]
  })
}

function renderPrefs(list, el, name, color) {
  const rows = list || []
  chart(el, name).setOption({
    title: { text: name === 'shape' ? '甲型' : name === 'color' ? '色卡' : '图案', left: 'center',
             textStyle: { fontSize: 13, color: '#4A443E' } },
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 30, top: 36, bottom: 10, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: rows.map(r => r.name).reverse() },
    series: [{ type: 'bar', barWidth: 16, itemStyle: { color, borderRadius: 4 },
               data: rows.map(r => r.count).reverse() }]
  })
}

async function loadPrefs() {
  const res = await request.get('/merchant/analytics/design-prefs')
  const d = res.data || {}
  renderPrefs(d.topShapes, shapeRef.value, 'shape', '#C8813C')
  renderPrefs(d.topColors, colorRef.value, 'color', '#f56c6c')
  renderPrefs(d.topPatterns, patternRef.value, 'pattern', '#67c23a')
}

function onResize() {
  Object.values(charts).forEach(c => c.resize())
}

onMounted(async () => {
  await nextTick()
  await Promise.all([loadOverview(), loadTrend(), loadPrefs()])
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
