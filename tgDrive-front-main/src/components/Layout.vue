<!-- src/components/Layout.vue -->
<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <el-icon class="logo-icon"><Cloudy /></el-icon>
          <div class="logo-text">
            <span class="main-title">TG-Drive</span>
            <span class="sub-title">Stanley_Legend</span>
          </div>
        </div>
        <div class="actions">
          <el-dropdown @command="handleThemeCommand" trigger="click">
            <span class="el-dropdown-link" style="outline: none; cursor: pointer;">
              <el-icon :size="20"><component :is="themeIcon" /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="light" :icon="Sunny">亮色模式</el-dropdown-item>
                <el-dropdown-item command="dark" :icon="Moon">暗色模式</el-dropdown-item>
                <el-dropdown-item command="auto" :icon="Monitor">跟随系统</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="info" plain @click="goToAbout">关于</el-button>
          <el-button type="primary" plain @click="goToAdmin">管理</el-button>
          
          <!-- 根据登录状态显示不同的按钮 -->
          <el-button v-if="!isLoggedIn" type="primary" @click="goToLogin">登录</el-button>
          <el-dropdown v-else @command="handleUserCommand" trigger="click">
            <el-avatar
              :size="32"
              src="/110871356.jpg"
              style="cursor: pointer;"
            />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Sunny, Moon, Cloudy, Monitor, SwitchButton } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useAutoSEO } from '@/composables/useSEO'

type Theme = 'light' | 'dark' | 'auto'

const router = useRouter()
const theme = ref<Theme>('auto')

// 自动加载SEO设置
useAutoSEO()

// 登录状态检查
const isLoggedIn = computed(() => {
  const token = localStorage.getItem('token')
  return !!token
})

// --- Theme Switching Logic ---
const themeIcon = computed(() => {
  if (theme.value === 'light') return Sunny
  if (theme.value === 'dark') return Moon
  return Monitor
})

const applyTheme = () => {
  if (theme.value === 'auto') {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)')
    document.documentElement.classList.toggle('dark', prefersDark.matches)
  } else {
    document.documentElement.classList.toggle('dark', theme.value === 'dark')
  }
}

const handleThemeCommand = (command: Theme) => {
  theme.value = command
  localStorage.setItem('theme', command)
  applyTheme()
}

const systemThemeChangeHandler = (e: MediaQueryListEvent) => {
  if (theme.value === 'auto') {
    document.documentElement.classList.toggle('dark', e.matches)
  }
}

// --- Background Settings ---
const applyBackgroundSettings = (settings: any) => {
  // 移除所有背景设置，使用默认白色背景
  const body = document.body
  body.style.backgroundImage = ''
  body.style.backgroundColor = '#ffffff'
  
  // 移除任何现有的遮罩层
  const overlay = document.querySelector('.background-overlay')
  if (overlay) {
    overlay.remove()
  }
}

// --- Component Lifecycle ---
onMounted(() => {
  const savedTheme = localStorage.getItem('theme') as Theme | null
  theme.value = savedTheme || 'auto'
  applyTheme()
  // 移除背景设置加载，使用默认白色背景
  applyBackgroundSettings({})
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', systemThemeChangeHandler)
})

onBeforeUnmount(() => {
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemThemeChangeHandler)
})

// --- Navigation Logic ---
const goToLogin = () => {
  router.push('/login')
}

const goToAbout = () => {
  router.push('/about')
}

const goToAdmin = () => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role');

  if (!token || userRole === 'visitor') {
    ElMessageBox.alert('您当前是访客，请使用管理员账号登录！', '权限提示', {
      confirmButtonText: '确定',
      type: 'warning'
    });
    return;
  }
  router.push('/home');
}

// 处理用户下拉菜单命令
const handleUserCommand = (command: string) => {
  if (command === 'logout') {
    handleLogout()
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {
    // 用户取消退出
  })
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background-color: var(--background-color);
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--container-bg-color);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 1200px; /* Or your preferred max-width */
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-color);
}

.logo-icon {
  font-size: 28px;
  color: #409EFF;
  filter: drop-shadow(0 0 3px rgba(64, 158, 255, 0.3));
  animation: iconColorShift 4s ease-in-out infinite;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.main-title {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(90deg, #409EFF, #ffffff, #409EFF);
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientMove 4s linear infinite;
}

.sub-title {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-regular);
  margin-top: -2px;
}

@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 200% 50%;
  }
}

@keyframes iconColorShift {
  0%, 100% {
    color: #409EFF;
    filter: drop-shadow(0 0 3px rgba(64, 158, 255, 0.3));
  }
  50% {
    color: #ffffff;
    filter: drop-shadow(0 0 3px rgba(255, 255, 255, 0.5));
  }
}

.actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.main-content {
  display: flex;
  justify-content: center;
  padding: 20px;
}

/* You might want to wrap the router-view in a container */
:deep(.el-main > *) {
  width: 100%;
  max-width: 1200px;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .header {
    height: 60px;
    padding: 8px 0;
  }

  .header-content {
    padding: 0 8px;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .logo-icon {
    font-size: 20px;
  }

  .main-title {
    font-size: 18px;
  }

  .sub-title {
    font-size: 10px;
  }

  .actions {
    gap: 4px;
    flex-wrap: nowrap;
    justify-content: flex-end;
  }

  .actions .el-button {
    min-height: 32px;
    padding: 4px 8px;
    font-size: 12px;
  }

  .main-content {
    padding: 8px;
  }

  :deep(.el-main > *) {
    max-width: 100%;
  }
}

/* 小屏幕 (平板, 576px 到 767px) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .header-content {
    padding: 0 12px;
    gap: 12px;
  }

  .logo-icon {
    font-size: 24px;
  }

  .main-title {
    font-size: 20px;
  }

  .sub-title {
    font-size: 11px;
  }

  .actions {
    gap: 8px;
    flex-wrap: wrap;
    justify-content: center;
  }

  .main-content {
    padding: 12px;
  }

  :deep(.el-main > *) {
    max-width: 100%;
  }
}

/* 中等屏幕 (768px 到 991px) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .header-content {
    padding: 0 16px;
    justify-content: space-between;
  }

  .main-content {
    padding: 16px;
  }

  :deep(.el-main > *) {
    max-width: 900px;
    margin: 0 auto;
  }
}

/* 大屏幕及以上 (992px+) */
@media (min-width: 992px) {
  .header-content {
    padding: 0 20px;
    justify-content: space-between;
  }

  .main-content {
    padding: 20px;
  }

  :deep(.el-main > *) {
    max-width: 1200px;
    margin: 0 auto;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .actions .el-button {
    min-height: 44px;
    min-width: 44px;
  }
  
  .el-dropdown-link {
    min-height: 44px;
    min-width: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

/* 横屏模式优化 */
@media (max-width: 767px) and (orientation: landscape) {
  .header {
    height: 50px;
    min-height: 50px;
  }
  
  .header-content {
    flex-direction: row;
    justify-content: space-between;
    padding: 0 12px;
  }
  
  .logo-icon {
    font-size: 20px;
  }

  .main-title {
    font-size: 18px;
  }

  .sub-title {
    font-size: 10px;
  }
  
  .actions {
    gap: 6px;
  }
  
  .main-content {
    padding: 8px 12px;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .header {
    border-bottom-width: 0.5px;
  }
}
</style>