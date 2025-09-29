<template>
  <div class="queue-page">
    <!-- 队列控制面板 -->
    <el-card class="control-panel">
      <template #header>
        <div class="panel-header">
          <span>上传队列控制</span>
          <div class="header-actions">
            <el-button @click="refreshQueue" :icon="Refresh" circle />
            <el-button v-if="!isRunning" @click="startQueue" type="success" :icon="VideoPlay">
              启动队列
            </el-button>
            <el-button v-else @click="stopQueue" type="warning" :icon="VideoPause">
              停止队列
            </el-button>
            <el-button @click="clearQueue" type="danger" :icon="Delete">
              清空队列
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计信息 -->
      <el-row :gutter="20" class="statistics">
        <el-col :xs="12" :sm="6">
          <el-statistic title="队列任务" :value="statistics.total">
            <template #suffix>个</template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="正在上传" :value="statistics.uploading">
            <template #suffix>个</template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="当前带宽" :value="formatBandwidth(statistics.currentBandwidth)" />
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="预计时间" :value="formatTime(statistics.estimatedTime)" />
        </el-col>
      </el-row>

      <!-- 总进度条 -->
      <div class="total-progress">
        <span>总体进度</span>
        <el-progress :percentage="Math.round(statistics.totalProgress)" :status="getProgressStatus()" />
      </div>

      <!-- 队列设置 -->
      <div class="queue-settings">
        <el-form inline>
          <el-form-item label="最大并发数">
            <el-input-number
              v-model="maxConcurrent"
              :min="1"
              :max="10"
              @change="updateSettings"
            />
          </el-form-item>
          <el-form-item label="带宽限制">
            <el-select v-model="bandwidthLimit" @change="updateSettings">
              <el-option label="无限制" :value="0" />
              <el-option label="1 MB/s" :value="1048576" />
              <el-option label="5 MB/s" :value="5242880" />
              <el-option label="10 MB/s" :value="10485760" />
              <el-option label="20 MB/s" :value="20971520" />
            </el-select>
          </el-form-item>
          <el-form-item label="自动开始">
            <el-switch v-model="autoStart" @change="updateSettings" />
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 文件选择 -->
    <el-card class="file-selector">
      <template #header>
        <span>添加文件到队列</span>
      </template>

      <el-upload
        ref="uploadRef"
        drag
        multiple
        action="#"
        :auto-upload="false"
        :on-change="handleFileAdd"
        :show-file-list="false"
        class="upload-area"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击选择</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持批量添加，文件将按优先级排序上传
          </div>
        </template>
      </el-upload>
    </el-card>

    <!-- 队列任务列表 -->
    <el-card class="queue-list">
      <template #header>
        <div class="list-header">
          <span>队列任务列表</span>
          <el-radio-group v-model="filterStatus" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pending">等待</el-radio-button>
            <el-radio-button label="uploading">上传中</el-radio-button>
            <el-radio-button label="completed">完成</el-radio-button>
            <el-radio-button label="failed">失败</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="filteredTasks" style="width: 100%">
        <el-table-column prop="file.name" label="文件名" min-width="200">
          <template #default="scope">
            <div class="file-info">
              <el-icon><Document /></el-icon>
              <span>{{ scope.row.file.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="大小" width="100">
          <template #default="scope">
            {{ formatFileSize(scope.row.file.size) }}
          </template>
        </el-table-column>

        <el-table-column prop="priority" label="优先级" width="120">
          <template #default="scope">
            <el-input-number
              v-if="scope.row.status === 'pending'"
              v-model="scope.row.priority"
              :min="1"
              :max="10"
              size="small"
              @change="updatePriority(scope.row)"
            />
            <span v-else>{{ scope.row.priority }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="progress" label="进度" width="150">
          <template #default="scope">
            <el-progress
              :percentage="Math.round(scope.row.progress)"
              :status="getTaskProgressStatus(scope.row.status)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                v-if="scope.row.status === 'paused'"
                @click="resumeTask(scope.row)"
                type="primary"
                size="small"
                :icon="VideoPlay"
                circle
              />
              <el-button
                v-else-if="scope.row.status === 'uploading'"
                @click="pauseTask(scope.row)"
                type="warning"
                size="small"
                :icon="VideoPause"
                circle
              />
              <el-button
                v-if="scope.row.status === 'failed'"
                @click="retryTask(scope.row)"
                type="success"
                size="small"
                :icon="RefreshRight"
                circle
              />
              <el-button
                @click="removeTask(scope.row)"
                type="danger"
                size="small"
                :icon="Delete"
                circle
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  UploadFilled, Document, VideoPlay, VideoPause, Delete,
  Refresh, RefreshRight
} from '@element-plus/icons-vue';
import uploadQueueManager from '@/utils/uploadQueueManager';
import type { QueuedTask } from '@/utils/uploadQueueManager';

// 状态
const tasks = ref<QueuedTask[]>([]);
const statistics = ref({
  total: 0,
  pending: 0,
  uploading: 0,
  completed: 0,
  failed: 0,
  paused: 0,
  totalProgress: 0,
  estimatedTime: 0,
  currentBandwidth: 0
});
const filterStatus = ref('all');
const isRunning = ref(false);
const maxConcurrent = ref(3);
const bandwidthLimit = ref(0);
const autoStart = ref(true);
const uploadRef = ref();

// 计算属性
const filteredTasks = computed(() => {
  if (filterStatus.value === 'all') {
    return tasks.value;
  }
  return tasks.value.filter(task => task.status === filterStatus.value);
});

// 方法
const refreshQueue = () => {
  tasks.value = uploadQueueManager.getTasks();
  statistics.value = uploadQueueManager.getStatistics();
};

const startQueue = () => {
  uploadQueueManager.start();
  isRunning.value = true;
  ElMessage.success('队列已启动');
};

const stopQueue = () => {
  uploadQueueManager.stop();
  isRunning.value = false;
  ElMessage.warning('队列已停止');
};

const clearQueue = () => {
  uploadQueueManager.clear();
  refreshQueue();
  ElMessage.success('队列已清空');
};

const handleFileAdd = (file: any) => {
  const priority = 5; // 默认优先级
  uploadQueueManager.addTask(file.raw, priority);
  ElMessage.success(`已添加 ${file.name} 到队列`);
  refreshQueue();
};

const updatePriority = (task: QueuedTask) => {
  uploadQueueManager.setPriority(task.id, task.priority);
  refreshQueue();
};

const pauseTask = (task: QueuedTask) => {
  uploadQueueManager.pauseTask(task.id);
  refreshQueue();
};

const resumeTask = (task: QueuedTask) => {
  uploadQueueManager.resumeTask(task.id);
  refreshQueue();
};

const retryTask = (task: QueuedTask) => {
  task.status = 'pending';
  task.retries = 0;
  if (isRunning.value) {
    uploadQueueManager.start();
  }
  refreshQueue();
};

const removeTask = (task: QueuedTask) => {
  uploadQueueManager.removeTask(task.id);
  refreshQueue();
};

const updateSettings = () => {
  // 更新队列设置
  ElMessage.success('设置已更新');
};

// 格式化函数
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

const formatBandwidth = (bytesPerSecond: number): string => {
  if (bytesPerSecond === 0) return '0 KB/s';
  return formatFileSize(bytesPerSecond) + '/s';
};

const formatTime = (seconds: number): string => {
  if (seconds === 0) return '--';
  if (seconds < 60) return Math.round(seconds) + '秒';
  if (seconds < 3600) return Math.round(seconds / 60) + '分钟';
  return Math.round(seconds / 3600) + '小时';
};

const getProgressStatus = () => {
  if (statistics.value.failed > 0) return 'exception';
  if (statistics.value.completed === statistics.value.total && statistics.value.total > 0) return 'success';
  return undefined;
};

const getTaskProgressStatus = (status: string) => {
  if (status === 'failed') return 'exception';
  if (status === 'completed') return 'success';
  return undefined;
};

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    pending: 'info',
    uploading: 'primary',
    completed: 'success',
    failed: 'danger',
    paused: 'warning'
  };
  return types[status] || 'info';
};

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    pending: '等待中',
    uploading: '上传中',
    completed: '已完成',
    failed: '失败',
    paused: '已暂停'
  };
  return texts[status] || status;
};

// 生命周期
let refreshInterval: number;

onMounted(() => {
  refreshQueue();
  // 定期刷新
  refreshInterval = window.setInterval(refreshQueue, 1000);
});

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});
</script>

<style scoped>
.queue-page {
  padding: 20px;
}

.control-panel {
  margin-bottom: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.statistics {
  margin-bottom: 20px;
}

.total-progress {
  margin: 20px 0;
}

.queue-settings {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.file-selector {
  margin-bottom: 20px;
}

.upload-area {
  width: 100%;
}

.queue-list {
  margin-bottom: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

@media (max-width: 768px) {
  .statistics .el-col {
    margin-bottom: 10px;
  }

  .queue-settings .el-form-item {
    margin-bottom: 10px;
  }
}
</style>