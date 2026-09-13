<template>
  <header class="header">
    <div class="container header-inner">
      <!-- 特色 Logo：Nail 衬线深 + DIY 玫粉，辨识度高（品牌字标保留英文） -->
      <router-link to="/" class="brand" aria-label="Nail DIY 美甲定制平台">
        <span class="brand-logo">
          <span class="logo-nail">Nail</span><span class="logo-diy">DIY</span>
        </span>
      </router-link>

      <!-- 顶部导航 -->
      <nav class="nav">
        <router-link to="/" class="nav-link" exact-active-class="active">
          <span>首页</span>
          <i class="dot"></i>
        </router-link>
        <router-link to="/templates" class="nav-link" active-class="active">
          <span>款式</span>
          <i class="dot"></i>
        </router-link>
        <router-link to="/designer" class="nav-link" active-class="active">
          <span>设计台</span>
          <i class="dot"></i>
        </router-link>
      </nav>

      <div class="actions">
        <template v-if="auth.token">
          <router-link to="/profile" class="user-chip">
            <el-avatar v-if="auth.user?.avatar" :size="22" :src="auth.user.avatar" class="avatar-img" />
            <span v-else class="avatar-dot"></span>
            {{ auth.user?.nickname || '我' }}
          </router-link>
          <el-button text @click="onLogout">退出登录</el-button>
        </template>
        <template v-else>
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" round class="signup-btn" @click="$router.push('/register')">
            注册
          </el-button>
        </template>
        <router-link to="/cart" class="cart-btn" aria-label="购物车">
          <el-icon :size="20"><ShoppingCart /></el-icon>
          <el-badge v-if="cartStore.count > 0" :value="cartStore.count" class="cart-badge" />
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'
import { useCartStore } from '../store/cart'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const cartStore = useCartStore()
const router = useRouter()
const route = useRoute()

// 登录态变化（登录/退出）与路由切换时同步购物车徽标；加购等写操作由 request 拦截器即时刷新
watch(() => auth.token, () => cartStore.refresh())
watch(() => route.path, () => cartStore.refresh())

function onLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  cartStore.refresh()
})
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-background);
  border-bottom: 1px solid transparent;
}
.header-inner {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

/* ---------- Logo（核心视觉资产）---------- */
.brand-logo {
  font-family: var(--font-heading);
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -0.5px;
  line-height: 1;
  display: inline-flex;
  user-select: none;
}
.logo-nail { color: var(--color-foreground); }
.logo-diy {
  color: var(--color-primary);
  font-style: italic;
  transform: translateY(-1px);
  letter-spacing: -0.5px;
}

/* ---------- 导航：大写无衬线 + 小圆点指示 ---------- */
.nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  justify-content: center;
}
.nav-link {
  position: relative;
  padding: 8px 18px;
  border-radius: var(--radius-full);
  color: var(--color-muted-foreground);
  font-family: var(--font-ui);
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  transition: color .2s ease, background .2s ease;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.nav-link span { line-height: 1; }
.nav-link .dot {
  display: block;
  width: 4px; height: 4px;
  border-radius: 50%;
  background: transparent;
  transition: background .2s ease;
}
.nav-link:hover {
  color: var(--color-foreground);
  background: transparent;
}
.nav-link:hover .dot { background: var(--color-muted-foreground); }
.nav-link.active {
  color: var(--color-foreground);
  font-weight: 700;
}
.nav-link.active .dot { background: var(--color-primary); }

/* ---------- 右侧操作 ---------- */
.actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.actions .el-button {
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  padding: 8px 14px;
}
.signup-btn {
  padding: 8px 20px !important;
  font-size: 12px !important;
  background: var(--color-solid) !important;
  border-color: var(--color-solid) !important;
  color: var(--color-on-solid) !important;
  box-shadow: 0 3px 10px var(--color-solid-shadow) !important;
  transition: all .15s ease !important;
}
.signup-btn:hover {
  background: var(--color-solid-hover) !important;
  border-color: var(--color-solid-hover) !important;
  transform: translateY(-1px) !important;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 6px 8px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-foreground);
  letter-spacing: 0.3px;
}
.avatar-dot {
  width: 22px; height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
}
.avatar-img { flex-shrink: 0; }
.cart-btn {
  position: relative;
  width: 40px; height: 40px;
  display: grid; place-items: center;
  border-radius: 50%;
  color: var(--color-foreground);
  transition: background .2s ease, color .2s ease;
}
.cart-btn:hover { background: #fff; color: var(--color-primary); }
.cart-badge :deep(.el-badge__content) {
  background: var(--color-primary);
  border: none;
  font-size: 10px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .nav { display: none; }
  .header-inner { gap: 12px; }
}
</style>
