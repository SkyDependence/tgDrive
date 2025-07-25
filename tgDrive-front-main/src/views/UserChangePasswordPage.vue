<template>
  <div class="change-password-page">
    <div class="password-container">
      <el-card class="password-card">
        <template #header>
          <div class="card-header">
            <h2 class="page-title">修改密码</h2>
            <p class="page-subtitle">为了您的账户安全，请定期更换密码</p>
          </div>
        </template>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="120px"
          class="password-form"
        >
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              placeholder="请输入当前密码"
              show-password
              size="large"
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
              size="large"
            />
            <div class="password-strength">
              <div class="strength-label">密码强度：</div>
              <div class="strength-bar">
                <div 
                  class="strength-fill" 
                  :class="passwordStrengthClass"
                  :style="{ width: passwordStrengthWidth }"
                ></div>
              </div>
              <div class="strength-text" :class="passwordStrengthClass">
                {{ passwordStrengthText }}
              </div>
            </div>
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <el-button 
                type="primary" 
                size="large"
                :loading="loading"
                @click="handleChangePassword"
              >
                {{ loading ? '修改中...' : '修改密码' }}
              </el-button>
              <el-button 
                size="large"
                @click="resetForm"
              >
                重置
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 安全提示 -->
      <el-card class="security-tips">
        <template #header>
          <h3>安全提示</h3>
        </template>
        <ul class="tips-list">
          <li>密码长度至少8位，建议包含大小写字母、数字和特殊字符</li>
          <li>不要使用与用户名相同或过于简单的密码</li>
          <li>不要在多个网站使用相同的密码</li>
          <li>定期更换密码，建议每3-6个月更换一次</li>
          <li>如果怀疑密码泄露，请立即修改密码</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const passwordFormRef = ref<FormInstance>()
const loading = ref(false)

const passwordForm = reactive<PasswordForm>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码强度计算
const passwordStrength = computed(() => {
  const password = passwordForm.newPassword
  if (!password) return 0
  
  let strength = 0
  
  // 长度检查
  if (password.length >= 8) strength += 1
  if (password.length >= 12) strength += 1
  
  // 字符类型检查
  if (/[a-z]/.test(password)) strength += 1
  if (/[A-Z]/.test(password)) strength += 1
  if (/[0-9]/.test(password)) strength += 1
  if (/[^a-zA-Z0-9]/.test(password)) strength += 1
  
  return Math.min(strength, 4)
})

const passwordStrengthClass = computed(() => {
  const strength = passwordStrength.value
  if (strength <= 1) return 'weak'
  if (strength <= 2) return 'medium'
  if (strength <= 3) return 'strong'
  return 'very-strong'
})

const passwordStrengthWidth = computed(() => {
  return `${(passwordStrength.value / 4) * 100}%`
})

const passwordStrengthText = computed(() => {
  const strength = passwordStrength.value
  if (strength === 0) return ''
  if (strength <= 1) return '弱'
  if (strength <= 2) return '中等'
  if (strength <= 3) return '强'
  return '很强'
})

// 表单验证规则
const passwordRules: FormRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === passwordForm.currentPassword) {
          callback(new Error('新密码不能与当前密码相同'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return
    
    if (loading.value) return
    
    loading.value = true
    
    await request.put('/api/user/change-password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
    
    ElMessage.success('密码修改成功！')
    
    // 清空表单
    resetForm()
    
    // 提示用户重新登录
    setTimeout(() => {
      ElMessageBox.confirm(
        '密码已修改成功，为了安全起见，请重新登录。',
        '修改成功',
        {
          confirmButtonText: '重新登录',
          cancelButtonText: '稍后登录',
          type: 'success'
        }
      ).then(() => {
        // 清除登录信息并跳转到登录页
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        localStorage.removeItem('userId')
        window.location.href = '/login'
      }).catch(() => {
        // 用户选择稍后登录
      })
    }, 1000)
    
  } catch (error: any) {
    if (error.response?.status === 400) {
      ElMessage.error('当前密码错误')
    } else {
      ElMessage.error('密码修改失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields()
  }
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>

<style scoped>
.change-password-page {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.password-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.password-card {
  border-radius: 8px;
}

.card-header {
  text-align: center;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--el-text-color-regular);
  margin: 0;
}

.password-form {
  padding: 20px 0;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
}

.strength-label {
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background-color: var(--el-fill-color);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s;
  border-radius: 2px;
}

.strength-fill.weak {
  background-color: #f56c6c;
}

.strength-fill.medium {
  background-color: #e6a23c;
}

.strength-fill.strong {
  background-color: #67c23a;
}

.strength-fill.very-strong {
  background-color: #409eff;
}

.strength-text {
  white-space: nowrap;
  font-weight: 500;
}

.strength-text.weak {
  color: #f56c6c;
}

.strength-text.medium {
  color: #e6a23c;
}

.strength-text.strong {
  color: #67c23a;
}

.strength-text.very-strong {
  color: #409eff;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 20px;
}

.security-tips {
  border-radius: 8px;
}

.security-tips h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.tips-list li {
  margin-bottom: 8px;
}

.tips-list li:last-child {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .change-password-page {
    padding: 10px;
  }
  
  .password-form {
    padding: 10px 0;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .password-strength {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }
  
  .strength-label {
    align-self: flex-start;
  }
}
</style>