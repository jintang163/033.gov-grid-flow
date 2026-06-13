import React, { useState, useMemo, useCallback } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { useSync } from '@/hooks/useSync';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useOfflineStore } from '@/store/useOfflineStore';
import type { SyncLogItem } from '@/types/sync';

const mockLogs: SyncLogItem[] = [
  {
    id: 'log-1',
    startTime: Date.now() - 3600000,
    endTime: Date.now() - 3580000,
    totalCount: 5,
    successCount: 5,
    failedCount: 0,
    status: 'success'
  },
  {
    id: 'log-2',
    startTime: Date.now() - 7200000,
    endTime: Date.now() - 7150000,
    totalCount: 8,
    successCount: 6,
    failedCount: 2,
    status: 'partial',
    errorMessage: '网络不稳定，部分数据同步失败'
  },
  {
    id: 'log-3',
    startTime: Date.now() - 86400000,
    endTime: Date.now() - 86350000,
    totalCount: 3,
    successCount: 0,
    failedCount: 3,
    status: 'failed',
    errorMessage: '服务器连接超时'
  },
  {
    id: 'log-4',
    startTime: Date.now() - 86400000 * 2,
    endTime: Date.now() - 86400000 * 2 + 120000,
    totalCount: 12,
    successCount: 12,
    failedCount: 0,
    status: 'success'
  },
  {
    id: 'log-5',
    startTime: Date.now() - 86400000 * 3,
    endTime: Date.now() - 86400000 * 3 + 45000,
    totalCount: 2,
    successCount: 2,
    failedCount: 0,
    status: 'success'
  }
];

const failedEvents = [
  { clientId: 'event-1', title: '路灯损坏报修', error: '图片上传失败：网络超时' },
  { clientId: 'event-2', title: '垃圾清运不及时', error: '服务器返回500错误' }
];

type FilterType = 'all' | 'success' | 'partial' | 'failed';

const FILTER_TABS: Array<{ key: FilterType; label: string; icon: string }> = [
  { key: 'all', label: '全部', icon: '📋' },
  { key: 'success', label: '成功', icon: '✅' },
  { key: 'partial', label: '部分成功', icon: '⚠️' },
  { key: 'failed', label: '失败', icon: '❌' }
];

