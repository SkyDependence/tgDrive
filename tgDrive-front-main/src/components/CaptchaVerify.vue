<template>
  <div class="captcha-container">
    <div class="captcha-box" :class="{ 'verified': isVerified, 'loading': isLoading }">
      <div class="captcha-content">
        <div class="checkbox-wrapper">
          <div 
            class="checkbox" 
            :class="{ 'checked': isVerified, 'loading': isLoading }"
            @click="handleVerify"
          >
            <el-icon v-if="isVerified" class="check-icon"><Check /></el-icon>
            <div v-else-if="isLoading" class="loading-spinner"></div>
          </div>
        </div>
        <div class="captcha-text">
          <span v-if="!isVerified && !isLoading">我不是机器人</span>
          <span v-else-if="isLoading">验证中...</span>
          <span v-else class="verified-text">验证成功</span>
        </div>
      </div>
      <div class="captcha-logo">
        <div class="logo-text">reCAPTCHA</div>
        <div class="logo-subtext">隐私 - 条款</div>
      </div>
    </div>
    
    <!-- 验证失败时显示的图片验证 -->
    <div v-if="showImageVerify" class="image-verify-modal">
      <div class="modal-overlay" @click="closeImageVerify"></div>
      <div class="modal-content">
        <div class="modal-header">
          <h3>选择所有的 <strong>{{ challengeType }}</strong> 图标</h3>
          <el-button 
            type="text" 
            @click="closeImageVerify"
            class="close-btn"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="icon-grid">
          <div 
            v-for="(iconItem, index) in challengeIcons" 
            :key="index"
            class="icon-item"
            :class="{ 'selected': selectedImages.includes(index) }"
            @click="toggleImageSelection(index)"
          >
            <div class="icon-wrapper">
              <el-icon class="challenge-icon">
                <component :is="iconItem.icon" />
              </el-icon>
              <span class="icon-label">{{ iconItem.name }}</span>
            </div>
            <div v-if="selectedImages.includes(index)" class="selection-overlay">
              <el-icon class="check-mark"><Check /></el-icon>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <el-button @click="refreshChallenge" type="text">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button 
            type="primary" 
            @click="submitImageVerify"
            :disabled="selectedImages.length === 0"
          >
            验证
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineEmits } from 'vue'
import { Check, Close, Refresh, House, Star, Bell, User, Setting, Search, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{
  verified: [value: boolean]
}>()

const isVerified = ref(false)
const isLoading = ref(false)
const showImageVerify = ref(false)
const challengeType = ref('汽车')
const selectedImages = ref<number[]>([])

// 图标验证数据
const challengeIcons = ref([
  { icon: House, name: '房子', isCorrect: false },
  { icon: Star, name: '星星', isCorrect: false },
  { icon: Bell, name: '铃铛', isCorrect: false },
  { icon: User, name: '用户', isCorrect: false },
  { icon: Setting, name: '设置', isCorrect: false },
  { icon: Search, name: '搜索', isCorrect: false },
  { icon: Edit, name: '编辑', isCorrect: false },
  { icon: Delete, name: '删除', isCorrect: false },
  { icon: Close, name: '关闭', isCorrect: false }
])

const handleVerify = async () => {
  if (isVerified.value || isLoading.value) return
  
  isLoading.value = true
  
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1500))
  
  // 随机决定是否需要图片验证
  const needImageVerify = Math.random() > 0.3
  
  if (needImageVerify) {
    isLoading.value = false
    showImageVerify.value = true
    generateNewChallenge()
  } else {
    // 直接通过验证
    isVerified.value = true
    isLoading.value = false
    emit('verified', true)
    ElMessage.success('验证成功')
  }
}

const toggleImageSelection = (index: number) => {
  const selectedIndex = selectedImages.value.indexOf(index)
  if (selectedIndex > -1) {
    selectedImages.value.splice(selectedIndex, 1)
  } else {
    selectedImages.value.push(index)
  }
}

