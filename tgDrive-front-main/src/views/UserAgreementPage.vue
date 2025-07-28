<template>
  <div class="agreement-container">
    <el-card class="agreement-card">
      <template #header>
        <div class="card-header">
          <div class="header-content">
            <el-icon class="header-icon"><Document /></el-icon>
            <div class="header-text">
              <h1 class="title">用户服务协议</h1>
              <p class="subtitle">TG-Drive 云存储服务条款</p>
            </div>
          </div>
          <el-button 
            type="text" 
            @click="goBack" 
            class="back-button"
            :icon="ArrowLeft"
          >
            返回
          </el-button>
        </div>
      </template>

      <div class="agreement-content">
        <!-- 协议更新信息 -->
        <el-alert
          title="协议更新提醒"
          type="info"
          :closable="false"
          show-icon
          class="update-alert"
        >
          <template #default>
            <p>本协议最后更新时间：<strong>{{ lastUpdateDate }}</strong></p>
            <p>生效时间：<strong>{{ effectiveDate }}</strong></p>
          </template>
        </el-alert>

        <!-- 协议内容 -->
        <div class="content-section">
          <h2><el-icon><InfoFilled /></el-icon> 1. 服务说明</h2>
          <p>TG-Drive 是一个基于 Telegram 的云存储服务平台，为用户提供文件存储、管理和分享功能。通过使用本服务，您同意遵守本协议的所有条款。</p>
          
          <h3>1.1 服务特性</h3>
          <ul>
            <li>安全的文件存储和管理</li>
            <li>多种文件格式支持</li>
            <li>便捷的文件分享功能</li>
            <li>WebDAV 协议支持</li>
            <li>多用户权限管理</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><User /></el-icon> 2. 用户责任</h2>
          <p>作为本服务的用户，您需要承担以下责任：</p>
          
          <h3>2.1 账户安全</h3>
          <ul>
            <li>保护您的账户信息和密码安全</li>
            <li>不得与他人共享账户凭据</li>
            <li>发现账户异常时及时联系管理员</li>
          </ul>

          <h3>2.2 内容规范</h3>
          <ul>
            <li>不得上传违法、有害或侵犯他人权益的内容</li>
            <li>不得上传病毒、恶意软件或其他有害程序</li>
            <li>尊重知识产权，不得上传盗版内容</li>
            <li>不得利用服务进行任何非法活动</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><Lock /></el-icon> 3. 隐私保护</h2>
          <p>我们重视您的隐私权，承诺按照以下原则保护您的个人信息：</p>
          
          <h3>3.1 信息收集</h3>
          <ul>
            <li>仅收集提供服务所必需的信息</li>
            <li>明确告知信息收集的目的和用途</li>
            <li>获得您的明确同意后收集敏感信息</li>
          </ul>

          <h3>3.2 信息使用</h3>
          <ul>
            <li>仅用于提供和改善服务</li>
            <li>不会向第三方出售或泄露您的个人信息</li>
            <li>采用行业标准的安全措施保护数据</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><Setting /></el-icon> 4. 服务条款</h2>
          
          <h3>4.1 服务可用性</h3>
          <ul>
            <li>我们努力保证服务的稳定性和可用性</li>
            <li>因维护、升级等原因可能暂时中断服务</li>
            <li>不可抗力因素导致的服务中断不承担责任</li>
          </ul>

          <h3>4.2 存储限制</h3>
          <ul>
            <li>根据用户类型提供不同的存储配额</li>
            <li>超出配额的文件可能被限制上传</li>
            <li>长期未使用的文件可能被清理</li>
          </ul>

          <h3>4.3 服务变更</h3>
          <ul>
            <li>我们保留修改或终止服务的权利</li>
            <li>重大变更将提前通知用户</li>
            <li>用户可选择接受变更或停止使用服务</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><WarningFilled /></el-icon> 5. 免责声明</h2>
          
          <h3>5.1 服务限制</h3>
          <ul>
            <li>本服务按"现状"提供，不提供任何明示或暗示的保证</li>
            <li>不保证服务完全无错误或不中断</li>
            <li>用户应自行备份重要数据</li>
          </ul>

          <h3>5.2 责任限制</h3>
          <ul>
            <li>对于因使用服务导致的任何直接或间接损失，我们不承担责任</li>
            <li>我们的责任限于提供服务本身</li>
            <li>用户应对其行为承担全部责任</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><Phone /></el-icon> 6. 联系我们</h2>
          <p>如果您对本协议有任何疑问或建议，请通过以下方式联系我们：</p>
          
          <div class="contact-info">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <div class="contact-item">
                  <el-icon><Message /></el-icon>
                  <span>邮箱：Protocol@tgdrive.cfd</span>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div class="contact-item">
                  <el-icon><Link /></el-icon>
                  <span>项目地址：<a href="https://github.com/StanleyLegEnd1024/tgDrive" target="_blank" rel="noopener noreferrer">GitHub</a></span>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>

        <div class="content-section">
          <h2><el-icon><Calendar /></el-icon> 7. 协议生效</h2>
          <p>本协议自2025年7月26日起生效，直至您停止使用服务或我们终止提供服务。</p>
          <p>我们保留随时修改本协议的权利，修改后的协议将在网站上公布。继续使用服务即表示您接受修改后的协议。</p>
        </div>

        <!-- 同意按钮区域 -->
        <div class="agreement-actions">
          <el-divider />
          <div class="action-buttons">
            <el-button 
              type="primary" 
              size="large"
              @click="agreeAndContinue"
              :icon="CircleCheck"
            >
              我已阅读并同意此协议
            </el-button>
            <el-button 
              size="large"
              @click="goBack"
            >
              返回
            </el-button>
          </div>
          
          <div class="agreement-note">
            <el-icon><InfoFilled /></el-icon>
            <span>点击"我已阅读并同意此协议"表示您已仔细阅读并完全理解本协议的所有条款</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  ArrowLeft,
  InfoFilled,
  User,
  Lock,
  Setting,
  WarningFilled,
  Phone,
  Message,
  Link,
  Calendar,
  CircleCheck
} from '@element-plus/icons-vue'

