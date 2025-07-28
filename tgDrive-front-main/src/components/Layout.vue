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
          <!-- 普通用户导航 - 大屏幕显示按钮 -->
          <div v-if="isLoggedIn && userRole === 'user'" class="user-nav-desktop">
            <el-button type="text" @click="goToUserHome">我的文件</el-button>
            <el-button type="text" @click="goToUpload">上传文件</el-button>
            <el-button type="text" @click="goToChangePassword">修改密码</el-button>
          </div>
          
          <!-- 普通用户导航 - 小屏幕显示下拉菜单 -->
          <el-dropdown v-if="isLoggedIn && userRole === 'user'" class="user-nav-mobile" @command="handleNavCommand" trigger="click">
            <el-button type="text" :icon="Menu">
              菜单
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home" :icon="Folder">我的文件</el-dropdown-item>
                <el-dropdown-item command="upload" :icon="Upload">上传文件</el-dropdown-item>
                <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <el-button type="info" plain @click="goToAbout">关于</el-button>
          <el-button v-if="userRole === 'admin'" type="primary" plain @click="goToAdmin">管理</el-button>
          
          <!-- 根据登录状态显示不同的按钮 -->
          <el-button v-if="!isLoggedIn" type="primary" @click="goToLogin">登录</el-button>
          <el-dropdown v-else @command="handleUserCommand" trigger="click">
            <div class="user-info">
              <el-avatar
                :size="32"
                src="/110871356.jpg"
                style="cursor: pointer;"
              />
              <div class="user-details">
                <div class="username">{{ currentUsername }}</div>
                <div class="user-role">{{ userRoleText }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="dropdown-user-info">
                    <div class="dropdown-username">{{ currentUsername }}</div>
                    <div class="dropdown-email">{{ currentEmail }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 导航菜单已移至顶部header中 -->

    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
    
    <!-- 页脚 -->
    <el-footer class="footer">
      <div class="footer-content">
        <div class="footer-links">
          <el-button type="text" @click="goToAbout" class="footer-link">
            关于我们
          </el-button>
          <el-divider direction="vertical" />
          <el-button type="text" @click="goToAgreement" class="footer-link">
            用户协议
          </el-button>
          <el-divider direction="vertical" />
          <el-button type="text" @click="goToPrivacy" class="footer-link">
            隐私政策
          </el-button>
        </div>
        <div class="footer-copyright">
          <span>© 2025 TG-Drive. All rights reserved.</span>
        </div>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Sunny, Moon, Cloudy, Monitor, SwitchButton, Menu, Folder, Upload, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

type Theme = 'light' | 'dark' | 'auto'

const router = useRouter()
const theme = ref<Theme>('auto')
const userStore = useUserStore()

// 使用store中的用户状态
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userRole = computed(() => userStore.role)
const currentUsername = computed(() => userStore.username || '用户')
const currentEmail = computed(() => userStore.email || '')
const userRoleText = computed(() => userStore.userRoleText)

// 当前激活的菜单项
const activeIndex = computed(() => {
  return router.currentRoute.value.path
})

// 处理菜单选择
const handleMenuSelect = (index: string) => {
  router.push(index)
}

// --- Theme Switching Logic ---
const themeIcon = computed(() => {
  if (theme.value === 'light') return Sunny
  if (theme.value === 'dark') return Moon
  return Monitor
})

const applyTheme = () => {
  // 添加主题切换动画类
  document.body.classList.add('theme-switching')
  
  // 延迟应用主题以配合动画
  setTimeout(() => {
    if (theme.value === 'auto') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)')
      document.documentElement.classList.toggle('dark', prefersDark.matches)
    } else {
      document.documentElement.classList.toggle('dark', theme.value === 'dark')
    }
  }, 80)
  
  // 移除动画类
  setTimeout(() => {
    document.body.classList.remove('theme-switching')
  }, 600)
}

const handleThemeCommand = (command: Theme) => {
  theme.value = command
  localStorage.setItem('theme', command)
  
  // 添加触觉反馈（如果支持）
  if ('vibrate' in navigator) {
    navigator.vibrate(50)
  }
  
  applyTheme()
}

