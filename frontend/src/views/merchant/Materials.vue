<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>素材管理（甲型 / 色卡 / 图案）</span>
          <div>
            <el-button @click="$router.push('/merchant')">返回后台</el-button>
            <el-button type="primary" @click="openCreate">新增素材</el-button>
          </div>
        </div>
      </template>

      <!-- 按类型切换 -->
      <el-tabs v-model="query.type" @tab-change="loadList">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="甲型" name="SHAPE" />
        <el-tab-pane label="色卡" name="COLOR" />
        <el-tab-pane label="图案" name="PATTERN" />
      </el-tabs>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按名称搜索" clearable style="width: 200px"
                  @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <!-- 素材列表 -->
      <el-table :data="list" v-loading="loading">
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <el-image v-if="row.type !== 'COLOR'" :src="row.imageUrl" fit="contain" class="thumb" />
            <div v-else class="color-block" :style="{ background: row.colorCode }"
                 :title="row.colorCode" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="颜色代码" width="110">
          <template #default="{ row }">{{ row.colorCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该素材？" @confirm="removeMaterial(row)">
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑素材' : '新增素材'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="素材类型">
          <el-radio-group v-model="form.type" :disabled="!!form.id">
            <el-radio value="SHAPE">甲型</el-radio>
            <el-radio value="COLOR">色卡</el-radio>
            <el-radio value="PATTERN">图案</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入素材名称" />
        </el-form-item>
        <!-- 色卡：颜色选择器，零图片成本 -->
        <el-form-item v-if="form.type === 'COLOR'" label="颜色代码">
          <el-color-picker v-model="form.colorCode" />
          <span class="color-text">{{ form.colorCode || '请选择颜色' }}</span>
        </el-form-item>
        <!-- 甲型 / 图案：图片上传 -->
        <el-form-item v-else label="素材图片">
          <el-upload :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" :http-request="uploadImage">
            <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="contain" class="upload-preview" />
            <el-button v-else type="primary" plain>选择图片上传（≤5MB）</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.onSale" />
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

const TYPE_LABEL = { SHAPE: '甲型', COLOR: '色卡', PATTERN: '图案' }
const typeLabel = (t) => TYPE_LABEL[t] || t

// ---- 列表 ----
const query = reactive({ type: '', status: undefined, keyword: '', page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/merchant/materials', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ---- 新增 / 编辑 ----
const dialogVisible = ref(false)
const saving = ref(false)
const emptyForm = () => ({ id: null, type: 'SHAPE', name: '', imageUrl: '', colorCode: '', sort: 0, onSale: true })
const form = reactive(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row, onSale: row.status === 1 })
  dialogVisible.value = true
}

async function submit() {
  if (!form.name) {
    ElMessage.warning('请填写素材名称')
    return
  }
  if (form.type === 'COLOR' && !form.colorCode) {
    ElMessage.warning('请选择色卡颜色')
    return
  }
  if (form.type !== 'COLOR' && !form.imageUrl) {
    ElMessage.warning('请上传素材图片')
    return
  }
  saving.value = true
  try {
    const body = {
      type: form.type,
      name: form.name,
      imageUrl: form.type === 'COLOR' ? '' : form.imageUrl,
      colorCode: form.type === 'COLOR' ? form.colorCode : '',
      sort: form.sort,
      status: form.onSale ? 1 : 0
    }
    if (form.id) {
      await request.put(`/merchant/materials/${form.id}`, body)
    } else {
      await request.post('/merchant/materials', body)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// ---- 上下架 / 删除 ----
async function toggleStatus(row, onSale) {
  const body = {
    type: row.type, name: row.name, imageUrl: row.imageUrl,
    colorCode: row.colorCode, sort: row.sort, status: onSale ? 1 : 0
  }
  await request.put(`/merchant/materials/${row.id}`, body)
  ElMessage.success(onSale ? '已上架' : '已下架')
  loadList()
}

async function removeMaterial(row) {
  await request.delete(`/merchant/materials/${row.id}`)
  ElMessage.success('删除成功')
  loadList()
}

// ---- 图片上传（自定义请求，走统一 request 注入 token） ----
async function uploadImage({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await request.post('/merchant/upload/image', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  form.imageUrl = res.data
  ElMessage.success('图片上传成功')
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.thumb { width: 60px; height: 60px; }
.color-block { width: 60px; height: 60px; border-radius: 6px; border: 1px solid #eee; }
.color-text { margin-left: 10px; color: #4A443E; }
.upload-preview { width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 6px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
