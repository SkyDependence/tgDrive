<template>
  <div class="page-container">
    <!-- Form for creating/updating config -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>Bot 配置</span>
        </div>
      </template>
      <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="rules"
        label-position="top"
        class="config-form"
        @submit.prevent="handleSubmit"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-form-item label="配置名称" prop="name">
              <el-input v-model="ruleForm.name" placeholder="为您的配置命名" :prefix-icon="Document" size="large" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-form-item label="Chat ID" prop="target">
              <el-input v-model="ruleForm.target" placeholder="请输入接收文件的频道/用户 ID" :prefix-icon="ChatDotRound" size="large" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
            <el-form-item label="Bot Token" prop="token">
              <el-input v-model="ruleForm.token" placeholder="请输入 Telegram Bot Token" :prefix-icon="Key" size="large" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-form-item label="自定义 URL (选填)" prop="url">
              <el-input v-model="ruleForm.url" placeholder="用于生成下载链接的域名" :prefix-icon="Link" size="large" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-form-item label="访问密码 (选填)" prop="pass">
              <el-input v-model="ruleForm.pass" type="password" placeholder="为配置设置访问密码" show-password :prefix-icon="Lock" size="large" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item class="form-actions">
          <div class="form-button-group">
            <el-button type="primary" native-type="submit" :loading="isSubmitting" size="large">
              {{ isSubmitting ? '提交中...' : '保存并提交' }}
            </el-button>
            <el-button @click="resetForm" size="large">重置表单</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Section for loading existing configs -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <el-icon><Files /></el-icon>
          <span>加载现有配置</span>
        </div>
      </template>
      <div class="load-config-wrapper">
        <el-select
          v-model="selectedConfig"
          placeholder="请选择一个配置文件进行加载"
          class="config-select"
          size="large"
          :loading="isLoadingConfigs"
          @change="handleConfigSelect"
          filterable
        >
          <el-option
            v-for="config in configList"
            :key="config.name"
            :label="config.name"
            :value="config.name"
          />
        </el-select>
        <div class="config-button-group">
          <el-button type="success" @click="loadConfig" :loading="isLoading" :disabled="!selectedConfig" size="large" plain>
            加载选中配置
          </el-button>
          <el-button type="danger" @click="deleteConfig" :loading="isDeleting" :disabled="!selectedConfig" size="large" plain>
            删除配置
          </el-button>
        </div>
      </div>

      <el-collapse-transition>
        <div v-if="selectedConfigData" class="config-preview">
          <el-divider />
          <h3>配置预览</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="名称">{{ selectedConfigData.name }}</el-descriptions-item>
            <el-descriptions-item label="Token">{{ selectedConfigData.token }}</el-descriptions-item>
            <el-descriptions-item label="Chat ID">{{ selectedConfigData.target }}</el-descriptions-item>
            <el-descriptions-item label="URL">{{ selectedConfigData.url || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="密码">{{ selectedConfigData.pass ? '******' : '未设置' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-collapse-transition>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import request from '../utils/request'
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Document, Key, ChatDotRound, Link, Lock, Setting, Files } from '@element-plus/icons-vue';

interface ConfigForm {
  name: string;
  token: string;
  target: string;
  url?: string;
  pass?: string;
}

const ruleFormRef = ref<FormInstance>();

const ruleForm = reactive<ConfigForm>({
  name: '',
  token: '',
  target: '',
  url: '',
  pass: '',
});

const isSubmitting = ref(false);
const isLoading = ref(false);
const isLoadingConfigs = ref(false);
const isDeleting = ref(false);
const configList = ref<ConfigForm[]>([]);
const selectedConfig = ref('');
const selectedConfigData = ref<ConfigForm | null>(null);

const rules = reactive<FormRules>({
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  token: [{ required: true, message: '请输入 Bot Token', trigger: 'blur' }],
  target: [{ required: true, message: '请输入 Chat ID', trigger: 'blur' }],
});

const fetchConfigList = async () => {
  isLoadingConfigs.value = true;
  try {
    const response = await request.get('/config/configs');
    if (response.data.code === 1 && Array.isArray(response.data.data)) {
      configList.value = response.data.data;
    } else {
      ElMessage.error('获取配置列表失败');
    }
  } catch (error) {
    ElMessage.error('获取配置列表失败，请检查网络');
  } finally {
    isLoadingConfigs.value = false;
  }
};

const handleConfigSelect = (configName: string) => {
  selectedConfigData.value = configList.value.find(config => config.name === configName) || null;
};

const handleSubmit = async () => {
  if (!ruleFormRef.value) return;
  await ruleFormRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        const response = await request.post('/config', ruleForm);
        if (response.data.code === 1) {
          ElMessage.success(response.data.msg || '配置提交成功');
          ruleFormRef.value?.resetFields();
          fetchConfigList();
        } else {
          ElMessage.error(response.data.msg || '提交失败');
        }
      } catch (error: any) {
        ElMessage.error(error.response?.data?.msg || '提交失败，请检查网络');
      } finally {
        isSubmitting.value = false;
      }
    }
  });
};

