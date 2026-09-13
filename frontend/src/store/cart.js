import { defineStore } from 'pinia'
import request from '../utils/request'
import { useAuthStore } from './auth'

// 购物车徽标计数：顶栏跨页面共享；任何 /cart 写操作成功后由 request 拦截器调用 refresh 同步
export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0
  }),
  actions: {
    // 拉取购物车合计数量（未登录或接口失败归零，静默处理不打扰用户）
    async refresh() {
      const auth = useAuthStore()
      if (!auth.token) {
        this.count = 0
        return
      }
      try {
        const res = await request.get('/cart')
        this.count = (res.data || []).reduce((s, it) => s + it.quantity, 0)
      } catch (e) {
        this.count = 0
      }
    }
  }
})
