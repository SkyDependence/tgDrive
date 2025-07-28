<template>
  <div class="privacy-policy-container">
    <el-card class="privacy-policy-card">
      <template #header>
        <div class="card-header">
          <div class="header-content">
            <el-icon class="header-icon"><Lock /></el-icon>
            <div class="header-text">
              <h1 class="title">隐私政策</h1>
              <p class="subtitle">TG-Drive 隐私保护条款</p>
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

      <div class="privacy-policy-content">
        <!-- 协议更新信息 -->
        <el-alert
          title="隐私政策更新提醒"
          type="info"
          :closable="false"
          show-icon
          class="update-alert"
        >
          <template #default>
            <p>本隐私政策最后更新时间：<strong>{{ lastUpdateDate }}</strong></p>
            <p>生效时间：<strong>{{ effectiveDate }}</strong></p>
          </template>
        </el-alert>

        <!-- 隐私政策内容 -->
        <div class="content-section">
          <h2><el-icon><InfoFilled /></el-icon> 1. 信息收集</h2>
          <p>我们收集以下类型的信息：</p>
          
          <h3>1.1 收集的信息类型</h3>
          <ul>
            <li><strong>账户信息：</strong>用户名、邮箱地址等注册信息</li>
            <li><strong>文件信息：</strong>您上传的文件名称、大小、类型等元数据</li>
            <li><strong>使用数据：</strong>访问日志、操作记录等技术信息</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><Setting /></el-icon> 2. 信息使用</h2>
          <p>我们使用收集的信息用于：</p>
          
          <h3>2.1 服务提供</h3>
          <ul>
            <li>提供和维护我们的服务</li>
            <li>改善用户体验和服务质量</li>
            <li>处理用户请求和技术支持</li>
            <li>确保服务安全和防止滥用</li>
            <li>遵守法律法规要求</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><Lock /></el-icon> 3. 信息保护</h2>
          <p>我们采取以下措施保护您的个人信息：</p>
          
          <h3>3.1 安全措施</h3>
          <ul>
            <li><strong>加密传输：</strong>使用HTTPS协议确保数据传输安全</li>
            <li><strong>访问控制：</strong>严格限制对个人信息的访问权限</li>
            <li><strong>数据备份：</strong>定期备份数据以防止意外丢失</li>
            <li><strong>安全监控：</strong>持续监控系统安全状态</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><Share /></el-icon> 4. 信息共享</h2>
          <p>我们不会向第三方出售、交易或转让您的个人信息，除非：</p>
          
          <h3>4.1 共享情况</h3>
          <ul>
            <li>获得您的明确同意</li>
            <li>法律法规要求</li>
            <li>保护我们的权利和安全</li>
            <li>与可信的服务提供商合作（在严格保密协议下）</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><DataLine /></el-icon> 5. Cookie使用</h2>
          <p>我们使用Cookie和类似技术来：</p>
          
          <h3>5.1 Cookie用途</h3>
          <ul>
            <li>记住您的登录状态</li>
            <li>保存您的偏好设置</li>
            <li>分析网站使用情况</li>
            <li>提供个性化体验</li>
          </ul>
          <p>您可以通过浏览器设置管理Cookie偏好。</p>
        </div>

        <div class="content-section">
          <h2><el-icon><Calendar /></el-icon> 6. 数据保留</h2>
          <p>我们会在以下期限内保留您的信息：</p>
          
          <h3>6.1 保留期限</h3>
          <ul>
            <li><strong>账户信息：</strong>账户存续期间及注销后30天</li>
            <li><strong>文件数据：</strong>根据您的设置和服务条款</li>
            <li><strong>日志数据：</strong>通常保留90天</li>
            <li><strong>备份数据：</strong>最长保留1年</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><User /></el-icon> 7. 您的权利</h2>
          <p>您对个人信息享有以下权利：</p>
          
          <h3>7.1 用户权利</h3>
          <ul>
            <li><strong>访问权：</strong>查看我们持有的您的个人信息</li>
            <li><strong>更正权：</strong>更新或修正不准确的信息</li>
            <li><strong>删除权：</strong>要求删除您的个人信息</li>
            <li><strong>限制权：</strong>限制对您信息的处理</li>
            <li><strong>携带权：</strong>获取您的数据副本</li>
          </ul>
        </div>

        <div class="content-section">
          <h2><el-icon><WarningFilled /></el-icon> 8. 未成年人保护</h2>
          <p>我们的服务不面向13岁以下的儿童。如果我们发现收集了13岁以下儿童的个人信息，我们会立即删除这些信息。</p>
        </div>

        <div class="content-section">
          <h2><el-icon><CircleCheck /></el-icon> 9. 政策更新</h2>
          <p>我们可能会不时更新本隐私政策。重大变更时，我们会通过网站公告或邮件通知您。继续使用我们的服务即表示您接受更新后的政策。</p>
        </div>

        <div class="content-section">
          <h2><el-icon><Phone /></el-icon> 10. 联系我们</h2>
          <p>如果您对本隐私政策有任何疑问或建议，请通过以下方式联系我们：</p>
          
          <div class="contact-info">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <div class="contact-item">
                  <el-icon><Message /></el-icon>
                  <span>邮箱：Privacy@tgdrive.cfd</span>
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

        <!-- 操作按钮区域 -->
        <div class="privacy-policy-actions">
          <div class="action-buttons">
            <el-button @click="goBack" size="large" class="back-btn">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <el-button type="primary" @click="acceptPolicy" size="large" class="accept-btn">
              <el-icon><CircleCheck /></el-icon>
              我已阅读并理解此隐私政策
            </el-button>
          </div>
          
          <div class="privacy-policy-note">
            <el-icon><InfoFilled /></el-icon>
            <span>阅读并同意隐私政策有助于保护您的个人信息安全</span>
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
  ArrowLeft,
  Lock,
  InfoFilled,
  Setting,
  Share,
  DataLine,
  Calendar,
  User,
  WarningFilled,
  CircleCheck,
  Phone,
  Message,
  Link
} from '@element-plus/icons-vue'

const router = useRouter()

// 隐私政策日期
const lastUpdateDate = ref('2025年7月26日')
const effectiveDate = ref('2025年7月26日')

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 同意隐私政策并继续
const acceptPolicy = () => {
  // 可以在这里记录用户同意隐私政策的状态
  localStorage.setItem('privacyPolicyAccepted', 'true')
  localStorage.setItem('privacyPolicyAcceptedDate', new Date().toISOString())
  
  ElMessage.success('感谢您阅读并理解我们的隐私政策！')
  
  // 返回主页
  router.push('/')
}
</script>

<style scoped>
.privacy-policy-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
  min-height: 100vh;
}

.privacy-policy-card {
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

.privacy-policy-content {
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

.privacy-policy-actions {
  margin-top: 32px;
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 24px 0;
}

.privacy-policy-note {
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

.privacy-policy-note .el-icon {
  color: var(--el-color-info);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .privacy-policy-container {
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

.dark .privacy-policy-note {
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
.privacy-policy-content::-webkit-scrollbar {
  width: 6px;
}

.privacy-policy-content::-webkit-scrollbar-track {
  background: var(--el-fill-color-lighter);
  border-radius: 3px;
}

.privacy-policy-content::-webkit-scrollbar-thumb {
  background: var(--el-color-primary-light-5);
  border-radius: 3px;
}

.privacy-policy-content::-webkit-scrollbar-thumb:hover {
  background: var(--el-color-primary);
}
</style>