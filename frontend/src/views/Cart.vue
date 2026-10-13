<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h1>购物车</h1>
          <div>
            <el-button @click="$router.push('/my-designs')">我的设计</el-button>
            <el-button @click="$router.push('/')">首页</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <el-image v-if="row.previewUrl" :src="row.previewUrl" fit="contain" class="thumb" />
            <div v-else class="thumb thumb-empty">无快照</div>
          </template>
        </el-table-column>
        <el-table-column prop="designName" label="设计名称" min-width="160" />
        <el-table-column label="单价" width="100">
          <template #default="{ row }">￥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number :model-value="row.quantity" :min="1" size="small"
                             @change="(v) => changeQty(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100">
          <template #default="{ row }">￥{{ (row.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-popconfirm title="移除该条目？" @confirm="removeItem(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="购物车空空如也，先去设计一款吧">
        <el-button type="primary" @click="$router.push('/designer')">开始设计</el-button>
      </el-empty>

      <div v-if="list.length > 0" class="footer">
        <el-popconfirm title="确定清空购物车？" @confirm="clearCart">
          <template #reference>
            <el-button>清空购物车</el-button>
          </template>
        </el-popconfirm>
        <div class="total">
          合计：<span class="amount">￥{{ total.toFixed(2) }}</span>
          <el-button type="primary" @click="$router.push('/checkout')">去结算</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const list = ref([])
const loading = ref(false)

const total = computed(() => list.value.reduce((sum, it) => sum + it.price * it.quantity, 0))

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/cart')
    list.value = res.data
  } finally {
    loading.value = false
  }
}

async function changeQty(row, v) {
  if (!v || v < 1) return
  await request.put(`/cart/${row.id}`, { quantity: v })
  loadList()
}

async function removeItem(row) {
  await request.delete(`/cart/${row.id}`)
  ElMessage.success('已移除')
  loadList()
}

async function clearCart() {
  await request.delete('/cart')
  ElMessage.success('购物车已清空')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page { min-height: 100vh; padding: 16px; background: #F5EFE6; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.thumb { width: 48px; height: 72px; border-radius: 6px; background: #F1EDE9; }
.thumb-empty { display: flex; align-items: center; justify-content: center; color: #c0c4cc;
               font-size: 12px; border: 1px dashed #e4e7ed; }
.footer { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; }
.total { display: flex; align-items: center; gap: 12px; }
.amount { color: #e6a23c; font-size: 18px; font-weight: 700; }
</style>
