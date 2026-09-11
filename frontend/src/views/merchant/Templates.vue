<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>款式模板管理</span>
          <div>
            <el-button @click="$router.push('/merchant')">返回后台</el-button>
            <el-button type="primary" @click="openCreate">新增模板</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按名称搜索" clearable style="width: 200px"
                  @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="query.styleTag" placeholder="风格标签" clearable style="width: 140px" @change="loadList">
          <el-option v-for="tag in STYLE_TAGS" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <!-- 模板列表 -->
      <el-table :data="list" v-loading="loading">
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image :src="row.coverUrl" fit="contain" class="thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="styleTag" label="风格标签" width="100" />
        <el-table-column label="参考价" width="100">
          <template #default="{ row }">￥{{ row.price }}</template>
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
            <el-popconfirm title="确定删除该模板？" @confirm="removeTemplate(row)">
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑模板' : '新增模板'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="风格标签">
          <el-select v-model="form.styleTag" filterable allow-create default-first-option
                     placeholder="选择或输入标签" style="width: 220px">
            <el-option v-for="tag in STYLE_TAGS" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" :http-request="uploadCover">
            <el-image v-if="form.coverUrl" :src="form.coverUrl" fit="contain" class="upload-preview" />
            <el-button v-else type="primary" plain>选择图片上传（≤5MB）</el-button>
          </el-upload>
        </el-form-item>
        <!-- 设计参数：选甲型/色卡/图案组装，与用户 DIY 设计同一结构 -->
        <el-form-item label="默认甲型">
          <el-select v-model="form.shapeId" placeholder="选择甲型" style="width: 220px">
            <el-option v-for="m in shapes" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认色卡">
          <el-select v-model="form.colorId" placeholder="选择色卡" style="width: 220px">
            <el-option v-for="m in colors" :key="m.id" :label="`${m.name}（${m.colorCode}）`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认图案">
          <el-select v-model="form.patternIds" multiple placeholder="可多选图案" style="width: 320px">
            <el-option v-for="m in patterns" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="100" placeholder="给顾客的说明（可选）" />
        </el-form-item>
        <el-form-item label="参考价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
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

// 常用风格标签（可选可自定义输入）
const STYLE_TAGS = ['可爱', '轻奢', '法式', '日系', '简约', '复古', '甜美']

// ---- 列表 ----
const query = reactive({ styleTag: '', status: undefined, keyword: '', page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/merchant/templates', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ---- 素材选项（组装设计参数用） ----
const shapes = ref([])
const colors = ref([])
const patterns = ref([])

async function loadMaterials() {
  const [s, c, p] = await Promise.all([
    request.get('/merchant/materials', { params: { type: 'SHAPE', status: 1, page: 1, size: 100 } }),
    request.get('/merchant/materials', { params: { type: 'COLOR', status: 1, page: 1, size: 100 } }),
    request.get('/merchant/materials', { params: { type: 'PATTERN', status: 1, page: 1, size: 100 } })
  ])
  shapes.value = s.data.records
  colors.value = c.data.records
  patterns.value = p.data.records
}

// ---- 新增 / 编辑 ----
const dialogVisible = ref(false)
const saving = ref(false)
const emptyForm = () => ({
  id: null, name: '', styleTag: '', coverUrl: '',
  shapeId: null, colorId: null, patternIds: [], remark: '',
  price: 39, sort: 0, onSale: true
})
const form = reactive(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  // 解析设计参数 JSON 回填表单
  let params = { shapeId: null, colorId: null, patternIds: [], remark: '' }
  try { params = { ...params, ...JSON.parse(row.designParams) } } catch (e) { /* 容错：结构异常按空处理 */ }
  Object.assign(form, {
    id: row.id, name: row.name, styleTag: row.styleTag, coverUrl: row.coverUrl,
    shapeId: params.shapeId ?? null, colorId: params.colorId ?? null,
    patternIds: params.patternIds || [], remark: params.remark || '',
    price: Number(row.price), sort: row.sort, onSale: row.status === 1
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.name) { ElMessage.warning('请填写模板名称'); return }
  if (!form.coverUrl) { ElMessage.warning('请上传封面图'); return }
  if (!form.shapeId || !form.colorId) { ElMessage.warning('请选择默认甲型和色卡'); return }
  saving.value = true
  try {
    const body = {
      name: form.name,
      styleTag: form.styleTag || '',
      coverUrl: form.coverUrl,
      designParams: JSON.stringify({
        shapeId: form.shapeId, colorId: form.colorId,
        patternIds: form.patternIds, remark: form.remark || ''
      }),
      price: form.price,
      sort: form.sort,
      status: form.onSale ? 1 : 0
    }
    if (form.id) {
      await request.put(`/merchant/templates/${form.id}`, body)
    } else {
      await request.post('/merchant/templates', body)
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
    name: row.name, styleTag: row.styleTag, coverUrl: row.coverUrl,
    designParams: row.designParams, price: Number(row.price),
    sort: row.sort, status: onSale ? 1 : 0
  }
  await request.put(`/merchant/templates/${row.id}`, body)
  ElMessage.success(onSale ? '已上架' : '已下架')
  loadList()
}

async function removeTemplate(row) {
  await request.delete(`/merchant/templates/${row.id}`)
  ElMessage.success('删除成功')
  loadList()
}

// ---- 封面图上传（复用素材上传接口） ----
async function uploadCover({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await request.post('/merchant/upload/image', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  form.coverUrl = res.data
  ElMessage.success('封面上传成功')
}

onMounted(() => {
  loadList()
  loadMaterials()
})
</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.thumb { width: 60px; height: 60px; }
.upload-preview { width: 140px; height: 140px; border: 1px dashed #d9d9d9; border-radius: 6px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
