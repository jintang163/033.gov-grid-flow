import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import Taro, { useDidShow, usePullDownRefresh, useRouter } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import EventCard from '@/components/EventCard';
import SyncProgress from '@/components/SyncProgress';
import StatusBadge from '@/components/StatusBadge';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useSync } from '@/hooks/useSync';
import { useOfflineStore } from '@/store/useOfflineStore';
import { useSyncStore } from '@/store/useSyncStore';
import type { OfflineEvent, SyncStatus } from '@/types/event';
import type { SyncResult } from '@/types/sync';

type FilterType = 'all' | 'pending' | 'synced' | 'failed';

const FILTER_TABS: Array<{ key: FilterType; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待同步' },
  { key: 'synced', label: '已同步' },
  { key: 'failed', label: '同步失败' }
];

const OfflinePage: React.FC = () => {
  const router = useRouter();
  const { events, deleteEvent, formatDate, formatRelativeTime } = useOfflineDB();
  const { isSyncing, syncProgress, syncAll, syncSingle, cancelSync } = useSync();
  const { refreshAll } = useOfflineStore();
  const { setPendingCount, setFailedCount } = useSyncStore();

  const [activeFilter, setActiveFilter] = useState<FilterType>('all');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isBatchMode, setIsBatchMode] = useState(false);

  useDidShow(() => {
    console.log('[OfflinePage] 页面显示');
    refreshAll();
    updateCounts();

    const highlightId = router.params.highlightId;
    if (highlightId) {
      console.log('[OfflinePage] 高亮事件:', highlightId);
    }
  });

  usePullDownRefresh(async () => {
    console.log('[OfflinePage] 下拉刷新');
    await refreshAll();
    updateCounts();
    Taro.stopPullDownRefresh();
    Taro.showToast({
      title: '刷新成功',
      icon: 'success'
    });
  });

  const updateCounts = useCallback(() => {
    const pending = events.filter(e => e.syncStatus === 'pending' || e.syncStatus === 'failed').length;
    const failed = events.filter(e => e.syncStatus === 'failed').length;
    setPendingCount(pending);
    setFailedCount(failed);
  }, [events, setPendingCount, setFailedCount]);

  useEffect(() => {
    updateCounts();
  }, [events, updateCounts]);

  const filteredEvents = useMemo(() => {
    if (activeFilter === 'all') return events;
    return events.filter(e => e.syncStatus === activeFilter);
  }, [events, activeFilter]);

  const getFilterCount = (filter: FilterType) => {
    if (filter === 'all') return events.length;
    return events.filter(e => e.syncStatus === filter).length;
  };

  const handleSelectAll = () => {
    if (selectedIds.size === filteredEvents.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredEvents.map(e => e.clientId)));
    }
  };

  const handleSelectItem = (clientId: string) => {
    const newSelected = new Set(selectedIds);
    if (newSelected.has(clientId)) {
      newSelected.delete(clientId);
    } else {
      newSelected.add(clientId);
    }
    setSelectedIds(newSelected);
  };

  const handleSyncSelected = async () => {
    const selectedEvents = events.filter(e => selectedIds.has(e.clientId));
    if (selectedEvents.length === 0) {
      Taro.showToast({
        title: '请选择要同步的事件',
        icon: 'none'
      });
      return;
    }
    console.log('[OfflinePage] 批量同步:', selectedEvents.length);
    await syncAll(selectedEvents);
    setSelectedIds(new Set());
    setIsBatchMode(false);
  };

  const handleDeleteSelected = async () => {
    if (selectedIds.size === 0) {
      Taro.showToast({
        title: '请选择要删除的事件',
        icon: 'none'
      });
      return;
    }

    const res = await Taro.showModal({
      title: '确认删除',
      content: `确定要删除选中的 ${selectedIds.size} 条记录吗？',
      confirmText: '删除',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      for (const id of selectedIds) {
        await deleteEvent(id);
      }
      setSelectedIds(new Set());
      setIsBatchMode(false);
      Taro.showToast({
        title: '删除成功',
        icon: 'success'
      });
    }
  };

  const handleSyncSingle = useCallback(async (event: OfflineEvent) => {
    console.log('[OfflinePage] 同步单条事件:', event.clientId);
    await syncSingle(event);
  }, [syncSingle]);

  const handleDeleteSingle = useCallback(async (clientId: string) => {
    deleteEvent(clientId);
  }, [deleteEvent]);

  const handleSyncAllPending = async () => {
    console.log('[OfflinePage] 一键同步所有待同步');
    syncAll();
  };

  const handleClearSynced = async () => {
    const syncedEvents = events.filter(e => e.syncStatus === 'synced');
    if (syncedEvents.length === 0) {
      Taro.showToast({
        title: '没有可清理',
        icon: 'none'
      });
      return;
    }

    const res = await Taro.showModal({
      title: '清理已同步记录',
      content: `确定要清理所有已同步的 ${syncedEvents.length} 条记录吗？',
      confirmText: '清理',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      for (const event of syncedEvents) {
        await deleteEvent(event.clientId);
      }
      Taro.showToast({
        title: '清理成功',
        icon: 'success'
      });
    }
  };

  return (
    <ScrollView
      className={styles.offlinePage}
      scrollY
      enhanced
      showScrollbar={false}
    >
      <View className={styles.filterBar}>
        <View className={styles.filterTabs}>
          {FILTER_TABS.map(tab => (
            <View
              key={tab.key}
              className={classnames(styles.filterTab, {
                [styles.active]: activeFilter === tab.key
              })}
              onClick={() => setActiveFilter(tab.key)}
            >
              <Text className={styles.filterTabText}>{tab.label}</Text>
              <Text className={styles.filterTabCount}>{getFilterCount(tab.key)}</Text>
            </View>
          ))}
        </View>

        <View className={styles.actionBar}>
          <View
            className={classnames(styles.actionBtn, styles.primaryAction)}
            onClick={handleSyncAllPending}
          >
            <Text className={styles.actionBtnText}>一键同步</Text>
          </View>
          <View
            className={classnames(styles.actionBtn, styles.secondaryAction)}
            onClick={() => setIsBatchMode(!isBatchMode)}
          >
            <Text className={styles.actionBtnText}>
              {isBatchMode ? '取消' : '批量管理'}
            </Text>
          </View>
          <View
            className={classnames(styles.actionBtn, styles.dangerAction)}
            onClick={handleClearSynced}
          >
            <Text className={styles.actionBtnText}>清理已同步</Text>
          </View>
        </View>
      </View>

      {isSyncing && syncProgress && (
        <View className={styles.syncProgressSection}>
          <SyncProgress
            progress={syncProgress}
            onCancel={cancelSync}
          />
        </View>
      )}

      <View className={styles.eventList}>
        {filteredEvents.length > 0 ? (
          filteredEvents.map(event => (
          <View key={event.clientId}>
            {isBatchMode && (
              <View
                onClick={() => handleSelectItem(event.clientId)}
              >
                <View
                  className={classnames(styles.checkbox, {
                    [styles.checked]: selectedIds.has(event.clientId)
                  })}
                >
                  {selectedIds.has(event.clientId) && (
                    <Text className={styles.checkmark}>✓</Text>
                  )}
                </View>
              </View>
            )}
            <EventCard
              event={event}
              showSyncButton={!isBatchMode && (event.syncStatus === 'pending' || event.syncStatus === 'failed')}
              onSync={() => handleSyncSingle(event)}
              onDelete={() => handleDeleteSingle(event.clientId)}
            />
          </View>
        )) : (
          <View className={styles.emptyState}>
            <Text className={styles.emptyIcon}>📦</Text>
            <Text className={styles.emptyTitle}>暂无数据</Text>
            <Text className={styles.emptyDesc}>
              {activeFilter === 'all'
                ? '还没有本地事件记录'
                : `没有${FILTER_TABS.find(t => t.key === activeFilter)?.label}记录`}
            </Text>
          </View>
        )}
      </View>

      {isBatchMode && (
        <View className={styles.fixedBottom}>
          <View className={styles.batchSelectBar}>
            <View className={styles.selectAll} onClick={handleSelectAll}>
              <View
                className={classnames(styles.checkbox, {
                  [styles.checked]: selectedIds.size === filteredEvents.length && filteredEvents.length > 0
                })}
              >
                {selectedIds.size === filteredEvents.length && filteredEvents.length > 0 && (
                  <Text className={styles.checkmark}>✓</Text>
                )}
              </View>
              <Text className={styles.selectAllText}>全选</Text>
            </View>
            <Text className={styles.selectedCount}>
              已选择 {selectedIds.size} 项
            </Text>
          </View>
          <View className={styles.batchActions}>
            <View
              className={classnames(styles.batchBtn, styles.batchSyncBtn, {
                [styles.disabled]: selectedIds.size === 0
              })}
              onClick={handleSyncSelected}
            >
              <Text className={styles.batchBtnText}>批量同步</Text>
            </View>
            <View
              className={classnames(styles.batchBtn, styles.batchDeleteBtn, {
                [styles.disabled]: selectedIds.size === 0
              })}
              onClick={handleDeleteSelected}
            >
              <Text className={styles.batchBtnText}>批量删除</Text>
            </View>
          </View>
        </View>
      )}
    </ScrollView>
  );
};

export default OfflinePage;
