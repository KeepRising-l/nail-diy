<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>门店管理</span>
          <el-button type="primary" @click="openCreate">新增门店</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按名称/地址搜索" clearable style="width: 220px"
                  @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="营业中" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <!-- 门店列表 -->
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="name" label="门店名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="businessHours" label="营业时间" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该门店？（停用即可对顾客隐藏）" @confirm="removeStore(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="query.page" v-model:page-size="query.size"
                     :total="total" layout="total, prev, pager, next" @current-change="loadList" />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑门店' : '新增门店'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="255" placeholder="请输入门店地址" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="20" placeholder="联系电话（可选）" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.businessHours" maxlength="100" placeholder="如：周一至周日 10:00-21:00" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sort" :min="0" />
          <span class="form-tip">数字越小，结算页越靠前</span>
        </el-form-item>
        <el-form-item label="营业中">
          <el-switch v-model="form.open" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

// ---- 列表 ----
const query = reactive({ keyword: '', status: undefined, page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/merchant/stores', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ---- 新增 / 编辑 ----
const dialogVisible = ref(false)
const saving = ref(false)
const emptyForm = () => ({ id: null, name: '', address: '', phone: '', businessHours: '', sort: 0, open: true })
const form = reactive(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, name: row.name, address: row.address, phone: row.phone,
    businessHours: row.businessHours, sort: row.sort, open: row.status === 1
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) { ElMessage.warning('请填写门店名称'); return }
  if (!form.address.trim()) { ElMessage.warning('请填写门店地址'); return }
  saving.value = true
  try {
    const body = {
      name: form.name.trim(), address: form.address.trim(), phone: form.phone.trim(),
      businessHours: form.businessHours.trim(), sort: form.sort, status: form.open ? 1 : 0
    }
    if (form.id) {
      await request.put(`/merchant/stores/${form.id}`, body)
    } else {
      await request.post('/merchant/stores', body)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// ---- 营业状态 / 删除 ----
async function toggleStatus(row, open) {
  await request.put(`/merchant/stores/${row.id}`, {
    name: row.name, address: row.address, phone: row.phone,
    businessHours: row.businessHours, sort: row.sort, status: open ? 1 : 0
  })
  ElMessage.success(open ? '已恢复营业' : '已停用（顾客端不再展示）')
  loadList()
}

async function removeStore(row) {
  await request.delete(`/merchant/stores/${row.id}`)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.form-tip { font-size: 12px; color: #8A8177; margin-left: 10px; }
</style>
