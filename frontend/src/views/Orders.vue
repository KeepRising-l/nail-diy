<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的订单</span>
          <el-button @click="$router.push('/addresses')">地址簿</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in TABS" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

      <div v-loading="loading">
        <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
        <div v-for="o in orders" :key="o.order.id" class="order-card">
          <div class="order-head">
            <span class="order-no">订单号：{{ o.order.orderNo }}</span>
            <el-tag :type="statusTagType(o.order.status)">{{ statusLabel(o.order.status) }}</el-tag>
          </div>

          <div v-for="it in o.items" :key="it.id" class="item-row">
            <el-image v-if="it.previewUrl" :src="it.previewUrl" fit="contain" class="thumb" />
            <div v-else class="thumb thumb-empty">无快照</div>
            <div class="item-info">
              <div class="item-name">{{ it.designName }}</div>
              <div class="item-sub">￥{{ it.price }} × {{ it.quantity }}</div>
            </div>
          </div>

          <div class="order-meta">
            <span v-if="o.order.deliveryType === 'SELF_PICKUP'">
              到店自提<template v-if="o.store"> · {{ o.store.name }}（{{ o.store.address }}）</template>
              <el-tag v-if="o.order.status === 'WAIT_PICKUP'" type="warning" class="pickup-tag">
                自提码：{{ o.order.pickupCode }}
              </el-tag>
            </span>
            <span v-else>
              商家邮寄 · {{ o.order.receiverName }} {{ o.order.receiverAddr }}
              <span v-if="o.order.expressNo" class="express">物流：{{ o.order.expressCompany }} {{ o.order.expressNo }}</span>
            </span>
            <span>商品 ￥{{ o.order.goodsAmount }} + 运费 ￥{{ o.order.freight }}</span>
          </div>

          <!-- 状态时间线 -->
          <el-collapse v-if="o.logs && o.logs.length" class="timeline-collapse">
            <el-collapse-item title="订单进度">
              <el-timeline>
                <el-timeline-item v-for="l in o.logs" :key="l.id" :timestamp="l.createTime" size="small">
                  {{ statusLabel(l.toStatus) }}
                  <span class="log-op">{{ l.operator }} · {{ l.remark }}</span>
                </el-timeline-item>
              </el-timeline>
            </el-collapse-item>
          </el-collapse>

          <!-- 已评价展示 -->
          <div v-if="o.review" class="review-box">
            <div class="review-head">
              <el-rate :model-value="o.review.rating" disabled size="small" />
              <el-tag size="small" type="info">已评价</el-tag>
            </div>
            <div v-if="o.review.content" class="review-content">{{ o.review.content }}</div>
            <div v-if="o.review.images && o.review.images.length" class="review-images">
              <el-image v-for="(img, i) in o.review.images" :key="i" :src="img" fit="cover" class="review-img"
                        :preview-src-list="o.review.images" :initial-index="i" preview-teleported />
            </div>
            <div v-if="o.review.reply" class="review-reply">商家回复：{{ o.review.reply }}</div>
          </div>

          <div class="order-foot">
            <span class="total">合计：<span class="amount">￥{{ o.order.totalAmount }}</span></span>
            <div>
              <el-button v-if="o.order.status === 'PENDING_PAY'" type="primary"
                         @click="confirmPay(o)">立即支付</el-button>
              <el-button v-if="(o.order.status === 'VERIFIED' || o.order.status === 'RECEIVED') && !o.review"
                         type="warning" @click="openReview(o)">评价</el-button>
              <el-popconfirm v-if="o.order.status === 'SHIPPED'" title="确认已收到商品？"
                             @confirm="receive(o)">
                <template #reference>
                  <el-button type="success">确认收货</el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm v-if="o.order.status === 'PENDING_PAY' || o.order.status === 'PAID'"
                             title="确定取消该订单？" @confirm="cancelOrder(o)">
                <template #reference>
                  <el-button type="danger" plain>取消订单</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>

        <el-pagination v-if="total > 0" layout="prev, pager, next" :total="total" :page-size="10"
                       :current-page="page" @current-change="onPageChange" class="pager" />
      </div>
    </el-card>
    <!-- 评价弹窗：评分 + 文字 + 晒图（≤3 张） -->
    <el-dialog v-model="showReview" title="评价晒图" width="460px">
      <el-form label-width="70px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="reviewForm.content" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="说说这次美甲体验吧（选填）" />
        </el-form-item>
        <el-form-item label="晒图">
          <el-upload list-type="picture-card" :file-list="[]" :show-file-list="false"
                     :limit="3" :http-request="uploadReviewImage" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">最多 3 张，已传 {{ reviewForm.images.length }}/3</div>
        </el-form-item>
      </el-form>
      <div v-if="reviewForm.images.length" class="review-images">
        <el-image v-for="(img, i) in reviewForm.images" :key="i" :src="img" fit="cover" class="review-img" />
      </div>
      <template #footer>
        <el-button @click="showReview = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">发表评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../utils/request'