const systemThemeChangeHandler = (e: MediaQueryListEvent) => {
  if (theme.value === 'auto') {
    // 系统主题变化时也添加动画
    document.body.classList.add('theme-switching')
    
    setTimeout(() => {
      document.documentElement.classList.toggle('dark', e.matches)
    }, 80)
    
    setTimeout(() => {
      document.body.classList.remove('theme-switching')
    }, 600)
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
  // 初始化用户信息
  userStore.initUserInfo()
  
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

// 导航到用户主页
const goToUserHome = () => {
  router.push('/user/home')
}

// 导航到上传页面
const goToUpload = () => {
  router.push('/user/upload')
}

// 导航到修改密码页面
const goToChangePassword = () => {
  router.push('/user/changePassword')
}

// 导航到用户协议页面
const goToAgreement = () => {
  router.push('/agreement')
}

// 导航到隐私政策页面（暂时跳转到关于页面）
const goToPrivacy = () => {
  router.push('/privacy')
}

// 处理导航下拉菜单命令
const handleNavCommand = (command: string) => {
  switch (command) {
    case 'home':
      goToUserHome()
      break
    case 'upload':
      goToUpload()
      break
    case 'password':
      goToChangePassword()
      break
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.clearUserInfo()
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

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: var(--el-fill-color-light);
}

.user-details {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.user-role {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1;
}

.dropdown-user-info {
  padding: 8px 0;
  text-align: center;
}

.dropdown-username {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.dropdown-email {
  font-size: 12px;
  color: var(--el-text-color-regular);
  word-break: break-all;
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

/* 用户导航菜单样式 */
.user-nav-menu {
  border-bottom: 1px solid var(--border-color);
  background-color: var(--container-bg-color);
  justify-content: center;
}

.user-nav-menu .el-menu-item {
  color: var(--text-color);
  border-bottom: 2px solid transparent;
}

.user-nav-menu .el-menu-item:hover {
  background-color: var(--el-fill-color-light);
}

.user-nav-menu .el-menu-item.is-active {
  color: var(--el-color-primary);
  border-bottom-color: var(--el-color-primary);
}

/* 响应式设计 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .header {
    height: 60px;
    padding: 8px 0;
  }
  
  .user-nav-menu {
    flex-direction: column;
    height: auto;
  }
  
  .user-nav-menu .el-menu-item {
    height: 40px;
    line-height: 40px;
  }
  
  .user-details {
    display: none;
  }
  
  .actions {
    gap: 8px;
  }
}

/* 中等屏幕 (平板, 576px-768px) */
@media (max-width: 768px) {
  .username {
    font-size: 13px;
  }
  
  .user-role {
    font-size: 11px;
  }
}

/* 超小屏幕样式继续 */
@media (max-width: 575.98px) {
  .header-content {
    padding: 0 8px;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .logo {
    gap: 8px;
  }

  .logo-icon {
    font-size: 20px;
  }

  .logo-text {
    flex-direction: row;
    align-items: center;
    gap: 4px;
  }

  .main-title {
    font-size: 16px;
    white-space: nowrap;
  }

  .sub-title {
    display: none;
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

  .logo {
    gap: 10px;
  }

  .logo-icon {
    font-size: 24px;
  }

  .logo-text {
    flex-direction: row;
    align-items: center;
    gap: 6px;
  }

  .main-title {
    font-size: 18px;
    white-space: nowrap;
  }

  .sub-title {
    font-size: 10px;
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

/* 用户导航响应式样式 */
.user-nav-desktop {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-nav-mobile {
  display: none;
}

/* 小屏幕时隐藏桌面导航，显示移动端下拉菜单 */
@media (max-width: 768px) {
  .user-nav-desktop {
    display: none;
  }
  
  .user-nav-mobile {
    display: block;
  }
}

/* 页脚样式 */
.footer {
  height: auto;
  padding: 20px 0;
  background: var(--container-bg-color);
  border-top: 1px solid var(--border-color);
  margin-top: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-link {
  color: var(--el-text-color-regular);
  font-size: 14px;
  padding: 4px 8px;
  transition: all 0.3s ease;
}

.footer-link:hover {
  color: var(--el-color-primary);
  transform: translateY(-1px);
}

.footer-copyright {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
}

/* 页脚响应式设计 */
@media (max-width: 768px) {
  .footer {
    padding: 16px 0;
  }
  
  .footer-content {
    padding: 0 16px;
  }
  
  .footer-links {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .footer-link {
    font-size: 13px;
  }
  
  .footer-copyright {
    font-size: 11px;
  }
}

/* 暗色模式页脚适配 */
.dark .footer {
  background: var(--el-bg-color-page);
  border-top-color: var(--el-border-color);
}

/* 超小屏幕优化 */
@media (max-width: 480px) {
  .header-content {
    padding: 0 12px;
  }
  
  .actions {
    gap: 4px;
  }
  
  .actions .el-button {
    padding: 8px 12px;
    font-size: 14px;
  }
  
  .user-nav-mobile .el-button {
    padding: 8px 10px;
  }
}
</style>