const SyncLogPage: React.FC = () => {
  const { syncAll, isSyncing } = useSync();
  const { formatDate, formatRelativeTime, formatFileSize } = useOfflineDB();
  const { syncLogs, refreshAll } = useOfflineStore();

  const [logs, setLogs] = useState<SyncLogItem[]>(mockLogs);
  const [activeFilter, setActiveFilter] = useState<FilterType>('all');
  const [loading, setLoading] = useState(false);
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      await refreshAll();
      setLogs(mockLogs);
    } catch (error) {
      console.error('[SyncLogPage] 加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  }, [refreshAll]);

  useDidShow(() => {
    console.log('[SyncLogPage] 页面显示');
    loadData();
  });

  usePullDownRefresh(async () => {
    console.log('[SyncLogPage] 下拉刷新');
    await loadData();
    Taro.stopPullDownRefresh();
    Taro.showToast({
      title: '刷新成功',
      icon: 'success'
    });
  });

  const totalSyncs = logs.length;
  const totalSuccess = logs.filter(l => l.status === 'success').length;
  const totalPartial = logs.filter(l => l.status === 'partial').length;
  const totalFailed = logs.filter(l => l.status === 'failed').length;
  const totalEventsSynced = logs.reduce((sum, l) => sum + l.successCount, 0);

  const filteredLogs = useMemo(() => {
    if (activeFilter === 'all') return logs;
    return logs.filter(l => l.status === activeFilter);
  }, [logs, activeFilter]);

  const groupedLogs = useMemo(() => {
    const groups: Record<string, SyncLogItem[]> = {};
    filteredLogs.forEach(log => {
      const date = new Date(log.startTime);
      const dateKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      if (!groups[dateKey]) {
        groups[dateKey] = [];
      }
      groups[dateKey].push(log);
    });
    return groups;
  }, [filteredLogs]);

  const formatDuration = (start: number, end: number): string => {
    const diff = end - start;
    if (diff < 60000) return `${Math.floor(diff / 1000)}秒`;
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分${Math.floor((diff % 60000) / 1000)}秒`;
    return `${Math.floor(diff / 3600000)}小时${Math.floor((diff % 3600000) / 60000)}分`;
  };

  const getStatusConfig = (status: SyncLogItem['status']) => {
    switch (status) {
      case 'success': return { text: '全部成功', className: styles.success, icon: '✅' };
      case 'partial': return { text: '部分成功', className: styles.partial, icon: '⚠️' };
      case 'failed': return { text: '全部失败', className: styles.failed, icon: '❌' };
      default: return { text: '未知', className: '', icon: '❓' };
    }
  };

  const handleRetryFailed = useCallback(async (log: SyncLogItem) => {
    console.log('[SyncLogPage] 重试失败事件:', log.id);
    if (!isSyncing) {
      await syncAll();
      loadData();
    }
  }, [syncAll, isSyncing, loadData]);

  const handleViewDetails = useCallback((log: SyncLogItem) => {
    console.log('[SyncLogPage] 查看详情:', log.id);
    setExpandedLogId(expandedLogId === log.id ? null : log.id);
  }, [expandedLogId]);

  const handleClearLogs = useCallback(async () => {
    if (logs.length === 0) {
      Taro.showToast({
        title: '暂无记录',
        icon: 'none'
      });
      return;
    }

    const res = await Taro.showModal({
      title: '清空记录',
      content: '确定要清空所有同步记录吗？此操作不会影响已同步的数据。',
      confirmText: '清空',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      setLogs([]);
      Taro.showToast({
        title: '已清空',
        icon: 'success'
      });
    }
  }, [logs.length]);

  const handleSyncNow = useCallback(async () => {
    console.log('[SyncLogPage] 立即同步');
    if (!isSyncing) {
      await syncAll();
      loadData();
    }
  }, [syncAll, isSyncing, loadData]);

  const formatDateGroup = (dateKey: string): string => {
    const date = new Date(dateKey);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (dateKey === `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`) {
      return '今天';
    }
    if (dateKey === `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`) {
      return '昨天';
    }
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  };

  if (loading) {
    return (
      <View className={styles.syncLogPage}>
        <View className={styles.loadingState}>
          <Text className={styles.loadingIcon}>⏳</Text>
          <Text className={styles.loadingText}>加载中...</Text>
        </View>
      </View>
    );
  }

  return (
    <View className={styles.syncLogPage}>
      <View className={styles.statsHeader}>
        <Text className={styles.statsTitle}>📋 同步记录统计</Text>
        <View className={styles.statsGrid}>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>🔄</Text>
            <Text className={styles.statValue}>{totalSyncs}</Text>
            <Text className={styles.statLabel}>总次数</Text>
          </View>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>✅</Text>
            <Text className={styles.statValue}>{totalSuccess}</Text>
            <Text className={styles.statLabel}>成功</Text>
          </View>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>⚠️</Text>
            <Text className={styles.statValue}>{totalPartial}</Text>
            <Text className={styles.statLabel}>部分</Text>
          </View>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>📊</Text>
            <Text className={styles.statValue}>{totalEventsSynced}</Text>
            <Text className={styles.statLabel}>同步总数</Text>
          </View>
        </View>
      </View>

      <View
        className={styles.clearBtn}
        onClick={handleSyncNow}
        style={{
          marginTop: '24rpx',
          background: 'linear-gradient(135deg, #165dff 0%, #4080ff 100%)',
          border: 'none'
        }}
      >
        <Text className={styles.clearIcon}>🔄</Text>
        <Text className={styles.clearText} style={{ color: '#fff' }}>
          {isSyncing ? '同步中...' : '立即同步所有待同步数据'}
        </Text>
      </View>

      <View className={styles.filterBar}>
        {FILTER_TABS.map(tab => (
          <View
            key={tab.key}
            className={classnames(styles.filterTab, {
              [styles.active]: activeFilter === tab.key
            })}
            onClick={() => setActiveFilter(tab.key)}
          >
            <Text>{tab.icon} {tab.label}</Text>
          </View>
        ))}
      </View>

      {Object.keys(groupedLogs).length > 0 ? (
        <View className={styles.logList}>
          {Object.entries(groupedLogs).map(([dateKey, dateLogs]) => (
            <View key={dateKey} className={styles.dateGroup}>
              <Text className={styles.dateHeader}>
                {formatDateGroup(dateKey)} · {dateLogs.length}条记录
              </Text>
              {dateLogs.map(log => {
                const statusConfig = getStatusConfig(log.status);
                const isExpanded = expandedLogId === log.id;

                return (
                  <View
                    key={log.id}
                    className={classnames(styles.logCard, styles[log.status])}
                  >
                    <View className={styles.logHeader}>
                      <View className={styles.logStatus}>
                        <Text className={styles.statusIcon}>{statusConfig.icon}</Text>
                        <Text className={classnames(styles.statusText, statusConfig.className)}>
                          {statusConfig.text}
                        </Text>
                      </View>
                      <Text className={styles.logTime}>{formatRelativeTime(log.startTime)}</Text>
                    </View>

                    <View className={styles.logStats}>
                      <View className={styles.logStatItem}>
                        <Text className={styles.logStatValue}>{log.totalCount}</Text>
                        <Text className={styles.logStatLabel}>总数</Text>
                      </View>
                      <View className={styles.logStatItem}>
                        <Text className={classnames(styles.logStatValue, styles.success)}>
                          {log.successCount}
                        </Text>
                        <Text className={styles.logStatLabel}>成功</Text>
                      </View>
                      <View className={styles.logStatItem}>
                        <Text className={classnames(styles.logStatValue, styles.failed)}>
                          {log.failedCount}
                        </Text>
                        <Text className={styles.logStatLabel}>失败</Text>
                      </View>
                    </View>

                    <View className={styles.logDuration}>
                      <Text>⏱️ 耗时: {formatDuration(log.startTime, log.endTime)}</Text>
                      <Text>📅 {formatDate(log.startTime)}</Text>
                    </View>

                    {log.errorMessage && log.status !== 'success' && (
                      <View style={{
                        marginBottom: '24rpx',
                        padding: '16rpx',
                        backgroundColor: '#fff7e8',
                        borderRadius: '12rpx',
                        fontSize: '24rpx',
                        color: '#FF7D00'
                      }}>
                        <Text>⚠️ {log.errorMessage}</Text>
                      </View>
                    )}

                    {isExpanded && log.failedCount > 0 && (
                      <View className={styles.failedItems}>
                        <Text className={styles.failedTitle}>
                          <Text>❌</Text>
                          失败详情 ({log.failedCount}项)
                        </Text>
                        <View className={styles.failedList}>
                          {failedEvents.map((item, index) => (
                            <View key={index} className={styles.failedItem}>
                              <Text className={styles.failedItemIcon}>⚠️</Text>
                              <View className={styles.failedItemContent}>
                                <Text className={styles.failedItemTitle}>{item.title}</Text>
                                <Text className={styles.failedItemError}>{item.error}</Text>
                              </View>
                            </View>
                          ))}
                        </View>
                      </View>
                    )}

                    <View className={styles.logActions}>
                      {log.status !== 'success' && (
                        <View
                          className={classnames(styles.actionBtn, styles.primary, {
                            [styles.disabled]: isSyncing
                          })}
                          onClick={() => handleRetryFailed(log)}
                        >
                          <Text>🔄 重试失败</Text>
                        </View>
                      )}
                      <View
                        className={classnames(styles.actionBtn, styles.secondary)}
                        onClick={() => handleViewDetails(log)}
                      >
                        <Text>{isExpanded ? '收起详情' : '查看详情'}</Text>
                      </View>
                    </View>
                  </View>
                );
              })}
            </View>
          ))}
        </View>
      ) : (
        <View className={styles.emptyState}>
          <Text className={styles.emptyIcon}>📭</Text>
          <Text className={styles.emptyTitle}>暂无同步记录</Text>
          <Text className={styles.emptyDesc}>
            同步记录将在这里显示
          </Text>
        </View>
      )}

      {logs.length > 0 && (
        <View className={styles.clearBtn} onClick={handleClearLogs}>
          <Text className={styles.clearIcon}>🗑️</Text>
          <Text className={styles.clearText}>清空所有同步记录</Text>
        </View>
      )}
    </View>
  );
};

export default SyncLogPage;
