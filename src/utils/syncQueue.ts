import type { OfflineEvent } from '@/types/event';
import type { SyncProgress, SyncResult, SyncLogItem } from '@/types/sync';
import { updateEventSyncStatus, saveSyncLog, getSettings } from './offlineDB';
import { reportEvent, uploadImage, batchSyncEvents } from '@/services/event';
import { generateUUID } from './uuid';

interface SyncQueueOptions {
  onProgress?: (progress: SyncProgress) => void;
  onComplete?: (result: SyncResult) => void;
  onError?: (error: Error) => void;
}

class SyncQueue {
  private isProcessing = false;
  private queue: OfflineEvent[] = [];
  private options: SyncQueueOptions = {};

  setOptions(options: SyncQueueOptions) {
    this.options = options;
  }

  addEvents(events: OfflineEvent[]) {
    const newEvents = events.filter(
      e => !this.queue.find(q => q.clientId === e.clientId)
    );
    this.queue.push(...newEvents);
    console.log('[SyncQueue] 添加事件到队列，当前队列长度:', this.queue.length);
  }

  clear() {
    this.queue = [];
    this.isProcessing = false;
  }

  async process(): Promise<SyncResult> {
    if (this.isProcessing) {
      console.warn('[SyncQueue] 已有同步任务在进行中');
      throw new Error('同步任务进行中');
    }

    this.isProcessing = true;
    const startTime = Date.now();
    const settings = await getSettings();
    const total = this.queue.length;
    let completed = 0;
    let failed = 0;
    const failedItems: Array<{ clientId: string; error: string }> = [];

    console.log('[SyncQueue] 开始处理同步队列，共', total, '条事件');

    try {
      const sortedEvents = [...this.queue].sort((a, b) => a.createdAt - b.createdAt);

      for (const event of sortedEvents) {
        if (!this.isProcessing) {
          console.log('[SyncQueue] 同步任务已取消');
          break;
        }

        this.options.onProgress?.({
          total,
          completed,
          failed,
          currentEvent: event
        });

        try {
          await updateEventSyncStatus(event.clientId, 'syncing');
          console.log('[SyncQueue] 正在同步事件:', event.clientId);

          const uploadedImages = await Promise.all(
            event.images.map(async (img) => {
              if (img.uploadStatus === 'synced' && img.remoteUrl) {
                return img.remoteUrl;
              }
              try {
                const remoteUrl = await uploadImage(img.localPath);
                return remoteUrl;
              } catch (imgError) {
                console.error('[SyncQueue] 图片上传失败:', img.id, imgError);
                throw new Error(`图片上传失败: ${imgError instanceof Error ? imgError.message : '未知错误'}`);
              }
            })
          );

          const result = await reportEvent({
            type: event.type,
            title: event.title,
            description: event.description,
            priority: event.priority,
            imageUrls: uploadedImages,
            longitude: event.location.longitude,
            latitude: event.location.latitude,
            address: event.location.address,
            anonymous: event.anonymous,
            clientId: event.clientId,
            timestamp: event.createdAt
          });

          await updateEventSyncStatus(event.clientId, 'synced', undefined, result.id);
          completed++;
          console.log('[SyncQueue] 事件同步成功:', event.clientId, '服务器ID:', result.id);

        } catch (error) {
          failed++;
          const errorMsg = error instanceof Error ? error.message : '同步失败';
          failedItems.push({ clientId: event.clientId, error: errorMsg });

          const shouldRetry = settings.autoRetry && event.syncRetryCount < settings.maxRetryCount;
          await updateEventSyncStatus(
            event.clientId,
            shouldRetry ? 'pending' : 'failed',
            errorMsg
          );

          console.error('[SyncQueue] 事件同步失败:', event.clientId, errorMsg,
            shouldRetry ? '将自动重试' : '已达最大重试次数');
        }
      }

      const endTime = Date.now();
      const result: SyncResult = {
        success: failed === 0,
        total,
        successCount: completed,
        failedCount: failed,
        failedItems
      };

      const log: SyncLogItem = {
        id: generateUUID(),
        startTime,
        endTime,
        totalCount: total,
        successCount: completed,
        failedCount: failed,
        status: failed === 0 ? 'success' : completed > 0 ? 'partial' : 'failed',
        errorMessage: failed > 0 ? `${failed} 条同步失败` : undefined
      };
      await saveSyncLog(log);

      console.log('[SyncQueue] 同步完成:', result);

      this.options.onComplete?.(result);
      return result;

    } catch (error) {
      console.error('[SyncQueue] 同步过程发生严重错误:', error);
      this.options.onError?.(error instanceof Error ? error : new Error('同步失败'));
      throw error;
    } finally {
      this.isProcessing = false;
      this.queue = [];
    }
  }

