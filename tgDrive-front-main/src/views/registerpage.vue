<template>
  <div class="register-page-container">
    <div class="register-card">
      <div class="register-header">
        <el-icon :size="28" color="var(--el-color-primary)"><Cloudy /></el-icon>
        <h2 class="register-title">创建账户</h2>
        <p class="register-subtitle">注册以开始使用 ST-TG网盘</p>
      </div>

      <el-form 
        :model="registerForm" 
        :rules="rules" 
        ref="registerFormRef" 
        label-position="top" 
        @keyup.enter="handleRegister"
        class="register-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="请输入用户名" 
            :prefix-icon="User" 
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password 
            :prefix-icon="Lock" 
            size="large"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="请确认密码" 
            show-password 
            :prefix-icon="Lock" 
            size="large"
          />
        </el-form-item>

        <el-form-item label="邮箱（可选）" prop="email">
          <el-input 
            v-model="registerForm.email" 
            type="email" 
            placeholder="请输入邮箱地址" 
            :prefix-icon="Message" 
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <CaptchaVerify @verified="onCaptchaVerified" ref="captchaRef" />
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleRegister" 
            :loading="loading" 
            :disabled="!isCaptchaVerified"
            class="register-button"
            size="large"
            block
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>

        <el-form-item class="login-link">
          <div class="login-prompt">
            <span class="prompt-text">已有账户？</span>
            <router-link to="/login" class="login-btn">
              <el-button type="primary" plain size="small">
                立即登录
              </el-button>
            </router-link>
          </div>
        </el-form-item>
        
        <!-- 协议链接 -->
        <el-form-item class="agreement-links">
          <div class="agreement-text">
            <span class="agreement-prompt">注册即表示您同意我们的</span>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Cloudy, Message } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '@/store/user'
import CaptchaVerify from '@/components/CaptchaVerify.vue'

const router = useRouter()
const userStore = useUserStore()
const registerFormRef = ref<FormInstance>()
const captchaRef = ref()
const loading = ref(false)
const isCaptchaVerified = ref(false)

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule: any, value: string) => value === registerForm.value.password, message: '两次输入密码不一致', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 验证码验证成功回调
const onCaptchaVerified = (verified: boolean) => {
  isCaptchaVerified.value = verified
}

