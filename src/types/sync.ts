import type { OfflineEvent } from './event';

export interface SyncProgress {
  total: number;
  completed: number;
  failed: number;
  currentEvent?: OfflineEvent;
}

export interface SyncResult {
  success: boolean;
  total: number;
  successCount: number;
  failedCount: number;
  failedItems: Array<{
    clientId: string;
    error: string;
  }>;
}

export interface SyncLogItem {
  id: string;
  startTime: number;
  endTime: number;
  totalCount: number;
  successCount: number;
  failedCount: number;
  status: 'success' | 'partial' | 'failed';
  errorMessage?: string;
}

export interface BatchSyncRequest {
  events: OfflineEvent[];
  timestamp: number;
  deviceId: string;
}

export interface BatchSyncResponse {
  success: boolean;
  message: string;
  results: Array<{
    clientId: string;
    serverId: number;
    success: boolean;
    error?: string;
  }>;
}
