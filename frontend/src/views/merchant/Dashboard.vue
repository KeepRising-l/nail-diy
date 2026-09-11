<template>
  <el-container class="page">
    <el-aside width="200px" style="background: #fff">
      <el-menu :router="true" :default-active="$route.path">
        <el-menu-item index="/merchant">数据看板（change-08）</el-menu-item>
        <el-menu-item index="/merchant/materials">素材管理</el-menu-item>
        <el-menu-item index="/merchant/templates">模板管理</el-menu-item>
        <el-menu-item index="/merchant/orders">订单中心</el-menu-item>
        <el-menu-item index="/merchant/reviews">评价管理</el-menu-item>
        <el-menu-item index="/merchant/analytics">统计看板</el-menu-item>
        <el-menu-item index="5" disabled>门店与公告（change-09）</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <el-card>
        <template #header>商家后台（占位页）</template>
        <p>商家端骨架已就绪，各功能模块将按 ROADMAP 顺序实现。</p>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
</script>

<style scoped>
.page { min-height: 100vh; }
</style>
