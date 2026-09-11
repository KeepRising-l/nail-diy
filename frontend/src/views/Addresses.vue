<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>地址簿（{{ list.length }}/10）</span>
          <div>
            <el-button @click="$router.push('/orders')">我的订单</el-button>
            <el-button type="primary" @click="openForm(null)">新增地址</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" description="还没有收货地址，新增一个吧" />
        <div v-for="a in list" :key="a.id" class="addr-card">
          <div class="addr-title">
            {{ a.receiver }} {{ a.phone }}
            <el-tag v-if="a.isDefault === 1" size="small" type="danger">默认</el-tag>
          </div>
          <div class="addr-detail">{{ a.province }} {{ a.city }} {{ a.district }} {{ a.detail }}</div>
          <div class="addr-actions">
            <el-button v-if="a.isDefault !== 1" link type="primary" @click="setDefault(a)">设为默认</el-button>
            <el-button link type="primary" @click="openForm(a)">编辑</el-button>
            <el-popconfirm title="确定删除该地址？" @confirm="remove(a)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="showForm" :title="form.id ? '编辑地址' : '新增地址'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="form.receiver" maxlength="50" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="11" /></el-form-item>
        <el-form-item label="省">
          <el-select v-model="form.province" placeholder="选择省份" filterable allow-create>
            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区/县"><el-input v-model="form.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" maxlength="255" /></el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.defaultFlag" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const provinces = ['北京市', '上海市', '广东省', '浙江省', '江苏省', '山东省', '四川省', '湖北省', '福建省', '河南省']

const list = ref([])
const loading = ref(false)
const showForm = ref(false)
const saving = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return { id: null, receiver: '', phone: '', province: '', city: '', district: '', detail: '', defaultFlag: false }
}

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/addresses')
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openForm(addr) {
  form.value = addr
    ? { id: addr.id, receiver: addr.receiver, phone: addr.phone, province: addr.province,
        city: addr.city, district: addr.district, detail: addr.detail, defaultFlag: addr.isDefault === 1 }
    : emptyForm()
  showForm.value = true
}

async function save() {
  const f = form.value
  if (!f.receiver || !f.phone || !f.province || !f.city || !f.district || !f.detail) {
    ElMessage.warning('请完整填写地址信息')
    return
  }
  saving.value = true
  try {
    const body = {
      receiver: f.receiver, phone: f.phone, province: f.province,
      city: f.city, district: f.district, detail: f.detail,
      isDefault: f.defaultFlag ? 1 : 0
    }
    if (f.id) {
      await request.put(`/addresses/${f.id}`, body)
    } else {
      await request.post('/addresses', body)
    }
    ElMessage.success('已保存')
    showForm.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function setDefault(addr) {
  await request.put(`/addresses/${addr.id}/default`)
  ElMessage.success('已设为默认地址')
  loadList()
}

async function remove(addr) {
  await request.delete(`/addresses/${addr.id}`)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.addr-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; }
.addr-title { font-weight: 600; margin-bottom: 4px; }
.addr-detail { font-size: 13px; color: #4A443E; margin-bottom: 8px; }
.addr-actions { display: flex; gap: 8px; }
</style>
