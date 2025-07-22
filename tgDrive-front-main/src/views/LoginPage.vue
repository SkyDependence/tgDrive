<template>
  <div class="login-page-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="28" color="var(--el-color-primary)"><Cloudy /></el-icon>
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-subtitle">请使用访客账户登录</p>
        <div class="account-info">
          <p><strong>访客账户:</strong> visitor / 111111</p>
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
            placeholder="请输入用户名" 
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
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Cloudy } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const loginForm = ref({
  username: '',
  password: ''
})
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名长度至少为3个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6个字符', trigger: 'blur' }
  ]
}

const handleLogin = () => {
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
        localStorage.setItem('token', userLogin.token)
        localStorage.setItem('role', userLogin.role)
        localStorage.setItem('userId', userLogin.UserId)

        if (rememberMe.value) {
          localStorage.setItem('username', loginForm.value.username)
        } else {
          localStorage.removeItem('username')
        }
        ElMessage.success(response.data.msg || '登录成功')
        router.push('/home')
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

onMounted(() => {
  const savedUsername = localStorage.getItem('username')
  if (savedUsername) {
    loginForm.value.username = savedUsername
    rememberMe.value = true
  }
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

.login-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0 0 15px 0;
}

.account-info {
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  border-left: 4px solid var(--el-color-primary);
}

.account-info p {
  margin: 5px 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.account-info strong {
  color: var(--el-text-color-primary);
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