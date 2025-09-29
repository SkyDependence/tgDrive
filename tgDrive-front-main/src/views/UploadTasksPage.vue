<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>上传任务管理</span>
          <div class="header-actions">
            <el-button @click="refreshTasks" :icon="Refresh" circle />
            <el-button @click="deleteSelected" :disabled="selectedTasks.length === 0" type="danger" :icon="Delete">
              删除选中 ({{ selectedTasks.length }})
            </el-button>
          </div>
        </div>
      </template>

      <!-- 任务过滤器 -->
      <div class="filter-section">
        <el-radio-group v-model="filterStatus" @change="refreshTasks" size="small">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="pending">等待中</el-radio-button>
          <el-radio-button label="uploading">上传中</el-radio-button>
          <el-radio-button label="paused">已暂停</el-radio-button>
          <el-radio-button label="failed">失败</el-radio-button>
          <el-radio-button label="completed">已完成</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 任务列表 -->
      <el-table
        v-loading="loading"
        :data="filteredTasks"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        empty-text="暂无上传任务"
      >
        <el-table-column type="selection" width="55" />

        <el-table-column prop="fileName" label="文件名" min-width="200">
          <template #default="scope">
            <div class="file-name-cell">
              <el-icon><Document /></el-icon>
              <span>{{ scope.row.fileName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="fileSizeStr" label="文件大小" width="120" />

        <el-table-column prop="progress" label="上传进度" width="200">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="scope.row.progress" :status="getProgressStatus(scope.row.status)" />
              <span class="progress-text">
                {{ scope.row.uploadedChunks }}/{{ scope.row.totalChunks }} 块
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="statusText" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ scope.row.statusText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="expiresAt" label="过期时间" width="180">
          <template #default="scope">
            <el-tooltip v-if="scope.row.expiresAt" :content="formatDate(scope.row.expiresAt)">
              <span :class="{ 'expiring-soon': isExpiringSoon(scope.row.expiresAt) }">
                {{ getRelativeTime(scope.row.expiresAt) }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                v-if="scope.row.resumable && scope.row.status !== 'completed'"
                @click="resumeTask(scope.row)"
                type="primary"
                size="small"
                :icon="VideoPlay"
              >
                恢复
              </el-button>
              <el-button
                @click="deleteTask(scope.row)"
                type="danger"
                size="small"
                :icon="Delete"
                plain
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 任务详情对话框 -->
      <el-dialog v-model="showDetails" title="任务详情" width="600px">
        <el-descriptions v-if="selectedTask" :column="1" border>
          <el-descriptions-item label="任务ID">{{ selectedTask.id }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ selectedTask.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ selectedTask.fileSizeStr }}</el-descriptions-item>
          <el-descriptions-item label="总分块">{{ selectedTask.totalChunks }}</el-descriptions-item>
          <el-descriptions-item label="已上传">{{ selectedTask.uploadedChunks }}</el-descriptions-item>
          <el-descriptions-item label="剩余大小">{{ selectedTask.remainingSizeStr }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedTask.status)">{{ selectedTask.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedTask.errorMessage" label="错误信息">
            <span class="error-message">{{ selectedTask.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Refresh, Document, VideoPlay } from '@element-plus/icons-vue';
import request from '@/utils/request';
import { useRouter } from 'vue-router';

interface UploadTask {
  id: string;
  fileName: string;
  fileSize: number;
  fileSizeStr: string;
  totalChunks: number;
  uploadedChunks: number;
  progress: number;
  status: string;
  statusText: string;
  createdAt: string;
  updatedAt: string;
  expiresAt: string;
  errorMessage?: string;
  resumable: boolean;
  remainingSize: number;
  remainingSizeStr: string;
}

const router = useRouter();
const loading = ref(false);
const tasks = ref<UploadTask[]>([]);
const selectedTasks = ref<UploadTask[]>([]);
const filterStatus = ref('all');
const showDetails = ref(false);
const selectedTask = ref<UploadTask | null>(null);

const filteredTasks = computed(() => {
  if (filterStatus.value === 'all') {
    return tasks.value;
  }
  return tasks.value.filter(task => task.status === filterStatus.value);
});

// 获取任务列表
const fetchTasks = async () => {
  loading.value = true;
  try {
    const response = await request.get('/resumable/tasks');
    if (response.data.code === 1) {
      tasks.value = response.data.data;
    } else {
      ElMessage.error('获取任务列表失败');
    }
  } catch (error) {
    ElMessage.error('获取任务列表失败');
  } finally {
    loading.value = false;
  }
};

// 刷新任务列表
const refreshTasks = () => {
  fetchTasks();
};

// 恢复任务
const resumeTask = async (task: UploadTask) => {
  try {
    const response = await request.post(`/resumable/resume/${task.id}`);
    if (response.data.code === 1) {
      ElMessage.success('任务已加入恢复队列');
      // 跳转到上传页面
      router.push({
        path: '/upload',
        query: {
          resumeTaskId: task.id,
          fileName: task.fileName
        }
      });
    } else {
      ElMessage.error(response.data.msg || '恢复任务失败');
    }
  } catch (error) {
    ElMessage.error('恢复任务失败');
  }
};

// 删除单个任务
const deleteTask = async (task: UploadTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务 "${task.fileName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const response = await request.delete('/resumable/tasks', {
      params: { taskIds: task.id }
    });

    if (response.data.code === 1) {
      ElMessage.success('任务已删除');
      refreshTasks();
    } else {
      ElMessage.error(response.data.msg || '删除失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
};

// 批量删除
const deleteSelected = async () => {
  if (selectedTasks.value.length === 0) {
    ElMessage.warning('请选择要删除的任务');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedTasks.value.length} 个任务吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const taskIds = selectedTasks.value.map(task => task.id);
    const response = await request.delete('/resumable/tasks', {
      params: { taskIds: taskIds.join(',') }
    });

    if (response.data.code === 1) {
      ElMessage.success(`成功删除 ${selectedTasks.value.length} 个任务`);
      refreshTasks();
    } else {
      ElMessage.error(response.data.msg || '删除失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
};

// 处理表格选择变化
const handleSelectionChange = (selection: UploadTask[]) => {
  selectedTasks.value = selection;
};

// 获取进度条状态
const getProgressStatus = (status: string) => {
  switch (status) {
    case 'completed':
      return 'success';
    case 'failed':
      return 'exception';
    case 'uploading':
      return undefined;
    default:
      return undefined;
  }
};

// 获取状态标签类型
const getStatusType = (status: string) => {
  switch (status) {
    case 'pending':
      return 'info';
    case 'uploading':
      return 'primary';
    case 'paused':
      return 'warning';
    case 'completed':
      return 'success';
    case 'failed':
      return 'danger';
    default:
      return 'info';
  }
};

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN');
};

// 获取相对时间
const getRelativeTime = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diff = date.getTime() - now.getTime();

  if (diff < 0) {
    return '已过期';
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

  if (days > 0) {
    return `${days}天${hours}小时后`;
  } else if (hours > 0) {
    return `${hours}小时后`;
  } else {
    const minutes = Math.floor(diff / (1000 * 60));
    return `${minutes}分钟后`;
  }
};

// 判断是否即将过期（24小时内）
const isExpiringSoon = (dateStr: string) => {
  if (!dateStr) return false;
  const date = new Date(dateStr);
  const now = new Date();
  const diff = date.getTime() - now.getTime();
  return diff > 0 && diff < 24 * 60 * 60 * 1000;
};

onMounted(() => {
  fetchTasks();
});
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-section {
  margin-bottom: 20px;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.expiring-soon {
  color: var(--el-color-warning);
  font-weight: bold;
}

.error-message {
  color: var(--el-color-danger);
  font-size: 12px;
}

@media (max-width: 768px) {
  .filter-section {
    overflow-x: auto;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>