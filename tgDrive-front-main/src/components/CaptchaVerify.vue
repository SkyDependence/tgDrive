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
        <div class="logo-text">ST-reCAPTCHA</div>
        <div class="logo-subtext">
          <router-link to="/privacy" class="privacy-link">隐私</router-link>
          <span class="separator"> - </span>
          <router-link to="/user-agreement" class="terms-link">条款</router-link>
        </div>
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
  max-width: 320px;
  margin: 0 auto;
}

.captcha-box {
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.captcha-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.6s ease;
}

.captcha-box:hover {
  border-color: #3b82f6;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.captcha-box:hover::before {
  left: 100%;
}

.captcha-box.verified {
  border-color: #10b981;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.2);
}

.captcha-box.loading {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.2);
}

.captcha-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.checkbox-wrapper {
  position: relative;
}

.checkbox {
  width: 28px;
  height: 28px;
  border: 2px solid #d1d5db;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.checkbox:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2);
}

.checkbox::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: all 0.3s ease;
}

.checkbox.checked {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-color: #10b981;
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

.checkbox.checked::before {
  width: 40px;
  height: 40px;
}

.checkbox.loading {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2);
}

.check-icon {
  color: white;
  font-size: 18px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  animation: checkBounce 0.6s ease-out;
}

@keyframes checkBounce {
  0% { transform: scale(0) rotate(0deg); opacity: 0; }
  50% { transform: scale(1.2) rotate(180deg); opacity: 0.8; }
  100% { transform: scale(1) rotate(360deg); opacity: 1; }
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(59, 130, 246, 0.2);
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite, pulse 2s ease-in-out infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.captcha-text {
  font-size: 15px;
  color: #374151;
  user-select: none;
  font-weight: 500;
  transition: all 0.3s ease;
}

.verified-text {
  color: #10b981;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(16, 185, 129, 0.2);
  animation: textGlow 0.8s ease-out;
}

@keyframes textGlow {
  0% { opacity: 0; transform: translateY(10px); }
  100% { opacity: 1; transform: translateY(0); }
}

.captcha-logo {
  text-align: right;
  opacity: 0.8;
  transition: opacity 0.3s ease;
}

.captcha-logo:hover {
  opacity: 1;
}

.logo-text {
  font-size: 11px;
  color: #6b7280;
  font-weight: bold;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #6b7280 0%, #9ca3af 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-subtext {
  font-size: 9px;
  color: #9ca3af;
  margin-top: 2px;
  letter-spacing: 0.3px;
}

.privacy-link,
.terms-link {
  color: #6b7280;
  text-decoration: none;
  transition: all 0.3s ease;
  cursor: pointer;
  border-radius: 2px;
  padding: 1px 2px;
}

.privacy-link:hover,
.terms-link:hover {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
  text-decoration: underline;
}

.separator {
  color: #9ca3af;
  user-select: none;
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
  animation: modalFadeIn 0.3s ease-out;
}

@keyframes modalFadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  animation: overlayFadeIn 0.3s ease-out;
}

@keyframes overlayFadeIn {
  0% { opacity: 0; backdrop-filter: blur(0px); }
  100% { opacity: 1; backdrop-filter: blur(8px); }
}

.modal-content {
  position: relative;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.8),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  max-width: 420px;
  width: 90%;
  max-height: 85vh;
  overflow: hidden;
  animation: modalSlideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modalSlideIn {
  0% { 
    opacity: 0; 
    transform: translateY(-20px) scale(0.95); 
  }
  100% { 
    opacity: 1; 
    transform: translateY(0) scale(1); 
  }
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid rgba(229, 231, 235, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.9) 100%);
  backdrop-filter: blur(10px);
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.close-btn {
  padding: 8px;
  color: #6b7280;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  transform: scale(1.1);
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 24px;
  background: linear-gradient(135deg, #fafbfc 0%, #f1f5f9 100%);
}

.icon-item {
  position: relative;
  aspect-ratio: 1;
  cursor: pointer;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.icon-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
  transition: left 0.5s ease;
}

.icon-item:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
}

.icon-item:hover::before {
  left: 100%;
}

.icon-item.selected {
  border-color: #10b981;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.2);
}

.icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  transition: all 0.3s ease;
}

.challenge-icon {
  font-size: 36px;
  color: #374151;
  transition: all 0.3s ease;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.icon-item:hover .challenge-icon {
  transform: scale(1.1);
  color: #3b82f6;
}

.icon-item.selected .challenge-icon {
  color: #10b981;
  transform: scale(1.05);
}

.icon-label {
  font-size: 13px;
  color: #6b7280;
  text-align: center;
  font-weight: 600;
  letter-spacing: 0.3px;
  transition: all 0.3s ease;
}

.icon-item:hover .icon-label {
  color: #3b82f6;
}

.icon-item.selected .icon-label {
  color: #10b981;
}

.selection-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(16, 185, 129, 0.4) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
  animation: selectionFadeIn 0.3s ease-out;
  border-radius: 12px;
}

