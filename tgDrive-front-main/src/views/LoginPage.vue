<template>
  <div class="login-page-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="28" color="var(--el-color-primary)"><Cloudy /></el-icon>
        <h2 class="login-title">欢迎回来</h2>
        <p v-if="isVisitorAllowed" class="login-subtitle">您可以使用访客账户登录</p>
        <div v-if="isVisitorAllowed" class="account-info">
          <p>
            <strong>访客账户:</strong>
            <span class="copyable-text" @click="copyToClipboard('visitor')">visitor</span> /
            <span class="copyable-text" @click="copyToClipboard('hello')">hello</span>
          </p>
        </div>
      </div>

      <el-form 
        :model="loginForm" 
        :rules="rules" 
        ref="loginFormRef" 
        label-position="top" 
        @keyup.enter="handleLogin"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="loginForm.username" 
            :prefix-icon="User" 
            placeholder="请输入用户名或邮箱" 
            size="large"
            clearable 
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="loginForm.password" 
            :prefix-icon="Lock" 
            type="password"
            placeholder="请输入密码" 
            size="large"
            show-password 
            clearable 
          />
        </el-form-item>

        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" @click="forgotPassword">忘记密码？</el-link>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleLogin" 
            :loading="loading" 
            class="login-button"
            size="large"
            block
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>

        <el-form-item v-if="isRegistrationAllowed" class="register-link">
          <div class="register-prompt">
            <span class="prompt-text">没有账户？</span>
            <router-link to="/register" class="register-btn">
              <el-button type="primary" plain size="small">
                立即注册
              </el-button>
            </router-link>
          </div>
        </el-form-item>
        
        <!-- 协议链接 -->
        <el-form-item class="agreement-links">
          <div class="agreement-text">
            <span class="agreement-prompt">登录即表示您同意我们的</span>
            <router-link to="/agreement" class="agreement-link">
              用户协议
            </router-link>
            <span class="agreement-separator">和</span>
            <router-link to="/privacy" class="agreement-link">
              隐私政策
            </router-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Cloudy } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '@/store/user'

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(`"${text}" 已复制到剪贴板！`);
  } catch (err) {
    console.error('复制失败了喵...', err);
    ElMessage.error('呜喵... 复制失败了，请手动复制哦');
  }
};
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loginForm = ref({
  username: '',
  password: ''
})
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)
const isRegistrationAllowed = ref(false)
const isVisitorAllowed = ref(false)

// 登录页只做“必填”校验：长度/复杂度是注册时的约束，
// 登录仅验证凭证是否匹配，套用注册规则会导致历史账号（如访客 hello）无法登录
const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = () => {
  if (loading.value) return // 防止重复提交
  
  loginFormRef.value?.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await request.post('/auth/login', {
        username: loginForm.value.username,
        password: loginForm.value.password
      })

      if (response.data.code === 1) {
        const userLogin = response.data.data
        
        // 使用store设置用户信息
        userStore.setUserInfo({
          token: userLogin.token,
          role: userLogin.role,
          userId: userLogin.UserId,
          username: userLogin.username,
          email: userLogin.email,
          tokenExpireAt: userLogin.expireAt ?? null
        })

        if (rememberMe.value) {
          localStorage.setItem('rememberedUsername', loginForm.value.username)
        } else {
          localStorage.removeItem('rememberedUsername')
        }
        ElMessage.success(response.data.msg || '登录成功')
        
        const redirect = route.query.redirect as string | undefined
        if (redirect) {
          router.push(redirect)
        } else {
          // 根据用户角色跳转到对应页面
          if (userLogin.role === 'admin') {
            router.push('/home')
          } else if (userLogin.role === 'user') {
            router.push('/user/home')
          } else {
            router.push('/')
          }
        }
      } else {
        ElMessage.error(response.data.msg || '登录失败')
      }
    } catch (error) {
      console.error('登录出错', error)
      ElMessage.error('登录失败，请检查网络或联系管理员')
    } finally {
      loading.value = false
    }
  })
}

const forgotPassword = () => {
  ElMessageBox.alert('请联系管理员重置您的密码。', '忘记密码', {
    confirmButtonText: '好的',
  })
}

const checkRegistrationStatus = async () => {
  try {
    const response = await request.get('/setting/registration-status')
    if (response.data.code === 1) {
      isRegistrationAllowed.value = response.data.data.isRegistrationAllowed
    }
  } catch (error) {
    console.error('获取注册状态失败:', error)
    // 出现错误时默认为不允许注册
    isRegistrationAllowed.value = false
  }
}

const checkVisitorStatus = async () => {
  try {
    const response = await request.get('/setting/visitor-status')
    if (response.data.code === 1) {
      isVisitorAllowed.value = response.data.data.isVisitorAllowed
    }
  } catch (error) {
    console.error('获取访客状态失败:', error)
    // 出现错误时默认不展示访客凭证
    isVisitorAllowed.value = false
  }
}

onMounted(() => {
  // 读取记住的用户名（键名与保存时保持一致）
  const savedUsername = localStorage.getItem('rememberedUsername')
  if (savedUsername) {
    loginForm.value.username = savedUsername
    rememberMe.value = true
  }
  checkRegistrationStatus()
  checkVisitorStatus()
})
</script>

<style scoped>
.login-page-container {
  display: flex;
  justify-content: center;
  align-items: center; /* Re-center vertically */
  min-height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Ensure scrolling is possible if content overflows */
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  border-radius: 12px;
  background-color: var(--container-bg-color);
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px var(--box-shadow-color);
  transition: all 0.3s ease;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  margin: 10px 0 5px;
  color: var(--text-color);
}

