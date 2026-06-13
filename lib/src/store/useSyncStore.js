"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useSyncStore = void 0;
const zustand_1 = require("zustand");
exports.useSyncStore = (0, zustand_1.create)((set, get) => ({
    isOnline: true,
    networkType: 'unknown',
    isSyncing: false,
    syncProgress: null,
    pendingCount: 0,
    failedCount: 0,
    showRedDot: false,
    setNetworkStatus: (isOnline, networkType) => {
        console.log('[SyncStore] 网络状态更新:', { isOnline, networkType });
        set({ isOnline, networkType });
        get().updateRedDot();
    },
    setSyncing: (isSyncing) => {
        console.log('[SyncStore] 同步状态更新:', isSyncing);
        set({ isSyncing });
    },
    setSyncProgress: (progress) => {
        set({ syncProgress: progress });
    },
    setPendingCount: (count) => {
        console.log('[SyncStore] 待同步数量更新:', count);
        set({ pendingCount: count });
        get().updateRedDot();
    },
    setFailedCount: (count) => {
        console.log('[SyncStore] 同步失败数量更新:', count);
        set({ failedCount: count });
        get().updateRedDot();
    },
    updateRedDot: () => {
        const { pendingCount, failedCount, isOnline } = get();
        const showRedDot = (pendingCount > 0 || failedCount > 0) && isOnline;
        console.log('[SyncStore] 红点状态更新:', { showRedDot, pendingCount, failedCount, isOnline });
        set({ showRedDot });
    }
}));
//# sourceMappingURL=useSyncStore.js.map