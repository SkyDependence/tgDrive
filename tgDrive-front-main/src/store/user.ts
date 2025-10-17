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
    localStorage.removeItem('tokenExpireAt')
  }
}

export interface UserInfo {
  token: string
  role: string
  userId: string
  username: string
  email: string
  tokenExpireAt: number | null
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const role = ref<string>('')
  const userId = ref<string>('')
  const username = ref<string>('')
  const email = ref<string>('')
  const tokenExpireAt = ref<number | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => {
    if (!token.value) return false
    if (tokenExpireAt.value === null) return true
    return Date.now() < tokenExpireAt.value
  })
  const userRoleText = computed(() => {
    if (role.value === 'admin') return '管理员'
    if (role.value === 'user') return '普通用户'
    return '访客'
  })
  const hasValidToken = computed(() => isLoggedIn.value)

  // 初始化用户信息（从localStorage读取）
  const initUserInfo = () => {
    token.value = localStorage.getItem('token') || ''
    role.value = localStorage.getItem('role') || ''
    userId.value = localStorage.getItem('userId') || ''
    username.value = localStorage.getItem('username') || ''
    email.value = localStorage.getItem('email') || ''
    const storedExpireAt = localStorage.getItem('tokenExpireAt')
    if (storedExpireAt) {
      const parsed = Number(storedExpireAt)
      tokenExpireAt.value = Number.isNaN(parsed) ? null : parsed
    } else {
      tokenExpireAt.value = null
    }
  }

  // 设置用户信息
  const setUserInfo = (userInfo: UserInfo) => {
    token.value = userInfo.token
    role.value = userInfo.role
    userId.value = userInfo.userId
    username.value = userInfo.username
    email.value = userInfo.email
    tokenExpireAt.value = userInfo.tokenExpireAt ?? null

    // 同步到localStorage
    localStorage.setItem('token', userInfo.token)
    localStorage.setItem('role', userInfo.role)
    localStorage.setItem('userId', userInfo.userId)
    localStorage.setItem('username', userInfo.username)
    localStorage.setItem('email', userInfo.email)
    if (tokenExpireAt.value !== null) {
      localStorage.setItem('tokenExpireAt', String(tokenExpireAt.value))
    } else {
      localStorage.removeItem('tokenExpireAt')
    }
  }

  // 清除用户信息
  const clearUserInfo = () => {
    token.value = ''
    role.value = ''
    userId.value = ''
    username.value = ''
    email.value = ''
    tokenExpireAt.value = null

    // 清除localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
    localStorage.removeItem('tokenExpireAt')
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
    tokenExpireAt,
    // 计算属性
    isLoggedIn,
    userRoleText,
    hasValidToken,
    // 方法
    initUserInfo,
    setUserInfo,
    clearUserInfo
  }
})
