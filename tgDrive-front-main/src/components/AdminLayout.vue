<!-- src/components/AdminLayout.vue -->
<template>
  <el-container class="app-container">
    <el-header class="header">
      <div class="header-logo">
        <div class="logo-container">
          <el-icon class="toggle-sidebar hidden-xs-only" @click="toggleSidebar">
            <Expand v-if="isCollapsed" />
            <Fold v-else />
          </el-icon>
          <el-icon class="logo-icon"><Monitor /></el-icon>
          <span class="logo-text hidden-xs-only" v-show="!isCollapsed">ST-TG网盘管理</span>
        </div>
      </div>
      <div class="header-actions">
        <!-- New mobile sidebar toggle -->
        <el-icon class="toggle-mobile-sidebar hidden-sm-and-up" @click="isMobileSidebarOpen = true">
          <Fold />
        </el-icon>
        <el-dropdown @command="handleThemeCommand" trigger="click">
          <span class="el-dropdown-link">
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
        <el-dropdown @command="handleUserCommand">
          <el-avatar 
            class="user-avatar" 
            :size="32"
            src="/public/favicon.ico"
          />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    
    <el-container class="main-container">
      <!-- Desktop Sidebar -->
      <el-aside v-if="!isMobile" :width="isCollapsed ? '64px' : '220px'" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleSelect"
          :collapse="isCollapsed"
          :collapse-transition="false"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          <el-menu-item index="/fileList">
            <el-icon><Folder /></el-icon>
            <template #title>文件列表</template>
          </el-menu-item>
          <el-menu-item index="/user-management">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/changePassword">
            <el-icon><EditPen /></el-icon>
            <template #title>修改密码</template>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><Top /></el-icon>
            <template #title>上传文件</template>
          </el-menu-item>
          <el-menu-item index="/backup">
            <el-icon><Download /></el-icon>
            <template #title>备份数据库</template>
          </el-menu-item>
          <el-menu-item index="/bot-keep-alive">
            <el-icon><Service /></el-icon>
            <template #title>机器人保活</template>
          </el-menu-item>
          <el-menu-item index="/webdav-config">
             <el-icon><Connection /></el-icon>
             <template #title>WebDAV配置</template>
           </el-menu-item>
           <el-menu-item index="/agreement">
             <el-icon><Setting /></el-icon>
             <template #title>用户协议</template>
           </el-menu-item>
           <el-menu-item index="/privacy">
             <el-icon><Setting /></el-icon>
             <template #title>隐私政策</template>
           </el-menu-item>

        </el-menu>
      </el-aside>

      <!-- Mobile Sidebar Drawer -->
      <el-drawer
        v-if="isMobile"
        v-model="isMobileSidebarOpen"
        direction="ltr"
        :with-header="false"
        size="220px"
      >
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleSelectAndCloseDrawer"
          :collapse="false"
          :collapse-transition="false"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          <el-menu-item index="/fileList">
            <el-icon><Folder /></el-icon>
            <template #title>文件列表</template>
          </el-menu-item>
          <el-menu-item index="/user-management">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/changePassword">
            <el-icon><EditPen /></el-icon>
            <template #title>修改密码</template>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><Top /></el-icon>
            <template #title>上传文件</template>
          </el-menu-item>
          <el-menu-item index="/backup">
            <el-icon><Download /></el-icon>
            <template #title>备份数据库</template>
          </el-menu-item>
          <el-menu-item index="/bot-keep-alive">
            <el-icon><Service /></el-icon>
            <template #title>机器人保活</template>
          </el-menu-item>
          <el-menu-item index="/webdav-config">
            <el-icon><Connection /></el-icon>
            <template #title>WebDAV配置</template>
          </el-menu-item>
          <el-menu-item index="/agreement">
            <el-icon><Setting /></el-icon>
            <template #title>用户协议</template>
          </el-menu-item>
          <el-menu-item index="/privacy">
            <el-icon><Setting /></el-icon>
            <template #title>隐私政策</template>
          </el-menu-item>
        </el-menu>
      </el-drawer>
      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  HomeFilled, Folder, Top, EditPen, Sunny, Moon, Expand, Fold, Download, Monitor, SwitchButton, Service, Connection, Setting, User
} from '@element-plus/icons-vue'
import request from '@/utils/request'

type Theme = 'light' | 'dark' | 'auto'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)
const isCollapsed = ref(false)
const theme = ref<Theme>('auto')
const isMobile = ref(false) // New ref for mobile detection
const isMobileSidebarOpen = ref(false) // New ref for mobile sidebar drawer

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

// --- Responsive Logic ---
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768 // Define mobile breakpoint
  if (isMobile.value) {
    isCollapsed.value = true // Collapse sidebar on mobile
  } else {
    // Restore sidebar state on desktop if it was expanded before
    const savedSidebarState = localStorage.getItem('sidebarState')
    isCollapsed.value = savedSidebarState === 'collapsed'
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
  // Initial check for mobile
  checkMobile()
  window.addEventListener('resize', checkMobile)

  // Restore sidebar state (only for desktop, mobile state is handled by checkMobile)
  if (!isMobile.value) {
    const savedSidebarState = localStorage.getItem('sidebarState')
    isCollapsed.value = savedSidebarState === 'collapsed'
  }
  
  // Restore theme
  const savedTheme = localStorage.getItem('theme') as Theme | null
  theme.value = savedTheme || 'auto'
  applyTheme()
  
  // 移除背景设置加载，使用默认白色背景
  applyBackgroundSettings({})
  
  // Add listener for system theme changes
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', systemThemeChangeHandler)
})

onBeforeUnmount(() => {
  // Clean up listeners
  window.removeEventListener('resize', checkMobile)
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemThemeChangeHandler)
})


// --- Other Logic ---
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebarState', isCollapsed.value ? 'collapsed' : 'expanded')
}

const handleUserCommand = (command: string) => {
  if (command === 'logout') {
    localStorage.removeItem('token');
    router.push('/login');
  }
}

const handleSelect = (index: string) => {
  router.push(index)
}

const handleSelectAndCloseDrawer = (index: string) => {
  handleSelect(index)
  isMobileSidebarOpen.value = false // Close drawer after selection
}
</script>

<style scoped>
/* Basic layout structure */
.app-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--container-bg-color);
  transition: background-color 0.3s, border-color 0.3s;
}

.header-logo, .header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.toggle-sidebar {
  cursor: pointer;
  font-size: 20px;
}

.user-avatar {
  cursor: pointer;
}

.sidebar {
  background-color: var(--container-bg-color);
  border-right: 1px solid var(--border-color);
  transition: width 0.3s, background-color 0.3s, border-color 0.3s;
  overflow: hidden;
}

.sidebar-menu {
  height: 100%;
  border-right: none;
}

.main-content {
  padding: 20px;
  background-color: var(--background-color);
  height: calc(100vh - 60px);
  overflow-y: auto;
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

/* Responsive styles */
/* Hide mobile elements on desktop */
.hidden-sm-and-up {
  display: none !important;
}

@media (max-width: 767px) { /* Mobile breakpoint */
  .hidden-xs-only {
    display: none !important;
  }
  .hidden-sm-and-up {
    display: flex !important; /* Or block, depending on element */
  }

  .header {
    padding: 0 15px; /* Smaller padding on mobile */
  }

  .header-logo .logo-text {
    display: none; /* Hide logo text on mobile */
  }

  .toggle-mobile-sidebar {
    cursor: pointer;
    font-size: 20px;
    margin-right: 10px; /* Space between toggle and logo */
  }

  .main-content {
    padding: 10px; /* Smaller padding for main content on mobile */
  }
}
</style>