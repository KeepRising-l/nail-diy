<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按手机号/昵称搜索" clearable style="width: 220px"
                  @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 120px" @change="loadList">
          <el-option label="顾客" value="USER" />
          <el-option label="商家" value="MERCHANT" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <!-- 用户列表 -->
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1"
                       :disabled="row.role === 'ADMIN'"
                       @change="(v) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="query.page" v-model:page-size="query.size"
                     :total="total" layout="total, prev, pager, next" @current-change="loadList" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

// ---- 列表 ----
const query = reactive({ keyword: '', status: undefined, role: undefined, page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/admin/users', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 管理员账号不允许在页面上禁用（后端同时兜底禁止自禁）
const roleLabel = (r) => ({ USER: '顾客', MERCHANT: '商家', ADMIN: '管理员' }[r] || r)
const roleTagType = (r) => ({ USER: 'info', MERCHANT: 'warning', ADMIN: 'danger' }[r] || 'info')

async function toggleStatus(row, enable) {
  const tip = enable ? `确定恢复用户 ${row.nickname || row.phone} ？` : `确定禁用用户 ${row.nickname || row.phone} ？禁用后将无法登录。`
  try {
    await ElMessageBox.confirm(tip, '确认操作', { type: 'warning' })
  } catch (e) {
    loadList() // 取消：还原开关显示
    return
  }
  await request.put(`/admin/users/${row.id}/status`, { status: enable ? 1 : 0 })
  ElMessage.success(enable ? '已恢复' : '已禁用')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
