<template>
  <el-container class="page">
    <el-header>
      <h1>我的设计</h1>
      <div class="nav">
        <router-link to="/designer">+ 新建设计</router-link>
        <router-link to="/cart">购物车</router-link>
        <router-link to="/orders">我的订单</router-link>
        <router-link to="/">首页</router-link>
      </div>
    </el-header>
    <el-main>
      <el-row v-loading="loading" :gutter="16">
        <el-col v-for="d in list" :key="d.id" :xs="12" :sm="8" :md="6">
          <el-card class="design-card" shadow="hover">
            <el-image v-if="d.previewUrl" :src="d.previewUrl" fit="contain" class="preview" />
            <div v-else class="preview preview-empty">暂无快照</div>
            <div class="name">{{ d.name }}</div>
            <div class="meta">
              <span class="time">{{ d.createTime }}</span>
              <span class="price">￥{{ d.price }}</span>
            </div>
            <div class="actions">
              <el-button size="small" @click="$router.push({ path: '/designer', query: { designId: d.id } })">编辑</el-button>
              <el-button size="small" @click="addToCart(d)">加购</el-button>
              <el-button size="small" type="primary" @click="buyNow(d)">下单</el-button>
              <el-popconfirm title="确定删除该设计？" @confirm="removeDesign(d)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && list.length === 0" description="还没有设计，去设计器创作一款吧">
        <el-button type="primary" @click="$router.push('/designer')">开始设计</el-button>
      </el-empty>

      <el-pagination class="pagination" v-model:current-page="query.page" v-model:page-size="query.size"
                     :total="total" layout="total, prev, pager, next" @current-change="loadList" />
    </el-main>
  </el-container>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()

const query = reactive({ page: 1, size: 8 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/designs', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function removeDesign(d) {
  await request.delete(`/designs/${d.id}`)
  ElMessage.success('删除成功')
  loadList()
}

// 加入购物车
async function addToCart(d) {
  await request.post('/cart', { designId: d.id, quantity: 1 })
  ElMessage.success('已加入购物车')
}

// 下单：加入购物车后直接去结算
async function buyNow(d) {
  await request.post('/cart', { designId: d.id, quantity: 1 })
  router.push('/checkout')
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; background: #F5EFE6; }
.page .el-header { display: flex; align-items: center; justify-content: space-between; background: #fff; }
.nav a { margin-left: 16px; text-decoration: none; color: #C8813C; }
.design-card { margin-bottom: 16px; }
.preview { width: 100%; height: 130px; border-radius: 6px; background: #F1EDE9; }
.preview-empty { display: flex; align-items: center; justify-content: center; color: #c0c4cc;
                 border: 1px dashed #e4e7ed; }
.name { margin-top: 8px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { display: flex; justify-content: space-between; margin-top: 6px; font-size: 12px; }
.time { color: #8A8177; }
.price { color: #e6a23c; font-weight: 600; }
.actions { display: flex; justify-content: space-between; margin-top: 10px; }
.pagination { margin-top: 4px; justify-content: flex-end; }
</style>
