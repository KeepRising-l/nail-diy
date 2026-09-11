<template>
  <el-card class="auth-card">
    <template #header>
      <div class="title">
        <span>注册账号</span>
        <router-link to="/login" class="link">已有账号？去登录</router-link>
      </div>
    </template>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="70px" @keyup.enter="onRegister">
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="6-20位，需含字母和数字" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="onRegister">注 册</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({ phone: '', nickname: '', password: '', confirmPassword: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,20}$/, message: '6-20位，需同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function onRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await request.post('/auth/register', {
      phone: form.phone,
      nickname: form.nickname,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
