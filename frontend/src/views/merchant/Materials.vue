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
        <el-tab-pane label="图案" name="PATTERN_IMG" />
        <el-tab-pane label="贴纸" name="STICKER" />
      </el-tabs>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按名称搜索" clearable style="width: 200px"
                  @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="loadList">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <!-- 素材列表 -->
      <el-table :data="list" v-loading="loading">
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <MaterialThumb :material="row" :size="60" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="来源 / 审核" width="140">
          <template #default="{ row }">
            <el-tag :type="row.source === 'UGC' ? 'warning' : 'info'" size="small">
              {{ row.source === 'UGC' ? 'UGC' : '平台' }}
            </el-tag>
            <el-tag v-if="row.auditStatus && row.auditStatus !== 'APPROVED'" size="small"
                    :type="row.auditStatus === 'PENDING' ? 'warning' : 'danger'" style="margin-left: 4px">
              {{ auditLabel(row.auditStatus) }}
            </el-tag>
          </template>
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
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <template v-if="row.source === 'UGC' && row.auditStatus === 'PENDING'">
              <el-button link type="success" @click="auditMaterial(row, true)">通过</el-button>
              <el-button link type="danger" @click="auditMaterial(row, false)">驳回</el-button>
            </template>
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
            <el-radio value="PATTERN_IMG">图案</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入素材名称" />
        </el-form-item>
        <!-- 甲型：代码注册素材，从受支持的 8 个 code 中选，不需要图片 -->
        <el-form-item v-if="form.type === 'SHAPE'" label="甲型代码">
          <el-select v-model="form.code" placeholder="请选择甲型轮廓" style="width: 220px"
                     @change="onShapeCodeChange">
            <el-option v-for="s in SHAPE_OPTIONS" :key="s.code"
                       :label="`${s.name}（${s.code}）`" :value="s.code" />
          </el-select>
          <MaterialThumb v-if="form.code" class="form-thumb" :size="44"
                         :material="{ type: 'SHAPE', code: form.code, name: form.name }" />
        </el-form-item>
        <!-- 色卡：颜色选择器，零图片成本 -->
        <el-form-item v-else-if="form.type === 'COLOR'" label="颜色代码">
          <el-color-picker v-model="form.colorCode" />
          <span class="color-text">{{ form.colorCode || '请选择颜色' }}</span>
        </el-form-item>
        <!-- 图案：图片素材，必须上传 -->
        <el-form-item v-else label="素材图片">
          <el-upload :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" :http-request="uploadImage">
            <MaterialThumb v-if="form.imageUrl" :material="form" :size="120" class="upload-preview" />
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
import MaterialThumb from '../../components/MaterialThumb.vue'
import { SHAPE_OPTIONS } from '../../engine/contours'

// 类型 → 中文标签（与后端 Material.TYPE_* 常量保持一致）
const TYPE_LABEL = {
  SHAPE: '甲型',
  COLOR: '色卡',
  PATTERN_IMG: '图案',
  PATTERN_STYLE: '图案款式',
  DECOR: '钻饰',
  STICKER: '贴纸',
  OVERLAY: '甲油效果'
}
const typeLabel = (t) => TYPE_LABEL[t] || t

// ---- 列表 ----
const query = reactive({ type: '', status: undefined, auditStatus: undefined, keyword: '', page: 1, size: 10 })
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
const emptyForm = () => ({ id: null, type: 'SHAPE', code: '', name: '', imageUrl: '', colorCode: '', sort: 0, onSale: true })
const form = reactive(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row, onSale: row.status === 1 })
  dialogVisible.value = true
}

/** 选中甲型代码后，名称留空则自动填默认名（仍可手改） */
function onShapeCodeChange(code) {
  const hit = SHAPE_OPTIONS.find((s) => s.code === code)
  if (hit && !form.name) form.name = hit.name
}

async function submit() {
  if (!form.name) {
    ElMessage.warning('请填写素材名称')
    return
  }
  if (form.type === 'SHAPE' && !form.code) {
    ElMessage.warning('请选择甲型代码')
    return
  }
  if (form.type === 'COLOR' && !form.colorCode) {
    ElMessage.warning('请选择色卡颜色')
    return
  }
  if (form.type === 'PATTERN_IMG' && !form.imageUrl) {
    ElMessage.warning('请上传素材图片')
    return
  }
  saving.value = true
  try {
    const body = {
      type: form.type,
      code: ['SHAPE', 'PATTERN_STYLE', 'DECOR'].includes(form.type) ? form.code : '',
      name: form.name,
      imageUrl: ['PATTERN_IMG', 'STICKER', 'DECOR'].includes(form.type) ? form.imageUrl : '',
      colorCode: form.type === 'COLOR' ? form.colorCode : '',
      category: form.category || '',
      tags: form.tags || '',
      attrs: form.attrs || null,
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

// ---- UGC 贴纸审核 ----
const auditLabel = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s)

async function auditMaterial(row, approved) {
  await request.post(`/merchant/materials/${row.id}/audit`, { approved })
  ElMessage.success(approved ? '已通过，贴纸对顾客可见' : '已驳回')
  loadList()
}

// ---- 上下架 / 删除 ----
async function toggleStatus(row, onSale) {
  await request.post('/merchant/materials/batch-status', {
    ids: [row.id],
    status: onSale ? 1 : 0
  })
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
.color-text { margin-left: 10px; color: #4A443E; }
.form-thumb { margin-left: 12px; }
.upload-preview { width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 6px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
