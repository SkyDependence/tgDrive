/**
 * 上传队列管理器
 * 支持任务优先级、自动调度、队列持久化等功能
 */

import { ResumableUploader, UploadOptions } from './resumableUploader';

export interface QueuedTask {
  id: string;
  file: File;
  priority: number; // 优先级 1-10，数字越大优先级越高
  status: 'pending' | 'uploading' | 'completed' | 'failed' | 'paused';
  progress: number;
  addedAt: Date;
  startedAt?: Date;
  completedAt?: Date;
  error?: string;
  retries: number;
  options?: UploadOptions;
  result?: any;
  serverTaskId?: string;
}

export interface QueueOptions {
  maxConcurrent?: number; // 最大并发上传数
  autoStart?: boolean; // 自动开始上传
  persistQueue?: boolean; // 是否持久化队列到localStorage
  onTaskComplete?: (task: QueuedTask) => void;
  onTaskFailed?: (task: QueuedTask, error: Error) => void;
  onQueueUpdate?: (queue: QueuedTask[]) => void;
  maxBandwidth?: number; // 最大带宽限制 (bytes/second)
}

export class UploadQueueManager {
  private queue: Map<string, QueuedTask> = new Map();
  private activeUploads: Map<string, ResumableUploader> = new Map();
  private options: Required<QueueOptions>;
  private isRunning = false;
  private bandwidthMonitor: BandwidthMonitor;

  private readonly STORAGE_KEY = 'upload_queue';
  private readonly DEFAULT_OPTIONS: Required<QueueOptions> = {
    maxConcurrent: 3,
    autoStart: true,
    persistQueue: true,
    onTaskComplete: () => {},
    onTaskFailed: () => {},
    onQueueUpdate: () => {},
    maxBandwidth: 0 // 0 means unlimited
  };

  constructor(options: QueueOptions = {}) {
    this.options = { ...this.DEFAULT_OPTIONS, ...options };
    this.bandwidthMonitor = new BandwidthMonitor();

    if (this.options.persistQueue) {
      this.loadQueueFromStorage();
    }

    if (this.options.autoStart) {
      this.start();
    }
  }

  /**
   * 添加文件到队列
   */
  addTask(file: File, priority: number = 5, options?: UploadOptions): string {
    const taskId = this.generateTaskId(file);

    const task: QueuedTask = {
      id: taskId,
      file,
      priority,
      status: 'pending',
      progress: 0,
      addedAt: new Date(),
      retries: 0,
      options
    };

    this.queue.set(taskId, task);
    this.sortQueue();
    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());

    if (this.isRunning) {
      this.processNext();
    }