  async processBatch(): Promise<SyncResult> {
    if (this.isProcessing) {
      console.warn('[SyncQueue] 已有同步任务在进行中');
      throw new Error('同步任务进行中');
    }

    this.isProcessing = true;
    const startTime = Date.now();
    const total = this.queue.length;

    console.log('[SyncQueue] 开始批量同步，共', total, '条事件');

    try {
      const sortedEvents = [...this.queue].sort((a, b) => a.createdAt - b.createdAt);

      this.options.onProgress?.({
        total,
        completed: 0,
        failed: 0,
        currentEvent: sortedEvents[0]
      });

      for (const event of sortedEvents) {
        await updateEventSyncStatus(event.clientId, 'syncing');
      }

      const batchResult = await batchSyncEvents(sortedEvents);

      let completed = 0;
      let failed = 0;
      const failedItems: Array<{ clientId: string; error: string }> = [];

      for (const result of batchResult.results) {
        if (result.success) {
          await updateEventSyncStatus(result.clientId, 'synced', undefined, result.serverId);
          completed++;
        } else {
          failed++;
          failedItems.push({ clientId: result.clientId, error: result.error || '同步失败' });
          await updateEventSyncStatus(result.clientId, 'failed', result.error);
        }
      }

      const endTime = Date.now();
      const syncResult: SyncResult = {
        success: failed === 0,
        total,
        successCount: completed,
        failedCount: failed,
        failedItems
      };

      const log: SyncLogItem = {
        id: generateUUID(),
        startTime,
        endTime,
        totalCount: total,
        successCount: completed,
        failedCount: failed,
        status: failed === 0 ? 'success' : completed > 0 ? 'partial' : 'failed',
        errorMessage: failed > 0 ? `${failed} 条同步失败` : undefined
      };
      await saveSyncLog(log);

      console.log('[SyncQueue] 批量同步完成:', syncResult);

      this.options.onProgress?.({
        total,
        completed,
        failed
      });

      this.options.onComplete?.(syncResult);
      return syncResult;

    } catch (error) {
      console.error('[SyncQueue] 批量同步失败:', error);
      for (const event of this.queue) {
        await updateEventSyncStatus(event.clientId, 'failed',
          error instanceof Error ? error.message : '批量同步失败');
      }

      const result: SyncResult = {
        success: false,
        total,
        successCount: 0,
        failedCount: total,
        failedItems: this.queue.map(e => ({
          clientId: e.clientId,
          error: error instanceof Error ? error.message : '同步失败'
        }))
      };

      this.options.onError?.(error instanceof Error ? error : new Error('批量同步失败'));
      return result;
    } finally {
      this.isProcessing = false;
      this.queue = [];
    }
  }

  cancel() {
    this.isProcessing = false;
    console.log('[SyncQueue] 同步任务已取消');
  }

  get isProcessingSync(): boolean {
    return this.isProcessing;
  }

  get queueSize(): number {
    return this.queue.length;
  }
}

export const syncQueue = new SyncQueue();