const resetForm = () => {
  ruleFormRef.value?.resetFields();
};

const loadConfig = async () => {
  if (!selectedConfig.value) {
    ElMessage.warning('请先选择一个配置');
    return;
  }
  isLoading.value = true;
  try {
    const response = await request.get(`/config/${selectedConfig.value}`);
    if (response.data.code === 1) {
      ElMessage.success(response.data.msg || '配置加载成功');
    } else {
      ElMessage.error(response.data.msg || '加载配置失败');
    }
  } catch (error) {
    ElMessage.error('加载失败，请检查网络');
  } finally {
    isLoading.value = false;
  }
};

const deleteConfig = async () => {
  if (!selectedConfig.value) {
    ElMessage.warning('请先选择一个配置');
    return;
  }
  
  // 确认删除
  try {
    await ElMessageBox.confirm(
      `确定要删除配置 "${selectedConfig.value}" 吗？此操作不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    return; // 用户取消删除
  }
  
  isDeleting.value = true;
  try {
    const response = await request.delete(`/config/${selectedConfig.value}`);
    if (response.data.code === 1) {
      ElMessage.success(response.data.msg || '配置删除成功');
      selectedConfig.value = '';
      selectedConfigData.value = null;
      fetchConfigList(); // 重新获取配置列表
    } else {
      ElMessage.error(response.data.msg || '删除配置失败');
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '删除失败，请检查网络');
  } finally {
    isDeleting.value = false;
  }
};

onMounted(() => {
  fetchConfigList();
});
</script>

<style scoped>
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-card {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-button-group {
  display: flex;
  gap: 8px;
}

.load-config-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.config-button-group {
  display: flex;
  gap: 8px;
}

.config-select {
  flex-grow: 1;
}

.button-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

.config-preview {
  margin-top: 20px;
}

.config-preview h3 {
  margin-bottom: 10px;
  font-size: 16px;
}

/* 响应式设计 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .page-container {
    padding: 8px;
    gap: 12px;
  }

  .content-card {
    border-radius: 8px;
  }

  .card-header {
    font-size: 16px;
    flex-wrap: wrap;
    justify-content: center;
    text-align: center;
  }

  .form-actions {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
    margin-top: 16px;
  }

  .form-button-group {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .form-button-group .el-button {
    flex: 1;
    min-height: 44px; /* 触摸友好的按钮高度 */
  }

  .load-config-wrapper {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .config-select {
    width: 100%;
  }

  .config-button-group {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .config-button-group .el-button {
    flex: 1;
    min-height: 44px;
  }

  .config-preview {
    margin-top: 16px;
  }

  .config-preview h3 {
    font-size: 14px;
    text-align: center;
  }
}

/* 小屏幕 (平板, 576px 到 767px) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .page-container {
    padding: 12px;
    gap: 16px;
  }

  .form-actions {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .form-button-group {
    display: flex;
    gap: 10px;
    width: 100%;
  }

  .form-button-group .el-button {
    flex: 1;
  }

  .load-config-wrapper {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .config-select {
    width: 100%;
  }

  .config-button-group {
    display: flex;
    gap: 10px;
    width: 100%;
  }

  .config-button-group .el-button {
    flex: 1;
  }
}

/* 中等屏幕 (768px 到 991px) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .page-container {
    padding: 16px;
    max-width: 800px;
    margin: 0 auto;
  }

  .form-actions {
    justify-content: center;
    gap: 12px;
  }

  .load-config-wrapper {
    max-width: 400px;
    margin: 0 auto;
  }
}

/* 大屏幕及以上 (992px+) */
@media (min-width: 992px) {
  .page-container {
    padding: 20px;
    max-width: 1000px;
    margin: 0 auto;
  }

  .form-actions {
    justify-content: flex-end;
  }

  .load-config-wrapper {
    max-width: 500px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .el-button {
    min-height: 44px;
  }
  
  .el-input {
    min-height: 44px;
  }
  
  .el-select {
    min-height: 44px;
  }
}

/* 横屏模式优化 */
@media (max-width: 767px) and (orientation: landscape) {
  .page-container {
    padding: 8px 16px;
  }
  
  .form-actions {
    flex-direction: row;
    justify-content: space-around;
  }
  
  .form-actions .el-button {
    width: auto;
    flex: 1;
    margin: 0 4px;
  }
}
</style>
