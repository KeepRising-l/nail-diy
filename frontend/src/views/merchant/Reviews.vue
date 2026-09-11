<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评价管理</span>
          <div class="filters">
            <el-select v-model="rating" placeholder="按评分筛选" clearable style="width: 140px" @change="onFilter">
              <el-option v-for="r in [5, 4, 3, 2, 1]" :key="r" :label="r + ' 星'" :value="r" />
            </el-select>
            <el-button @click="loadList">刷新</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" description="暂无评价" />
        <div v-for="rv in list" :key="rv.id" class="review-card">
          <div class="rv-head">
            <div>
              <el-rate :model-value="rv.rating" disabled size="small" />
              <span class="rv-user">{{ rv.nickname }} · {{ rv.createTime }}</span>
            </div>
            <span class="rv-order">订单 {{ rv.orderNo }} · {{ rv.designNames }}</span>
          </div>
          <div v-if="rv.content" class="rv-content">{{ rv.content }}</div>
          <div v-if="rv.images && rv.images.length" class="rv-images">
            <el-image v-for="(img, i) in rv.images" :key="i" :src="img" fit="cover" class="rv-img"
                      :preview-src-list="rv.images" :initial-index="i" preview-teleported />
          </div>
          <div v-if="rv.reply" class="rv-reply">商家回复：{{ rv.reply }}</div>
          <div class="rv-actions">
            <el-button size="small" @click="openReply(rv)">回复</el-button>
            <el-popconfirm title="确定删除该违规评价？" @confirm="removeReview(rv)">
              <template #reference>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <el-pagination v-if="total > 0" layout="prev, pager, next" :total="total" :page-size="10"
                       :current-page="page" @current-change="onPageChange" class="pager" />
      </div>
    </el-card>

    <el-dialog v-model="showReply" title="回复评价" width="440px">
      <p class="dialog-quote">{{ replyTarget?.nickname }}：{{ replyTarget?.content || '（该评价无文字内容）' }}</p>
      <el-input v-model="replyContent" type="textarea" :rows="3" maxlength="500" show-word-limit
                placeholder="输入回复内容（将展示在评价下方）" />
      <template #footer>
        <el-button @click="showReply = false">取消</el-button>
        <el-button type="primary" @click="doReply">保存回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const list = ref([])
const total = ref(0)
const page = ref(1)
const rating = ref(null)
const loading = ref(false)

const showReply = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/merchant/reviews', {
      params: { page: page.value, size: 10, rating: rating.value || undefined }
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onFilter() {
  page.value = 1
  loadList()
}

function onPageChange(p) {
  page.value = p
  loadList()
}

function openReply(rv) {
  replyTarget.value = rv
  replyContent.value = rv.reply || ''
  showReply.value = true
}

async function doReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  await request.post(`/merchant/reviews/${replyTarget.value.id}/reply`, { reply: replyContent.value })
  ElMessage.success('已回复')
  showReply.value = false
  loadList()
}

async function removeReview(rv) {
  await request.delete(`/merchant/reviews/${rv.id}`)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 10px; }
.review-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; }
.rv-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px; }
.rv-user { font-size: 13px; color: #4A443E; margin-left: 8px; }
.rv-order { font-size: 12px; color: #8A8177; }
.rv-content { margin-top: 8px; font-size: 14px; }
.rv-images { display: flex; gap: 8px; margin-top: 8px; }
.rv-img { width: 72px; height: 72px; border-radius: 6px; background: #F1EDE9; }
.rv-reply { margin-top: 8px; font-size: 13px; color: #e6a23c; background: #fdf6ec; padding: 6px 10px;
            border-radius: 6px; }
.rv-actions { display: flex; gap: 8px; margin-top: 10px; }
.dialog-quote { font-size: 13px; color: #8A8177; background: #F5EFE6; padding: 8px 10px;
                border-radius: 6px; margin: 0 0 12px; }
.pager { margin-top: 12px; justify-content: center; }
</style>
