<template>
  <div class="upload-page">
    <div class="upload-container">
      <h2 class="page-title">文件上传</h2>
      
      <el-card class="upload-card">
        <el-upload
          ref="uploadRef"
          class="upload-dragger"
          drag
          :action="uploadUrl"
          :headers="uploadHeaders"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :on-progress="handleUploadProgress"
          :before-upload="beforeUpload"
          :file-list="fileList"
          multiple
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持多文件上传，单个文件大小不超过100MB
            </div>
          </template>
        </el-upload>
      </el-card>

      <!-- 上传进度 -->
      <el-card v-if="uploadProgress.length > 0" class="progress-card">
        <h3>上传进度</h3>
        <div v-for="(progress, index) in uploadProgress" :key="index" class="progress-item">
          <div class="progress-info">
            <span class="file-name">{{ progress.fileName }}</span>
            <span class="progress-text">{{ progress.percentage }}%</span>
          </div>
          <el-progress :percentage="progress.percentage" :status="progress.status" />
        </div>
      </el-card>

      <!-- 最近上传的文件 -->
      <el-card v-if="recentFiles.length > 0" class="recent-files-card">
        <h3>最近上传</h3>
        <el-table :data="recentFiles" style="width: 100%">
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="fileSize" label="文件大小" />
          <el-table-column prop="uploadTime" label="上传时间" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="downloadFile(scope.row)">下载</el-button>
              <el-button size="small" type="danger" @click="deleteFile(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadProps, UploadUserFile } from 'element-plus'
import request from '@/utils/request'

interface UploadProgress {
  fileName: string
  percentage: number
  status: 'success' | 'exception' | 'warning' | undefined
}

interface RecentFile {
  fileId: string
  fileName: string
  fileSize: string
  uploadTime: string
  filePath: string
}

const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const uploadProgress = ref<UploadProgress[]>([])
const recentFiles = ref<RecentFile[]>([])

// 上传配置
const uploadUrl = computed(() => {
  return `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/files/upload`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return {
    'Authorization': `Bearer ${token}`
  }
})

// 上传前检查
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过100MB!')
    return false
  }
  
  // 添加到进度列表
  uploadProgress.value.push({
    fileName: file.name,
    percentage: 0,
    status: undefined
  })
  
  return true
}

// 上传进度
const handleUploadProgress = (evt: any, file: any) => {
  const progress = uploadProgress.value.find(p => p.fileName === file.name)
  if (progress) {
    progress.percentage = Math.round(evt.percent)
  }
}

// 上传成功
const handleUploadSuccess = (response: any, file: any) => {
  const progress = uploadProgress.value.find(p => p.fileName === file.name)
  if (progress) {
    progress.percentage = 100
    progress.status = 'success'
  }
  
  ElMessage.success(`${file.name} 上传成功!`)
  
  // 刷新最近文件列表
  loadRecentFiles()
  
  // 3秒后移除进度条
  setTimeout(() => {
    const index = uploadProgress.value.findIndex(p => p.fileName === file.name)
    if (index > -1) {
      uploadProgress.value.splice(index, 1)
    }
  }, 3000)
}

// 上传失败
const handleUploadError = (error: any, file: any) => {
  const progress = uploadProgress.value.find(p => p.fileName === file.name)
  if (progress) {
    progress.status = 'exception'
  }
  
  ElMessage.error(`${file.name} 上传失败!`)
}

// 加载最近上传的文件
const loadRecentFiles = async () => {
  try {
    const response = await request.get('/api/files/recent')
    recentFiles.value = response.data.map((file: any) => ({
      fileId: file.fileId,
      fileName: file.fileName,
      fileSize: formatFileSize(file.fileSize),
      uploadTime: new Date(file.uploadTime).toLocaleString(),
      filePath: file.filePath
    }))
  } catch (error) {
    console.error('加载最近文件失败:', error)
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 下载文件
const downloadFile = (file: RecentFile) => {
  const downloadUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/files/download/${file.fileId}`
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = file.fileName
  link.click()
}

// 删除文件
const deleteFile = async (file: RecentFile) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.fileName}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await request.delete(`/api/files/${file.fileId}`)
    ElMessage.success('文件删除成功!')
    loadRecentFiles()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('文件删除失败!')
    }
  }
}

onMounted(() => {
  loadRecentFiles()
})
</script>

<style scoped>
.upload-page {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.upload-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 20px 0;
  text-align: center;
}

.upload-card {
  border-radius: 8px;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger .el-upload {
  width: 100%;
}

.upload-dragger .el-upload-dragger {
  width: 100%;
  height: 200px;
  border: 2px dashed var(--el-border-color);
  border-radius: 8px;
  background-color: var(--el-fill-color-lighter);
  transition: all 0.3s;
}

.upload-dragger .el-upload-dragger:hover {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.progress-card,
.recent-files-card {
  border-radius: 8px;
}

.progress-card h3,
.recent-files-card h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.progress-item {
  margin-bottom: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.file-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.progress-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .upload-page {
    padding: 10px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .upload-dragger .el-upload-dragger {
    height: 150px;
  }
}
</style>