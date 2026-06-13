import { create } from 'zustand';
import type { SyncProgress } from '@/types/sync';

interface SyncState {
  isOnline: boolean;
  networkType: string;
  isSyncing: boolean;
  syncProgress: SyncProgress | null;
  pendingCount: number;
  failedCount: number;
  showRedDot: boolean;

  setNetworkStatus: (isOnline: boolean, networkType: string) => void;
  setSyncing: (isSyncing: boolean) => void;
  setSyncProgress: (progress: SyncProgress | null) => void;
  setPendingCount: (count: number) => void;
  setFailedCount: (count: number) => void;
  updateRedDot: () => void;
}

export const useSyncStore = create<SyncState>((set, get) => ({
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
