<template>
  <div class="about-container">
    <el-card class="about-card">
      <template #header>
        <div class="card-header">
          <el-button 
            type="primary" 
            :icon="ArrowLeft" 
            @click="goHome"
            class="back-button"
            size="small"
          >
            返回首页
          </el-button>
          <h1 class="title">
            <el-icon class="title-icon"><InfoFilled /></el-icon>
            关于 TG Drive
          </h1>
          <div class="placeholder"></div>
        </div>
      </template>

      <!-- 项目简介 -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Document /></el-icon>
          项目简介
        </h2>
        <p class="description">
          TG Drive 是一个基于 Telegram Bot API 的云存储解决方案，利用 Telegram 的免费存储空间为用户提供安全、可靠的文件存储服务。
          支持大文件的高效传输和管理。
        </p>
      </div>

      <!-- 项目作者信息 -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><User /></el-icon>
          项目作者
        </h2>
        
        <!-- 开发者信息 -->
        <div class="authors-row">
          <!-- 原始作者 -->
          <div class="author-card original-author">
            <div class="author-avatar">
              <img src="/110871356.jpg" alt="SkyDev" class="avatar-img" />
            </div>
            <div class="author-details">
              <h3 class="author-name">
                SkyDev
                <el-tag type="warning" size="small" class="author-tag">原始开创者</el-tag>
              </h3>
              <p class="author-description">tgDrive 项目的原始作者和开创者，为我们提供了优秀的基础框架</p>
              <div class="author-links">
                <el-tag type="info" size="small" @click="openLink('https://github.com/SkyDependence')" class="clickable-tag">
                  <el-icon><Link /></el-icon>
                  GitHub: SkyDependence
                </el-tag>
                <el-tag type="primary" size="small" @click="openLink('https://github.com/SkyDependence/tgDrive')" class="clickable-tag">
                  <el-icon><Link /></el-icon>
                  原始项目地址
                </el-tag>
              </div>
            </div>
          </div>
          
          <!-- 当前维护者 -->
          <div class="author-card">
            <div class="author-avatar">
              <img src="/favicon.ico" alt="Stanley-DEV" class="avatar-img" />
            </div>
            <div class="author-details">
              <h3 class="author-name">
                Stanley-DEV
                <el-tag type="success" size="small" class="author-tag">当前维护者</el-tag>
              </h3>
              <p class="author-description">负责项目优化和维护，改进文件管理、上传体验等功能</p>
              <div class="author-links">
                <el-tag type="info" size="small">
                  <el-icon><Link /></el-icon>
                  开源贡献者
                </el-tag>
                <el-tag type="primary" size="small" @click="openLink('https://me.lixvyao.com')" class="clickable-tag">
                  <el-icon><Link /></el-icon>
                  ST个人主页
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 技术原理 -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Setting /></el-icon>
          技术原理
        </h2>
        <div class="tech-grid">
          <div class="tech-item">
            <div class="tech-icon">
              <el-icon size="24" color="#409EFF"><Upload /></el-icon>
            </div>
            <h4>直接上传</h4>
            <p>支持大文件直接上传，简化传输流程提高上传效率</p>
          </div>
          <div class="tech-item">
            <div class="tech-icon">
              <el-icon size="24" color="#67C23A"><ChatDotRound /></el-icon>
            </div>
            <h4>Telegram API</h4>
            <p>基于Telegram Bot API，利用其稳定的文件存储服务</p>
          </div>
          <div class="tech-item">
            <div class="tech-icon">
              <el-icon size="24" color="#E6A23C"><Connection /></el-icon>
            </div>
            <h4>实时进度</h4>
            <p>WebSocket实时推送上传进度，提供流畅的用户体验</p>
          </div>
          <div class="tech-item">
            <div class="tech-icon">
              <el-icon size="24" color="#F56C6C"><Key /></el-icon>
            </div>
            <h4>安全认证</h4>
            <p>基于Token的身份验证，确保数据访问安全</p>
          </div>
        </div>
      </div>

      <!-- 数据安全 -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Lock /></el-icon>
          数据安全保障
        </h2>
        <div class="security-features">
          <div class="security-row">
            <div class="security-item">
              <el-icon class="security-icon" color="#67C23A"><CircleCheck /></el-icon>
              <div class="security-content">
                <h4>端到端加密</h4>
                <p>文件在传输过程中采用HTTPS加密，确保数据传输安全</p>
              </div>
            </div>
            <div class="security-item">
              <el-icon class="security-icon" color="#67C23A"><CircleCheck /></el-icon>
              <div class="security-content">
                <h4>访问控制</h4>
                <p>基于角色的权限管理，严格控制文件访问权限</p>
              </div>
            </div>
          </div>
          <div class="security-row">
            <div class="security-item">
              <el-icon class="security-icon" color="#67C23A"><CircleCheck /></el-icon>
              <div class="security-content">
                <h4>数据备份</h4>
                <p>Telegram服务器提供多重备份，确保数据不丢失</p>
              </div>
            </div>
            <div class="security-item">
              <el-icon class="security-icon" color="#67C23A"><CircleCheck /></el-icon>
              <div class="security-content">
                <h4>隐私保护</h4>
                <p>文件存储在您的Telegram Bot中，完全由您控制</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 技术栈 -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Cpu /></el-icon>
          技术栈
        </h2>
        <div class="tech-stack">
          <div class="stack-row">
            <div class="stack-category">
              <h4>前端技术</h4>
              <div class="tech-tags">
                <el-tag>Vue 3</el-tag>
                <el-tag>TypeScript</el-tag>
                <el-tag>Element Plus</el-tag>
                <el-tag>Vite</el-tag>
              </div>
            </div>
            <div class="stack-category">
              <h4>后端技术</h4>
              <div class="tech-tags">
                <el-tag type="success">Spring Boot</el-tag>
                <el-tag type="success">Java 17</el-tag>
                <el-tag type="success">WebSocket</el-tag>
                <el-tag type="success">SQLite</el-tag>
              </div>
            </div>
          </div>
          <div class="stack-row">
            <div class="stack-category">
              <h4>第三方服务</h4>
              <div class="tech-tags">
                <el-tag type="warning">Telegram Bot API</el-tag>
                <el-tag type="warning">Maven</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 版本信息 -->
      <div class="section">
        <h2 class="section-title">
          <el-icon><Flag /></el-icon>
          版本信息
        </h2>
        <div class="version-info">
          <div class="version-item">
            <span class="version-label">当前版本:</span>
            <el-tag type="primary">v114514</el-tag>
          </div>
          <div class="version-item">
            <span class="version-label">发布日期:</span>
            <span>2025年</span>
          </div>
          <div class="version-item">
            <span class="version-label">许可证:</span>
            <el-tag type="info">MIT License</el-tag>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  InfoFilled,
  Document,
  User,
  Avatar,
  Link,
  Setting,
  Upload,
  ChatDotRound,
  Connection,
  Key,
  Lock,
  CircleCheck,
  Cpu,
  Flag,
  ArrowLeft,
  Star
} from '@element-plus/icons-vue';

