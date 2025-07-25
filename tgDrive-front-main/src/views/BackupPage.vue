<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <el-icon><DataLine /></el-icon>
          <span>数据库管理</span>
        </div>
      </template>

      <div class="action-section">
        <div class="action-item">
          <h3>备份数据库</h3>
          <p>将当前数据库下载为 .db 文件。请妥善保管备份文件。</p>
          <el-button 
            type="primary" 
            @click="handleBackup"
            :loading="backupLoading"
            :icon="Download"
            size="large"
          >
            下载备份
          </el-button>
        </div>

        <el-divider direction="horizontal" />

        <div class="action-item">
          <h3>恢复数据库</h3>
          <p>从 .db 备份文件恢复数据库。此操作将覆盖当前所有数据，请谨慎操作。</p>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="false"
            accept=".db"
            :on-change="handleFileSelect"
          >
            <el-button 
              type="danger" 
              :loading="restoreLoading"
              :icon="Upload"
              size="large"
              plain
            >
              选择文件并恢复
            </el-button>
          </el-upload>
        </div>
      </div>

    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Download, Upload, DataLine } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, UploadInstance, UploadFile } from 'element-plus'
import request from '@/utils/request'

const backupLoading = ref(false)
const restoreLoading = ref(false)
const uploadRef = ref<UploadInstance>()

const handleFileSelect = (file: UploadFile) => {
  const isDB = file.raw?.type === 'application/octet-stream' || file.name.endsWith('.db');
  if (!isDB) {
    ElMessage.error('只能上传 .db 格式的数据库文件!');
    return;
  }

  ElMessageBox.confirm(
    `您确定要使用文件 "${file.name}" 恢复数据库吗？此操作不可逆。`,
    '警告',
    {
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    handleUpload(file);
  }).catch(() => {
    ElMessage.info('已取消恢复操作');
  });
};

const handleUpload = async (file: UploadFile) => {
  if (!file.raw) return;

  restoreLoading.value = true;
  const formData = new FormData();
  formData.append('multipartFile', file.raw);

  try {
    const response = await request({
      url: '/backup/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    if (response.data.code !== 1) {
      throw new Error(response.data.msg || '数据库恢复失败');
    }
    
    ElMessage.success('数据库恢复成功');
  } catch (error: any) {
    ElMessage.error(error.message || '数据库恢复失败');
  } finally {
    restoreLoading.value = false;
    uploadRef.value?.clearFiles();
  }
};

const handleBackup = async () => {
  backupLoading.value = true;
  try {
    const response = await request({
      url: '/backup/download',
      method: 'get',
      responseType: 'blob'
    });
    
    const blob = new Blob([response.data]);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    
    const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
    link.setAttribute('download', `tgDrive-backup-${timestamp}.db`);
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    ElMessage.success('数据库备份下载成功');
  } catch (error: any) {
    ElMessage.error('下载失败：' + error.message);
  } finally {
    backupLoading.value = false;
  }
};
</script>

<style scoped>
.page-container {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.content-card {
  width: 100%;
  max-width: 800px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

.action-section {
  padding: 10px;
}

.action-item {
  margin: 20px 0;
}

.action-item h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.action-item p {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
  line-height: 1.5;
}

.el-divider {
  margin: 40px 0;
}

/* Responsive styles for BackupPage.vue */
@media (max-width: 767px) { /* Mobile breakpoint */
  .page-container {
    padding: 10px; /* Reduce overall padding */
  }

  .content-card {
    max-width: 100%; /* Allow card to take full width */
    padding: 20px; /* Reduce card padding */
  }

  .card-header {
    font-size: 16px; /* Slightly smaller header font size */
  }

  .action-item {
    margin: 15px 0; /* Reduce margin between action items */
  }

  .action-item h3 {
    font-size: 15px; /* Slightly smaller heading */
  }

  .action-item p {
    font-size: 13px; /* Slightly smaller paragraph text */
  }

  .action-item .el-button {
    width: 100%; /* Make buttons full width */
  }

  .el-divider {
    margin: 30px 0; /* Reduce divider margin */
  }
}
</style>