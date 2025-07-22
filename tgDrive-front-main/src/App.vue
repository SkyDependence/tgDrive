<template>
  <router-view />
</template>

<script setup lang="ts">
// No script logic needed here, it's handled by layouts.
</script>

<style>
/* 全局样式重置和基础设置 */
* {
  box-sizing: border-box;
}

html, body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

#app {
  height: 100vh;
  width: 100vw;
  overflow-x: hidden; /* Prevent horizontal scrolling */
  overflow-y: auto;   /* Allow vertical scrolling when needed */
}

/* 全局响应式断点 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .container {
    padding: 0 10px;
  }
  
  .el-card {
    margin: 10px 0;
    border-radius: 8px;
  }
  
  .el-button {
    font-size: 14px;
    padding: 8px 12px;
  }
}

/* 小屏幕 (平板, 576px 及以上) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .container {
    padding: 0 15px;
  }
}

/* 中等屏幕 (桌面显示器, 768px 及以上) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .container {
    padding: 0 20px;
  }
}

/* 大屏幕 (大桌面显示器, 992px 及以上) */
@media (min-width: 992px) and (max-width: 1199.98px) {
  .container {
    padding: 0 25px;
  }
}

/* 超大屏幕 (超大桌面显示器, 1200px 及以上) */
@media (min-width: 1200px) {
  .container {
    padding: 0 30px;
    max-width: 1200px;
    margin: 0 auto;
  }
}

/* 通用响应式工具类 */
.text-center {
  text-align: center;
}

.w-100 {
  width: 100%;
}

.d-flex {
  display: flex;
}

.justify-content-center {
  justify-content: center;
}

.align-items-center {
  align-items: center;
}

.flex-column {
  flex-direction: column;
}

.flex-wrap {
  flex-wrap: wrap;
}

.gap-1 {
  gap: 8px;
}

.gap-2 {
  gap: 16px;
}

.gap-3 {
  gap: 24px;
}

/* 响应式显示/隐藏类 */
@media (max-width: 575.98px) {
  .hidden-xs {
    display: none !important;
  }
}

@media (min-width: 576px) {
  .hidden-sm-and-up {
    display: none !important;
  }
}

@media (max-width: 767.98px) {
  .hidden-sm-and-down {
    display: none !important;
  }
}

@media (min-width: 768px) {
  .hidden-md-and-up {
    display: none !important;
  }
}

/* 响应式间距 */
@media (max-width: 767px) {
  .responsive-padding {
    padding: 10px !important;
  }
  
  .responsive-margin {
    margin: 10px !important;
  }
}

@media (min-width: 768px) {
  .responsive-padding {
    padding: 20px !important;
  }
  
  .responsive-margin {
    margin: 20px !important;
  }
}
</style>