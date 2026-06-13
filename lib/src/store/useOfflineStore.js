"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useOfflineStore = void 0;
const zustand_1 = require("zustand");
const offline_1 = require("@/types/offline");
const offlineDB_1 = require("@/utils/offlineDB");
exports.useOfflineStore = (0, zustand_1.create)((set, get) => ({
    events: [],
    pendingEvents: [],
    syncLogs: [],
    mapCacheRegions: [],
    settings: offline_1.DEFAULT_SETTINGS,
    storageStats: {
        totalSize: 0,
        eventCount: 0,
        imageCount: 0,
        mapCacheSize: 0,
        otherSize: 0
    },
    isLoading: false,
    initStore: async () => {
        console.log('[OfflineStore] 初始化Store');
        set({ isLoading: true });
        try {
            await Promise.all([
                get().loadEvents(),
                get().loadPendingEvents(),
                get().loadSyncLogs(),
                get().loadMapCacheRegions(),
                get().loadSettings(),
                get().loadStorageStats()
            ]);
        }
        catch (error) {
            console.error('[OfflineStore] 初始化失败:', error);
        }
        finally {
            set({ isLoading: false });
        }
    },
    loadEvents: async () => {
        const events = await (0, offlineDB_1.getEvents)();
        set({ events });
        console.log('[OfflineStore] 加载事件列表，数量:', events.length);
    },
    loadPendingEvents: async () => {
        const pendingEvents = await (0, offlineDB_1.getPendingEvents)();
        set({ pendingEvents });
        console.log('[OfflineStore] 加载待同步事件，数量:', pendingEvents.length);
    },
    loadSyncLogs: async () => {
        const syncLogs = await (0, offlineDB_1.getSyncLogs)();
        set({ syncLogs });
        console.log('[OfflineStore] 加载同步日志，数量:', syncLogs.length);
    },
    loadMapCacheRegions: async () => {
        const mapCacheRegions = await (0, offlineDB_1.getMapCacheRegions)();
        set({ mapCacheRegions });
        console.log('[OfflineStore] 加载地图缓存区域，数量:', mapCacheRegions.length);
    },
    loadSettings: async () => {
        const settings = await (0, offlineDB_1.getSettings)();
        set({ settings });
        console.log('[OfflineStore] 加载设置:', settings);
    },
    loadStorageStats: async () => {
        const storageStats = await (0, offlineDB_1.getStorageStats)();
        set({ storageStats });
        console.log('[OfflineStore] 加载存储统计:', storageStats);
    },
    addEvent: async (event) => {
        await (0, offlineDB_1.saveEvent)(event);
        await get().refreshAll();
        console.log('[OfflineStore] 添加事件:', event.clientId);
    },
    updateEvent: async (event) => {
        await (0, offlineDB_1.saveEvent)(event);
        await get().refreshAll();
        console.log('[OfflineStore] 更新事件:', event.clientId);
    },
    removeEvent: async (clientId) => {
        await (0, offlineDB_1.deleteEvent)(clientId);
        await get().refreshAll();
        console.log('[OfflineStore] 删除事件:', clientId);
    },
    updateSettings: async (settings) => {
        await (0, offlineDB_1.saveSettings)(settings);
        await get().loadSettings();
        console.log('[OfflineStore] 更新设置:', settings);
    },
    refreshAll: async () => {
        await Promise.all([
            get().loadEvents(),
            get().loadPendingEvents(),
            get().loadSyncLogs(),
            get().loadStorageStats()
        ]);
    }
}));
//# sourceMappingURL=useOfflineStore.js.map