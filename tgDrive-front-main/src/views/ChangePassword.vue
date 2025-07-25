<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <el-icon><Edit /></el-icon>
          <span>{{ isAdmin ? '管理员修改用户密码' : '修改密码' }}</span>
        </div>
      </template>

      <el-form 
          :model="passwordForm" 
          :rules="rules" 
          ref="passwordFormRef" 
          label-position="top"
          class="password-form"
        >
        <el-form-item v-if="isAdmin" label="用户名" prop="username">
          <el-input 
            v-model="passwordForm.username" 
            :prefix-icon="User" 
            placeholder="请输入要修改密码的用户名"
            size="large"
            clearable 
          />
        </el-form-item>

        <el-form-item v-if="!isAdmin" label="旧密码" prop="oldPassword">
          <el-input 
            v-model="passwordForm.oldPassword" 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请输入您的当前密码"
            size="large"
            show-password 
            clearable 
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="passwordForm.newPassword" 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请输入 6-16 位的新密码"
            size="large"
            show-password 
            clearable 
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input 
            v-model="passwordForm.confirmPassword" 
            :prefix-icon="CircleCheck" 
            type="password" 
            placeholder="请再次输入新密码"
            size="large"
            show-password 
            clearable 
          />
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="changePassword" 
            :loading="loading" 
            class="submit-button"
            size="large"
            block
          >
            {{ loading ? '正在提交...' : (isAdmin ? '修改用户密码' : '确认修改') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { Lock, Edit, CircleCheck, User } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')

const passwordForm = ref({
  username: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordFormRef = ref<FormInstance>()
const loading = ref(false)

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.value.newPassword) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const rules = computed(() => {
  const baseRules: FormRules = {
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, max: 16, message: '密码长度为6-16位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  }
  
  if (isAdmin.value) {
    baseRules.username = [
      { required: true, message: '请输入用户名', trigger: 'blur' }
    ]
  } else {
    baseRules.oldPassword = [
      { required: true, message: '请输入旧密码', trigger: 'blur' }
    ]
  }
  
  return baseRules
})

const changePassword = () => {
  passwordFormRef.value?.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      let response
      if (isAdmin.value) {
        // 管理员修改用户密码
        response = await request.post('/auth/admin/change-password', {
          username: passwordForm.value.username,
          newPassword: passwordForm.value.newPassword
        })
      } else {
        // 普通用户修改自己的密码
        response = await request.post('/auth/change-password', {
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword
        })
      }

      if (response.data.code === 1) {
        ElMessage.success('密码修改成功')
        passwordFormRef.value?.resetFields()
      } else {
        ElMessage.error(response.data.msg || '密码修改失败')
      }
    } catch (error) {
      console.error('修改密码失败', error)
      ElMessage.error('修改密码失败，请检查网络或联系管理员')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.page-container {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.content-card {
  width: 100%;
  max-width: 600px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

.password-form {
  margin-top: 20px;
}

.submit-button {
  width: 100%;
  margin-top: 10px;
}

/* Responsive styles for ChangePassword.vue */
@media (max-width: 767px) { /* Mobile breakpoint */
  .page-container {
    padding: 10px; /* Reduce overall padding */
  }

  .content-card {
    max-width: 100%; /* Allow card to take full width */
    padding: 20px; /* Reduce card padding */
  }

  .card-header {
    font-size: 16px; /* Slightly smaller header font size */
  }

  .password-form .el-form-item {
    margin-bottom: 18px; /* Adjust form item spacing */
  }

  .submit-button {
    margin-top: 5px; /* Adjust button margin */
  }
}
</style>