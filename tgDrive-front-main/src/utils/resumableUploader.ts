/**
 * 断点续传上传器
 * 支持文件分块、断点续传、秒传等功能
 */

import request from '@/utils/request';
import SparkMD5 from 'spark-md5';

export interface UploadPrepareResponse {
  taskId: string;
  resumable: boolean;
  completed: boolean;
  totalChunks?: number;
  uploadedChunks?: number[];
  chunkSize?: number;
  finalFileId?: string;
  downloadUrl?: string;
  uploadedSize?: number;
  uploadProgress?: number;
}

export interface ChunkUploadResponse {
  taskId: string;
  chunkIndex: number;
  chunkFileId: string;
  success: boolean;
  message: string;
  uploadedChunksCount: number;
  progressPercentage: number;
}

export interface UploadOptions {
  onProgress?: (progress: number, chunkIndex: number, totalChunks: number, taskId: string, stageOneRatio: number) => void;
  onChunkComplete?: (chunkIndex: number, totalChunks: number, taskId: string, completedCount: number) => void;
  onComplete?: (result: any) => void;
  onError?: (error: Error) => void;
  concurrency?: number; // 并发数，默认3
  maxRetries?: number; // 最大重试次数，默认3
  retryDelay?: number; // 重试延迟(毫秒)，默认1000
}

export class ResumableUploader {
  private readonly CHUNK_SIZE = 10 * 1024 * 1024; // 10MB
  private readonly DEFAULT_CONCURRENCY = 3;
  private readonly DEFAULT_MAX_RETRIES = 3;
  private readonly DEFAULT_RETRY_DELAY = 1000; // 1秒
  private abortControllers: Map<number, AbortController> = new Map();
  private isPaused = false;
  private isCancelled = false;
  private chunkRetryCount: Map<number, number> = new Map(); // 记录每个分块的重试次数