const submitImageVerify = () => {
  // 检查选择是否正确
  const correctIcons = challengeIcons.value
    .map((icon, index) => icon.isCorrect ? index : -1)
    .filter(index => index !== -1)
  
  const isCorrect = selectedImages.value.length === correctIcons.length &&
    selectedImages.value.every(index => correctIcons.includes(index))
  
  if (isCorrect) {
    isVerified.value = true
    showImageVerify.value = false
    selectedImages.value = []
    emit('verified', true)
    ElMessage.success('验证成功')
  } else {
    ElMessage.error('验证失败，请重试')
    refreshChallenge()
  }
}

const refreshChallenge = () => {
  selectedImages.value = []
  // 重新生成挑战
  generateNewChallenge()
}

const generateNewChallenge = () => {
  const challenges = ['房子', '星星', '铃铛', '用户', '设置', '搜索', '编辑', '删除', '关闭']
  challengeType.value = challenges[Math.floor(Math.random() * challenges.length)]
  
  // 根据选择的题目类型设置正确答案
  challengeIcons.value.forEach(icon => {
    icon.isCorrect = icon.name === challengeType.value
  })
  
  // 如果没有匹配的图标，随机设置一些正确答案
  if (!challengeIcons.value.some(icon => icon.isCorrect)) {
    const randomCount = Math.floor(Math.random() * 3) + 1
    for (let i = 0; i < randomCount; i++) {
      const randomIndex = Math.floor(Math.random() * challengeIcons.value.length)
      challengeIcons.value[randomIndex].isCorrect = true
    }
  }
}

const closeImageVerify = () => {
  showImageVerify.value = false
  selectedImages.value = []
}

// 重置验证状态
const reset = () => {
  isVerified.value = false
  isLoading.value = false
  showImageVerify.value = false
  selectedImages.value = []
  emit('verified', false)
}

// 暴露重置方法
defineExpose({
  reset
})
</script>

<style scoped>
.captcha-container {
  width: 100%;
  max-width: 300px;
}

.captcha-box {
  border: 2px solid #d1d5db;
  border-radius: 4px;
  background: #f9fafb;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s ease;
  cursor: pointer;
}

.captcha-box:hover {
  border-color: #9ca3af;
}

.captcha-box.verified {
  border-color: #10b981;
  background: #f0fdf4;
}

.captcha-box.loading {
  border-color: #3b82f6;
  background: #eff6ff;
}

.captcha-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.checkbox-wrapper {
  position: relative;
}

.checkbox {
  width: 24px;
  height: 24px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.checkbox.checked {
  background: #10b981;
  border-color: #10b981;
}

.checkbox.loading {
  border-color: #3b82f6;
}

.check-icon {
  color: white;
  font-size: 16px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e5e7eb;
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.captcha-text {
  font-size: 14px;
  color: #374151;
  user-select: none;
}

.verified-text {
  color: #10b981;
  font-weight: 500;
}

.captcha-logo {
  text-align: right;
}

.logo-text {
  font-size: 10px;
  color: #6b7280;
  font-weight: bold;
}

.logo-subtext {
  font-size: 8px;
  color: #9ca3af;
}

/* 图片验证模态框 */
.image-verify-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
}

.modal-content {
  position: relative;
  background: white;
  border-radius: 8px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  max-width: 400px;
  width: 90%;
  max-height: 80vh;
  overflow: hidden;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #111827;
}

.close-btn {
  padding: 4px;
  color: #6b7280;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 20px;
}

.icon-item {
  position: relative;
  aspect-ratio: 1;
  cursor: pointer;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-item:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.icon-item.selected {
  border-color: #10b981;
  background: #f0fdf4;
}

.icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
}

.challenge-icon {
  font-size: 32px;
  color: #374151;
}

.icon-label {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  font-weight: 500;
}

.selection-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(16, 185, 129, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-mark {
  color: #10b981;
  font-size: 24px;
  background: white;
  border-radius: 50%;
  padding: 4px;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .modal-content {
    width: 95%;
    max-width: none;
  }
  
  .modal-header,
  .modal-footer {
    padding: 16px;
  }
  
  .icon-grid {
    padding: 16px;
  }
}
</style>