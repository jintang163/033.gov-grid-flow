"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useSync = void 0;
const react_1 = require("react");
const taro_1 = require("@tarojs/taro");
const useSyncStore_1 = require("@/store/useSyncStore");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const syncQueue_1 = require("@/utils/syncQueue");
const useSync = () => {
    const { isOnline, isSyncing, syncProgress, setSyncing, setSyncProgress } = (0, useSyncStore_1.useSyncStore)();
    const { pendingEvents, refreshAll } = (0, useOfflineStore_1.useOfflineStore)();
    const handleProgress = (0, react_1.useCallback)((progress) => {
        console.log('[useSync] 同步进度:', progress);
        setSyncProgress(progress);
    }, [setSyncProgress]);
    const handleComplete = (0, react_1.useCallback)(async (result) => {
        console.log('[useSync] 同步完成:', result);
        setSyncing(false);
        setSyncProgress(null);
        await refreshAll();
        if (result.success) {
            taro_1.default.showToast({
                title: `同步成功 ${result.successCount} 条`,
                icon: 'success',
                duration: 2000
            });
        }
        else if (result.successCount > 0) {
            taro_1.default.showToast({
                title: `成功${result.successCount}条，失败${result.failedCount}条`,
                icon: 'none',
                duration: 3000
            });
        }
        else {
            taro_1.default.showToast({
                title: `同步失败 ${result.failedCount} 条`,
                icon: 'error',
                duration: 3000
            });
        }
    }, [setSyncing, setSyncProgress, refreshAll]);
    const handleError = (0, react_1.useCallback)((error) => {
        console.error('[useSync] 同步错误:', error);
        setSyncing(false);
        setSyncProgress(null);
        taro_1.default.showToast({
            title: error.message || '同步失败',
            icon: 'error',
            duration: 3000
        });
    }, [setSyncing, setSyncProgress]);
    const syncAll = (0, react_1.useCallback)(async (events) => {
        if (!isOnline) {
            taro_1.default.showToast({
                title: '当前无网络，请检查网络连接',
                icon: 'none'
            });
            return null;
        }
        if (isSyncing) {
            taro_1.default.showToast({
                title: '正在同步中，请稍候',
                icon: 'none'
            });
            return null;
        }
        const eventsToSync = events || pendingEvents;
        if (eventsToSync.length === 0) {
            taro_1.default.showToast({
                title: '没有需要同步的数据',
                icon: 'none'
            });
            return null;
        }
        console.log('[useSync] 开始同步，数量:', eventsToSync.length);
        setSyncing(true);
        syncQueue_1.syncQueue.setOptions({
            onProgress: handleProgress,
            onComplete: handleComplete,
            onError: handleError
        });
        syncQueue_1.syncQueue.clear();
        syncQueue_1.syncQueue.addEvents(eventsToSync);
        return syncQueue_1.syncQueue.process();
    }, [isOnline, isSyncing, pendingEvents, setSyncing, handleProgress, handleComplete, handleError]);
    const syncSingle = (0, react_1.useCallback)(async (event) => {
        return syncAll([event]);
    }, [syncAll]);
    const syncBatch = (0, react_1.useCallback)(async (events) => {
        if (!isOnline) {
            taro_1.default.showToast({
                title: '当前无网络，请检查网络连接',
                icon: 'none'
            });
            return null;
        }
        if (isSyncing) {
            taro_1.default.showToast({
                title: '正在同步中，请稍候',
                icon: 'none'
            });
            return null;
        }
        const eventsToSync = events || pendingEvents;
        if (eventsToSync.length === 0) {
            taro_1.default.showToast({
                title: '没有需要同步的数据',
                icon: 'none'
            });
            return null;
        }
        console.log('[useSync] 开始批量同步，数量:', eventsToSync.length);
        setSyncing(true);
        syncQueue_1.syncQueue.setOptions({
            onProgress: handleProgress,
            onComplete: handleComplete,
            onError: handleError
        });
        syncQueue_1.syncQueue.clear();
        syncQueue_1.syncQueue.addEvents(eventsToSync);
        return syncQueue_1.syncQueue.processBatch();
    }, [isOnline, isSyncing, pendingEvents, setSyncing, handleProgress, handleComplete, handleError]);
    const cancelSync = (0, react_1.useCallback)(() => {
        syncQueue_1.syncQueue.cancel();
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
exports.useSync = useSync;
//# sourceMappingURL=useSync.js.map