  /**
   * 计算文件的MD5哈希值
   */
  private async calculateFileHash(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const spark = new SparkMD5.ArrayBuffer();
      const fileReader = new FileReader();
      const chunkSize = 2 * 1024 * 1024; // 2MB chunks for hashing
      let currentChunk = 0;
      const chunks = Math.ceil(file.size / chunkSize);

      fileReader.onload = (e) => {
        if (e.target?.result) {
          spark.append(e.target.result as ArrayBuffer);
        }
        currentChunk++;

        if (currentChunk < chunks) {
          loadNext();
        } else {
          resolve(spark.end());
        }
      };

      fileReader.onerror = () => {
        reject(new Error('文件读取失败'));
      };

      function loadNext() {
        const start = currentChunk * chunkSize;
        const end = Math.min(start + chunkSize, file.size);
        fileReader.readAsArrayBuffer(file.slice(start, end));
      }

      loadNext();
    });
  }

  /**
   * 准备上传，检查文件状态
   */
  private async prepareUpload(file: File, fileHash: string): Promise<UploadPrepareResponse> {
    const response = await request.post('/resumable/prepare', null, {
      params: {
        fileName: file.name,
        fileSize: file.size,
        fileHash: fileHash
      }
    });

    if (response.data.code !== 1) {
      throw new Error(response.data.msg || '准备上传失败');
    }

    return response.data.data;
  }

  /**
   * 上传单个分块
   */
  private async uploadChunk(
    taskId: string,
    chunkIndex: number,
    chunk: Blob,
    onProgress?: (loaded: number, total: number) => void
  ): Promise<ChunkUploadResponse> {
    const formData = new FormData();
    formData.append('taskId', taskId);
    formData.append('chunkIndex', String(chunkIndex));
    formData.append('chunk', chunk);

    const abortController = new AbortController();
    this.abortControllers.set(chunkIndex, abortController);

    try {
      const response = await request.post('/resumable/chunk', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        signal: abortController.signal,
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            onProgress(progressEvent.loaded, progressEvent.total);
          }
        }
      });

      if (response.data.code !== 1) {
        throw new Error(response.data.msg || `分块 ${chunkIndex} 上传失败`);
      }

      return response.data.data;
    } finally {
      this.abortControllers.delete(chunkIndex);
    }
  }

  /**
   * 上传单个分块（带重试机制）
   */
  private async uploadChunkWithRetry(
    taskId: string,
    chunkIndex: number,
    chunk: Blob,
    maxRetries: number,
    retryDelay: number,
    onProgress?: (loaded: number, total: number) => void
  ): Promise<ChunkUploadResponse> {
    let lastError: Error | null = null;
    const retryCount = this.chunkRetryCount.get(chunkIndex) || 0;

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      if (this.isCancelled) {
        throw new Error('上传已取消');
      }

      // 如果是重试，等待一段时间
      if (attempt > 0) {
        console.log(`分块 ${chunkIndex} 第 ${attempt} 次重试，等待 ${retryDelay}ms`);
        await new Promise(resolve => setTimeout(resolve, retryDelay * Math.pow(2, attempt - 1))); // 指数退避
      }

      try {
        const response = await this.uploadChunk(taskId, chunkIndex, chunk, onProgress);
        // 成功后重置重试计数
        this.chunkRetryCount.delete(chunkIndex);
        return response;
      } catch (error) {
        lastError = error as Error;
        this.chunkRetryCount.set(chunkIndex, attempt + 1);

        // 如果是网络错误，继续重试
        if (error instanceof Error && error.message.includes('network')) {
          console.warn(`分块 ${chunkIndex} 上传失败（网络错误），准备重试`);
          continue;
        }

        // 如果是其他错误，可能需要停止重试
        if (attempt === maxRetries) {
          console.error(`分块 ${chunkIndex} 上传失败，已达到最大重试次数`);
        }
      }
    }

    throw lastError || new Error(`分块 ${chunkIndex} 上传失败`);
  }

  /**
   * 完成上传
   */
  private async completeUpload(taskId: string): Promise<any> {
    const response = await request.post('/resumable/complete', null, {
      params: { taskId }
    });

    if (response.data.code !== 1) {
      throw new Error(response.data.msg || '完成上传失败');
    }

    return response.data.data;
  }

  /**
   * 上传文件（主方法）
   */
  async upload(file: File, options: UploadOptions = {}): Promise<any> {
    const {
      onProgress,
      onChunkComplete,
      onComplete,
      onError,
      concurrency = this.DEFAULT_CONCURRENCY,
      maxRetries = this.DEFAULT_MAX_RETRIES,
      retryDelay = this.DEFAULT_RETRY_DELAY
    } = options;

    try {
      this.isCancelled = false;
      this.isPaused = false;

      // 1. 计算文件哈希
      const fileHash = await this.calculateFileHash(file);

      // 2. 准备上传
      const prepareResponse = await this.prepareUpload(file, fileHash);

      // 3. 如果文件已存在（秒传）
      if (prepareResponse.completed) {
        if (onComplete) {
          onComplete({
            fileName: file.name,
            downloadLink: prepareResponse.downloadUrl,
            fileId: prepareResponse.finalFileId
          });
        }
        return prepareResponse;
      }

      // 4. 计算需要上传的分块
      const totalChunks = prepareResponse.totalChunks || Math.ceil(file.size / this.CHUNK_SIZE);
      const uploadedChunks = new Set(prepareResponse.uploadedChunks || []);
      const chunksToUpload: number[] = [];

      for (let i = 0; i < totalChunks; i++) {
        if (!uploadedChunks.has(i)) {
          chunksToUpload.push(i);
        }
      }

      // 5. 如果所有分块都已上传，直接完成
      if (chunksToUpload.length === 0) {
        const result = await this.completeUpload(prepareResponse.taskId);
        if (onComplete) {
          onComplete(result);
        }
        return result;
      }

      // 6. 并发上传分块
      const uploadQueue = [...chunksToUpload];
      const chunkProgress = new Map<number, number>();
      let completedCount = uploadedChunks.size;
      let fatalError: Error | null = null;
      let abortUploads = false;

      const uploadNextChunk = async (): Promise<void> => {
        if (this.isCancelled || abortUploads || uploadQueue.length === 0) {
          return;
        }

        if (this.isPaused) {
          await new Promise(resolve => {
            const checkInterval = setInterval(() => {
              if (!this.isPaused || this.isCancelled) {
                clearInterval(checkInterval);
                resolve(undefined);
              }
            }, 100);
          });
        }

        const chunkIndex = uploadQueue.shift();
        if (chunkIndex === undefined) {
          return;
        }

        const start = chunkIndex * this.CHUNK_SIZE;
        const end = Math.min(start + this.CHUNK_SIZE, file.size);
        const chunk = file.slice(start, end);

        try {
          await this.uploadChunkWithRetry(
            prepareResponse.taskId,
            chunkIndex,
            chunk,
            maxRetries,
            retryDelay,
            (loaded, total) => {
              if (onProgress) {
                const partialProgress = total > 0 ? (loaded / total) * 0.5 : 0;
                chunkProgress.set(chunkIndex, Math.min(partialProgress, 0.5));
                const inFlight = Array.from(chunkProgress.values()).reduce((sum, value) => sum + value, 0);
                const overallProgress = ((completedCount + inFlight) / totalChunks) * 100;
                const stageOneRatio = Math.min((partialProgress / 0.5), 1);
                onProgress(Math.min(overallProgress, 100), chunkIndex, totalChunks, prepareResponse.taskId, stageOneRatio);
              }
            }
          );

          chunkProgress.delete(chunkIndex);
          completedCount++;
          if (onChunkComplete) {
            onChunkComplete(chunkIndex, totalChunks, prepareResponse.taskId, completedCount);
          }
          if (onProgress) {
            onProgress((completedCount / totalChunks) * 100, chunkIndex, totalChunks, prepareResponse.taskId, 1);
          }
        } catch (error) {
          chunkProgress.delete(chunkIndex);

          // 如果是取消操作，直接返回
          if (this.isCancelled) {
            return;
          }

          // 清理重试计数
          this.chunkRetryCount.delete(chunkIndex);

          // 终止后续上传并记录首个错误
          abortUploads = true;
          if (!fatalError) {
            fatalError = error instanceof Error ? error : new Error(String(error));
          }

          // 清空剩余待上传的分块，避免继续发送请求
          uploadQueue.length = 0;

          // 取消仍在进行的请求
          this.abortControllers.forEach(controller => controller.abort());
          this.abortControllers.clear();

          return;
        }

        // 继续上传下一个分块
        if (!abortUploads && uploadQueue.length > 0) {
          await uploadNextChunk();
        }
      };

      // 启动并发上传
      const workers: Promise<void>[] = [];
      for (let i = 0; i < Math.min(concurrency, chunksToUpload.length); i++) {
        workers.push(uploadNextChunk());
      }

      await Promise.all(workers);

      if (fatalError) {
        throw fatalError;
      }

      // 7. 完成上传
      if (!this.isCancelled) {
        const result = await this.completeUpload(prepareResponse.taskId);
        if (onComplete) {
          onComplete(result);
        }
        return result;
      }

      return null;

    } catch (error) {
      if (onError) {
        onError(error as Error);
      }
      throw error;
    }
  }

  /**
   * 暂停上传
   */
  pause() {
    this.isPaused = true;
  }

  /**
   * 恢复上传
   */
  resume() {
    this.isPaused = false;
  }

  /**
   * 取消上传
   */
  cancel() {
    this.isCancelled = true;
    // 取消所有正在进行的请求
    this.abortControllers.forEach(controller => {
      controller.abort();
    });
    this.abortControllers.clear();
  }

  /**
   * 取消上传任务（从服务器删除）
   */
  async cancelTask(taskId: string): Promise<void> {
    await request.delete(`/resumable/cancel/${taskId}`);
  }
}

// 导出默认实例
export default new ResumableUploader();
