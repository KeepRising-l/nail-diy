<template>
  <footer class="page-footer">
    <div class="container">
      <p class="font-heading" style="font-size: 18px; margin-bottom: 6px;">Nail DIY 美甲定制</p>
      <p>在线设计你的专属美甲 · 到店自提或邮寄到家</p>
      <p class="copy">© {{ year }} 毕业设计项目 · DIY 美甲定制平台</p>
    </div>
  </footer>
</template>

<script setup>
const year = new Date().getFullYear()
</script>