const handleRegister = async () => {
  if (loading.value) return // 防止重复提交
  if (!registerFormRef.value) return
  
  // 检查验证码是否已验证
  if (!isCaptchaVerified.value) {
    ElMessage.warning('请先完成人机验证')
    return
  }
  
  try {
    await registerFormRef.value.validate()
    loading.value = true
    
    const response = await request.post('/auth/register', {
      username: registerForm.value.username,
      password: registerForm.value.password,
      confirmPassword: registerForm.value.confirmPassword,
      email: registerForm.value.email
    })
    
    if (response.data.code === 1) {
      const userLogin = response.data.data
      
      // 使用store设置用户信息
      userStore.setUserInfo({
        token: userLogin.token,
        role: userLogin.role,
        userId: userLogin.UserId,
        username: userLogin.username,
        email: userLogin.email
      })
      
      ElMessage.success('注册成功，已自动登录')
      // 根据用户角色跳转到对应页面
      if (userLogin.role === 'admin') {
        router.push('/home')
      } else if (userLogin.role === 'user') {
        router.push('/user/home')
      } else {
        router.push('/')
      }
    } else {
      ElMessage.error(response.data.msg || '注册失败')
      // 注册失败时重置验证码
      if (captchaRef.value) {
        captchaRef.value.reset()
        isCaptchaVerified.value = false
      }
    }
  } catch (error) {
    ElMessage.error('注册失败，请重试')
    // 注册失败时重置验证码
    if (captchaRef.value) {
      captchaRef.value.reset()
      isCaptchaVerified.value = false
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  overflow-y: auto;
  background-color: var(--background-color);
  transition: background-color 0.3s ease;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  border-radius: 12px;
  background-color: var(--container-bg-color);
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px var(--box-shadow-color);
  transition: all 0.3s ease;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-title {
  font-size: 24px;
  font-weight: 600;
  margin: 10px 0 5px;
  color: var(--text-color);
}

/* 暗色模式下的标题发光效果 */
@media (prefers-color-scheme: dark) {
  .register-title {
    text-shadow: 0 0 10px rgba(64, 158, 255, 0.4), 0 0 20px rgba(64, 158, 255, 0.2);
  }
}

.dark .register-title {
  text-shadow: 0 0 10px rgba(64, 158, 255, 0.4), 0 0 20px rgba(64, 158, 255, 0.2);
}

.register-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0 0 15px 0;
}

.register-form .el-form-item {
  margin-bottom: 25px;
}

.register-button {
  width: 100%;
}

.login-link {
  text-align: center;
  margin-top: 20px;
}

.login-prompt {
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

.login-btn {
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
  transition: all 0.3s ease;
  border-bottom: 1px solid transparent;
}

.agreement-link:hover {
  color: var(--el-color-primary-light-3);
  border-bottom-color: var(--el-color-primary-light-3);
  transform: translateY(-1px);
}

.login-btn .el-button {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.login-btn .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 响应式设计 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .register-page-container {
    padding: 8px;
    min-height: 100vh;
    justify-content: flex-start;
    padding-top: 20px;
  }

  .register-card {
    padding: 20px;
    border-radius: 8px;
    max-width: 100%;
    margin: 0 8px;
    box-shadow: 0 4px 12px var(--box-shadow-color);
  }

  .register-header {
    margin-bottom: 24px;
  }

  .register-title {
    font-size: 20px;
  }

  .register-subtitle {
    font-size: 12px;
  }

  .register-form .el-form-item {
    margin-bottom: 20px;
  }

  .login-prompt {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .register-button {
    min-height: 44px;
    font-size: 16px;
  }

  .el-input__inner {
    min-height: 44px;
    font-size: 16px;
  }
}

/* 小屏幕 (平板, 576px 到 767px) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .register-page-container {
    padding: 12px;
  }

  .register-card {
    padding: 30px;
    border-radius: 10px;
    max-width: 450px;
  }

  .register-title {
    font-size: 22px;
  }

  .register-subtitle {
    font-size: 13px;
  }

  .register-form .el-form-item {
    margin-bottom: 22px;
  }
}

/* 中等屏幕 (768px 到 991px) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .register-page-container {
    padding: 16px;
  }

  .register-card {
    padding: 35px;
    max-width: 420px;
  }
}

/* 大屏幕及以上 (992px+) */
@media (min-width: 992px) {
  .register-page-container {
    padding: 20px;
  }

  .register-card {
    padding: 40px;
    max-width: 400px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .register-button {
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
  .register-page-container {
    padding: 8px 20px;
    justify-content: center;
  }
  
  .register-card {
    padding: 16px 24px;
    max-width: 500px;
  }
  
  .register-header {
    margin-bottom: 16px;
  }
  
  .register-form .el-form-item {
    margin-bottom: 16px;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .register-card {
    border-width: 0.5px;
  }
}

/* 暗色模式特定样式 */
html.dark .register-page-container {
  background: linear-gradient(135deg, #0c0c0c 0%, #1a1a1a 50%, #0c0c0c 100%);
  position: relative;
}

html.dark .register-page-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(circle at 20% 20%, rgba(64, 158, 255, 0.1), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(112, 79, 229, 0.1), transparent 40%),
    radial-gradient(circle at 40% 60%, rgba(16, 185, 129, 0.05), transparent 30%);
  z-index: -1;
}

html.dark .register-card {
  background-color: rgba(26, 26, 26, 0.9);
  border: 1px solid rgba(64, 158, 255, 0.2);
  box-shadow: 
    0 20px 40px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(64, 158, 255, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
}

html.dark .register-title {
  color: #ffffff;
  text-shadow: 0 0 20px rgba(64, 158, 255, 0.3);
}

html.dark .register-subtitle {
  color: rgba(255, 255, 255, 0.7);
}

/* 暗色模式下的输入框样式增强 */
html.dark .el-input__wrapper {
  background-color: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
  transition: all 0.3s ease;
}

html.dark .el-input__wrapper:hover {
  border-color: rgba(64, 158, 255, 0.4) !important;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.1);
}

html.dark .el-input__wrapper.is-focus {
  border-color: var(--el-color-primary) !important;
  box-shadow: 0 0 15px rgba(64, 158, 255, 0.2);
}

html.dark .el-input__inner {
  color: #ffffff !important;
}

html.dark .el-input__inner::placeholder {
  color: rgba(255, 255, 255, 0.5) !important;
}

/* 暗色模式下的按钮样式增强 */
html.dark .register-button.el-button--primary {
  background: linear-gradient(135deg, #409eff 0%, #5a9cff 100%);
  border: none;
  box-shadow: 
    0 8px 20px rgba(64, 158, 255, 0.3),
    0 0 0 1px rgba(64, 158, 255, 0.2);
  transition: all 0.3s ease;
}

html.dark .register-button.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 12px 25px rgba(64, 158, 255, 0.4),
    0 0 0 1px rgba(64, 158, 255, 0.3);
}

html.dark .register-button.el-button--primary:active {
  transform: translateY(0);
}
</style>