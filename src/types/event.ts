export type EventType = 
  | 'environment'
  | 'public_facility'
  | 'dispute'
  | 'safety_hazard'
  | 'traffic'
  | 'service'
  | 'security'
  | 'other';

export type EventPriority = 'low' | 'medium' | 'high' | 'urgent';

export type EventStatus = 
  | 'pending'
  | 'processing'
  | 'completed'
  | 'rejected';

export type SyncStatus = 
  | 'pending'
  | 'syncing'
  | 'synced'
  | 'failed';

export interface EventImage {
  id: string;
  localPath: string;
  remoteUrl?: string;
  size: number;
  uploadStatus: SyncStatus;
  createdAt: number;
}

export interface LocationInfo {
  longitude: number;
  latitude: number;
  address: string;
  gridCode?: string;
  gridName?: string;
}

export interface OfflineEvent {
  clientId: string;
  type: EventType;
  title: string;
  description: string;
  priority: EventPriority;
  images: EventImage[];
  location: LocationInfo;
  reporterId: string;
  reporterName: string;
  anonymous: number;
  createdAt: number;
  updatedAt: number;
  syncStatus: SyncStatus;
  syncRetryCount: number;
  syncError?: string;
  syncedAt?: number;
  serverId?: number;
}

export interface EventTypeOption {
  code: EventType;
  name: string;
  icon: string;
}

export const EVENT_TYPE_OPTIONS: EventTypeOption[] = [
  { code: 'environment', name: '环境卫生', icon: '🧹' },
  { code: 'public_facility', name: '公共设施', icon: '🏢' },
  { code: 'dispute', name: '矛盾纠纷', icon: '⚖️' },
  { code: 'safety_hazard', name: '安全隐患', icon: '⚠️' },
  { code: 'traffic', name: '交通出行', icon: '🚗' },
  { code: 'service', name: '民生服务', icon: '🤝' },
  { code: 'security', name: '治安问题', icon: '👮' },
  { code: 'other', name: '其他问题', icon: '📝' }
];

export const PRIORITY_OPTIONS = [
  { code: 'low', name: '低', color: '#86909C' },
  { code: 'medium', name: '中', color: '#165DFF' },
  { code: 'high', name: '高', color: '#FF7D00' },
  { code: 'urgent', name: '紧急', color: '#F53F3F' }
];

export const SYNC_STATUS_OPTIONS = [
  { code: 'pending', name: '待同步', color: '#FF7D00' },
  { code: 'syncing', name: '同步中', color: '#165DFF' },
  { code: 'synced', name: '已同步', color: '#00B42A' },
  { code: 'failed', name: '同步失败', color: '#F53F3F' }
];