@keyframes selectionFadeIn {
  0% { opacity: 0; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1); }
}

.check-mark {
  color: #10b981;
  font-size: 28px;
  background: linear-gradient(135deg, #ffffff 0%, #f0fdf4 100%);
  border-radius: 50%;
  padding: 6px;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  animation: checkMarkBounce 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes checkMarkBounce {
  0% { transform: scale(0) rotate(-180deg); }
  50% { transform: scale(1.2) rotate(0deg); }
  100% { transform: scale(1) rotate(0deg); }
}

.modal-footer {
  padding: 24px;
  border-top: 1px solid rgba(229, 231, 235, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.9) 100%);
  backdrop-filter: blur(10px);
}

/* 暗色模式支持 */
html.dark .captcha-box {
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

html.dark .captcha-box:hover {
  border-color: #3b82f6;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.3);
}

html.dark .captcha-box.verified {
  background: linear-gradient(135deg, #064e3b 0%, #065f46 100%);
  border-color: #10b981;
}

html.dark .captcha-box.loading {
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);
  border-color: #3b82f6;
}

html.dark .checkbox {
  background: linear-gradient(135deg, #374151 0%, #1f2937 100%);
  border-color: #4b5563;
}

html.dark .captcha-text {
  color: #e5e7eb;
}

html.dark .logo-text {
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

html.dark .modal-content {
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

html.dark .modal-header {
  background: linear-gradient(135deg, rgba(31, 41, 55, 0.9) 0%, rgba(17, 24, 39, 0.9) 100%);
  border-bottom-color: rgba(75, 85, 99, 0.3);
}

html.dark .modal-header h3 {
  color: #f9fafb;
}

html.dark .icon-grid {
  background: linear-gradient(135deg, #111827 0%, #0f172a 100%);
}

html.dark .icon-item {
  background: linear-gradient(135deg, #374151 0%, #1f2937 100%);
  border-color: #4b5563;
}

html.dark .icon-item:hover {
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);
  border-color: #3b82f6;
}

html.dark .icon-item.selected {
  background: linear-gradient(135deg, #064e3b 0%, #065f46 100%);
  border-color: #10b981;
}

html.dark .challenge-icon {
  color: #d1d5db;
}

html.dark .icon-label {
  color: #9ca3af;
}

html.dark .modal-footer {
  background: linear-gradient(135deg, rgba(31, 41, 55, 0.9) 0%, rgba(17, 24, 39, 0.9) 100%);
  border-top-color: rgba(75, 85, 99, 0.3);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .captcha-container {
    max-width: 100%;
  }
  
  .captcha-box {
    padding: 16px;
    border-radius: 10px;
  }
  
  .checkbox {
    width: 26px;
    height: 26px;
  }
  
  .captcha-text {
    font-size: 14px;
  }
  
  .image-verify-modal {
    padding: 10px;
    align-items: center;
    justify-content: center;
  }
  
  .modal-content {
    width: calc(100% - 20px);
    max-width: none;
    border-radius: 12px;
    max-height: 90vh;
    margin: 0 10px;
    display: flex;
    flex-direction: column;
  }
  
  .modal-header {
    padding: 16px;
    flex-shrink: 0;
  }
  
  .modal-footer {
    padding: 16px;
    flex-shrink: 0;
  }
  
  .modal-header h3 {
    font-size: 16px;
  }
  
  .icon-grid {
    padding: 16px;
    gap: 10px;
    flex: 1;
    overflow-y: auto;
    min-height: 0;
  }
  
  .challenge-icon {
    font-size: 28px;
  }
  
  .icon-label {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .captcha-box {
    padding: 14px;
  }
  
  .captcha-text {
    font-size: 13px;
  }
  
  .image-verify-modal {
    padding: 5px;
  }
  
  .modal-content {
    width: calc(100% - 10px);
    margin: 0 5px;
    max-height: 95vh;
  }
  
  .modal-header {
    padding: 12px;
  }
  
  .modal-footer {
    padding: 12px;
  }
  
  .modal-header h3 {
    font-size: 14px;
  }
  
  .icon-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    padding: 12px;
  }
  
  .challenge-icon {
    font-size: 24px;
  }
  
  .icon-wrapper {
    padding: 10px;
  }
  
  .icon-label {
    font-size: 10px;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .captcha-box {
    border-width: 1px;
  }
  
  .checkbox {
    border-width: 1px;
  }
  
  .icon-item {
    border-width: 1px;
  }
}

/* 减少动画效果（用户偏好） */
@media (prefers-reduced-motion: reduce) {
  .captcha-box,
  .checkbox,
  .icon-item,
  .modal-content {
    animation: none;
    transition: none;
  }
  
  .captcha-box::before,
  .icon-item::before {
    display: none;
  }
}
</style>