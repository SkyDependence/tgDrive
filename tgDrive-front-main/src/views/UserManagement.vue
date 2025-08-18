<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </div>
          <div class="header-right">
            <el-input v-model="searchQuery" placeholder="搜索用户名或邮箱" clearable style="width: 200px; margin-right: 10px;" @keyup.enter="handleSearch" />
            <div class="search-button-group">
              <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
              <el-button type="default" @click="clearSearch" :icon="Refresh">刷新</el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 系统设置 -->
      <div class="settings-container">
        <div class="setting-item">
          <span class="setting-label">允许用户注册</span>
          <el-switch
            v-model="allowRegistration"
            @change="handleRegistrationStatusChange"
            :loading="registrationSettingLoading"
          />
        </div>
      </div>

      <!-- 用户统计卡片 -->
      <div class="stats-container">
        <div class="stat-card">
          <div class="stat-icon total-users">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon online-users">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ onlineUsers }}</div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon admin-users">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ adminUsers }}</div>
            <div class="stat-label">管理员</div>
          </div>
        </div>
      </div>

      <!-- Desktop Table View -->
      <el-table
        v-if="!isMobile"
        :data="filteredUserList"
        v-loading="loading"
        height="calc(100vh - 280px)"
        style="width: 100%;"
      >
        <el-table-column prop="id" label="用户ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon><User /></el-icon>
              <span>{{ scope.row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.role === 'admin' ? (scope.row.email || '') : scope.row.email }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">{{ getRoleText(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后上线时间" width="180" align="center">
          <template #default="scope">
            {{ scope.row.lastLoginTime || '从未登录' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="scope">
            <el-button-group>
              <el-button 
                type="primary" 
                size="small" 
                @click="openSetRoleDialog(scope.row)" 
                :icon="User"
                :disabled="scope.row.username === 'visitor'"
              >
                设角色
              </el-button>
              <el-button 
                type="warning" 
                size="small" 
                @click="openChangePasswordDialog(scope.row)" 
                :icon="EditPen"
                :disabled="scope.row.username === 'visitor'"
              >
                改密
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="deleteUser(scope.row)" 
                :icon="Delete"
                :disabled="scope.row.role === 'admin' || scope.row.username === 'visitor'"
              >
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Mobile List View -->
      <div v-if="isMobile" class="mobile-user-list">
        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="filteredUserList.length === 0" description="暂无用户" />
        <div v-else>
          <div 
            v-for="user in filteredUserList" 
            :key="user.id" 
            class="mobile-user-item"
          >
            <div class="user-info">
              <el-icon class="user-icon"><User /></el-icon>
              <div class="user-details">
                <div class="user-name">{{ user.username }}</div>
                <div class="user-meta">
                  <span>ID: {{ user.id }}</span>
                  <el-tag :type="getRoleTagType(user.role)" size="small">{{ getRoleText(user.role) }}</el-tag>
                </div>
                <div class="user-email">{{ user.email }}</div>
                <div class="user-login-time">最后上线: {{ user.lastLoginTime || '从未登录' }}</div>
              </div>
            </div>
            <div class="user-actions">
              <el-button-group>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="openSetRoleDialog(user)" 
                  :icon="User" 
                  :disabled="user.username === 'visitor'"
                >
                  设角色
                </el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  @click="openChangePasswordDialog(user)" 
                  :icon="EditPen" 
                  :disabled="user.username === 'visitor'"
                >
                  改密
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="deleteUser(user)" 
                  :icon="Delete" 
                  :disabled="user.role === 'admin' || user.username === 'visitor'"
                >
                  删除
                </el-button>
              </el-button-group>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="changePasswordDialogVisible"
      title="修改用户密码"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="changePasswordFormRef"
        :model="changePasswordForm"
        :rules="changePasswordRules"
        label-width="100px"
      >
        <el-form-item label="用户名">
          <el-input v-model="changePasswordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="changePasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="changePasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="changePasswordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmChangePassword" :loading="changePasswordLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 设置角色对话框 -->
    <el-dialog
      v-model="setRoleDialogVisible"
      title="设置用户角色"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="setRoleFormRef"
        :model="setRoleForm"
        :rules="setRoleRules"
        label-width="100px"
      >
        <el-form-item label="用户名">
          <el-input v-model="setRoleForm.username" disabled />
        </el-form-item>
        <el-form-item label="当前角色">
          <el-tag :type="getRoleTagType(setRoleForm.currentRole)">{{ getRoleText(setRoleForm.currentRole) }}</el-tag>
        </el-form-item>
        <el-form-item label="新角色" prop="newRole">
          <el-select v-model="setRoleForm.newRole" placeholder="请选择新角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="setRoleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSetRole" :loading="setRoleLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { User, Search, Refresh, EditPen, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface UserItem {
  id: number
  username: string
  email: string
  role: string
  lastLoginTime: string
}

interface ChangePasswordForm {
  username: string
  newPassword: string
  confirmPassword: string
}

interface SetRoleForm {
  username: string
  currentRole: string
  newRole: string
}

interface SettingItem {
  key: string;
  value: string;
  description: string;
}

const userList = ref<UserItem[]>([])
const loading = ref(false)
const searchQuery = ref('')
const isMobile = ref(false)
const changePasswordDialogVisible = ref(false)
const changePasswordLoading = ref(false)
const changePasswordFormRef = ref<FormInstance>()
const setRoleDialogVisible = ref(false)
const setRoleLoading = ref(false)
const setRoleFormRef = ref<FormInstance>()
const selectedUser = ref<UserItem | null>(null)

// 用户统计数据
const totalUsers = ref(0)
const onlineUsers = ref(0)
const adminUsers = ref(0)

// 系统设置
const allowRegistration = ref(false)
const registrationSettingLoading = ref(false)

const changePasswordForm = ref<ChangePasswordForm>({
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const setRoleForm = ref<SetRoleForm>({
  username: '',
  currentRole: '',
  newRole: ''
})

const changePasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== changePasswordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const setRoleRules = {
  newRole: [
    { required: true, message: '请选择新角色', trigger: 'change' }
  ]
}

const filteredUserList = computed(() => {
  if (!searchQuery.value || !userList.value) {
    return userList.value || []
  }
  return userList.value.filter(user => 
    user.username?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    user.email?.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const getRoleTagType = (role: string) => {
  switch (role) {
    case 'admin':
      return 'danger'
    case 'user':
      return 'success'
    case 'visitor':
      return 'info'
    default:
      return ''
  }
}

const getRoleText = (role: string) => {
  switch (role) {
    case 'admin':
      return '管理员'
    case 'user':
      return '用户'
    case 'visitor':
      return '访客'
    default:
      return role
  }
}

const fetchUserList = async () => {
  loading.value = true
  try {
    const response = await request.get('/auth/admin/users')
    if (response.data?.code === 1) {
      userList.value = response.data.data || []
      updateUserStats()
    } else {
      ElMessage.error(response.data?.msg || '获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 获取系统设置
const fetchSettings = async () => {
  try {
    const response = await request.get('/setting/settings')
    if (response.data?.code === 1) {
      const settings: SettingItem[] = response.data.data
      const registrationSetting = settings.find(s => s.key === 'allow_registration')
      if (registrationSetting) {
        allowRegistration.value = registrationSetting.value === 'true'
      }
    } else {
      ElMessage.error(response.data?.msg || '获取系统设置失败')
    }
  } catch (error) {
    console.error('获取系统设置失败:', error)
    ElMessage.error('获取系统设置失败，请检查网络连接')
  }
}

// 更新注册状态
const handleRegistrationStatusChange = async (newValue: boolean | string | number) => {
  registrationSettingLoading.value = true
  try {
    const response = await request.post('/setting', {
      key: 'allow_registration',
      value: newValue.toString()
    })
    if (response.data?.code === 1) {
      ElMessage.success('设置更新成功')
    } else {
      ElMessage.error(response.data?.msg || '设置更新失败')
      allowRegistration.value = !newValue
    }
  } catch (error) {
    console.error('设置更新失败:', error)
    ElMessage.error('设置更新失败，请检查网络连接')
    allowRegistration.value = !newValue
  } finally {
    registrationSettingLoading.value = false
  }
}

// 更新用户统计数据
const updateUserStats = () => {
  totalUsers.value = userList.value.length
  adminUsers.value = userList.value.filter(user => user.role === 'admin').length
  // 这里简单模拟在线用户数，实际项目中应该从后端获取
  onlineUsers.value = Math.floor(totalUsers.value * 0.1) // 假设10%的用户在线
}

const handleSearch = () => {
  // 搜索功能由计算属性实现，这里不需要额外操作
}

const clearSearch = () => {
  searchQuery.value = ''
  fetchUserList() // 刷新数据
}

const openChangePasswordDialog = (user: UserItem) => {
  selectedUser.value = user
  changePasswordForm.value = {
    username: user.username,
    newPassword: '',
    confirmPassword: ''
  }
  changePasswordDialogVisible.value = true
}

const confirmChangePassword = async () => {
  if (!changePasswordFormRef.value) return
  
  try {
    await changePasswordFormRef.value.validate()
    changePasswordLoading.value = true
    
    const response = await request.post('/auth/admin/change-password', {
      username: changePasswordForm.value.username,
      newPassword: changePasswordForm.value.newPassword
    })
    
    if (response.data?.code === 1) {
      ElMessage.success('密码修改成功')
      changePasswordDialogVisible.value = false
    } else {
      ElMessage.error(response.data?.msg || '密码修改失败')
    }
  } catch (error: any) {
    if (error !== 'validation failed') {
      ElMessage.error('密码修改失败: ' + (error.response?.data?.msg || error.message))
    }
  } finally {
    changePasswordLoading.value = false
  }
}

const openSetRoleDialog = (user: UserItem) => {
  selectedUser.value = user
  setRoleForm.value = {
    username: user.username,
    currentRole: user.role,
    newRole: user.role
  }
  setRoleDialogVisible.value = true
}

const confirmSetRole = async () => {
  if (!setRoleFormRef.value) return
  
  try {
    await setRoleFormRef.value.validate()
    
    if (setRoleForm.value.newRole === setRoleForm.value.currentRole) {
      ElMessage.warning('新角色与当前角色相同')
      return
    }
    
    setRoleLoading.value = true
    
    const response = await request.put(`/auth/admin/users/${selectedUser.value?.id}/role?role=${setRoleForm.value.newRole}`)
    
    if (response.data?.code === 1) {
      ElMessage.success('用户角色设置成功')
      setRoleDialogVisible.value = false
      fetchUserList() // 刷新用户列表
    } else {
      ElMessage.error(response.data?.msg || '角色设置失败')
    }
  } catch (error: any) {
    if (error !== 'validation failed') {
      ElMessage.error('角色设置失败: ' + (error.response?.data?.msg || error.message))
    }
  } finally {
    setRoleLoading.value = false
  }
}

const deleteUser = async (user: UserItem) => {
  if (user.role === 'admin') {
    ElMessage.warning('不能删除管理员账户')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    const response = await request.delete(`/auth/admin/users/${user.id}`)
    if (response.data?.code === 1) {
      ElMessage.success('用户删除成功')
      fetchUserList()
    } else {
      ElMessage.error(response.data?.msg || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败: ' + (error.response?.data?.msg || error.message))
    }
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  fetchUserList()
  fetchSettings()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.search-button-group {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

:deep(.el-card__body) {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

/* 统计卡片样式 */
.stats-container {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: var(--el-bg-color-page);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.stat-card {
  flex: 1;
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: var(--el-box-shadow-light);
  transition: all 0.3s ease;
  border: 1px solid var(--el-border-color-lighter);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.total-users {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.stat-icon.online-users {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.stat-icon.admin-users {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.el-table {
  flex-grow: 1;
}

/* Mobile List View Styles */
.mobile-user-list {
  padding: 10px;
  flex-grow: 1;
  overflow-y: auto;
}

.mobile-user-item {
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: var(--el-box-shadow-light);
  transition: background-color 0.3s, border-color 0.3s;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.user-icon {
  font-size: 1.5em;
  color: var(--el-color-primary);
}

.user-details {
  flex: 1;
}

.user-name {
  font-weight: 600;
  font-size: 16px;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.user-email {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}

.user-login-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.user-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.user-actions .el-button-group {
  width: 100%;
}

.user-actions .el-button {
  flex: 1;
}

/* 设置容器样式 */
.settings-container {
  padding: 20px;
  background: var(--el-bg-color-page);
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 20px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.setting-label {
  font-weight: 500;
}

/* Responsive styles */
@media (max-width: 767px) {
  .page-container {
    padding: 10px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .card-header .header-right {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .card-header .header-right .el-input {
    width: 100% !important;
  }

  .search-button-group {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .search-button-group .el-button {
    flex: 1;
  }

  .settings-container {
    padding: 15px 10px;
  }

  /* 移动端统计卡片样式 */
  .stats-container {
    flex-direction: row;
    gap: 8px;
    padding: 10px;
  }

  .stat-card {
    padding: 10px;
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }

  .stat-icon {
    width: 30px;
    height: 30px;
    font-size: 16px;
    margin: 0 auto;
  }

  .stat-number {
    font-size: 18px;
  }

  .stat-label {
    font-size: 12px;
  }

  /* 移动端用户列表优化 */
  .mobile-user-list {
    padding: 5px;
  }

  .mobile-user-item {
    padding: 12px;
    margin-bottom: 8px;
  }

  .user-info {
    align-items: flex-start;
  }

  .user-details {
    min-width: 0;
  }

  .user-name {
    font-size: 15px;
    word-break: break-word;
  }

  .user-email {
    font-size: 12px;
    word-break: break-all;
  }

  .user-meta {
    flex-wrap: wrap;
    gap: 6px;
  }

  .user-login-time {
    font-size: 11px;
  }
}
</style>