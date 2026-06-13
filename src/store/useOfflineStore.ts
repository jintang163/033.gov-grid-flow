import { create } from 'zustand';
import type { OfflineEvent } from '@/types/event';
import type { SyncLogItem, MapCacheRegion, AppSettings, StorageStats } from '@/types/offline';
import { DEFAULT_SETTINGS } from '@/types/offline';
import {
  getEvents,
  saveEvent,
  deleteEvent,
  getPendingEvents,
  getSyncLogs,
  getMapCacheRegions,
  getSettings,
  saveSettings,
  getStorageStats
} from '@/utils/offlineDB';

interface OfflineState {
  events: OfflineEvent[];
  pendingEvents: OfflineEvent[];
  syncLogs: SyncLogItem[];
  mapCacheRegions: MapCacheRegion[];
  settings: AppSettings;
  storageStats: StorageStats;
  isLoading: boolean;

  initStore: () => Promise<void>;
  loadEvents: () => Promise<void>;
  loadPendingEvents: () => Promise<void>;
  loadSyncLogs: () => Promise<void>;
  loadMapCacheRegions: () => Promise<void>;
  loadSettings: () => Promise<void>;
  loadStorageStats: () => Promise<void>;
  addEvent: (event: OfflineEvent) => Promise<void>;
  updateEvent: (event: OfflineEvent) => Promise<void>;
  removeEvent: (clientId: string) => Promise<void>;
  updateSettings: (settings: Partial<AppSettings>) => Promise<void>;
  refreshAll: () => Promise<void>;
}

export const useOfflineStore = create<OfflineState>((set, get) => ({
  events: [],
  pendingEvents: [],
  syncLogs: [],
  mapCacheRegions: [],
  settings: DEFAULT_SETTINGS,
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
    } catch (error) {
      console.error('[OfflineStore] 初始化失败:', error);
    } finally {
      set({ isLoading: false });
    }
  },

  loadEvents: async () => {
    const events = await getEvents();
    set({ events });
    console.log('[OfflineStore] 加载事件列表，数量:', events.length);
  },

  loadPendingEvents: async () => {
    const pendingEvents = await getPendingEvents();
    set({ pendingEvents });
    console.log('[OfflineStore] 加载待同步事件，数量:', pendingEvents.length);
  },

  loadSyncLogs: async () => {
    const syncLogs = await getSyncLogs();
    set({ syncLogs });
    console.log('[OfflineStore] 加载同步日志，数量:', syncLogs.length);
  },

  loadMapCacheRegions: async () => {
    const mapCacheRegions = await getMapCacheRegions();
    set({ mapCacheRegions });
    console.log('[OfflineStore] 加载地图缓存区域，数量:', mapCacheRegions.length);
  },

  loadSettings: async () => {
    const settings = await getSettings();
    set({ settings });
    console.log('[OfflineStore] 加载设置:', settings);
  },

  loadStorageStats: async () => {
    const storageStats = await getStorageStats();
    set({ storageStats });
    console.log('[OfflineStore] 加载存储统计:', storageStats);
  },

  addEvent: async (event) => {
    await saveEvent(event);
    await get().refreshAll();
    console.log('[OfflineStore] 添加事件:', event.clientId);
  },

  updateEvent: async (event) => {
    await saveEvent(event);
    await get().refreshAll();
    console.log('[OfflineStore] 更新事件:', event.clientId);
  },

  removeEvent: async (clientId) => {
    await deleteEvent(clientId);
    await get().refreshAll();
    console.log('[OfflineStore] 删除事件:', clientId);
  },

  updateSettings: async (settings) => {
    await saveSettings(settings);
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