    return taskId;
  }

  /**
   * 批量添加文件
   */
  addBatch(files: File[], priority: number = 5, options?: UploadOptions): string[] {
    const taskIds: string[] = [];

    for (const file of files) {
      taskIds.push(this.addTask(file, priority, options));
    }

    return taskIds;
  }

  /**
   * 移除任务
   */
  removeTask(taskId: string): boolean {
    const task = this.queue.get(taskId);
    if (!task) return false;

    // 如果正在上传，先取消
    const uploader = this.activeUploads.get(taskId);
    if (uploader) {
      uploader.cancel();
      this.activeUploads.delete(taskId);
    }

    this.queue.delete(taskId);
    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());

    return true;
  }

  /**
   * 取消正在上传的任务
   */
  async cancelTask(taskId: string): Promise<boolean> {
    const task = this.queue.get(taskId);
    if (!task) {
      return false;
    }

    const uploader = this.activeUploads.get(taskId);
    if (uploader) {
      try {
        uploader.cancel();
        if (task.serverTaskId) {
          await uploader.cancelTask(task.serverTaskId);
        }
      } catch (error) {
        console.warn(`取消任务 ${taskId} 时发生异常`, error);
      }
      this.activeUploads.delete(taskId);
    }

    this.queue.delete(taskId);
    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());
    return true;
  }

  /**
   * 取消所有任务
   */
  async cancelAll(): Promise<void> {
    const taskIds = Array.from(this.queue.keys());
    for (const taskId of taskIds) {
      await this.cancelTask(taskId);
    }
  }

  /**
   * 暂停任务
   */
  pauseTask(taskId: string): boolean {
    const task = this.queue.get(taskId);
    if (!task) return false;

    const uploader = this.activeUploads.get(taskId);
    if (uploader) {
      uploader.pause();
      task.status = 'paused';
      this.activeUploads.delete(taskId);
      this.processNext(); // 处理下一个任务
    } else if (task.status === 'pending') {
      task.status = 'paused';
    }

    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());

    return true;
  }

  /**
   * 恢复任务
   */
  resumeTask(taskId: string): boolean {
    const task = this.queue.get(taskId);
    if (!task || task.status !== 'paused') return false;

    task.status = 'pending';
    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());

    if (this.isRunning) {
      this.processNext();
    }

    return true;
  }

  /**
   * 调整任务优先级
   */
  setPriority(taskId: string, priority: number): boolean {
    const task = this.queue.get(taskId);
    if (!task || task.status !== 'pending') return false;

    task.priority = Math.max(1, Math.min(10, priority));
    this.sortQueue();
    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());

    return true;
  }

  /**
   * 开始处理队列
   */
  start(): void {
    this.isRunning = true;
    this.processNext();
  }

  /**
   * 停止处理队列
   */
  stop(): void {
    this.isRunning = false;

    // 暂停所有活动上传
    for (const [taskId, uploader] of this.activeUploads) {
      uploader.pause();
      const task = this.queue.get(taskId);
      if (task) {
        task.status = 'paused';
      }
    }

    this.activeUploads.clear();
    this.saveQueueToStorage();
  }

  /**
   * 清空队列
   */
  clear(): void {
    this.stop();
    this.queue.clear();
    this.saveQueueToStorage();
    this.options.onQueueUpdate([]);
  }

  /**
   * 获取所有任务
   */
  getTasks(): QueuedTask[] {
    return Array.from(this.queue.values());
  }

  /**
   * 获取队列统计信息
   */
  getStatistics() {
    const tasks = this.getTasks();
    const stats = {
      total: tasks.length,
      pending: tasks.filter(t => t.status === 'pending').length,
      uploading: tasks.filter(t => t.status === 'uploading').length,
      completed: tasks.filter(t => t.status === 'completed').length,
      failed: tasks.filter(t => t.status === 'failed').length,
      paused: tasks.filter(t => t.status === 'paused').length,
      totalProgress: 0,
      estimatedTime: 0,
      currentBandwidth: this.bandwidthMonitor.getCurrentBandwidth()
    };

    // 计算总进度
    if (tasks.length > 0) {
      const totalProgress = tasks.reduce((sum, t) => sum + (t.progress || 0), 0);
      stats.totalProgress = totalProgress / tasks.length;
    }

    // 估算剩余时间
    if (stats.currentBandwidth > 0) {
      const remainingBytes = tasks
        .filter(t => t.status === 'pending' || t.status === 'uploading')
        .reduce((sum, t) => sum + (t.file.size * (1 - (t.progress || 0) / 100)), 0);
      stats.estimatedTime = remainingBytes / stats.currentBandwidth; // 秒
    }

    return stats;
  }

  /**
   * 处理下一个任务
   */
  private async processNext(): Promise<void> {
    if (!this.isRunning) return;

    // 检查并发限制
    if (this.activeUploads.size >= this.options.maxConcurrent) return;

    // 获取下一个待处理任务
    const nextTask = this.getNextPendingTask();
    if (!nextTask) return;

    // 开始上传
    nextTask.status = 'uploading';
    nextTask.startedAt = new Date();
    this.saveQueueToStorage();
    this.options.onQueueUpdate(this.getTasks());

    const uploader = new ResumableUploader();
    this.activeUploads.set(nextTask.id, uploader);

    try {
      const taskOptions = nextTask.options || {};
      const {
        onProgress: taskOnProgress,
        onChunkComplete: taskOnChunkComplete,
        onComplete: taskOnComplete,
        onError: taskOnError,
        ...restTaskOptions
      } = taskOptions;

      // 配置带宽限制
      const uploadOptions: UploadOptions = {
        ...restTaskOptions,
        onProgress: (progress, chunkIndex, totalChunks, taskId, stageOneRatio) => {
          if (taskId && nextTask.serverTaskId !== taskId) {
            nextTask.serverTaskId = taskId;
          }
          nextTask.progress = progress;
          this.bandwidthMonitor.recordProgress(progress, nextTask.file.size);
          this.options.onQueueUpdate(this.getTasks());
          taskOnProgress?.(progress, chunkIndex, totalChunks, taskId, stageOneRatio);
        },
        onChunkComplete: (chunkIndex, totalChunks, taskId, completedCount) => {
          taskOnChunkComplete?.(chunkIndex, totalChunks, taskId, completedCount);
        },
        onComplete: (result) => {
          nextTask.status = 'completed';
          nextTask.completedAt = new Date();
          nextTask.result = result;
          nextTask.progress = 100;
          this.activeUploads.delete(nextTask.id);
          this.saveQueueToStorage();
          this.options.onTaskComplete(nextTask);
          this.options.onQueueUpdate(this.getTasks());
          taskOnComplete?.(result);
          this.processNext(); // 处理下一个
        },
        onError: (error) => {
          nextTask.status = 'failed';
          nextTask.error = error.message;
          nextTask.retries++;
          this.activeUploads.delete(nextTask.id);

          // 如果重试次数未超限，重新加入队列
          if (nextTask.retries < 3) {
            nextTask.status = 'pending';
            console.log(`任务 ${nextTask.id} 失败，准备第 ${nextTask.retries} 次重试`);
          } else {
            this.options.onTaskFailed(nextTask, error);
          }

          this.saveQueueToStorage();
          this.options.onQueueUpdate(this.getTasks());
          taskOnError?.(error);
          this.processNext(); // 处理下一个
        }
      };

      // 应用带宽限制
      if (this.options.maxBandwidth > 0) {
        uploadOptions.concurrency = this.calculateOptimalConcurrency();
      }

      await uploader.upload(nextTask.file, uploadOptions);

    } catch (error) {
      console.error('上传任务处理失败:', error);
      nextTask.status = 'failed';
      nextTask.error = (error as Error).message;
      this.activeUploads.delete(nextTask.id);
      this.saveQueueToStorage();
      this.options.onQueueUpdate(this.getTasks());
      this.processNext();
    }
  }

  /**
   * 获取下一个待处理任务
   */
  private getNextPendingTask(): QueuedTask | undefined {
    const pendingTasks = Array.from(this.queue.values())
      .filter(t => t.status === 'pending')
      .sort((a, b) => {
        // 优先级高的优先
        if (a.priority !== b.priority) {
          return b.priority - a.priority;
        }
        // 优先级相同，先进先出
        return a.addedAt.getTime() - b.addedAt.getTime();
      });

    return pendingTasks[0];
  }

  /**
   * 排序队列
   */
  private sortQueue(): void {
    const sorted = Array.from(this.queue.entries())
      .sort(([, a], [, b]) => {
        if (a.priority !== b.priority) {
          return b.priority - a.priority;
        }
        return a.addedAt.getTime() - b.addedAt.getTime();
      });

    this.queue = new Map(sorted);
  }

  /**
   * 生成任务ID
   */
  private generateTaskId(file: File): string {
    return `${Date.now()}_${file.name}_${file.size}`;
  }

  /**
   * 保存队列到localStorage
   */
  private saveQueueToStorage(): void {
    if (!this.options.persistQueue) return;

    const serializable = Array.from(this.queue.values()).map(task => ({
      ...task,
      file: undefined, // File对象不能序列化
      fileInfo: {
        name: task.file.name,
        size: task.file.size,
        type: task.file.type,
        lastModified: task.file.lastModified
      }
    }));

    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(serializable));
  }

  /**
   * 从localStorage加载队列
   */
  private loadQueueFromStorage(): void {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (!stored) return;

    try {
      const tasks = JSON.parse(stored);
      // 注意：File对象无法从localStorage恢复，需要重新选择文件
      console.log('已从localStorage恢复队列信息，但需要重新选择文件');
    } catch (error) {
      console.error('加载队列失败:', error);
    }
  }

  /**
   * 根据带宽计算最优并发数
   */
  private calculateOptimalConcurrency(): number {
    const currentBandwidth = this.bandwidthMonitor.getCurrentBandwidth();
    const targetChunkBandwidth = this.options.maxBandwidth / this.options.maxConcurrent;

    if (currentBandwidth > targetChunkBandwidth * 2) {
      return Math.min(5, this.options.maxConcurrent + 1); // 可以增加并发
    } else if (currentBandwidth < targetChunkBandwidth * 0.5) {
      return Math.max(1, this.options.maxConcurrent - 1); // 需要减少并发
    }

    return this.options.maxConcurrent;
  }
}

/**
 * 带宽监控器
 */
class BandwidthMonitor {
  private measurements: Array<{ time: number; bytes: number }> = [];
  private readonly WINDOW_SIZE = 10; // 保留最近10次测量

  recordProgress(progress: number, totalBytes: number): void {
    const bytes = (progress / 100) * totalBytes;
    this.measurements.push({
      time: Date.now(),
      bytes
    });

    // 保持窗口大小
    if (this.measurements.length > this.WINDOW_SIZE) {
      this.measurements.shift();
    }
  }

  getCurrentBandwidth(): number {
    if (this.measurements.length < 2) return 0;

    const recent = this.measurements.slice(-5); // 使用最近5次测量
    if (recent.length < 2) return 0;

    const first = recent[0];
    const last = recent[recent.length - 1];
    const timeDiff = (last.time - first.time) / 1000; // 转换为秒
    const bytesDiff = last.bytes - first.bytes;

    return timeDiff > 0 ? bytesDiff / timeDiff : 0; // bytes/second
  }

  getAverageBandwidth(): number {
    return this.getCurrentBandwidth();
  }
}

// 导出默认实例
export default new UploadQueueManager();
