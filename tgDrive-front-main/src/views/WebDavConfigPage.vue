<template>
  <div class="webdav-config-container">
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <h2>WebDAV配置</h2>
          <p class="subtitle">配置WebDAV服务的访问权限和功能设置</p>
        </div>
      </template>

      <el-form
        ref="configFormRef"
        :model="configForm"
        :rules="configRules"
        label-width="140px"
        class="config-form"
      >
        <!-- 基础设置 -->
        <el-divider content-position="left">
          <el-icon><Setting /></el-icon>
          基础设置
        </el-divider>

        <el-form-item label="启用WebDAV" prop="enabled">
          <el-switch
            v-model="configForm.enabled"
            active-text="启用"
            inactive-text="禁用"
            @change="handleEnabledChange"
          />
          <div class="form-tip">启用后可通过WebDAV协议访问文件</div>
        </el-form-item>

        <el-form-item label="需要认证" prop="requireAuth">
          <el-switch
            v-model="configForm.requireAuth"
            active-text="需要"
            inactive-text="不需要"
            :disabled="!configForm.enabled"
          />
          <div class="form-tip">是否需要用户名密码认证（正在施工中...）</div>
        </el-form-item>

        <!-- 权限设置 -->
        <el-divider content-position="left">
          <el-icon><User /></el-icon>
          权限设置
        </el-divider>

        <el-form-item label="允许的角色" prop="allowedRoles">
          <el-select
            v-model="configForm.allowedRoles"
            placeholder="请选择允许访问的用户角色"
            :disabled="!configForm.enabled"
          >
            <el-option label="仅管理员" value="admin" />
            <el-option label="访客用户" value="visitor" />
          </el-select>
          <div class="form-tip">设置哪些角色的用户可以访问WebDAV</div>
        </el-form-item>

        <!-- 功能设置 -->
        <el-divider content-position="left">
          <el-icon><Tools /></el-icon>
          功能设置
        </el-divider>

        <el-form-item label="允许创建目录" prop="allowMkdir">
          <el-switch
            v-model="configForm.allowMkdir"
            active-text="允许"
            inactive-text="禁止"
            :disabled="!configForm.enabled"
          />
          <div class="form-tip">（正在施工中...）</div>
        </el-form-item>

        <el-form-item label="允许删除文件" prop="allowDelete">
          <el-switch
            v-model="configForm.allowDelete"
            active-text="允许"
            inactive-text="禁止"
            :disabled="!configForm.enabled"
          />
          <div class="form-tip">（正在施工中...）</div>
        </el-form-item>

        <el-form-item label="允许移动文件" prop="allowMove">
          <el-switch
            v-model="configForm.allowMove"
            active-text="允许"
            inactive-text="禁止"
            :disabled="!configForm.enabled"
          />
          <div class="form-tip">（正在施工中...）</div>
        </el-form-item>

        <el-form-item label="允许复制文件" prop="allowCopy">
          <el-switch
            v-model="configForm.allowCopy"
            active-text="允许"
            inactive-text="禁止"
            :disabled="!configForm.enabled"
          />
          <div class="form-tip">（正在施工中...）</div>
        </el-form-item>

        <el-form-item label="配置描述" prop="description">
          <el-input
            v-model="configForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述"
            :disabled="!configForm.enabled"
          />
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <div class="button-group">
            <el-button type="primary" @click="saveConfig" :loading="saving">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
            <el-button @click="resetConfig">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button @click="resetToDefault" :loading="resetting">
              <el-icon><RefreshLeft /></el-icon>
              恢复默认
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- WebDAV使用说明 -->
    <el-card class="usage-card" v-if="configForm.enabled">
      <template #header>
        <div class="card-header">
          <h3>WebDAV使用说明</h3>
        </div>
      </template>
      
      <div class="usage-content">
        <el-alert
          title="WebDAV访问地址"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="webdav-url">
              <code>{{ webdavUrl }}</code>
              <el-button
                size="small"
                type="primary"
                link
                @click="copyUrl"
              >
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </template>
        </el-alert>

        <div class="usage-steps">
          <h4>使用步骤：</h4>
          <ol>
            <li>在支持WebDAV的客户端中添加网络位置</li>
            <li>输入上述WebDAV访问地址</li>
            <li>使用您的用户名和密码进行认证</li>
            <li>即可像本地文件夹一样操作远程文件</li>
          </ol>
        </div>

        <div class="client-recommendations">
          <h4>推荐客户端：</h4>
          <ul>
            <li><strong>Windows:</strong> 文件资源管理器、WinSCP</li>
            <li><strong>macOS:</strong> Finder、Cyberduck</li>
            <li><strong>Linux:</strong> Nautilus、Dolphin</li>
            <li><strong>移动端:</strong> ES文件浏览器、FE文件管理器</li>
          </ul>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Setting,
  User,
  Tools,
  Check,
  Refresh,
  RefreshLeft,
  CopyDocument
} from '@element-plus/icons-vue'
import request from '@/utils/request'

// 响应式数据
const configFormRef = ref<FormInstance>()
const saving = ref(false)
const resetting = ref(false)

// 配置表单数据
const configForm = reactive({
  enabled: true,
  requireAuth: true,
  allowedRoles: 'admin',
  allowMkdir: true,
  allowDelete: true,
  allowMove: true,
  allowCopy: true,
  description: 'WebDAV服务配置'
})

