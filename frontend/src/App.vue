<template>
  <!-- 管理端：平台管理布局 -->
  <admin-layout v-if="isAdmin">
    <router-view />
  </admin-layout>

  <!-- 商家端：独立后台布局 -->
  <merchant-layout v-else-if="isMerchant">
    <router-view />
  </merchant-layout>

  <!-- 顾客端：顶部导航 + 页脚布局 -->
  <template v-else>
    <app-header />
    <router-view />
    <app-footer />
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import MerchantLayout from './components/MerchantLayout.vue'
import AdminLayout from './components/AdminLayout.vue'

const route = useRoute()
const isMerchant = computed(() => route.path.startsWith('/merchant'))
const isAdmin = computed(() => route.path.startsWith('/admin'))
</script>