/* 暗色模式下的发光效果 */
@media (prefers-color-scheme: dark) {
  .login-title {
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.3), 0 0 16px rgba(255, 255, 255, 0.1);
  }
}

/* Element Plus 暗色主题适配 */
.dark .login-title {
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3), 0 0 16px rgba(255, 255, 255, 0.1);
}

.login-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0 0 15px 0;
}

.account-info {
  background: var(--el-bg-color-page);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color);
  position: relative;
}

.account-info::after {
  content: '🎭';
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: 24px;
  opacity: 0.9;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1)) drop-shadow(0 0 12px rgba(59, 130, 246, 0.6)) brightness(1.2);
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-2px) rotate(5deg); }
}

.account-info:hover {
  border-color: var(--el-border-color-hover);
}

.account-info p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.account-info p::before {
  content: '🔑';
  font-size: 16px;
}

.account-info strong {
  color: var(--el-color-primary);
  font-weight: 600;
}

.copyable-text {
  cursor: pointer; /* 这会让鼠标放上去的时候，变成一只小手手，告诉主人这里可以点哦！ */
  font-weight: bold; /* 让它和密码一样显眼 */
  color: var(--el-color-primary); /* 用主题色，更漂亮！ */
  transition: color 0.2s ease; /* 加个小小的过渡动画 */
  padding: 0 2px;
}

.copyable-text:hover {
  color: var(--el-color-primary-light-3); /* 鼠标放上去会变色，就像是在说‘点我喵！点我喵！’ */
  border-bottom-style: solid;
}

/* 暗色模式和Element Plus主题会自动通过CSS变量适配 */

/* 响应式优化 */
@media (max-width: 575.98px) {
  .account-info {
    padding: 12px;
    margin-bottom: 12px;
    border-radius: 12px;
  }
  
  .account-info::after {
    top: 12px;
    right: 16px;
    font-size: 20px;
  }
  
  .account-info p {
    font-size: 13px;
    gap: 6px;
  }
  
  .account-info p::before {
    font-size: 14px;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .account-info {
    border-width: 0.5px;
  }
}

/* 减少动画效果的用户偏好 */
@media (prefers-reduced-motion: reduce) {
  .account-info {
    transition: none;
  }
  
  .account-info::before {
    animation: none;
  }
  
  .account-info:hover {
    transform: none;
  }
}

.login-form .el-form-item {
  margin-bottom: 25px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-button {
  width: 100%;
}

.register-link {
  text-align: center;
  margin-top: 20px;
}

.register-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.prompt-text {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.register-btn {
  text-decoration: none;
}

/* 协议链接样式 */
.agreement-links {
  margin-top: 16px;
}

.agreement-text {
  text-align: center;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-placeholder);
}

.agreement-prompt,
.agreement-separator {
  color: var(--el-text-color-placeholder);
}

.agreement-link {
  color: var(--el-color-primary);
  text-decoration: none;
  margin: 0 2px;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.3s ease;
  border-bottom: 1px solid transparent;
  position: relative;
}

.agreement-link:hover {
  color: var(--el-color-primary-light-3);
  background-color: rgba(64, 158, 255, 0.1);
  border-bottom-color: var(--el-color-primary-light-3);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.agreement-link:active {
  color: var(--el-color-primary-dark-2);
  background-color: rgba(64, 158, 255, 0.2);
  transform: translateY(0px);
  box-shadow: 0 1px 4px rgba(64, 158, 255, 0.3);
}

.register-btn .el-button {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.register-btn .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 响应式设计 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .login-page-container {
    padding: 8px;
    min-height: 100vh;
    justify-content: flex-start;
    padding-top: 20px;
  }

  .login-card {
    padding: 20px;
    border-radius: 8px;
    max-width: 100%;
    margin: 0 8px;
    box-shadow: 0 4px 12px var(--box-shadow-color);
  }

  .login-header {
    margin-bottom: 24px;
  }

  .login-title {
    font-size: 20px;
  }

  .login-subtitle {
    font-size: 12px;
  }

  .login-form .el-form-item {
    margin-bottom: 20px;
  }

  .login-options {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .login-button {
    min-height: 44px; /* 触摸友好的按钮高度 */
    font-size: 16px;
  }

  .el-input__inner {
    min-height: 44px;
    font-size: 16px; /* 防止iOS缩放 */
  }
}

/* 小屏幕 (平板, 576px 到 767px) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .login-page-container {
    padding: 12px;
  }

  .login-card {
    padding: 30px;
    border-radius: 10px;
    max-width: 450px;
  }

  .login-title {
    font-size: 22px;
  }

  .login-subtitle {
    font-size: 13px;
  }

  .login-form .el-form-item {
    margin-bottom: 22px;
  }
}

/* 中等屏幕 (768px 到 991px) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .login-page-container {
    padding: 16px;
  }

  .login-card {
    padding: 35px;
    max-width: 420px;
  }
}

/* 大屏幕及以上 (992px+) */
@media (min-width: 992px) {
  .login-page-container {
    padding: 20px;
  }

  .login-card {
    padding: 40px;
    max-width: 400px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .login-button {
    min-height: 48px;
  }
  
  .el-input {
    min-height: 48px;
  }
  
  .el-checkbox {
    min-height: 44px;
  }
}

/* 横屏模式优化 */
@media (max-width: 767px) and (orientation: landscape) {
  .login-page-container {
    padding: 8px 20px;
    justify-content: center;
  }
  
  .login-card {
    padding: 16px 24px;
    max-width: 500px;
  }
  
  .login-header {
    margin-bottom: 16px;
  }
  
  .login-form .el-form-item {
    margin-bottom: 16px;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .login-card {
    border-width: 0.5px;
  }
}
</style>
