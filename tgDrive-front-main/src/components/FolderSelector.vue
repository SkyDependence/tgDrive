<template>
    <div class="folder-selector">
        <el-popover :visible="popoverVisible" placement="bottom-start" :width="400" trigger="click"
            @show="loadFolders('/')">
            <template #reference>
                <el-input :model-value="localValue" :placeholder="placeholder" :disabled="disabled" clearable
                    @update:model-value="handleInput" @focus="showSuggestions = true" @blur="handleBlur">
                    <template #prepend>上传路径</template>
                    <template #suffix>
                        <el-icon class="folder-icon" @click.stop="togglePopover">
                            <FolderOpened />
                        </el-icon>
                    </template>
                </el-input>
            </template>

            <div class="folder-browser">
                <!-- 面包屑导航 -->
                <div class="breadcrumb-nav">
                    <el-breadcrumb separator="/">
                        <el-breadcrumb-item>
                            <span class="breadcrumb-item" @click="navigateTo('/')">根目录</span>
                        </el-breadcrumb-item>
                        <el-breadcrumb-item v-for="(segment, index) in pathSegments" :key="index">
                            <span class="breadcrumb-item" @click="navigateTo(buildPath(index))">
                                {{ segment }}
                            </span>
                        </el-breadcrumb-item>
                    </el-breadcrumb>
                </div>

                <!-- 加载状态 -->
                <div v-if="loading" class="loading-state">
                    <el-icon class="is-loading">
                        <Loading />
                    </el-icon>
                    <span>加载中...</span>
                </div>

                <!-- 文件夹列表 -->
                <div v-else class="folder-list">
                    <div v-if="folders.length === 0" class="empty-folders">
                        <el-empty description="该目录下没有子文件夹" :image-size="60" />
                    </div>
                    <div v-for="folder in folders" :key="folder.path" class="folder-item"
                        @click="handleFolderClick(folder)">
                        <el-icon>
                            <Folder />
                        </el-icon>
                        <span class="folder-name">{{ folder.name }}</span>
                        <el-icon v-if="folder.hasChildren" class="arrow-icon">
                            <ArrowRight />
                        </el-icon>
                    </div>
                </div>

                <!-- 操作按钮 -->
                <div class="folder-actions">
                    <el-button size="small" @click="popoverVisible = false">取消</el-button>
                    <el-button type="primary" size="small" @click="confirmSelection">
                        选择当前目录
                    </el-button>
                </div>
            </div>
        </el-popover>

        <!-- 搜索建议下拉 -->
        <div v-if="showSuggestions && suggestions.length > 0" class="suggestions-dropdown">
            <div v-for="suggestion in suggestions" :key="suggestion.path" class="suggestion-item"
                @mousedown.prevent="selectSuggestion(suggestion)">
                <el-icon>
                    <Folder />
                </el-icon>
                <span>{{ suggestion.path }}</span>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Folder, FolderOpened, ArrowRight, Loading } from '@element-plus/icons-vue'
import axios from 'axios'

interface FolderInfo {
    path: string
    name: string
    hasChildren?: boolean
}

const props = defineProps<{
    modelValue: string
    placeholder?: string
    disabled?: boolean
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: string): void
}>()

const popoverVisible = ref(false)
const currentPath = ref('/')
const folders = ref<FolderInfo[]>([])
const loading = ref(false)
const suggestions = ref<FolderInfo[]>([])
const showSuggestions = ref(false)
const searchTimer = ref<number | null>(null)

// 本地值，用于 v-model 绑定
const localValue = computed({
    get: () => props.modelValue,
    set: (value: string) => emit('update:modelValue', value)
})

// 计算路径段
const pathSegments = computed(() => {
    if (currentPath.value === '/') return []
    return currentPath.value.split('/').filter(s => s.length > 0)
})

// 构建路径
const buildPath = (index: number) => {
    return '/' + pathSegments.value.slice(0, index + 1).join('/') + '/'
}

// 切换弹窗
const togglePopover = () => {
    popoverVisible.value = !popoverVisible.value
}

// 加载文件夹
const loadFolders = async (path: string) => {
    loading.value = true
    currentPath.value = path
    try {
        const response = await axios.get('/webdav/folders', { params: { path } })
        if (response.data.code === 1) {
            folders.value = response.data.data
        }
    } catch (error) {
        console.error('加载文件夹失败:', error)
        folders.value = []
    } finally {
        loading.value = false
    }
}

// 导航到指定路径
const navigateTo = (path: string) => {
    loadFolders(path)
}

// 点击文件夹
const handleFolderClick = (folder: FolderInfo) => {
    if (folder.hasChildren) {
        loadFolders(folder.path)
    } else {
        currentPath.value = folder.path
    }
}

// 确认选择
const confirmSelection = () => {
    emit('update:modelValue', currentPath.value)
    popoverVisible.value = false
}

// 处理输入（模糊搜索）
const handleInput = (value: string) => {
    localValue.value = value

    // 防抖搜索
    if (searchTimer.value) {
        clearTimeout(searchTimer.value)
    }

    if (value && value.length >= 1) {
        searchTimer.value = window.setTimeout(async () => {
            try {
                const response = await axios.get('/webdav/folders/search', { params: { keyword: value } })
                if (response.data.code === 1) {
                    suggestions.value = response.data.data
                    showSuggestions.value = true
                }
            } catch (error) {
                console.error('搜索失败:', error)
                suggestions.value = []
            }
        }, 300)
    } else {
        suggestions.value = []
        showSuggestions.value = false
    }
}

// 处理失焦
const handleBlur = () => {
    setTimeout(() => {
        showSuggestions.value = false
    }, 200)
}

// 选择建议
const selectSuggestion = (suggestion: FolderInfo) => {
    emit('update:modelValue', suggestion.path)
    suggestions.value = []
    showSuggestions.value = false
}
</script>

<style scoped>
.folder-selector {
    position: relative;
}

.folder-icon {
    cursor: pointer;
    color: var(--el-color-primary);
}

.folder-icon:hover {
    color: var(--el-color-primary-light-3);
}

.folder-browser {
    max-height: 400px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.breadcrumb-nav {
    padding: 8px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
    margin-bottom: 8px;
}

.breadcrumb-item {
    cursor: pointer;
    color: var(--el-color-primary);
}

.breadcrumb-item:hover {
    text-decoration: underline;
}

.loading-state {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 20px;
    color: var(--el-text-color-secondary);
}

.folder-list {
    max-height: 250px;
    overflow-y: auto;
    flex: 1;
}

.empty-folders {
    padding: 20px;
}

.folder-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    cursor: pointer;
    border-radius: 4px;
    transition: background-color 0.2s;
}

.folder-item:hover {
    background-color: var(--el-fill-color-light);
}

.folder-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.arrow-icon {
    color: var(--el-text-color-secondary);
}

.folder-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
    margin-top: 8px;
}

.suggestions-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 1000;
    background: white;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    max-height: 200px;
    overflow-y: auto;
}

.suggestion-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    cursor: pointer;
}

.suggestion-item:hover {
    background-color: var(--el-fill-color-light);
}
</style>
