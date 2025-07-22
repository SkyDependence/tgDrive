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

        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleRegister" 
            :loading="loading" 
            class="register-button"
            size="large"
            block
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>

        <el-form-item class="login-link">
          <router-link to="/login">已有账户？立即登录</router-link>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Cloudy } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: ''
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
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    loading.value = true
    
    const response = await request.post('/api/register', {
      username: registerForm.value.username,
      password: registerForm.value.password
    })
    
    if (response.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(response.data.msg || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 使用与LoginPage相同的样式 */
.register-page-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, var(--bg-gradient-start), var(--bg-gradient-end));
  padding: 20px;
  box-sizing: border-box;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: var(--card-bg);
  border-radius: 12px;
  box-shadow: 0 10px 30px var(--box-shadow-color);
  transition: all 0.3s ease;
}

/* 其余样式与LoginPage相同 */
</style>