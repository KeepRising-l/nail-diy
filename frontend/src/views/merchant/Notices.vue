<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <el-button type="primary" @click="openCreate">新增公告</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按标题搜索" clearable style="width: 200px"
                  @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="发布" :value="1" />
          <el-option label="下线" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>

      <!-- 公告列表 -->
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isTop === 1" size="small" type="danger">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ row.updateTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该公告？" @confirm="removeNotice(row)">
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新增公告'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="2000" show-word-limit
                    placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.published">
            <el-radio :value="true">发布</el-radio>
            <el-radio :value="false">下线</el-radio>
          </el-radio-group>
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
    const res = await request.get('/merchant/notices', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ---- 新增 / 编辑 ----
const dialogVisible = ref(false)
const saving = ref(false)
const emptyForm = () => ({ id: null, title: '', content: '', isTop: false, published: true })
const form = reactive(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, title: row.title, content: row.content,
    isTop: row.isTop === 1, published: row.status === 1
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.title.trim()) { ElMessage.warning('请填写公告标题'); return }
  if (!form.content.trim()) { ElMessage.warning('请填写公告内容'); return }
  saving.value = true
  try {
    const body = {
      title: form.title.trim(), content: form.content.trim(),
      isTop: form.isTop ? 1 : 0, status: form.published ? 1 : 0
    }
    if (form.id) {
      await request.put(`/merchant/notices/${form.id}`, body)
    } else {
      await request.post('/merchant/notices', body)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// ---- 上下线 / 删除 ----
async function toggleStatus(row, published) {
  await request.put(`/merchant/notices/${row.id}`, {
    title: row.title, content: row.content, isTop: row.isTop, status: published ? 1 : 0
  })
  ElMessage.success(published ? '已发布' : '已下线')
  loadList()
}

async function removeNotice(row) {
  await request.delete(`/merchant/notices/${row.id}`)
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
</style>
