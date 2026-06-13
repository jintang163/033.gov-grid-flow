"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_SETTINGS = exports.STORAGE_KEYS = void 0;
exports.STORAGE_KEYS = {
    EVENTS: 'offline_events',
    IMAGES: 'offline_images',
    MAP_CACHE_REGIONS: 'map_cache_regions',
    MAP_TILES: 'map_tiles',
    SYNC_LOGS: 'sync_logs',
    DEVICE_INFO: 'device_info',
    USER_INFO: 'user_info',
    SETTINGS: 'app_settings'
};
exports.DEFAULT_SETTINGS = {
    autoSync: true,
    syncOnlyOnWifi: false,
    autoRetry: true,
    maxRetryCount: 3,
    enableMapCache: true,
    maxCacheSize: 500 * 1024 * 1024
};
//# sourceMappingURL=offline.js.map