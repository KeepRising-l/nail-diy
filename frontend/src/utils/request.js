import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import router from '../router'

// 统一 Axios 实例：注入 token、统一错误提示、401 跳登录
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        useAuthStore().logout()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore().logout()
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
