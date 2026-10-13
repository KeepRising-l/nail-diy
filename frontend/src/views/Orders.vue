<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h1>我的订单</h1>
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
            <span class="head-tags">
              <el-tag v-if="o.afterSaleStatus" :type="afterSaleTagType(o.afterSaleStatus)" effect="plain">
                售后 · {{ afterSaleLabel(o.afterSaleStatus) }}
              </el-tag>
              <el-tag :type="statusTagType(o.order.status)">{{ statusLabel(o.order.status) }}</el-tag>
            </span>
          </div>

          <div v-for="it in o.items" :key="it.id" class="item-row" @click="openDesignDetail(it)">
            <el-image v-if="it.previewUrl" :src="it.previewUrl" fit="contain" class="thumb" />
            <div v-else class="thumb thumb-empty">无快照</div>
            <div class="item-info">
              <div class="item-name">{{ it.designName }}</div>
              <div class="item-sub">￥{{ it.price }} × {{ it.quantity }}</div>
            </div>
            <span class="detail-link">设计详情 ›</span>
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
              <el-button v-if="canRebuy(o)" plain @click="buyAgain(o)">再来一副</el-button>
              <el-button v-if="canAfterSale(o)" plain @click="openAfterSale(o)">申请售后</el-button>
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

    <!-- 申请售后：类型 + 原因 + 说明 + 凭证图（退款金额由后端按订单实付计算） -->
    <el-dialog v-model="showAfterSale" title="申请售后" width="480px">
      <el-alert type="info" :closable="false" class="sale-alert"
                :title="`退款金额：￥${afterSaleTarget?.order.totalAmount ?? '—'}（按订单实付金额）`" />
      <el-form label-width="80px">
        <el-form-item label="售后类型">
          <el-radio-group v-model="afterSaleForm.type">
            <el-radio value="REFUND_ONLY">仅退款</el-radio>
            <el-radio value="RETURN_REFUND">退货退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-select v-model="afterSaleForm.reason" placeholder="请选择原因" style="width: 100%">
            <el-option v-for="r in REASONS" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="afterSaleForm.description" type="textarea" :rows="3" maxlength="500"
                    show-word-limit placeholder="详细描述遇到的问题（选填）" />
        </el-form-item>
        <el-form-item label="凭证图">
          <el-upload list-type="picture-card" :file-list="[]" :show-file-list="false"
                     :limit="3" :http-request="uploadAfterSaleImage" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">最多 3 张，已传 {{ afterSaleForm.images.length }}/3</div>
        </el-form-item>
      </el-form>
      <div v-if="afterSaleForm.images.length" class="review-images">
        <el-image v-for="(img, i) in afterSaleForm.images" :key="i" :src="img" fit="cover" class="review-img" />
      </div>
      <template #footer>
        <el-button @click="showAfterSale = false">取消</el-button>
        <el-button type="primary" :loading="applyingSale" @click="submitAfterSale">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 设计详情：所见即所得（下单时刻的预览图 + 参数清单） -->
    <el-dialog v-model="showDetail" title="设计详情 · 所见即所得" width="520px">
      <template v-if="detail">
        <div class="detail-hero">
          <el-image v-if="detail.previewUrl" :src="detail.previewUrl" fit="contain" class="detail-img"
                    :preview-src-list="[detail.previewUrl]" preview-teleported />
          <div v-else class="detail-img detail-img-empty">无快照</div>
        </div>
        <p class="detail-assure">
          <el-icon><CircleCheckFilled /></el-icon>
          按此预览与配置手工制作，发货前会再比对一遍
        </p>
        <div class="detail-name">{{ detail.name }}</div>
        <div v-for="(f, i) in detail.fingers" :key="i" class="detail-finger">
          <span class="df-name">{{ f.name }}</span>
          <span class="df-chip" :style="{ background: f.colorCode }"></span>
          <span class="df-text">{{ f.shape }} · {{ f.lenLabel }} · {{ f.colorName }}</span>
          <span v-if="f.patterns.length" class="df-pat">贴花：{{ f.patterns.join(' / ') }}</span>
          <span v-if="f.extras.length" class="df-extra">{{ f.extras.join(' · ') }}</span>
        </div>
        <p v-if="!detail.fingers.length" class="detail-empty">该订单未包含参数快照（旧数据），以预览图为准</p>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useRouter, useRoute } from 'vue-router'

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

