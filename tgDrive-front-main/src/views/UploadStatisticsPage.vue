<template>
  <div class="statistics-dashboard">
    <el-row :gutter="20">
      <!-- 总览卡片 -->
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <el-statistic
            title="总上传文件"
            :value="statistics.totalFiles"
          >
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <el-statistic
            title="总存储空间"
            :value="formatFileSize(statistics.totalStorage)"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <el-statistic
            title="节省空间(去重)"
            :value="formatFileSize(statistics.savedStorage)"
          >
            <template #prefix>
              <el-icon style="vertical-align: -0.125em">
                <TrendCharts />
              </el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <el-statistic
            title="平均上传速度"
            :value="formatBandwidth(statistics.averageSpeed)"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 上传趋势图 -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>上传趋势（最近7天）</span>
              <el-radio-group v-model="chartPeriod" size="small">
                <el-radio-button label="7d">7天</el-radio-button>
                <el-radio-button label="30d">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="uploadTrendChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 文件类型分布 -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <span>文件类型分布</span>
          </template>
          <div ref="fileTypeChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户上传排行 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <span>用户上传排行榜 TOP 10</span>
          </template>
          <el-table :data="topUsers" style="width: 100%">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="uploadCount" label="上传数量">
              <template #default="scope">
                {{ scope.row.uploadCount }} 个
              </template>
            </el-table-column>
            <el-table-column prop="totalSize" label="总大小">
              <template #default="scope">
                {{ formatFileSize(scope.row.totalSize) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 去重效果统计 -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <span>去重效果统计</span>
          </template>
          <div class="dedup-stats">
            <div class="dedup-item">
              <div class="dedup-label">重复文件数量</div>
              <div class="dedup-value">{{ statistics.duplicateCount }} 个</div>
            </div>
            <div class="dedup-item">
              <div class="dedup-label">去重率</div>
              <div class="dedup-value">
                <el-progress
                  :percentage="statistics.deduplicationRate"
                  :stroke-width="10"
                />
              </div>
            </div>
            <div class="dedup-item">
              <div class="dedup-label">节省存储空间</div>
              <div class="dedup-value highlight">
                {{ formatFileSize(statistics.savedStorage) }}
              </div>
            </div>
            <div class="dedup-item">
              <div class="dedup-label">平均文件引用次数</div>
              <div class="dedup-value">{{ statistics.averageReferences }} 次</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时活动监控 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="activity-header">
          <span>实时上传活动</span>
          <el-tag type="success" effect="plain">
            <el-icon class="is-loading"><Loading /></el-icon>
            实时更新
          </el-tag>
        </div>
      </template>
      <el-table :data="recentActivities" style="width: 100%">
        <el-table-column prop="time" label="时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="150" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="speed" label="速度" width="120">
          <template #default="scope">
            {{ formatBandwidth(scope.row.speed) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { TrendCharts, Loading } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import request from '@/utils/request';

// 状态
const statistics = ref({
  totalFiles: 0,
  totalStorage: 0,
  savedStorage: 0,
  averageSpeed: 0,
  duplicateCount: 0,
  deduplicationRate: 0,
  averageReferences: 0
});

const chartPeriod = ref('7d');
const topUsers = ref<any[]>([]);
const recentActivities = ref<any[]>([]);

// Chart refs
const uploadTrendChart = ref();
const fileTypeChart = ref();

let trendChart: echarts.ECharts | null = null;
let typeChart: echarts.ECharts | null = null;
let updateInterval: number;

// 初始化图表
const initCharts = () => {
  // 上传趋势图
  if (uploadTrendChart.value) {
    trendChart = echarts.init(uploadTrendChart.value);
    const trendOption = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: {
        type: 'value',
        name: '上传数量'
      },
      series: [
        {
          name: '上传数量',
          type: 'line',
          smooth: true,
          data: [120, 200, 150, 80, 70, 110, 130],
          areaStyle: {
            opacity: 0.3
          }
        },
        {
          name: '上传大小(GB)',
          type: 'bar',
          yAxisIndex: 1,
          data: [10, 20, 15, 8, 7, 11, 13]
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '上传数量'
        },
        {
          type: 'value',
          name: '大小(GB)'
        }
      ]
    };
    trendChart.setOption(trendOption);
  }

  // 文件类型分布图
  if (fileTypeChart.value) {
    typeChart = echarts.init(fileTypeChart.value);
    const typeOption = {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '5%',
        left: 'center'
      },
      series: [
        {
          name: '文件类型',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '20',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: 1048, name: '视频' },
            { value: 735, name: '图片' },
            { value: 580, name: '文档' },
            { value: 484, name: '音频' },
            { value: 300, name: '其他' }
          ]
        }
      ]
    };
    typeChart.setOption(typeOption);
  }
};

// 获取统计数据
const fetchStatistics = async () => {
  try {
    // 这里应该调用实际的API
    // const response = await request.get('/statistics/overview');
    // statistics.value = response.data;

    // 模拟数据
    statistics.value = {
      totalFiles: 12345,
      totalStorage: 1099511627776, // 1TB
      savedStorage: 107374182400, // 100GB
      averageSpeed: 10485760, // 10MB/s
      duplicateCount: 1234,
      deduplicationRate: 23,
      averageReferences: 2.3
    };

    topUsers.value = [
      { username: 'user1', uploadCount: 456, totalSize: 53687091200 },
      { username: 'user2', uploadCount: 321, totalSize: 42949672960 },
      { username: 'user3', uploadCount: 234, totalSize: 32212254720 }
    ];

    recentActivities.value = [
      {
        time: new Date(),
        username: 'user1',
        fileName: 'document.pdf',
        fileSize: 5242880,
        status: '上传中',
        speed: 2097152
      }
    ];
  } catch (error) {
    console.error('获取统计数据失败:', error);
  }
};

// 格式化函数
const formatFileSize = (bytes: number): string => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

const formatBandwidth = (bytesPerSecond: number): string => {
  if (!bytesPerSecond) return '0 KB/s';
  return formatFileSize(bytesPerSecond) + '/s';
};

const formatTime = (date: Date | string): string => {
  const d = typeof date === 'string' ? new Date(date) : date;
  return d.toLocaleString('zh-CN');
};

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    '上传中': 'primary',
    '已完成': 'success',
    '失败': 'danger'
  };
  return types[status] || 'info';
};

// 生命周期
onMounted(() => {
  fetchStatistics();
  initCharts();

  // 自动刷新
  updateInterval = window.setInterval(() => {
    fetchStatistics();
  }, 5000);

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    trendChart?.resize();
    typeChart?.resize();
  });
});

onUnmounted(() => {
  if (updateInterval) {
    clearInterval(updateInterval);
  }
  trendChart?.dispose();
  typeChart?.dispose();
});
</script>

<style scoped>
.statistics-dashboard {
  padding: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.dedup-stats {
  padding: 20px 0;
}

.dedup-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.dedup-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.dedup-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.dedup-value {
  font-size: 20px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.dedup-value.highlight {
  color: var(--el-color-success);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .stat-card {
    margin-bottom: 10px;
  }

  .chart-container {
    height: 250px;
  }
}
</style>