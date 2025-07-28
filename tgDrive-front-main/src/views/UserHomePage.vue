<template>
  <div class="user-home-page">
    <div class="home-container">
      <div class="page-header">
        <h2 class="page-title">我的文件</h2>
        <div class="header-actions">
          <el-button type="primary" @click="goToUpload">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <el-button @click="refreshFiles">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <!-- 文件统计 -->
      <div class="stats-row">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ fileStats.totalFiles }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon"><FolderOpened /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ fileStats.totalSize }}</div>
              <div class="stat-label">总大小</div>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon"><Clock /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ fileStats.recentUploads }}</div>
              <div class="stat-label">今日上传</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 搜索和过滤 -->
      <el-card class="search-card">
        <div class="search-row">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名..."
            class="search-input"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="fileTypeFilter" placeholder="文件类型" class="type-filter">
            <el-option label="全部" value="" />
            <el-option label="图片" value="image" />
            <el-option label="文档" value="document" />
            <el-option label="视频" value="video" />
            <el-option label="音频" value="audio" />
            <el-option label="其他" value="other" />
          </el-select>
        </div>
      </el-card>

      <!-- 文件列表 -->
      <el-card class="files-card">
        <div class="files-header">
          <h3>文件列表</h3>
          <div class="view-toggle">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="list">列表</el-radio-button>
              <el-radio-button label="grid">网格</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 列表视图 -->
        <el-table 
          v-if="viewMode === 'list'"
          :data="filteredFiles" 
          style="width: 100%"
          v-loading="loading"
        >
          <el-table-column label="文件名" min-width="200">
            <template #default="scope">
              <div class="file-name-cell">
                <el-icon class="file-icon">{{ getFileIcon(scope.row.fileName) }}</el-icon>
                <span class="file-name">{{ scope.row.fileName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120" />
          <el-table-column prop="uploadTime" label="上传时间" width="180" />
          <el-table-column prop="isPublic" label="公开" width="80">
            <template #default="scope">
              <el-switch
                v-model="scope.row.isPublic"
                @change="togglePublic(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" @click="downloadFile(scope.row)">下载</el-button>
              <el-button size="small" @click="shareFile(scope.row)">分享</el-button>
              <el-button size="small" type="danger" @click="deleteFile(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 网格视图 -->
        <div v-else class="files-grid" v-loading="loading">
          <div v-for="file in filteredFiles" :key="file.fileId" class="file-card">
            <div class="file-preview">
              <el-icon class="file-icon-large">{{ getFileIcon(file.fileName) }}</el-icon>
            </div>
            <div class="file-info">
              <div class="file-name" :title="file.fileName">{{ file.fileName }}</div>
              <div class="file-meta">
                <span class="file-size">{{ file.fileSize }}</span>
                <span class="file-time">{{ file.uploadTime }}</span>
              </div>
            </div>
            <div class="file-actions">
              <el-button size="small" @click="downloadFile(file)">下载</el-button>
              <el-button size="small" @click="shareFile(file)">分享</el-button>
              <el-button size="small" type="danger" @click="deleteFile(file)">删除</el-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && filteredFiles.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Document /></el-icon>
          <p class="empty-text">暂无文件</p>
          <el-button type="primary" @click="goToUpload">上传第一个文件</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, 
  Refresh, 
  Document, 
  FolderOpened, 
  Clock, 
  Search,
  Picture,
  VideoPlay,
  Headphone,
  Files
} from '@element-plus/icons-vue'
import request from '@/utils/request'

interface FileItem {
  fileId: string
  fileName: string
  fileSize: string
  uploadTime: string
  filePath: string
  isPublic: boolean
  fileType: string
}

interface FileStats {
  totalFiles: number
  totalSize: string
  recentUploads: number
}

const router = useRouter()
const loading = ref(false)
const files = ref<FileItem[]>([])
const searchKeyword = ref('')
const fileTypeFilter = ref('')
const viewMode = ref<'list' | 'grid'>('list')
const fileStats = ref<FileStats>({
  totalFiles: 0,
  totalSize: '0 B',
  recentUploads: 0
})

// 过滤后的文件列表
const filteredFiles = computed(() => {
  let result = files.value
  
  // 按关键词搜索
  if (searchKeyword.value) {
    result = result.filter(file => 
      file.fileName.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  // 按文件类型过滤
  if (fileTypeFilter.value) {
    result = result.filter(file => file.fileType === fileTypeFilter.value)
  }
  
  return result
})

// 获取文件图标
const getFileIcon = (fileName: string) => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext || '')) {
    return Picture
  } else if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'].includes(ext || '')) {
    return VideoPlay
  } else if (['mp3', 'wav', 'flac', 'aac', 'ogg'].includes(ext || '')) {
    return Headphone
  } else {
    return Files
  }
}

