<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h1>售后管理</h1>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-for="t in TABS" :key="t.value" :label="t.label" :name="t.value" />
      </el-tabs>

      <div v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" description="暂无售后申请" />

        <div v-for="row in list" :key="row.afterSale.id" class="sale-card">
          <div class="sale-head">
            <span class="sale-no">
              订单 {{ row.order?.orderNo || '—' }} · 申请于 {{ row.afterSale.createTime }}
            </span>
            <el-tag :type="statusTagType(row.afterSale.status)">{{ statusLabel(row.afterSale.status) }}</el-tag>
          </div>

          <div class="sale-body">
            <div class="row">
              <span class="label">顾客</span>
              <span>{{ row.userNickname || '—' }}<span class="muted">（{{ row.userPhone || '—' }}）</span></span>
            </div>
            <div class="row">
              <span class="label">售后类型</span>
              <span>{{ typeLabel(row.afterSale.type) }}</span>
            </div>
            <div class="row">
              <span class="label">申请原因</span>
              <span>{{ row.afterSale.reason }}</span>
            </div>
            <div v-if="row.afterSale.description" class="row">
              <span class="label">补充说明</span>
              <span>{{ row.afterSale.description }}</span>
            </div>
            <div v-if="imagesOf(row.afterSale).length" class="row">
              <span class="label">凭证图</span>
              <span class="imgs">
                <el-image v-for="(u, i) in imagesOf(row.afterSale)" :key="i" :src="u" fit="cover"
                          class="proof" :preview-src-list="imagesOf(row.afterSale)" preview-teleported />
              </span>
            </div>
            <div class="row">
              <span class="label">退款金额</span>
              <span class="amount">￥{{ row.afterSale.amount }}</span>
            </div>
            <div v-if="row.afterSale.status === 'REJECTED'" class="row">
              <span class="label">拒绝原因</span>
              <span class="reject">{{ row.afterSale.rejectReason }}</span>
            </div>
            <div v-if="row.afterSale.handleTime" class="row">
              <span class="label">处理时间</span>
              <span>{{ row.afterSale.handleTime }}</span>
            </div>
          </div>

          <div v-if="row.afterSale.status === 'PENDING'" class="sale-foot">
            <el-button type="primary" @click="handleApprove(row)">同意退款</el-button>
            <el-button type="danger" plain @click="openReject(row)">拒绝</el-button>
          </div>
        </div>

        <el-pagination v-if="total > 0" layout="prev, pager, next" :total="total" :page-size="10"
                       :current-page="page" @current-change="onPageChange" class="pager" />
      </div>
    </el-card>

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="showReject" title="拒绝售后申请" width="440px">
      <p class="dialog-tip">拒绝原因会推送给顾客，请说明清楚。</p>
      <el-input v-model="rejectReason" type="textarea" :rows="3" maxlength="200" show-word-limit
                placeholder="例如：已按设计参数制作完成，不存在质量问题" />
      <template #footer>
        <el-button @click="showReject = false">取消</el-button>
        <el-button type="danger" @click="doReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const TABS = [
  { label: '待处理', value: 'PENDING' },
  { label: '已退款', value: 'COMPLETED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '全部', value: '' }
]
const STATUS_MAP = { PENDING: '待处理', COMPLETED: '已退款', REJECTED: '已拒绝' }
const TAG_MAP = { PENDING: 'warning', COMPLETED: 'success', REJECTED: 'info' }
const TYPE_MAP = { REFUND_ONLY: '仅退款', RETURN_REFUND: '退货退款' }

const statusLabel = (s) => STATUS_MAP[s] || s
const statusTagType = (s) => TAG_MAP[s] || 'info'
const typeLabel = (t) => TYPE_MAP[t] || t

const list = ref([])
const total = ref(0)
const page = ref(1)
const activeTab = ref('PENDING')
const loading = ref(false)

const showReject = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)

/** 凭证图：后端存 JSON 数组字符串，容错解析 */
function imagesOf(sale) {
  try {
    return sale.images ? JSON.parse(sale.images) : []
  } catch {
    return []
  }
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/merchant/after-sales', {
      params: { page: page.value, size: 10, status: activeTab.value || undefined }
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(
      `确认同意退款 ￥${row.afterSale.amount}？退款将标记为已完成，并通知顾客。`,
      '同意退款',
      { confirmButtonText: '确认退款', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }
  await request.post(`/merchant/after-sales/${row.afterSale.id}/handle`, { approved: true })
  ElMessage.success('已同意退款')
  load()
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  showReject.value = true
}

async function doReject() {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  await request.post(`/merchant/after-sales/${rejectTarget.value.afterSale.id}/handle`, {
    approved: false,
    rejectReason: reason
  })
  ElMessage.success('已拒绝该申请')
  showReject.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.page { min-height: 100vh; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.sale-card { border: 1px solid var(--color-border); border-radius: 8px; padding: 12px 16px; margin-bottom: 14px; }
.sale-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.sale-no { font-size: 13px; color: var(--color-muted-foreground); }
.sale-body { display: flex; flex-direction: column; gap: 6px; font-size: 13px; }
.row { display: flex; gap: 10px; align-items: flex-start; }
.label { flex: 0 0 68px; color: var(--color-muted-foreground); }
.muted { color: var(--color-muted-foreground); }
.amount { color: var(--color-destructive); font-weight: 600; }
.reject { color: var(--color-destructive); }
.imgs { display: flex; gap: 8px; flex-wrap: wrap; }
.proof { width: 64px; height: 64px; border-radius: 6px; border: 1px solid var(--color-border); }
.sale-foot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px;
             border-top: 1px dashed var(--color-border); padding-top: 10px; }
.dialog-tip { font-size: 13px; color: var(--color-muted-foreground); margin: 0 0 10px; }
.pager { margin-top: 12px; justify-content: center; }
</style>