const router = useRouter();

const goHome = () => {
  router.push('/');
};

const openLink = (url: string) => {
  window.open(url, '_blank');
};
</script>

<style scoped>
.about-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.about-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  min-height: 60px;
  padding: 8px 0;
}

.back-button {
  flex-shrink: 0;
  margin-right: 12px;
}

.title {
  flex: 1;
  text-align: center;
  margin: 0 12px;
}

.placeholder {
  width: 80px;
  flex-shrink: 0;
}

.title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.title-icon {
  font-size: 32px;
  color: var(--el-color-primary);
}

.section {
  margin-bottom: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
  border-bottom: 2px solid var(--el-color-primary);
  padding-bottom: 8px;
}

.description {
  font-size: 16px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  margin: 0;
}

.authors-row {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
}

.author-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  max-width: 400px;
  flex: 1;
  min-width: 350px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.author-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.author-card.original-author {
  background-color: var(--el-color-warning-light-9);
  border: 2px solid var(--el-color-warning);
}

.author-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.author-details {
  flex: 1;
}

.author-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.author-description {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.author-links {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.author-tag {
  margin-left: 8px;
}

.clickable-tag {
  cursor: pointer;
  transition: all 0.3s ease;
}

.clickable-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.tech-item {
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  text-align: center;
  transition: all 0.3s ease;
}

.tech-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.tech-icon {
  margin-bottom: 12px;
}

.tech-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tech-item p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.security-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-row {
  display: flex;
  gap: 16px;
}

.security-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--el-fill-color-extra-light);
  border-radius: 8px;
  flex: 1;
}

.security-icon {
  margin-top: 2px;
  font-size: 20px;
}

.security-content h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.security-content p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.tech-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stack-row {
  display: flex;
  gap: 20px;
}

.stack-category {
  flex: 1;
}

.stack-category h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.version-info {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 20px;
}

.version-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
  flex: 1;
}

.version-label {
  font-weight: 600;
  color: var(--el-text-color-primary);
  min-width: 80px;
}

/* 响应式设计 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .about-container {
    padding: 8px;
  }
  
  .about-card {
    border-radius: 8px;
  }
  
  .card-header {
    flex-direction: column;
    gap: 12px;
    text-align: center;
    min-height: auto;
    padding: 12px 0;
  }
  
  .back-button {
    order: -1;
    margin-right: 0;
    align-self: flex-start;
  }
  
  .placeholder {
    display: none;
  }
  
  .title {
    font-size: 20px;
    order: 0;
    margin: 0;
  }
  
  .title-icon {
    font-size: 24px;
  }
  
  .section {
    margin-bottom: 24px;
  }
  
  .section-title {
    font-size: 18px;
  }
  
  .tech-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .tech-item {
    padding: 16px;
  }
  
  .author-card {
    flex-direction: column;
    text-align: center;
    padding: 16px;
    max-width: 100%;
  }
  
  .author-avatar {
    width: 50px;
    height: 50px;
  }
  
  .author-links {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .security-row {
    flex-direction: column;
    gap: 12px;
  }
  
  .security-item {
    padding: 12px;
  }
  
  .stack-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .tech-tags {
    justify-content: center;
  }
  
  .version-info {
    flex-direction: column;
    gap: 8px;
  }
  
  .version-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    min-width: auto;
  }
}

/* 小屏幕 (平板, 576px 到 767px) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .about-container {
    padding: 12px;
  }
  
  .title {
    font-size: 24px;
  }
  
  .tech-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .author-card {
    max-width: 500px;
  }
  
  .security-row {
    flex-direction: column;
  }
  
  .stack-row {
    flex-direction: column;
  }
}

/* 中等屏幕 (768px 到 991px) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .about-container {
    padding: 16px;
  }
  
  .tech-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 大屏幕及以上 (992px+) */
@media (min-width: 992px) {
  .about-container {
    padding: 20px;
  }
  
  .tech-grid {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .tech-item:hover {
    transform: none;
  }
  
  .tech-item:active {
    transform: scale(0.98);
  }
  
  .back-button {
    min-height: 44px; /* 确保触摸目标足够大 */
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .avatar-img {
    image-rendering: -webkit-optimize-contrast;
  }
}
</style>