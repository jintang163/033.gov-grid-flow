"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.clearAllData = exports.clearMapCache = exports.clearSyncLogs = exports.clearAllEvents = exports.getStorageStats = exports.saveSettings = exports.getSettings = exports.deleteMapCacheRegion = exports.saveMapCacheRegion = exports.getMapCacheRegions = exports.saveSyncLog = exports.getSyncLogs = exports.getPendingEvents = exports.updateEventSyncStatus = exports.deleteEvent = exports.saveEvent = exports.getEventByClientId = exports.getEvents = exports.initOfflineDB = void 0;
const taro_1 = require("@tarojs/taro");
const offline_1 = require("@/types/offline");
let dbInitialized = false;
const initOfflineDB = async () => {
    if (dbInitialized)
        return;
    console.log('[OfflineDB] 初始化离线数据库');
    try {
        const keys = [
            offline_1.STORAGE_KEYS.EVENTS,
            offline_1.STORAGE_KEYS.SYNC_LOGS,
            offline_1.STORAGE_KEYS.MAP_CACHE_REGIONS,
            offline_1.STORAGE_KEYS.SETTINGS
        ];
        for (const key of keys) {
            const data = await taro_1.default.getStorage({ key }).catch(() => null);
            if (!data) {
                const defaultValue = key === offline_1.STORAGE_KEYS.EVENTS ? [] :
                    key === offline_1.STORAGE_KEYS.SYNC_LOGS ? [] :
                        key === offline_1.STORAGE_KEYS.MAP_CACHE_REGIONS ? [] :
                            offline_1.DEFAULT_SETTINGS;
                await taro_1.default.setStorage({ key, data: defaultValue });
            }
        }
        dbInitialized = true;
        console.log('[OfflineDB] 初始化完成');
    }
    catch (error) {
        console.error('[OfflineDB] 初始化失败:', error);
        throw error;
    }
};
exports.initOfflineDB = initOfflineDB;
const getEvents = async () => {
    try {
        const data = await taro_1.default.getStorage({ key: offline_1.STORAGE_KEYS.EVENTS });
        return data || [];
    }
    catch (error) {
        console.error('[OfflineDB] 获取事件失败:', error);
        return [];
    }
};
exports.getEvents = getEvents;
const getEventByClientId = async (clientId) => {
    const events = await (0, exports.getEvents)();
    return events.find(e => e.clientId === clientId) || null;
};
exports.getEventByClientId = getEventByClientId;
const saveEvent = async (event) => {
    const events = await (0, exports.getEvents)();
    const existingIndex = events.findIndex(e => e.clientId === event.clientId);
    if (existingIndex >= 0) {
        events[existingIndex] = Object.assign(Object.assign({}, event), { updatedAt: Date.now() });
    }
    else {
        events.unshift(event);
    }
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.EVENTS, data: events });
    console.log('[OfflineDB] 事件已保存:', event.clientId);
};
exports.saveEvent = saveEvent;
const deleteEvent = async (clientId) => {
    const events = await (0, exports.getEvents)();
    const filteredEvents = events.filter(e => e.clientId !== clientId);
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.EVENTS, data: filteredEvents });
    console.log('[OfflineDB] 事件已删除:', clientId);
};
exports.deleteEvent = deleteEvent;
const updateEventSyncStatus = async (clientId, syncStatus, syncError, serverId) => {
    const events = await (0, exports.getEvents)();
    const eventIndex = events.findIndex(e => e.clientId === clientId);
    if (eventIndex >= 0) {
        const event = events[eventIndex];
        events[eventIndex] = Object.assign(Object.assign({}, event), { syncStatus,
            syncError,
            serverId, syncedAt: syncStatus === 'synced' ? Date.now() : event.syncedAt, syncRetryCount: syncStatus === 'failed' ? event.syncRetryCount + 1 : event.syncRetryCount, updatedAt: Date.now() });
        await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.EVENTS, data: events });
        console.log('[OfflineDB] 事件同步状态已更新:', clientId, syncStatus);
    }
};
exports.updateEventSyncStatus = updateEventSyncStatus;
const getPendingEvents = async () => {
    const events = await (0, exports.getEvents)();
    return events
        .filter(e => e.syncStatus === 'pending' || e.syncStatus === 'failed')
        .sort((a, b) => a.createdAt - b.createdAt);
};
exports.getPendingEvents = getPendingEvents;
const getSyncLogs = async () => {
    try {
        const data = await taro_1.default.getStorage({ key: offline_1.STORAGE_KEYS.SYNC_LOGS });
        return data || [];
    }
    catch (error) {
        console.error('[OfflineDB] 获取同步日志失败:', error);
        return [];
    }
};
exports.getSyncLogs = getSyncLogs;
const saveSyncLog = async (log) => {
    const logs = await (0, exports.getSyncLogs)();
    logs.unshift(log);
    if (logs.length > 100) {
        logs.length = 100;
    }
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.SYNC_LOGS, data: logs });
};
exports.saveSyncLog = saveSyncLog;
const getMapCacheRegions = async () => {
    try {
        const data = await taro_1.default.getStorage({ key: offline_1.STORAGE_KEYS.MAP_CACHE_REGIONS });
        return data || [];
    }
    catch (error) {
        console.error('[OfflineDB] 获取地图缓存区域失败:', error);
        return [];
    }
};
exports.getMapCacheRegions = getMapCacheRegions;
const saveMapCacheRegion = async (region) => {
    const regions = await (0, exports.getMapCacheRegions)();
    const existingIndex = regions.findIndex(r => r.id === region.id);
    if (existingIndex >= 0) {
        regions[existingIndex] = region;
    }
    else {
        regions.unshift(region);
    }
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.MAP_CACHE_REGIONS, data: regions });
};
exports.saveMapCacheRegion = saveMapCacheRegion;
const deleteMapCacheRegion = async (regionId) => {
    const regions = await (0, exports.getMapCacheRegions)();
    const filteredRegions = regions.filter(r => r.id !== regionId);
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.MAP_CACHE_REGIONS, data: filteredRegions });
};
exports.deleteMapCacheRegion = deleteMapCacheRegion;
const getSettings = async () => {
    try {
        const data = await taro_1.default.getStorage({ key: offline_1.STORAGE_KEYS.SETTINGS });
        return Object.assign(Object.assign({}, offline_1.DEFAULT_SETTINGS), data);
    }
    catch (error) {
        console.error('[OfflineDB] 获取设置失败:', error);
        return offline_1.DEFAULT_SETTINGS;
    }
};
exports.getSettings = getSettings;
const saveSettings = async (settings) => {
    const currentSettings = await (0, exports.getSettings)();
    const mergedSettings = Object.assign(Object.assign({}, currentSettings), settings);
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.SETTINGS, data: mergedSettings });
};
exports.saveSettings = saveSettings;
const getStorageStats = async () => {
    try {
        const events = await (0, exports.getEvents)();
        const logs = await (0, exports.getSyncLogs)();
        const regions = await (0, exports.getMapCacheRegions)();
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
    }
    catch (error) {
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
exports.getStorageStats = getStorageStats;
const clearAllEvents = async () => {
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.EVENTS, data: [] });
    console.log('[OfflineDB] 已清空所有事件');
};
exports.clearAllEvents = clearAllEvents;
const clearSyncLogs = async () => {
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.SYNC_LOGS, data: [] });
    console.log('[OfflineDB] 已清空同步日志');
};
exports.clearSyncLogs = clearSyncLogs;
const clearMapCache = async () => {
    await taro_1.default.setStorage({ key: offline_1.STORAGE_KEYS.MAP_CACHE_REGIONS, data: [] });
    console.log('[OfflineDB] 已清空地图缓存');
};
exports.clearMapCache = clearMapCache;
const clearAllData = async () => {
    await (0, exports.clearAllEvents)();
    await (0, exports.clearSyncLogs)();
    await (0, exports.clearMapCache)();
    console.log('[OfflineDB] 已清空所有数据');
};
exports.clearAllData = clearAllData;
//# sourceMappingURL=offlineDB.js.map