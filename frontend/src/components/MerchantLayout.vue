<template>
  <el-container class="merchant-layout">
    <!-- 桌面端：固定侧边栏 -->
    <el-aside width="220px" class="aside hide-mobile">
      <div class="mini-brand">
        <span class="mini-brand-icon"><el-icon><Brush /></el-icon></span>
        <span>商家后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="m in MENUS" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon><span>{{ m.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="merchant-header">
        <div class="header-left">
          <!-- 移动端菜单入口 -->
          <button class="m-menu-btn" aria-label="打开菜单" @click="drawer = true">
            <el-icon :size="20"><Menu /></el-icon>
          </button>
          <router-link to="/" class="back-home"><el-icon><Back /></el-icon> 返回前台</router-link>
        </div>
        <div class="header-right">
          <span class="muted">{{ auth.user?.nickname }}</span>
          <el-button text @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <slot />
      </el-main>
    </el-container>

    <!-- 移动端抽屉菜单 -->
    <el-drawer v-model="drawer" direction="ltr" size="68%" :with-header="false">
      <div class="m-drawer">
        <div class="mini-brand">
          <span class="mini-brand-icon"><el-icon><Brush /></el-icon></span>
          <span>商家后台</span>
        </div>
        <nav class="m-nav">
          <router-link v-for="m in MENUS" :key="m.path" :to="m.path" @click="drawer = false">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.label }}</span>
          </router-link>
        </nav>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Back, Bell, Box, Brush, ChatDotRound, Collection, DataAnalysis,
  Menu, PieChart, RefreshLeft, Shop, Tickets
} from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const drawer = ref(false)
const activeMenu = computed(() => route.path)

// 菜单项数据化：桌面侧边栏与移动端抽屉共用同一份定义，避免两处维护
const MENUS = [
  { path: '/merchant', label: '数据看板', icon: DataAnalysis },
  { path: '/merchant/materials', label: '素材管理', icon: Box },
  { path: '/merchant/templates', label: '模板管理', icon: Collection },
  { path: '/merchant/orders', label: '订单中心', icon: Tickets },
  { path: '/merchant/after-sales', label: '售后管理', icon: RefreshLeft },
  { path: '/merchant/reviews', label: '评价管理', icon: ChatDotRound },
  { path: '/merchant/notices', label: '公告管理', icon: Bell },
  { path: '/merchant/stores', label: '门店管理', icon: Shop },
  { path: '/merchant/analytics', label: '销售统计', icon: PieChart }
]

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.merchant-layout { min-height: 100vh; }
.aside {
  background: var(--color-card);
  border-right: 1px solid var(--color-border);
  padding-top: var(--space-sm);
}
.mini-brand {
  display: flex; align-items: center; gap: 8px;
  padding: var(--space-md) var(--space-lg);
  font-family: var(--font-heading); font-weight: 700; font-size: 18px;
  color: var(--color-foreground);
  margin-bottom: var(--space-sm);
}
.mini-brand-icon {
  width: 32px; height: 32px; display: grid; place-items: center;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  color: #fff; border-radius: 10px; font-size: 18px;
}
.menu { border-right: none; }
.merchant-header {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
}
.back-home { color: var(--color-accent); font-size: 14px; display: inline-flex; align-items: center; gap: 4px; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-right { display: flex; align-items: center; gap: 8px; }
.muted { color: var(--color-muted-foreground); }
.content { background: var(--color-background); padding: var(--space-lg); }

/* 移动端菜单按钮：桌面隐藏 */
.m-menu-btn {
  display: none;
  width: 36px; height: 36px;
  align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 8px;
  color: var(--color-foreground); cursor: pointer;
}
.m-menu-btn:hover { background: var(--color-muted); }

/* 抽屉内菜单 */
.m-drawer { display: flex; flex-direction: column; height: 100%; }
.m-nav { display: flex; flex-direction: column; }
.m-nav a {
  display: flex; align-items: center; gap: 10px;
  padding: 13px 6px; font-size: 15px;
  color: var(--color-foreground);
  border-bottom: 1px solid var(--color-border);
}
.m-nav a.router-link-active { color: var(--color-primary); font-weight: 600; }

/* ≤768px：侧边栏收起改抽屉；内容区留白收紧 */
@media (max-width: 768px) {
  .m-menu-btn { display: flex; }
  .content { padding: var(--space-md); }
  .mini-brand { padding: var(--space-md); }
}
</style>
