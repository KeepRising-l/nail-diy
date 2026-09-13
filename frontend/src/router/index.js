import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

// 路由约定：
// / 开头的页面 = 顾客端；/merchant 开头的页面 = 商家端（后台）
const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue') },
  {
    path: '/templates',
    name: 'templates',
    component: () => import('../views/Templates.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/designer',
    name: 'designer',
    component: () => import('../views/Designer.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-designs',
    name: 'myDesigns',
    component: () => import('../views/MyDesigns.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('../views/Cart.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('../views/Checkout.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/Orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/addresses',
    name: 'addresses',
    component: () => import('../views/Addresses.vue'),
    meta: { requiresAuth: true }
  },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue') },
  {
    path: '/merchant',
    name: 'merchant',
    component: () => import('../views/merchant/Dashboard.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/merchant/materials',
    name: 'merchantMaterials',
    component: () => import('../views/merchant/Materials.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/merchant/templates',
    name: 'merchantTemplates',
    component: () => import('../views/merchant/Templates.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/merchant/orders',
    name: 'merchantOrders',
    component: () => import('../views/merchant/Orders.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/merchant/reviews',
    name: 'merchantReviews',
    component: () => import('../views/merchant/Reviews.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/merchant/notices',
    name: 'merchantNotices',
    component: () => import('../views/merchant/Notices.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/merchant/stores',
    name: 'merchantStores',
    component: () => import('../views/merchant/Stores.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  },
  {
    path: '/admin/users',
    name: 'adminUsers',
    component: () => import('../views/admin/Users.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/merchant/analytics',
    name: 'merchantAnalytics',
    component: () => import('../views/merchant/Analytics.vue'),
    meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：受保护页面未登录跳登录页；已登录访问登录/注册页跳首页
router.beforeEach((to) => {
  const auth = useAuthStore()
  if ((to.path === '/login' || to.path === '/register') && auth.token) {
    return { path: '/' }
  }
  if (to.meta.requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.user?.role)) {
    return { path: '/' }
  }
  return true
})

export default router
