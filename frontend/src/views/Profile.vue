<template>
  <div class="page">
    <el-card class="profile-card" v-loading="loading">
      <template #header>
        <div class="card-header"><h1>个人中心</h1></div>
      </template>

      <div class="profile-body">
        <!-- 头像：点击上传（规则同设计快照 jpg/png/webp ≤5MB） -->
        <div class="avatar-block">
          <el-upload :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" :http-request="uploadAvatar">
            <el-avatar :size="88" :src="form.avatar || undefined" class="avatar">
              {{ (form.nickname || '我').slice(0, 1) }}
            </el-avatar>
            <div class="avatar-tip">点击更换头像</div>
          </el-upload>
        </div>

        <el-form :model="form" label-width="80px" class="form">
          <el-form-item label="手机号">
            <el-input :model-value="form.phone" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit placeholder="输入昵称" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({ phone: '', nickname: '', avatar: '' })

async function loadProfile() {
  loading.value = true
  try {
    const res = await request.get('/user/profile')
    form.phone = res.data.phone
    form.nickname = res.data.nickname
    form.avatar = res.data.avatar || ''
  } finally {
    loading.value = false
  }
}

async function uploadAvatar({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await request.post('/designs/upload/preview', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  form.avatar = res.data
  ElMessage.success('头像已上传，点击保存修改生效')
}

async function save() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const res = await request.put('/user/profile', { nickname: form.nickname.trim(), avatar: form.avatar })
    auth.setUser(res.data)
    ElMessage.success('个人信息已保存')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.page { min-height: 100vh; padding: 32px 16px; }
.profile-card { max-width: 560px; margin: 0 auto; }
.card-header { font-weight: 700; }
.profile-body { display: flex; gap: 32px; align-items: flex-start; padding: 8px 0; }
.avatar-block { text-align: center; flex-shrink: 0; }
.avatar {
  cursor: pointer;
  background: linear-gradient(135deg, var(--color-primary, #E8899A), var(--color-accent, #C8813C));
  font-size: 32px;
}
.avatar-tip { font-size: 12px; color: #8A8177; margin-top: 8px; }
.form { flex: 1; }
</style>
