<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单中心</span>
          <el-button @click="loadOrders">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in TABS" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

      <div v-loading="loading">
        <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
        <div v-for="o in orders" :key="o.order.id" class="order-card">
          <div class="order-head">
            <span class="order-no">{{ o.order.orderNo }} · {{ o.order.createTime }}</span>
            <el-tag :type="statusTagType(o.order.status)">{{ statusLabel(o.order.status) }}</el-tag>
          </div>

          <!-- 商品明细 + 设计参数（制作依据） -->
          <div v-for="it in o.items" :key="it.id" class="item-row">
            <el-image v-if="it.previewUrl" :src="it.previewUrl" fit="contain" class="thumb" />
            <div v-else class="thumb thumb-empty">无快照</div>
            <div class="item-info">
              <div class="item-name">{{ it.designName }}</div>
              <div class="item-sub">￥{{ it.price }} × {{ it.quantity }}</div>
            </div>
            <el-collapse class="params-collapse">
              <el-collapse-item title="设计参数（制作依据）">
                <pre class="params-pre">{{ fmtParams(it.designParams) }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 履约信息 -->
          <div class="order-meta">
            <div>
              <template v-if="o.order.deliveryType === 'SELF_PICKUP'">
                到店自提<template v-if="o.store"> · {{ o.store.name }}（{{ o.store.address }}）</template>
                <el-tag v-if="o.order.status === 'WAIT_PICKUP'" type="warning" class="pickup-tag">
                  自提码：{{ o.order.pickupCode }}
                </el-tag>
              </template>
              <template v-else>
                商家邮寄 · {{ o.order.receiverName }} {{ o.order.receiverPhone }} · {{ o.order.receiverAddr }}
                <span v-if="o.order.expressNo" class="express">
                  物流：{{ o.order.expressCompany }} {{ o.order.expressNo }}
                </span>
              </template>
              <span v-if="o.order.remark" class="remark">备注：{{ o.order.remark }}</span>
            </div>
            <span>合计 ￥{{ o.order.totalAmount }}（含运费 ￥{{ o.order.freight }}）</span>
          </div>

          <!-- 状态时间线 -->
          <el-collapse class="timeline-collapse">
            <el-collapse-item title="状态记录">
              <el-timeline>
                <el-timeline-item v-for="l in o.logs" :key="l.id" :timestamp="l.createTime" size="small">
                  {{ l.toStatus && STATUS_MAP ? statusLabel(l.toStatus) : '' }}
                  <span class="log-op">{{ l.operator }} · {{ l.remark }}</span>
                </el-timeline-item>
              </el-timeline>
            </el-collapse-item>
          </el-collapse>

          <div class="order-foot">
            <div></div>
            <div class="ops">
              <el-button v-if="o.order.status === 'PAID'" type="primary" @click="doAccept(o)">接单</el-button>
              <el-button v-if="o.order.status === 'PRODUCING'" type="primary" @click="doProduce(o)">制作完成</el-button>
              <el-button v-if="o.order.status === 'WAIT_PICKUP'" type="success" @click="openVerify(o)">核销自提</el-button>
              <el-button v-if="o.order.status === 'WAIT_SHIP'" type="warning" @click="openShip(o)">发货</el-button>
              <el-popconfirm v-if="o.order.status === 'PENDING_PAY' || o.order.status === 'PAID'"
                             title="确定取消该订单？" @confirm="doCancel(o)">
                <template #reference>
                  <el-button type="danger" plain>取消</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>

        <el-pagination v-if="total > 0" layout="prev, pager, next" :total="total" :page-size="10"
                       :current-page="page" @current-change="onPageChange" class="pager" />
      </div>
    </el-card>

    <!-- 核销弹窗 -->
    <el-dialog v-model="showVerify" title="自提核销" width="360px">
      <p class="dialog-tip">请让顾客出示自提码（当前订单自提码：{{ verifyTarget?.order.pickupCode }}）</p>
      <el-input v-model="verifyCode" maxlength="6" placeholder="输入 6 位自提码" />
      <template #footer>
        <el-button @click="showVerify = false">取消</el-button>
        <el-button type="primary" @click="doVerify">确认核销</el-button>
      </template>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="showShip" title="邮寄发货" width="400px">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.expressCompany" placeholder="选择快递公司" filterable allow-create>
            <el-option v-for="c in COMPANIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.expressNo" maxlength="100" placeholder="填写物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShip = false">取消</el-button>
        <el-button type="primary" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const TABS = [
  { label: '待接单', value: 'PAID' },
  { label: '制作中', value: 'PRODUCING' },
  { label: '待自提', value: 'WAIT_PICKUP' },
  { label: '待发货', value: 'WAIT_SHIP' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'RECEIVED' },
  { label: '已核销', value: 'VERIFIED' },
  { label: '待支付', value: 'PENDING_PAY' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '全部', value: '' }
]

const STATUS_MAP = {
  PENDING_PAY: '待支付', PAID: '待接单', PRODUCING: '制作中',
  WAIT_PICKUP: '待自提', VERIFIED: '已核销', WAIT_SHIP: '待发货',
  SHIPPED: '已发货', RECEIVED: '已签收', CANCELLED: '已取消'
}
const TAG_MAP = {
  PENDING_PAY: 'warning', PAID: 'primary', PRODUCING: 'primary',
  WAIT_PICKUP: 'success', VERIFIED: 'info', WAIT_SHIP: 'warning',
  SHIPPED: 'primary', RECEIVED: 'success', CANCELLED: 'info'
}
const COMPANIES = ['顺丰速运', '中通快递', '圆通速递', '韵达快递', '申通快递', '京东物流', '邮政EMS']

const statusLabel = (s) => STATUS_MAP[s] || s
const statusTagType = (s) => TAG_MAP[s] || 'info'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const activeTab = ref('PAID')
const loading = ref(false)

const showVerify = ref(false)
const verifyTarget = ref(null)
const verifyCode = ref('')
const showShip = ref(false)
const shipTarget = ref(null)
const shipForm = ref({ expressCompany: '', expressNo: '' })

function fmtParams(params) {
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch {
    return params || '（无）'
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await request.get('/merchant/orders', {
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

async function doAccept(o) {
  await request.post(`/merchant/orders/${o.order.id}/accept`)
  ElMessage.success('已接单，进入制作中')
  loadOrders()
}

async function doProduce(o) {
  await request.post(`/merchant/orders/${o.order.id}/produce`)
  ElMessage.success(o.order.deliveryType === 'SELF_PICKUP' ? '制作完成，已生成自提码' : '制作完成，待发货')
  loadOrders()
}

function openVerify(o) {
  verifyTarget.value = o
  verifyCode.value = ''
  showVerify.value = true
}

async function doVerify() {
  if (!/^\d{6}$/.test(verifyCode.value)) {
    ElMessage.warning('请输入 6 位数字自提码')
    return
  }
  await request.post(`/merchant/orders/${verifyTarget.value.order.id}/verify`, { pickupCode: verifyCode.value })
  ElMessage.success('核销成功')
  showVerify.value = false
  loadOrders()
}

function openShip(o) {
  shipTarget.value = o
  shipForm.value = { expressCompany: '', expressNo: '' }
  showShip.value = true
}

async function doShip() {
  if (!shipForm.value.expressCompany || !shipForm.value.expressNo) {
    ElMessage.warning('请填写快递公司与物流单号')
    return
  }
  await request.post(`/merchant/orders/${shipTarget.value.order.id}/ship`, shipForm.value)
  ElMessage.success('已发货')
  showShip.value = false
  loadOrders()
}

async function doCancel(o) {
  await request.post(`/merchant/orders/${o.order.id}/cancel`)
  ElMessage.success('订单已取消')
  loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.page { min-height: 100vh; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.order-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px 16px; margin-bottom: 14px; }
.order-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-size: 13px; color: #8A8177; }
.item-row { display: flex; align-items: flex-start; gap: 10px; padding: 6px 0; flex-wrap: wrap; }
.thumb { width: 48px; height: 72px; border-radius: 6px; background: #F1EDE9; flex-shrink: 0; }
.thumb-empty { display: flex; align-items: center; justify-content: center; color: #c0c4cc;
               font-size: 11px; border: 1px dashed #e4e7ed; }
.item-name { font-weight: 600; }
.item-sub { font-size: 12px; color: #8A8177; }
.params-collapse { flex-basis: 100%; margin-top: 2px; }
.params-pre { margin: 0; font-size: 12px; color: #4A443E; background: #F5EFE6; padding: 8px;
              border-radius: 6px; white-space: pre-wrap; word-break: break-all; }
.order-meta { display: flex; justify-content: space-between; gap: 12px; font-size: 12px; color: #8A8177;
              border-top: 1px dashed #E6DED2; padding-top: 8px; margin-top: 6px; flex-wrap: wrap; }
.pickup-tag { margin-left: 8px; }
.express { margin-left: 8px; color: #C8813C; }
.remark { margin-left: 8px; color: #e6a23c; }
.timeline-collapse { margin-top: 6px; }
.log-op { color: #8A8177; font-size: 12px; margin-left: 6px; }
.order-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.ops { display: flex; gap: 8px; }
.dialog-tip { font-size: 13px; color: #8A8177; margin: 0 0 10px; }
.pager { margin-top: 12px; justify-content: center; }
</style>