const TABS = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING_PAY' },
  { label: '待接单', value: 'PAID' },
  { label: '制作中', value: 'PRODUCING' },
  { label: '待自提', value: 'WAIT_PICKUP' },
  { label: '待发货', value: 'WAIT_SHIP' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已签收', value: 'RECEIVED' },
  { label: '已核销', value: 'VERIFIED' },
  { label: '已取消', value: 'CANCELLED' }
]

const STATUS_MAP = {
  PENDING_PAY: '待支付',
  PAID: '待接单',
  PRODUCING: '制作中',
  WAIT_PICKUP: '待自提',
  VERIFIED: '已核销',
  WAIT_SHIP: '待发货',
  SHIPPED: '已发货',
  RECEIVED: '已签收',
  CANCELLED: '已取消'
}
const TAG_MAP = {
  PENDING_PAY: 'warning',
  PAID: 'primary',
  PRODUCING: 'primary',
  WAIT_PICKUP: 'success',
  VERIFIED: 'info',
  WAIT_SHIP: 'warning',
  SHIPPED: 'primary',
  RECEIVED: 'success',
  CANCELLED: 'info'
}

const statusLabel = (s) => STATUS_MAP[s] || s
const statusTagType = (s) => TAG_MAP[s] || 'info'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const activeTab = ref('')
const loading = ref(false)

async function loadOrders() {
  loading.value = true
  try {
    const res = await request.get('/orders', {
      params: { page: page.value, size: 10, status: activeTab.value || undefined }
    })
    orders.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  loadOrders()
}

function onPageChange(p) {
  page.value = p
  loadOrders()
}

async function confirmPay(o) {
  try {
    await ElMessageBox.confirm(
      `确认支付 ￥${o.order.totalAmount} ？（模拟支付，点击确认即完成）`,
      '模拟支付',
      { confirmButtonText: '确认支付', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  await request.post(`/orders/${o.order.id}/pay`)
  ElMessage.success('支付成功，等待商家接单')
  loadOrders()
}

async function cancelOrder(o) {
  await request.post(`/orders/${o.order.id}/cancel`)
  ElMessage.success('订单已取消')
  loadOrders()
}

async function receive(o) {
  await request.post(`/orders/${o.order.id}/receive`)
  ElMessage.success('已确认收货')
  loadOrders()
}

// ---- 评价 ----
const showReview = ref(false)
const reviewing = ref(false)
const reviewTarget = ref(null)
const reviewForm = reactive({ rating: 5, content: '', images: [] })

function openReview(o) {
  reviewTarget.value = o
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewForm.images = []
  showReview.value = true
}

// 自定义上传：走 axios 拦截器自动带 token
async function uploadReviewImage(options) {
  if (reviewForm.images.length >= 3) {
    ElMessage.warning('晒图最多 3 张')
    return
  }
  const fd = new FormData()
  fd.append('file', options.file)
  const res = await request.post('/reviews/images', fd)
  reviewForm.images.push(res.data)
}

async function submitReview() {
  reviewing.value = true
  try {
    await request.post('/reviews', {
      orderId: reviewTarget.value.order.id,
      rating: reviewForm.rating,
      content: reviewForm.content,
      images: reviewForm.images
    })
    ElMessage.success('感谢评价！')
    showReview.value = false
    loadOrders()
  } finally {
    reviewing.value = false
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.order-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px 16px; margin-bottom: 14px; }
.order-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.order-no { font-size: 13px; color: #8A8177; }
.item-row { display: flex; align-items: center; gap: 10px; padding: 6px 0; }
.thumb { width: 48px; height: 48px; border-radius: 6px; background: #F1EDE9; flex-shrink: 0; }
.thumb-empty { display: flex; align-items: center; justify-content: center; color: #c0c4cc;
               font-size: 11px; border: 1px dashed #e4e7ed; }
.item-name { font-weight: 600; }
.item-sub { font-size: 12px; color: #8A8177; }
.order-meta { display: flex; justify-content: space-between; font-size: 12px; color: #8A8177;
              border-top: 1px dashed #E6DED2; padding-top: 8px; margin-top: 6px; flex-wrap: wrap; gap: 8px; }
.pickup-tag { margin-left: 8px; }
.express { margin-left: 8px; color: #C8813C; }
.timeline-collapse { margin-top: 6px; }
.log-op { color: #8A8177; font-size: 12px; margin-left: 6px; }
.review-box { background: #F5EFE6; border-radius: 8px; padding: 10px 12px; margin-top: 8px; }
.review-head { display: flex; align-items: center; gap: 10px; }
.review-content { font-size: 13px; margin-top: 4px; }
.review-images { display: flex; gap: 8px; margin-top: 6px; }
.review-img { width: 64px; height: 64px; border-radius: 6px; background: #fff; }
.review-reply { font-size: 12px; color: #e6a23c; margin-top: 6px; }
.upload-tip { font-size: 12px; color: #8A8177; margin-left: 10px; }
.order-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.amount { color: #e6a23c; font-size: 18px; font-weight: 700; }
.pager { margin-top: 12px; justify-content: center; }
</style>
