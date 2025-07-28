<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <el-icon><Service /></el-icon>
          <span>机器人保活管理</span>
        </div>
      </template>

      <div class="action-section">
        <!-- 保活状态显示 -->
        <div class="status-section">
          <el-alert
            :title="statusTitle"
            :type="statusType"
            :description="statusDescription"
            show-icon
            :closable="false"
          />
        </div>

        <el-divider direction="horizontal" />

        <!-- 手动发送保活消息 -->
        <div class="action-item">
          <h3>手动发送保活消息</h3>
          <p>立即发送一条保活消息到Telegram，用于测试机器人连接状态。</p>
          <div class="send-button-container">
            <el-button 
              type="primary" 
              @click="sendKeepAliveMessage"
              :loading="sendLoading"
              :icon="Message"
              size="large"
            >
              发送保活消息
            </el-button>
          </div>
        </div>

        <el-divider direction="horizontal" />

        <!-- 定时任务控制 -->
        <div class="action-item">
          <h3>定时保活任务</h3>
          <p>控制自动保活任务的启动和停止。任务将在每天凌晨2点自动发送保活消息。</p>
          <div class="task-controls">
            <el-button 
              type="success" 
              @click="startKeepAliveTask"
              :loading="startLoading"
              :disabled="taskRunning"
              :icon="VideoPlay"
              size="large"
            >
              启动定时任务
            </el-button>
            <el-button 
              type="warning" 
              @click="stopKeepAliveTask"
              :loading="stopLoading"
              :disabled="!taskRunning"
              :icon="VideoPause"
              size="large"
              plain
            >
              停止定时任务
            </el-button>
          </div>
        </div>

        <el-divider direction="horizontal" />

        <!-- 配置信息 -->
        <div class="action-item">
          <h3>保活配置</h3>
          <div class="config-info">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="发送时间">每天凌晨2点</el-descriptions-item>
              <el-descriptions-item label="发送间隔">24小时</el-descriptions-item>
              <el-descriptions-item label="任务状态">
                <el-tag :type="taskRunning ? 'success' : 'danger'">
                  {{ taskRunning ? '运行中' : '已停止' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="最后更新">{{ lastUpdateTime }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Service, 
  Message, 
  VideoPlay, 
  VideoPause 
} from '@element-plus/icons-vue'
import axios from 'axios'

// 响应式数据
const taskRunning = ref(false)
const sendLoading = ref(false)
const startLoading = ref(false)
const stopLoading = ref(false)
const lastUpdateTime = ref('')

// 计算属性
const statusTitle = computed(() => {
  return taskRunning.value ? '保活任务运行中' : '保活任务已停止'
})

const statusType = computed(() => {
  return taskRunning.value ? 'success' : 'warning'
})

const statusDescription = computed(() => {
  if (taskRunning.value) {
    return '定时保活任务正在运行，将在每天凌晨2点自动发送保活消息，确保机器人保持活跃状态。'
  } else {
    return '定时保活任务已停止，机器人可能会因长时间不活跃而被Telegram限制。建议启动定时任务。'
  }
})

// 方法
const updateLastUpdateTime = () => {
  lastUpdateTime.value = new Date().toLocaleString('zh-CN')
}

const getTaskStatus = async () => {
  try {
    const response = await axios.get('/api/bot/keep-alive/status')
    if (response.data.code === 1) {
      taskRunning.value = response.data.data === '运行中'
      updateLastUpdateTime()
    }
  } catch (error) {
    console.error('获取任务状态失败:', error)
    ElMessage.error('获取任务状态失败')
  }
}

const sendKeepAliveMessage = async () => {
  sendLoading.value = true
  try {
    const response = await axios.post('/api/bot/keep-alive/send')
    if (response.data.code === 1) {
      ElMessage.success('保活消息发送成功')
      updateLastUpdateTime()
    } else {
      ElMessage.error(response.data.msg || '发送失败')
    }
  } catch (error) {
    console.error('发送保活消息失败:', error)
    ElMessage.error('发送保活消息失败')
  } finally {
    sendLoading.value = false
  }
}

const startKeepAliveTask = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要启动定时保活任务吗？任务将在每天凌晨2点自动发送保活消息。',
      '确认启动',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    startLoading.value = true
    const response = await axios.post('/api/bot/keep-alive/start')
    if (response.data.code === 1) {
      ElMessage.success('定时保活任务启动成功')
      await getTaskStatus()
    } else {
      ElMessage.error(response.data.msg || '启动失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('启动定时任务失败:', error)
      ElMessage.error('启动定时任务失败')
    }
  } finally {
    startLoading.value = false
  }
}

const stopKeepAliveTask = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要停止定时保活任务吗？停止后机器人可能会因长时间不活跃而被限制。',
      '确认停止',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    stopLoading.value = true
    const response = await axios.post('/api/bot/keep-alive/stop')
    if (response.data.code === 1) {
      ElMessage.success('定时保活任务停止成功')
      await getTaskStatus()
    } else {
      ElMessage.error(response.data.msg || '停止失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('停止定时任务失败:', error)
      ElMessage.error('停止定时任务失败')
    }
  } finally {
    stopLoading.value = false
  }
}

// 生命周期
onMounted(() => {
  getTaskStatus()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.content-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.action-section {
  padding: 20px 0;
}

.status-section {
  margin-bottom: 20px;
}

.action-item {
  margin: 20px 0;
}

.action-item h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.action-item p {
  margin: 0 0 16px 0;
  color: #606266;
  line-height: 1.5;
}

.task-controls {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.send-button-container {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.config-info {
  margin-top: 16px;
}

@media (max-width: 768px) {
  .page-container {
    padding: 10px;
  }
  
  .card-header {
    justify-content: center;
    text-align: center;
  }
  
  .action-item h3 {
    text-align: center;
  }
  
  .action-item p {
    text-align: center;
  }
  
  .send-button-container {
    display: flex;
    justify-content: center;
    margin-top: 16px;
  }
  
  .task-controls {
    flex-direction: row;
    justify-content: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  
  .task-controls .el-button {
    flex: 1;
    min-width: 120px;
    max-width: 150px;
  }
}
</style>