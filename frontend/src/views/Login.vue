<template>
  <el-card class="auth-card">
    <template #header>
      <div class="title">
        <span>DIY美甲定制平台</span>
        <router-link to="/register" class="link">没有账号？去注册</router-link>
      </div>
    </template>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="70px" @keyup.enter="onLogin">
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="onLogin">登 录</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/login', form)
    auth.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.push(redirect || (res.data.user.role === 'MERCHANT' || res.data.user.role === 'ADMIN' ? '/merchant' : '/'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-card { max-width: 420px; margin: 80px auto; }
.title { display: flex; justify-content: space-between; align-items: center; }
.link { font-size: 13px; color: #C8813C; text-decoration: none; }
</style>
