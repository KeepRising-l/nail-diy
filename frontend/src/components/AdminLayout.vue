<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="mini-brand">
        <span class="mini-brand-icon"><el-icon><Platform /></el-icon></span>
        <span>平台管理</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon><span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
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
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
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
  background: linear-gradient(135deg, var(--color-foreground), #6b6257);
  color: #fff; border-radius: 10px; font-size: 18px;
}
.menu { border-right: none; }
.admin-header {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
}
.back-home { color: var(--color-accent); font-size: 14px; display: inline-flex; align-items: center; gap: 4px; }
.header-right { display: flex; align-items: center; gap: 8px; }
.muted { color: var(--color-muted-foreground); }
.content { background: var(--color-background); padding: var(--space-lg); }
</style>
