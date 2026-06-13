import React, { useEffect, useCallback } from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import NetworkStatus from '@/components/NetworkStatus';
import RedDot from '@/components/RedDot';
import SyncProgress from '@/components/SyncProgress';
import EventCard from '@/components/EventCard';
import { useNetwork } from '@/hooks/useNetwork';
import { useSync } from '@/hooks/useSync';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useSyncStore } from '@/store/useSyncStore';
import { useOfflineStore } from '@/store/useOfflineStore';

const HomePage: React.FC = () => {
  const { isOnline, networkType } = useNetwork();
  const { isSyncing, syncProgress, syncAll, cancelSync, pendingCount } = useSync();
  const { pendingEvents, formatRelativeTime, getPendingCount } = useOfflineDB();
  const { failedCount, showRedDot } = useSyncStore();
  const { refreshAll, storageStats } = useOfflineStore();

  useDidShow(() => {
    console.log('[HomePage] 页面显示，刷新数据');
    refreshAll();
    getPendingCount();
  });

  usePullDownRefresh(async () => {
    console.log('[HomePage] 下拉刷新');
    await refreshAll();
    getPendingCount();
    Taro.stopPullDownRefresh();
    Taro.showToast({
      title: '刷新成功',
      icon: 'success'
    });
  });

  useEffect(() => {
    refreshAll();
    getPendingCount();
  }, [refreshAll, getPendingCount]);

  const handleSyncAll = useCallback(async () => {
    console.log('[HomePage] 点击一键同步');
    await syncAll();
  }, [syncAll]);

  const handleNavigateTo = useCallback((url: string) => {
    console.log('[HomePage] 跳转到:', url);
    Taro.navigateTo({ url });
  }, []);

  const handleSwitchTab = useCallback((url: string) => {
    console.log('[HomePage] 切换到Tab:', url);
    Taro.switchTab({ url });
  }, []);

  const handleSyncSingle = useCallback(async (event) => {
    console.log('[HomePage] 同步单条事件:', event.clientId);
    await Taro.navigateTo({
      url: `/pages/offline/index?highlightId=${event.clientId}`
    });
  }, []);

  const handleDeleteEvent = useCallback(async (clientId: string) => {
    console.log('[HomePage] 删除事件:', clientId);
  }, []);

  const previewEvents = pendingEvents.slice(0, 3);

  return (
    <ScrollView
      className={styles.homePage}
      scrollY
      enhanced
      showScrollbar={false}
    >
      <View className={styles.header}>
        <View className={styles.headerTop}>
          <View className={styles.greeting}>
            <Text className={styles.greetingTitle}>您好，网格员</Text>
            <Text className={styles.greetingSubtitle}>
              {formatRelativeTime(Date.now() - 3600000)} 开始今日巡查
            </Text>
          </View>
          <View className={styles.statusBar}>
            <NetworkStatus
              isOnline={isOnline}
              networkType={networkType}
              size="small"
            />
          </View>
        </View>
      </View>

      <View className={styles.syncStatusCard}>
        <View className={styles.syncStatusRow}>
          <View className={styles.syncStats}>
            <View className={styles.statItem}>
              <Text className={classnames(styles.statNumber, styles.pending)}>
                {pendingCount}
              </Text>
              <Text className={styles.statLabel}>待同步</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={classnames(styles.statNumber, styles.failed)}>
                {failedCount}
              </Text>
              <Text className={styles.statLabel}>同步失败</Text>
            </View>
            <View className={styles.statItem}>
              <Text className={styles.statNumber}>{storageStats.eventCount}</Text>
              <Text className={styles.statLabel}>本地总数</Text>
            </View>
          </View>
          <View
            className={classnames('primaryBtn', {
              [styles.syncingBtn]: isSyncing
            })}
            onClick={handleSyncAll}
          >
            <Text>{isSyncing ? '同步中...' : '一键同步'}</Text>
          </View>
        </View>
      </View>

      {isSyncing && syncProgress && (
        <View className={styles.syncProgressContainer}>
          <SyncProgress
            progress={syncProgress}
            onCancel={cancelSync}
          />
        </View>
      )}

      <View className={styles.mapSection}>
        <Text className={styles.sectionTitle}>我的网格</Text>
        <View className={styles.mapContainer}>
          <View className={styles.mapGridInfo}>
            <Text className={styles.gridName}>📍 和平里第一网格</Text>
          </View>
          <View className={styles.mapPlaceholder}>
            <Text className={styles.mapIcon}>🗺️</Text>
            <Text className={styles.mapText}>网格地图</Text>
            <Text className={styles.mapSubText}>
              {isOnline ? '加载在线地图中...' : '使用离线地图缓存'}
            </Text>
          </View>
        </View>
      </View>

      <View className={styles.quickActions}>
        <Text className={styles.sectionTitle}>快捷操作</Text>
        <View className={styles.actionGrid}>
          <View
            className={styles.actionItem}
            onClick={() => handleSwitchTab('/pages/report/index')}
          >
            <Text className={styles.actionIcon}>📝</Text>
            <Text className={styles.actionText}>快速上报</Text>
          </View>
          <View
            className={styles.actionItem}
            onClick={() => handleSwitchTab('/pages/offline/index')}
          >
            <Text className={styles.actionIcon}>📦</Text>
            <Text className={styles.actionText}>本地事件</Text>
            <View className={styles.redDotWrapper}>
              <RedDot show={showRedDot} size="small" />
            </View>
          </View>
          <View
            className={styles.actionItem}
            onClick={() => handleNavigateTo('/pages/map-cache/index')}
          >
            <Text className={styles.actionIcon}>🗺️</Text>
            <Text className={styles.actionText}>地图缓存</Text>
          </View>
          <View
            className={styles.actionItem}
            onClick={() => handleNavigateTo('/pages/sync-log/index')}
          >
            <Text className={styles.actionIcon}>📋</Text>
            <Text className={styles.actionText}>同步记录</Text>
          </View>
        </View>
      </View>

      <View className={styles.pendingPreview}>
        <View className={styles.previewHeader}>
          <Text className={styles.sectionTitle}>待同步事件</Text>
          <Text
            className={styles.viewAllBtn}
            onClick={() => handleSwitchTab('/pages/offline/index')}
          >
            查看全部 →
          </Text>
        </View>

        {previewEvents.length > 0 ? (
          <View className={styles.previewList}>
            {previewEvents.map(event => (
              <EventCard
                key={event.clientId}
                event={event}
                showSyncButton
                onSync={() => handleSyncSingle(event)}
                onDelete={() => handleDeleteEvent(event.clientId)}
              />
            ))}
          </View>
        ) : (
          <View className={styles.emptyState}>
            <Text className={styles.emptyIcon}>✅</Text>
            <Text className={styles.emptyText}>暂无待同步事件</Text>
          </View>
        )}
      </View>
    </ScrollView>
  );
};

export default HomePage;
