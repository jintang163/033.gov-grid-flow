import Taro from '@tarojs/taro';
import type { OfflineEvent, SyncLogItem, MapCacheRegion, AppSettings, StorageStats } from '@/types/offline';
import { STORAGE_KEYS, DEFAULT_SETTINGS } from '@/types/offline';
import type { SyncStatus } from '@/types/event';

let dbInitialized = false;

export const initOfflineDB = async (): Promise<void> => {
  if (dbInitialized) return;
  console.log('[OfflineDB] 初始化离线数据库');
  try {
    const keys = [
      STORAGE_KEYS.EVENTS,
      STORAGE_KEYS.SYNC_LOGS,
      STORAGE_KEYS.MAP_CACHE_REGIONS,
      STORAGE_KEYS.SETTINGS
    ];
    for (const key of keys) {
      const data = await Taro.getStorage({ key }).catch(() => null);
      if (!data) {
        const defaultValue = key === STORAGE_KEYS.EVENTS ? [] :
                            key === STORAGE_KEYS.SYNC_LOGS ? [] :
                            key === STORAGE_KEYS.MAP_CACHE_REGIONS ? [] :
                            DEFAULT_SETTINGS;
        await Taro.setStorage({ key, data: defaultValue });
      }
    }
    dbInitialized = true;
    console.log('[OfflineDB] 初始化完成');
  } catch (error) {
    console.error('[OfflineDB] 初始化失败:', error);
    throw error;
  }
};

export const getEvents = async (): Promise<OfflineEvent[]> => {
  try {
    const data = await Taro.getStorage({ key: STORAGE_KEYS.EVENTS });
    return (data as OfflineEvent[]) || [];
  } catch (error) {
    console.error('[OfflineDB] 获取事件失败:', error);
    return [];
  }
};

export const getEventByClientId = async (clientId: string): Promise<OfflineEvent | null> => {
  const events = await getEvents();
  return events.find(e => e.clientId === clientId) || null;
};

export const saveEvent = async (event: OfflineEvent): Promise<void> => {
  const events = await getEvents();
  const existingIndex = events.findIndex(e => e.clientId === event.clientId);
  if (existingIndex >= 0) {
    events[existingIndex] = { ...event, updatedAt: Date.now() };
  } else {
    events.unshift(event);
  }
  await Taro.setStorage({ key: STORAGE_KEYS.EVENTS, data: events });
  console.log('[OfflineDB] 事件已保存:', event.clientId);
};

export const deleteEvent = async (clientId: string): Promise<void> => {
  const events = await getEvents();
  const filteredEvents = events.filter(e => e.clientId !== clientId);
  await Taro.setStorage({ key: STORAGE_KEYS.EVENTS, data: filteredEvents });
  console.log('[OfflineDB] 事件已删除:', clientId);
};

export const updateEventSyncStatus = async (
  clientId: string,
  syncStatus: SyncStatus,
  syncError?: string,
  serverId?: number
): Promise<void> => {
  const events = await getEvents();
  const eventIndex = events.findIndex(e => e.clientId === clientId);
  if (eventIndex >= 0) {
    const event = events[eventIndex];
    events[eventIndex] = {
      ...event,
      syncStatus,
      syncError,
      serverId,
      syncedAt: syncStatus === 'synced' ? Date.now() : event.syncedAt,
      syncRetryCount: syncStatus === 'failed' ? event.syncRetryCount + 1 : event.syncRetryCount,
      updatedAt: Date.now()
    };
    await Taro.setStorage({ key: STORAGE_KEYS.EVENTS, data: events });
    console.log('[OfflineDB] 事件同步状态已更新:', clientId, syncStatus);
  }
};

export const getPendingEvents = async (): Promise<OfflineEvent[]> => {
  const events = await getEvents();
  return events
    .filter(e => e.syncStatus === 'pending' || e.syncStatus === 'failed')
    .sort((a, b) => a.createdAt - b.createdAt);
};

export const getSyncLogs = async (): Promise<SyncLogItem[]> => {
  try {
    const data = await Taro.getStorage({ key: STORAGE_KEYS.SYNC_LOGS });
    return (data as SyncLogItem[]) || [];
  } catch (error) {
    console.error('[OfflineDB] 获取同步日志失败:', error);
    return [];
  }
};