// ---- 售后申请 ----
const REASONS = ['尺寸不合适', '色差明显', '做工瑕疵', '运输破损', '与设计不符', '不想要了', '其他原因']
const AFTER_SALE_MAP = { PENDING: '待商家处理', COMPLETED: '已退款', REJECTED: '已拒绝' }
const AFTER_SALE_TAG = { PENDING: 'warning', COMPLETED: 'success', REJECTED: 'info' }

const afterSaleLabel = (s) => AFTER_SALE_MAP[s] || s
const afterSaleTagType = (s) => AFTER_SALE_TAG[s] || 'info'

const showAfterSale = ref(false)
const applyingSale = ref(false)
const afterSaleTarget = ref(null)
const afterSaleForm = reactive({ type: 'REFUND_ONLY', reason: '', description: '', images: [] })

/** 已完成（已核销 / 已签收）且未申请过售后的订单才可申请 */
function canAfterSale(o) {
  const done = o.order.status === 'VERIFIED' || o.order.status === 'RECEIVED'
  return done && !o.afterSaleStatus
}

function openAfterSale(o) {
  afterSaleTarget.value = o
  afterSaleForm.type = 'REFUND_ONLY'
  afterSaleForm.reason = ''
  afterSaleForm.description = ''
  afterSaleForm.images = []
  showAfterSale.value = true
}

// 凭证图复用通用的图片上传接口（与晒图同一套规则：jpg/png/webp ≤5MB）
async function uploadAfterSaleImage(options) {
  if (afterSaleForm.images.length >= 3) {
    ElMessage.warning('凭证图最多 3 张')
    return
  }
  const fd = new FormData()
  fd.append('file', options.file)
  const res = await request.post('/reviews/images', fd)
  afterSaleForm.images.push(res.data)
}

async function submitAfterSale() {
  if (!afterSaleForm.reason) {
    ElMessage.warning('请选择申请原因')
    return
  }
  applyingSale.value = true
  try {
    await request.post('/after-sales', {
      orderId: afterSaleTarget.value.order.id,
      type: afterSaleForm.type,
      reason: afterSaleForm.reason,
      description: afterSaleForm.description,
      images: afterSaleForm.images
    })
    ElMessage.success('售后申请已提交，请等待商家处理')
    showAfterSale.value = false
    loadOrders()
  } finally {
    applyingSale.value = false
  }
}

// ---- 设计详情（所见即所得）：点击订单内商品行，展示下单时刻的预览图与参数清单 ----
const router = useRouter()
const route = useRoute()

// 从结算页跳来时会带 ?pay=<orderId>，自动引导完成支付（补齐"结算 → 支付"的转化闭环）
onMounted(async () => {
  await loadOrders()
  const payId = Number(route.query.pay)
  if (!payId) return
  const target = orders.value.find(o => o.order.id === payId)
  if (target && target.order.status === 'PENDING_PAY') {
    payOrder(target)
  }
})
const showDetail = ref(false)
const detail = ref(null)
const FINGER_NAMES = ['小指', '无名指', '中指', '食指', '大拇指']
const LEN_LABELS = [[0.85, '贴合'], [1.15, '标准'], [1.45, '加长'], [9, '极长']]
const STYLE_NAMES = { FRENCH: '微笑线', OMBRE: '渐变晕染', DOTS: '波点', LEOPARD: '豹纹', MARBLE: '大理石', TORTOISE: '玳瑁琥珀', AURA: '极光晕染', SWIRLS: '复古曲线', GEO: '几何拼色', STARS: '星星点缀' }
let matCache = null   // { shapes, colors, patterns } id → 素材，进程内缓存一次

async function ensureMaterials() {
  if (matCache) return matCache
  const toMap = (list) => new Map((list || []).map(m => [m.id, m]))
  const [s, c, p] = await Promise.all([
    request.get('/materials', { params: { type: 'SHAPE' } }),
    request.get('/materials', { params: { type: 'COLOR' } }),
    request.get('/materials', { params: { type: 'PATTERN_IMG' } })
  ])
  matCache = { shapes: toMap(s.data), colors: toMap(c.data), patterns: toMap(p.data) }
  return matCache
}