const router = useRouter()

// 协议日期
const lastUpdateDate = ref('2025年7月26日')
const effectiveDate = ref('2025年7月26日')

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 同意协议并继续
const agreeAndContinue = () => {
  // 可以在这里记录用户同意协议的状态
  localStorage.setItem('userAgreementAccepted', 'true')
  localStorage.setItem('userAgreementAcceptedDate', new Date().toISOString())
  
  ElMessage.success('感谢您同意用户协议！')
  
  // 返回主页
  router.push('/')
}
</script>

<style scoped>
.agreement-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
  min-height: 100vh;
}

.agreement-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 32px;
  color: var(--el-color-primary);
}

.header-text .title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-color);
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-text .subtitle {
  margin: 4px 0 0 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.back-button {
  font-size: 16px;
  padding: 8px 16px;
}

.agreement-content {
  padding: 0;
}

.update-alert {
  margin-bottom: 24px;
  border-radius: 8px;
}

.content-section {
  margin-bottom: 32px;
  padding: 24px;
  background: var(--container-bg-color);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.content-section:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.content-section h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color);
  padding-bottom: 8px;
  border-bottom: 2px solid var(--el-color-primary-light-8);
}

.content-section h2 .el-icon {
  color: var(--el-color-primary);
}

.content-section h3 {
  margin: 20px 0 12px 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-color);
}

.content-section p {
  margin: 12px 0;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.content-section ul {
  margin: 12px 0;
  padding-left: 20px;
}

.content-section li {
  margin: 8px 0;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  position: relative;
}

.content-section li::marker {
  color: var(--el-color-primary);
}

.contact-info {
  margin-top: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: var(--el-fill-color-extra-light);
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.contact-item:hover {
  background: var(--el-fill-color-light);
  transform: translateX(4px);
}

.contact-item .el-icon {
  color: var(--el-color-primary);
}

.contact-item a {
  color: var(--el-color-primary);
  text-decoration: none;
  transition: all 0.3s ease;
}

.contact-item a:hover {
  color: var(--el-color-primary-light-3);
  text-decoration: underline;
}

.agreement-actions {
  margin-top: 32px;
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 24px 0;
}

.agreement-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  background: var(--el-color-info-light-9);
  border-radius: 8px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.agreement-note .el-icon {
  color: var(--el-color-info);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .agreement-container {
    padding: 12px;
  }
  
  .card-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .header-content {
    width: 100%;
  }
  
  .header-text .title {
    font-size: 24px;
  }
  
  .content-section {
    padding: 16px;
    margin-bottom: 20px;
  }
  
  .content-section h2 {
    font-size: 18px;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .action-buttons .el-button {
    width: 100%;
    max-width: 300px;
  }
}

/* 暗色模式适配 */
.dark .content-section {
  background: var(--el-bg-color-page);
  border-color: var(--el-border-color);
}

.dark .contact-item {
  background: var(--el-fill-color);
}

.dark .contact-item:hover {
  background: var(--el-fill-color-light);
}

.dark .agreement-note {
  background: var(--el-color-info-light-9);
}

/* 动画效果 */
.content-section {
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 滚动条样式 */
.agreement-content::-webkit-scrollbar {
  width: 6px;
}

.agreement-content::-webkit-scrollbar-track {
  background: var(--el-fill-color-lighter);
  border-radius: 3px;
}

.agreement-content::-webkit-scrollbar-thumb {
  background: var(--el-color-primary-light-5);
  border-radius: 3px;
}

.agreement-content::-webkit-scrollbar-thumb:hover {
  background: var(--el-color-primary);
}
</style>