// 获取文件类型
const getFileType = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext || '')) {
    return 'image'
  } else if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'].includes(ext || '')) {
    return 'video'
  } else if (['mp3', 'wav', 'flac', 'aac', 'ogg'].includes(ext || '')) {
    return 'audio'
  } else if (['doc', 'docx', 'pdf', 'txt', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext || '')) {
    return 'document'
  } else {
    return 'other'
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

// 加载文件列表
const loadFiles = async () => {
  loading.value = true
  try {
    const response = await request.get('/api/files/user')
    files.value = response.data.map((file: any) => ({
      fileId: file.fileId,
      fileName: file.fileName,
      fileSize: formatFileSize(file.fileSize),
      uploadTime: new Date(file.uploadTime).toLocaleString(),
      filePath: file.filePath,
      isPublic: file.isPublic,
      fileType: getFileType(file.fileName)
    }))
    
    // 更新统计信息
    updateStats()
  } catch (error) {
    ElMessage.error('加载文件列表失败')
    console.error('加载文件失败:', error)
  } finally {
    loading.value = false
  }
}

// 更新统计信息
const updateStats = () => {
  fileStats.value.totalFiles = files.value.length
  
  // 计算总大小（这里简化处理，实际应该从后端获取准确数据）
  const totalBytes = files.value.reduce((sum, file) => {
    const sizeStr = file.fileSize
    const [size, unit] = sizeStr.split(' ')
    const multiplier = unit === 'GB' ? 1024 * 1024 * 1024 : 
                      unit === 'MB' ? 1024 * 1024 : 
                      unit === 'KB' ? 1024 : 1
    return sum + (parseFloat(size) * multiplier)
  }, 0)
  fileStats.value.totalSize = formatFileSize(totalBytes)
  
  // 计算今日上传数量
  const today = new Date().toDateString()
  fileStats.value.recentUploads = files.value.filter(file => 
    new Date(file.uploadTime).toDateString() === today
  ).length
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已在computed中处理
}

// 刷新文件列表
const refreshFiles = () => {
  loadFiles()
}

// 跳转到上传页面
const goToUpload = () => {
  router.push('/user/upload')
}

// 下载文件
const downloadFile = (file: FileItem) => {
  const downloadUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/files/download/${file.fileId}`
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = file.fileName
  link.click()
}

// 分享文件
const shareFile = (file: FileItem) => {
  const shareUrl = `${window.location.origin}/share/${file.fileId}`
  const message = '分享链接已复制到剪贴板';
  
  // 优先使用现代的 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(shareUrl).then(() => {
      ElMessage.success(message);
    }).catch(err => {
      console.error('Clipboard API failed:', err);
      fallbackCopyTextToClipboard(shareUrl, message);
    });
  } else {
    // 降级到传统方法
    fallbackCopyTextToClipboard(shareUrl, message);
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

// 切换公开状态
const togglePublic = async (file: FileItem) => {
  try {
    await request.put(`/api/files/${file.fileId}/public`, {
      isPublic: file.isPublic
    })
    ElMessage.success(`文件已${file.isPublic ? '设为公开' : '设为私有'}`)
  } catch (error) {
    file.isPublic = !file.isPublic // 回滚状态
    ElMessage.error('更新失败')
  }
}

// 删除文件
const deleteFile = async (file: FileItem) => {
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
    ElMessage.success('文件删除成功')
    loadFiles()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('文件删除失败')
    }
  }
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.user-home-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.home-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 32px;
  color: var(--el-color-primary);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
}

.search-card {
  border-radius: 8px;
}

.search-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.type-filter {
  width: 120px;
}

.files-card {
  border-radius: 8px;
}

.files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.files-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 16px;
  color: var(--el-color-primary);
}

.file-name {
  font-weight: 500;
}

.files-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.file-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}

.file-card:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.file-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 80px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 6px;
  margin-bottom: 12px;
}

.file-icon-large {
  font-size: 32px;
  color: var(--el-color-primary);
}

.file-info {
  margin-bottom: 12px;
}

.file-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.file-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: var(--el-text-color-regular);
  margin: 0 0 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-home-page {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input {
    max-width: none;
  }
  
  .files-grid {
    grid-template-columns: 1fr;
  }
}
</style>