function parseDesignFingers(paramsStr) {
  const m = matCache || { shapes: new Map(), colors: new Map(), patterns: new Map() }
  try {
    const p = JSON.parse(paramsStr || '{}')
    return (p.fingers || []).map((f, i) => {
      const len = Number(f.length) || 1
      const lenLabel = (LEN_LABELS.find(l => len < l[0]) || [9, '极长'])[1]
      const extras = []
      if (f.french?.enabled) extras.push('法式')
      if (f.dots?.enabled) extras.push(f.dots.kind === 'leopard' ? '豹纹' : '波点')
      if (f.catEye?.enabled) extras.push('猫眼')
      if (f.gradient?.enabled) extras.push('渐变')
      if (f.patternStyle?.code) extras.push(STYLE_NAMES[f.patternStyle.code] || f.patternStyle.code)
      return {
        name: FINGER_NAMES[i] || `第 ${i + 1} 指`,
        shape: m.shapes.get(f.shapeId)?.name || '—',
        colorName: m.colors.get(f.colorId)?.name || '定制色',
        colorCode: m.colors.get(f.colorId)?.colorCode || '#EFE7DC',
        lenLabel,
        patterns: (f.patternIds || []).map(id => m.patterns.get(id)?.name).filter(Boolean),
        extras
      }
    })
  } catch (e) {
    return []   // 旧数据/解析失败：弹层内只展示预览图
  }
}

async function openDesignDetail(it) {
  await ensureMaterials()
  detail.value = {
    name: it.designName,
    previewUrl: it.previewUrl,
    fingers: parseDesignFingers(it.designParams)
  }
  showDetail.value = true
}

// ---- 一键复购：已完成/已核销/已签收的订单，按原设计直接加购 ----
function canRebuy(o) {
  return ['RECEIVED', 'VERIFIED', 'COMPLETED'].includes(o.order.status) && o.items?.length
}

function buyAgain(o) {
  return request.post('/cart', { designId: o.items[0].designId, quantity: 1 })
    .then(() => {
      ElMessageBox.confirm('已按原设计加入购物车，去结算还是继续逛逛？', '再来一副', {
        confirmButtonText: '去结算',
        cancelButtonText: '继续逛逛',
        type: 'success'
      }).then(() => router.push('/checkout')).catch(() => {})
    })
}

</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.order-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px 16px; margin-bottom: 14px; }
.order-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.head-tags { display: flex; align-items: center; gap: 8px; }
.sale-alert { margin-bottom: 12px; }
.order-no { font-size: 13px; color: #8A8177; }
.item-row { display: flex; align-items: center; gap: 10px; padding: 6px 0; cursor: pointer; }
.item-row:hover { background: #FBF7F0; border-radius: 8px; }
.detail-link { margin-left: auto; font-size: 12px; color: #C8813C; white-space: nowrap; }
.detail-hero { text-align: center; background: #F5EFE6; border-radius: 10px; padding: 10px; }
.detail-img { width: 100%; height: 260px; border-radius: 8px; background: #fff; }
.detail-img-empty { display: flex; align-items: center; justify-content: center; color: #c0c4cc; }
.detail-assure { display: flex; align-items: center; gap: 6px; justify-content: center;
                 font-size: 13px; color: #7E947A; margin: 10px 0 4px; }
.detail-name { font-weight: 700; margin: 4px 0 8px; }
.detail-finger { display: flex; align-items: center; gap: 8px; font-size: 13px;
                 padding: 7px 4px; border-bottom: 1px dashed #EDE5D8; flex-wrap: wrap; }
.df-name { width: 42px; color: #8A8177; }
.df-chip { width: 14px; height: 14px; border-radius: 4px; border: 1px solid rgba(0,0,0,0.12); }
.df-text { color: #4A4238; }
.df-pat, .df-extra { width: 100%; padding-left: 50px; font-size: 12px; color: #8A8177; }
.detail-empty { font-size: 12px; color: #8A8177; text-align: center; }
.thumb { width: 48px; height: 72px; border-radius: 6px; background: #F1EDE9; flex-shrink: 0; }
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
