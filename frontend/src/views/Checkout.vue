<template>
  <div class="page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>订单结算</span>
          <el-button @click="$router.push('/cart')">返回购物车</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && items.length === 0" description="购物车为空，无法结算">
        <el-button type="primary" @click="$router.push('/cart')">去购物车</el-button>
      </el-empty>

      <template v-else>
        <!-- 商品明细 -->
        <div class="section">
          <div class="section-title">商品明细</div>
          <el-table :data="items">
            <el-table-column label="预览" width="80">
              <template #default="{ row }">
                <el-image v-if="row.previewUrl" :src="row.previewUrl" fit="contain" class="thumb" />
                <div v-else class="thumb thumb-empty">无快照</div>
              </template>
            </el-table-column>
            <el-table-column prop="designName" label="设计名称" min-width="160" />
            <el-table-column label="单价" width="100">
              <template #default="{ row }">￥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="小计" width="100">
              <template #default="{ row }">￥{{ (row.price * row.quantity).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 履约方式 -->
        <div class="section">
          <div class="section-title">履约方式</div>
          <el-radio-group v-model="deliveryType" @change="onDeliveryChange">
            <el-radio-button value="SELF_PICKUP">到店自提（免运费）</el-radio-button>
            <el-radio-button value="EXPRESS">商家邮寄（运费 ￥{{ freightAmount }}，满 {{ freeOver }} 包邮）</el-radio-button>
          </el-radio-group>

          <!-- 自提：选门店 -->
          <div v-if="deliveryType === 'SELF_PICKUP'" class="pick-list">
            <div v-if="stores.length === 0" class="empty-tip">暂无营业中的自提门店</div>
            <div v-for="s in stores" :key="s.id" class="pick-item"
                 :class="{ active: storeId === s.id }" @click="storeId = s.id">
              <div class="pick-title">{{ s.name }}</div>
              <div class="pick-sub">{{ s.address }}</div>
              <div class="pick-sub">营业时间：{{ s.businessHours }}</div>
            </div>
          </div>

          <!-- 邮寄：选地址 -->
          <div v-else class="pick-list">
            <div v-if="addresses.length === 0" class="empty-tip">暂无收货地址，请新增</div>
            <div v-for="a in addresses" :key="a.id" class="pick-item"
                 :class="{ active: addressId === a.id }" @click="addressId = a.id">
              <div class="pick-title">
                {{ a.receiver }} {{ a.phone }}
                <el-tag v-if="a.isDefault === 1" size="small" type="danger">默认</el-tag>
              </div>
              <div class="pick-sub">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detail }}</div>
            </div>
            <el-button class="add-btn" @click="showAddressForm = true">+ 新增收货地址</el-button>
          </div>
        </div>

        <!-- 金额合计 -->
        <div class="section">
          <div class="summary">
            <div>商品合计：￥{{ goodsAmount.toFixed(2) }}</div>
            <div>运费：￥{{ freight.toFixed(2) }}</div>
            <div class="summary-total">应付总额：<span class="amount">￥{{ totalAmount.toFixed(2) }}</span></div>
          </div>
        </div>

        <el-button type="primary" size="large" :loading="submitting" @click="submit">提交订单</el-button>
      </template>
    </el-card>

    <!-- 新增地址弹窗 -->
    <el-dialog v-model="showAddressForm" title="新增收货地址" width="480px">
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="addressForm.receiver" maxlength="50" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="addressForm.phone" maxlength="11" /></el-form-item>
        <el-form-item label="省">
          <el-select v-model="addressForm.province" placeholder="选择省份" filterable allow-create>
            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="市"><el-input v-model="addressForm.city" /></el-form-item>
        <el-form-item label="区/县"><el-input v-model="addressForm.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="addressForm.detail" maxlength="255" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressForm = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const provinces = ['北京市', '上海市', '广东省', '浙江省', '江苏省', '山东省', '四川省', '湖北省', '福建省', '河南省']

const FREIGHT_AMOUNT = 8
const FREE_OVER = 99
const freightAmount = FREIGHT_AMOUNT
const freeOver = FREE_OVER

const loading = ref(false)
const submitting = ref(false)
const items = ref([])
const deliveryType = ref('SELF_PICKUP')
const stores = ref([])
const addresses = ref([])
const storeId = ref(null)
const addressId = ref(null)
const showAddressForm = ref(false)
const addressForm = ref({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })

const goodsAmount = computed(() => items.value.reduce((s, it) => s + it.price * it.quantity, 0))
const freight = computed(() => deliveryType.value === 'EXPRESS' && goodsAmount.value < FREE_OVER ? FREIGHT_AMOUNT : 0)
const totalAmount = computed(() => goodsAmount.value + freight.value)

function onDeliveryChange() {
  storeId.value = stores.value[0]?.id ?? null
  addressId.value = addresses.value.find(a => a.isDefault === 1)?.id ?? addresses.value[0]?.id ?? null
}

async function loadAll() {
  loading.value = true
  try {
    const res = await request.get('/cart')
    items.value = res.data
    const [storeRes, addrRes] = await Promise.all([request.get('/stores'), request.get('/addresses')])
    stores.value = storeRes.data
    addresses.value = addrRes.data
    onDeliveryChange()
  } finally {
    loading.value = false
  }
}

async function saveAddress() {
  const f = addressForm.value
  if (!f.receiver || !f.phone || !f.province || !f.city || !f.district || !f.detail) {
    ElMessage.warning('请完整填写地址信息')
    return
  }
  await request.post('/addresses', { ...f, isDefault: 0 })
  ElMessage.success('地址已保存')
  showAddressForm.value = false
  addressForm.value = { receiver: '', phone: '', province: '', city: '', district: '', detail: '' }
  const addrRes = await request.get('/addresses')
  addresses.value = addrRes.data
  addressId.value = addresses.value[0]?.id ?? null
}

async function submit() {
  if (deliveryType.value === 'SELF_PICKUP' && !storeId.value) {
    ElMessage.warning('请选择自提门店')
    return
  }
  if (deliveryType.value === 'EXPRESS' && !addressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    await request.post('/orders', {
      deliveryType: deliveryType.value,
      storeId: deliveryType.value === 'SELF_PICKUP' ? storeId.value : null,
      addressId: deliveryType.value === 'EXPRESS' ? addressId.value : null
    })
    ElMessage.success('订单已创建，请尽快完成支付')
    router.push('/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.section { margin-bottom: 22px; }
.section-title { font-weight: 700; margin-bottom: 10px; }
.thumb { width: 48px; height: 48px; border-radius: 6px; background: #F1EDE9; }
.thumb-empty { display: flex; align-items: center; justify-content: center; color: #c0c4cc;
               font-size: 11px; border: 1px dashed #e4e7ed; }
.pick-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
.pick-item { width: 260px; padding: 10px 12px; border: 2px solid #e4e7ed; border-radius: 8px;
             cursor: pointer; transition: border-color .15s; }
.pick-item:hover { border-color: #c6e2ff; }
.pick-item.active { border-color: #C8813C; background: #F7ECDD; }
.pick-title { font-weight: 600; margin-bottom: 4px; }
.pick-sub { font-size: 12px; color: #8A8177; }
.empty-tip { color: #8A8177; }
.add-btn { height: 62px; border-style: dashed; }
.summary { text-align: right; font-size: 14px; line-height: 24px; }
.summary-total { font-size: 16px; }
.amount { color: #e6a23c; font-size: 20px; font-weight: 700; }
</style>
