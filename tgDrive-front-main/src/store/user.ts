import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

// 全局清除用户信息的方法，供request.ts等工具文件使用
let globalClearUserInfo: (() => void) | null = null

export const setGlobalClearUserInfo = (clearFn: () => void) => {
  globalClearUserInfo = clearFn
}

export const callGlobalClearUserInfo = () => {
  if (globalClearUserInfo) {
    globalClearUserInfo()
  } else {
    // 如果store还没初始化，直接清除localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
  }
}

export interface UserInfo {
  token: string
  role: string
  userId: string
  username: string
  email: string
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const role = ref<string>('')
  const userId = ref<string>('')
  const username = ref<string>('')
  const email = ref<string>('')

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userRoleText = computed(() => {
    if (role.value === 'admin') return '管理员'
    if (role.value === 'user') return '普通用户'
    return '访客'
  })

  // 初始化用户信息（从localStorage读取）
  const initUserInfo = () => {
    token.value = localStorage.getItem('token') || ''
    role.value = localStorage.getItem('role') || ''
    userId.value = localStorage.getItem('userId') || ''
    username.value = localStorage.getItem('username') || ''
    email.value = localStorage.getItem('email') || ''
  }

  // 设置用户信息
  const setUserInfo = (userInfo: UserInfo) => {
    token.value = userInfo.token
    role.value = userInfo.role
    userId.value = userInfo.userId
    username.value = userInfo.username
    email.value = userInfo.email

    // 同步到localStorage
    localStorage.setItem('token', userInfo.token)
    localStorage.setItem('role', userInfo.role)
    localStorage.setItem('userId', userInfo.userId)
    localStorage.setItem('username', userInfo.username)
    localStorage.setItem('email', userInfo.email)
  }

  // 清除用户信息
  const clearUserInfo = () => {
    token.value = ''
    role.value = ''
    userId.value = ''
    username.value = ''
    email.value = ''

    // 清除localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
  }

  // 注册全局清除方法
  setGlobalClearUserInfo(clearUserInfo)

  return {
    // 状态
    token,
    role,
    userId,
    username,
    email,
    // 计算属性
    isLoggedIn,
    userRoleText,
    // 方法
    initUserInfo,
    setUserInfo,
    clearUserInfo
  }
})