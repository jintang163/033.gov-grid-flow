export interface MapTileInfo {
  x: number;
  y: number;
  zoom: number;
  url: string;
  localPath: string;
  size: number;
  downloadedAt: number;
}

export interface MapCacheRegion {
  id: string;
  name: string;
  minZoom: number;
  maxZoom: number;
  bounds: {
    west: number;
    south: number;
    east: number;
    north: number;
  };
  totalTiles: number;
  downloadedTiles: number;
  totalSize: number;
  status: 'downloading' | 'completed' | 'paused' | 'failed';
  createdAt: number;
  completedAt?: number;
}

export interface DeviceInfo {
  deviceId: string;
  appVersion: string;
  systemVersion: string;
  model: string;
  platform: string;
}

export interface StorageStats {
  totalSize: number;
  eventCount: number;
  imageCount: number;
  mapCacheSize: number;
  otherSize: number;
}

export const STORAGE_KEYS = {
  EVENTS: 'offline_events',
  IMAGES: 'offline_images',
  MAP_CACHE_REGIONS: 'map_cache_regions',
  MAP_TILES: 'map_tiles',
  SYNC_LOGS: 'sync_logs',
  DEVICE_INFO: 'device_info',
  USER_INFO: 'user_info',
  SETTINGS: 'app_settings'
} as const;

export interface AppSettings {
  autoSync: boolean;
  syncOnlyOnWifi: boolean;
  autoRetry: boolean;
  maxRetryCount: number;
  enableMapCache: boolean;
  maxCacheSize: number;
}

export const DEFAULT_SETTINGS: AppSettings = {
  autoSync: true,
  syncOnlyOnWifi: false,
  autoRetry: true,
  maxRetryCount: 3,
  enableMapCache: true,
  maxCacheSize: 500 * 1024 * 1024
};