// 表单验证规则
const configRules: FormRules = {
  allowedRoles: [
    { required: true, message: '请选择允许的角色', trigger: 'change' }
  ]
}

// 计算WebDAV访问URL
const webdavUrl = computed(() => {
  const protocol = window.location.protocol
  const host = window.location.host
  return `${protocol}//${host}/webdav`
})

// 加载配置
const loadConfig = async () => {
  try {
    const response = await request.get('/webdav-config')
    if (response.data.code === 1) {
      Object.assign(configForm, response.data.data)
    } else {
      ElMessage.error(response.data.msg || '加载配置失败')
    }
  } catch (error) {
    console.error('加载WebDAV配置失败:', error)
    ElMessage.error('加载配置失败')
  }
}

// 保存配置
const saveConfig = async () => {
  if (!configFormRef.value) return
  
  try {
    await configFormRef.value.validate()
    saving.value = true
    
    const response = await request.put('/webdav-config', configForm)
    if (response.data.code === 1) {
      ElMessage.success('配置保存成功')
    } else {
      ElMessage.error(response.data.msg || '配置保存失败')
    }
  } catch (error) {
    console.error('保存WebDAV配置失败:', error)
    ElMessage.error('配置保存失败')
  } finally {
    saving.value = false
  }
}

// 重置配置
const resetConfig = () => {
  if (!configFormRef.value) return
  configFormRef.value.resetFields()
  loadConfig()
}

// 恢复默认配置
const resetToDefault = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复默认配置吗？这将覆盖当前所有设置。',
      '确认恢复默认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    resetting.value = true
    const response = await request.post('/webdav-config/reset')
    if (response.data.code === 1) {
      ElMessage.success('已恢复默认配置')
      await loadConfig()
    } else {
      ElMessage.error(response.data.msg || '恢复默认配置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复默认配置失败:', error)
      ElMessage.error('恢复默认配置失败')
    }
  } finally {
    resetting.value = false
  }
}

// 处理启用状态变化
const handleEnabledChange = (value: boolean) => {
  if (!value) {
    ElMessageBox.confirm(
      '禁用WebDAV服务后，所有WebDAV客户端将无法访问。确定要禁用吗？',
      '确认禁用',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).catch(() => {
      configForm.enabled = true
    })
  }
}

// 复制WebDAV URL
const copyUrl = async () => {
  const text = webdavUrl.value;
  const message = 'WebDAV地址已复制到剪贴板';
  
  // 优先使用现代的 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text);
      ElMessage.success(message);
    } catch (error) {
      console.error('Clipboard API failed:', error);
      fallbackCopyTextToClipboard(text, message);
    }
  } else {
    // 降级到传统方法
    fallbackCopyTextToClipboard(text, message);
  }
};

// 传统的复制方法作为降级方案
const fallbackCopyTextToClipboard = (text: string, message: string) => {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  
  // 避免滚动到底部
  textArea.style.top = '0';
  textArea.style.left = '0';
  textArea.style.position = 'fixed';
  textArea.style.opacity = '0';
  
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  
  try {
    const successful = document.execCommand('copy');
    if (successful) {
      ElMessage.success(message);
    } else {
      ElMessage.error('复制失败，请手动复制');
    }
  } catch (err) {
    console.error('Fallback copy failed:', err);
    ElMessage.error('复制失败，请手动复制');
  }
  
  document.body.removeChild(textArea);
}

// 组件挂载时加载配置
onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.webdav-config-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.config-card {
  margin-bottom: 20px;
}

.card-header h2 {
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.card-header .subtitle {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.config-form {
  margin-top: 20px;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

.unit {
  margin-left: 8px;
  color: var(--el-text-color-regular);
}

.usage-card {
  margin-top: 20px;
}

.usage-content {
  margin-top: 16px;
}

.webdav-url {
  display: flex;
  align-items: center;
  gap: 12px;
}

.webdav-url code {
  flex: 1;
  padding: 8px 12px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  word-break: break-all;
}

.usage-steps,
.client-recommendations {
  margin-top: 20px;
}

.usage-steps h4,
.client-recommendations h4 {
  margin: 0 0 12px 0;
  color: var(--el-text-color-primary);
}

.usage-steps ol,
.client-recommendations ul {
  margin: 0;
  padding-left: 20px;
}

.usage-steps li,
.client-recommendations li {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
}

.client-recommendations strong {
  color: var(--el-text-color-primary);
}

/* 去除卡片圆角 */
.config-card,
.usage-card {
  :deep(.el-card) {
    border-radius: 0;
  }
}

.button-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .webdav-config-container {
    padding: 12px;
  }
  
  .config-form {
    :deep(.el-form-item__label) {
      width: 100px !important;
    }
  }
  
  .webdav-url {
    flex-direction: column;
    align-items: stretch;
  }
  
  .button-group {
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    gap: 8px;
    width: 100%;
    margin: 0 auto;
  }
  
  .button-group :deep(.el-button) {
    flex: 0 0 auto;
    min-width: 80px;
    max-width: 120px;
    font-size: 14px !important;
    padding: 8px 12px !important;
    white-space: nowrap;
  }
  
  .config-form :deep(.el-form-item:last-child) {
    text-align: center;
    margin-bottom: 0;
  }
  
  .config-form :deep(.el-form-item:last-child .el-form-item__content) {
    justify-content: center;
    margin-left: 0 !important;
  }
}
</style>