export const saveSyncLog = async (log: SyncLogItem): Promise<void> => {
  const logs = await getSyncLogs();
  logs.unshift(log);
  if (logs.length > 100) {
    logs.length = 100;
  }
  await Taro.setStorage({ key: STORAGE_KEYS.SYNC_LOGS, data: logs });
};

export const getMapCacheRegions = async (): Promise<MapCacheRegion[]> => {
  try {
    const data = await Taro.getStorage({ key: STORAGE_KEYS.MAP_CACHE_REGIONS });
    return (data as MapCacheRegion[]) || [];
  } catch (error) {
    console.error('[OfflineDB] 获取地图缓存区域失败:', error);
    return [];
  }
};

export const saveMapCacheRegion = async (region: MapCacheRegion): Promise<void> => {
  const regions = await getMapCacheRegions();
  const existingIndex = regions.findIndex(r => r.id === region.id);
  if (existingIndex >= 0) {
    regions[existingIndex] = region;
  } else {
    regions.unshift(region);
  }
  await Taro.setStorage({ key: STORAGE_KEYS.MAP_CACHE_REGIONS, data: regions });
};

export const deleteMapCacheRegion = async (regionId: string): Promise<void> => {
  const regions = await getMapCacheRegions();
  const filteredRegions = regions.filter(r => r.id !== regionId);
  await Taro.setStorage({ key: STORAGE_KEYS.MAP_CACHE_REGIONS, data: filteredRegions });
};

export const getSettings = async (): Promise<AppSettings> => {
  try {
    const data = await Taro.getStorage({ key: STORAGE_KEYS.SETTINGS });
    return { ...DEFAULT_SETTINGS, ...(data as AppSettings) };
  } catch (error) {
    console.error('[OfflineDB] 获取设置失败:', error);
    return DEFAULT_SETTINGS;
  }
};

export const saveSettings = async (settings: Partial<AppSettings>): Promise<void> => {
  const currentSettings = await getSettings();
  const mergedSettings = { ...currentSettings, ...settings };
  await Taro.setStorage({ key: STORAGE_KEYS.SETTINGS, data: mergedSettings });
};

export const getStorageStats = async (): Promise<StorageStats> => {
  try {
    const events = await getEvents();
    const logs = await getSyncLogs();
    const regions = await getMapCacheRegions();

    let eventSize = 0;
    let imageCount = 0;
    events.forEach(event => {
      eventSize += JSON.stringify(event).length;
      imageCount += event.images.length;
    });

    const mapCacheSize = regions.reduce((sum, r) => sum + r.totalSize, 0);
    const logsSize = JSON.stringify(logs).length;
    const totalSize = eventSize + mapCacheSize + logsSize;

    return {
      totalSize,
      eventCount: events.length,
      imageCount,
      mapCacheSize,
      otherSize: logsSize
    };
  } catch (error) {
    console.error('[OfflineDB] 获取存储统计失败:', error);
    return {
      totalSize: 0,
      eventCount: 0,
      imageCount: 0,
      mapCacheSize: 0,
      otherSize: 0
    };
  }
};

export const clearAllEvents = async (): Promise<void> => {
  await Taro.setStorage({ key: STORAGE_KEYS.EVENTS, data: [] });
  console.log('[OfflineDB] 已清空所有事件');
};

export const clearSyncLogs = async (): Promise<void> => {
  await Taro.setStorage({ key: STORAGE_KEYS.SYNC_LOGS, data: [] });
  console.log('[OfflineDB] 已清空同步日志');
};

export const clearMapCache = async (): Promise<void> => {
  await Taro.setStorage({ key: STORAGE_KEYS.MAP_CACHE_REGIONS, data: [] });
  console.log('[OfflineDB] 已清空地图缓存');
};

export const clearAllData = async (): Promise<void> => {
  await clearAllEvents();
  await clearSyncLogs();
  await clearMapCache();
  console.log('[OfflineDB] 已清空所有数据');
};
