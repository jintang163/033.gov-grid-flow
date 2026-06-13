import { useCallback } from 'react';
import Taro from '@tarojs/taro';
import { useSyncStore } from '@/store/useSyncStore';
import { useOfflineStore } from '@/store/useOfflineStore';
import { syncQueue } from '@/utils/syncQueue';
import type { OfflineEvent } from '@/types/event';
import type { SyncProgress, SyncResult } from '@/types/sync';

export const useSync = () => {
  const { isOnline, isSyncing, syncProgress, setSyncing, setSyncProgress } = useSyncStore();
  const { pendingEvents, refreshAll } = useOfflineStore();

  const handleProgress = useCallback((progress: SyncProgress) => {
    console.log('[useSync] 同步进度:', progress);
    setSyncProgress(progress);
  }, [setSyncProgress]);

  const handleComplete = useCallback(async (result: SyncResult) => {
    console.log('[useSync] 同步完成:', result);
    setSyncing(false);
    setSyncProgress(null);
    await refreshAll();

    if (result.success) {
      Taro.showToast({
        title: `同步成功 ${result.successCount} 条`,
        icon: 'success',
        duration: 2000
      });
    } else if (result.successCount > 0) {
      Taro.showToast({
        title: `成功${result.successCount}条，失败${result.failedCount}条`,
        icon: 'none',
        duration: 3000
      });
    } else {
      Taro.showToast({
        title: `同步失败 ${result.failedCount} 条`,
        icon: 'error',
        duration: 3000
      });
    }
  }, [setSyncing, setSyncProgress, refreshAll]);

  const handleError = useCallback((error: Error) => {
    console.error('[useSync] 同步错误:', error);
    setSyncing(false);
    setSyncProgress(null);
    Taro.showToast({
      title: error.message || '同步失败',
      icon: 'error',
      duration: 3000
    });
  }, [setSyncing, setSyncProgress]);

  const syncAll = useCallback(async (events?: OfflineEvent[]): Promise<SyncResult | null> => {
    if (!isOnline) {
      Taro.showToast({
        title: '当前无网络，请检查网络连接',
        icon: 'none'
      });
      return null;
    }

    if (isSyncing) {
      Taro.showToast({
        title: '正在同步中，请稍候',
        icon: 'none'
      });
      return null;
    }

    const eventsToSync = events || pendingEvents;
    if (eventsToSync.length === 0) {
      Taro.showToast({
        title: '没有需要同步的数据',
        icon: 'none'
      });
      return null;
    }

    console.log('[useSync] 开始同步，数量:', eventsToSync.length);
    setSyncing(true);

    syncQueue.setOptions({
      onProgress: handleProgress,
      onComplete: handleComplete,
      onError: handleError
    });

    syncQueue.clear();
    syncQueue.addEvents(eventsToSync);

    return syncQueue.process();
  }, [isOnline, isSyncing, pendingEvents, setSyncing, handleProgress, handleComplete, handleError]);

  const syncSingle = useCallback(async (event: OfflineEvent): Promise<SyncResult | null> => {
    return syncAll([event]);
  }, [syncAll]);

  const syncBatch = useCallback(async (events?: OfflineEvent[]): Promise<SyncResult | null> => {
    if (!isOnline) {
      Taro.showToast({
        title: '当前无网络，请检查网络连接',
        icon: 'none'
      });
      return null;
    }

    if (isSyncing) {
      Taro.showToast({
        title: '正在同步中，请稍候',
        icon: 'none'
      });
      return null;
    }

    const eventsToSync = events || pendingEvents;
    if (eventsToSync.length === 0) {
      Taro.showToast({
        title: '没有需要同步的数据',
        icon: 'none'
      });
      return null;
    }

    console.log('[useSync] 开始批量同步，数量:', eventsToSync.length);
    setSyncing(true);

    syncQueue.setOptions({
      onProgress: handleProgress,
      onComplete: handleComplete,
      onError: handleError
    });

    syncQueue.clear();
    syncQueue.addEvents(eventsToSync);

    return syncQueue.processBatch();
  }, [isOnline, isSyncing, pendingEvents, setSyncing, handleProgress, handleComplete, handleError]);

  const cancelSync = useCallback(() => {
    syncQueue.cancel();
    setSyncing(false);
    setSyncProgress(null);
    console.log('[useSync] 同步已取消');
  }, [setSyncing, setSyncProgress]);

  return {
    isOnline,
    isSyncing,
    syncProgress,
    pendingCount: pendingEvents.length,
    syncAll,
    syncSingle,
    syncBatch,
    cancelSync
  };
};
