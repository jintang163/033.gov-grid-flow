import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro, { useDidShow, usePullDownRefresh, useRouter } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import StatusBadge from '@/components/StatusBadge';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useSync } from '@/hooks/useSync';
import { useOfflineStore } from '@/store/useOfflineStore';
import { EVENT_TYPE_OPTIONS, PRIORITY_OPTIONS, SYNC_STATUS_OPTIONS } from '@/types/event';
import type { OfflineEvent } from '@/types/event';

const DetailPage: React.FC = () => {
  const router = useRouter();
  const clientId = router.params.clientId as string;

  const { getEvent, deleteEvent, formatDate, formatRelativeTime, formatFileSize } = useOfflineDB();
  const { syncSingle, isSyncing } = useSync();
  const { refreshAll } = useOfflineStore();

  const [event, setEvent] = useState<OfflineEvent | null>(null);
  const [loading, setLoading] = useState(true);

  const loadEvent = useCallback(async () => {
    if (!clientId) return;
    setLoading(true);
    try {
      const data = await getEvent(clientId);
      setEvent(data);
    } catch (error) {
      console.error('[DetailPage] 加载事件失败:', error);
    } finally {
      setLoading(false);
    }
  }, [clientId, getEvent]);

  useDidShow(() => {
    console.log('[DetailPage] 页面显示');
    loadEvent();
  });

  usePullDownRefresh(async () => {
    console.log('[DetailPage] 下拉刷新');
    await loadEvent();
    Taro.stopPullDownRefresh();
  });

  useEffect(() => {
    loadEvent();
  }, [loadEvent]);

  const handleSync = useCallback(async () => {
    if (!event) return;
    console.log('[DetailPage] 同步事件:', event.clientId);
    await syncSingle(event);
    await loadEvent();
    await refreshAll();
  }, [event, syncSingle, loadEvent, refreshAll]);

  const handleDelete = useCallback(async () => {
    if (!event) return;
    await deleteEvent(event.clientId);
    Taro.navigateBack();
  }, [event, deleteEvent]);

  const handlePreviewImage = useCallback((current: string) => {
    if (!event) return;
    const urls = event.images.map(img => img.localPath);
    Taro.previewImage({
      current,
      urls
    });
  }, [event]);

  if (loading) {
    return (
      <View className={styles.detailPage}>
        <View className={styles.loadingState}>
          <Text className={styles.loadingIcon}>⏳</Text>
          <Text className={styles.loadingText}>加载中...</Text>
        </View>
      </View>
    );
  }

  if (!event) {
    return (
      <View className={styles.detailPage}>
        <View className={styles.emptyState}>
          <Text className={styles.emptyIcon}>📭</Text>
          <Text className={styles.emptyTitle}>事件不存在</Text>
          <Text className={styles.emptyDesc}>该事件可能已被删除</Text>
        </View>
      </View>
    );
  }

  const eventType = EVENT_TYPE_OPTIONS.find(opt => opt.code === event.type);
  const priority = PRIORITY_OPTIONS.find(opt => opt.code === event.priority);
  const syncStatus = SYNC_STATUS_OPTIONS.find(opt => opt.code === event.syncStatus);

  const getStatusIcon = () => {
    switch (event.syncStatus) {
      case 'pending': return '📦';
      case 'syncing': return '🔄';
      case 'synced': return '✅';
      case 'failed': return '❌';
      default: return '📝';
    }
  };

  const canSync = event.syncStatus === 'pending' || event.syncStatus === 'failed';

  return (
    <View className={styles.detailPage}>
      <View className={styles.statusHeader}>
        <View className={styles.statusLeft}>
          <Text className={styles.statusIcon}>{getStatusIcon()}</Text>
          <View className={styles.statusInfo}>
            <Text className={styles.statusText}>{syncStatus?.name || '未知状态'}</Text>
            <Text className={styles.statusTime}>
              {event.syncedAt
                ? `同步于 ${formatRelativeTime(event.syncedAt)}`
                : `创建于 ${formatRelativeTime(event.createdAt)}`}
            </Text>
          </View>
        </View>
        <StatusBadge status={event.syncStatus} size="medium" />
      </View>

      <View className={styles.contentSection}>
        <Text className={styles.sectionTitle}>
          <Text className={styles.sectionIcon}>📝</Text>
          基本信息
        </Text>
        <View className={styles.basicInfo}>
          <View className={styles.infoRow}>
            <Text className={styles.infoLabel}>事件类型</Text>
            <View className={styles.infoValue}>
              <Text className={styles.typeTag}>
                <Text>{eventType?.icon || '📝'}</Text>
                <Text>{eventType?.name || '其他'}</Text>
              </Text>
            </View>
          </View>

          <View className={styles.infoRow}>
            <Text className={styles.infoLabel}>优先级</Text>
            <View className={styles.infoValue}>
              <Text className={classnames(styles.priorityTag, styles[event.priority])}>
                {priority?.name || '中'}
              </Text>
            </View>
          </View>

          <View className={styles.infoRow}>
            <Text className={styles.infoLabel}>事件标题</Text>
            <View className={styles.infoValue}>
              <Text className={styles.titleText}>{event.title}</Text>
            </View>
          </View>

          <View className={styles.infoRow}>
            <Text className={styles.infoLabel}>事件描述</Text>
            <View className={styles.infoValue}>
              <Text className={styles.descText}>{event.description}</Text>
            </View>
          </View>
        </View>
      </View>

      {event.images.length > 0 && (
        <View className={styles.contentSection}>
          <Text className={styles.sectionTitle}>
            <Text className={styles.sectionIcon}>🖼️</Text>
            现场照片 ({event.images.length}张)
          </Text>
          <View className={styles.imageGrid}>
            {event.images.map((img) => (
              <View
                key={img.id}
                className={styles.imageItem}
                onClick={() => handlePreviewImage(img.localPath)}
              >
                <Image
                  className={styles.image}
                  src={img.localPath}
                  mode="aspectFill"
                />
                <View className={styles.imageUploadStatus}>
                  {img.uploadStatus === 'synced' ? '已上传' :
                   img.uploadStatus === 'syncing' ? '上传中' :
                   img.uploadStatus === 'failed' ? '上传失败' : '待上传'}
                </View>
              </View>
            ))}
          </View>
        </View>
      )}

      <View className={styles.contentSection}>
        <Text className={styles.sectionTitle}>
          <Text className={styles.sectionIcon}>📍</Text>
          位置信息
        </Text>
        <View className={styles.locationCard}>
          <Text className={styles.locationIcon}>📍</Text>
          <View className={styles.locationInfo}>
            <Text className={styles.locationAddress}>{event.location.address}</Text>
            <Text className={styles.locationCoords}>
              {event.location.gridName && `网格: ${event.location.gridName} · `}
              坐标: {event.location.longitude.toFixed(6)}, {event.location.latitude.toFixed(6)}
            </Text>
          </View>
        </View>
      </View>

      <View className={styles.contentSection}>
        <Text className={styles.sectionTitle}>
          <Text className={styles.sectionIcon}>👤</Text>
          上报信息
        </Text>
        <View className={styles.metaInfo}>
          <View className={styles.metaItem}>
            <Text className={styles.metaIcon}>🆔</Text>
            <Text>上报人: {event.anonymous ? '匿名' : event.reporterName}</Text>
          </View>
          <View className={styles.metaItem}>
            <Text className={styles.metaIcon}>📅</Text>
            <Text>创建时间: {formatDate(event.createdAt)}</Text>
          </View>
          <View className={styles.metaItem}>
            <Text className={styles.metaIcon}>✏️</Text>
            <Text>更新时间: {formatDate(event.updatedAt)}</Text>
          </View>
          {event.serverId && (
            <View className={styles.metaItem}>
              <Text className={styles.metaIcon}>🔗</Text>
              <Text>服务端ID: {event.serverId}</Text>
            </View>
          )}
        </View>
      </View>

      <View className={styles.syncInfo}>
        <Text className={styles.sectionTitle}>
          <Text className={styles.sectionIcon}>🔄</Text>
          同步信息
        </Text>
        <View className={styles.syncStatusRow}>
          <View>
            <Text style={{ fontSize: '28rpx', color: '#86909C' }}>同步状态</Text>
          </View>
          <StatusBadge status={event.syncStatus} size="medium" />
        </View>
        <View className={styles.syncDetails}>
          <View className={styles.syncDetailItem}>
            <Text className={styles.syncDetailLabel}>重试次数</Text>
            <Text className={styles.syncDetailValue}>{event.syncRetryCount} 次</Text>
          </View>
          {event.syncedAt && (
            <View className={styles.syncDetailItem}>
              <Text className={styles.syncDetailLabel}>同步时间</Text>
              <Text className={styles.syncDetailValue}>{formatDate(event.syncedAt)}</Text>
            </View>
          )}
          {event.images.length > 0 && (
            <View className={styles.syncDetailItem}>
              <Text className={styles.syncDetailLabel}>图片大小</Text>
              <Text className={styles.syncDetailValue}>
                {formatFileSize(event.images.reduce((sum, img) => sum + img.size, 0))}
              </Text>
            </View>
          )}
        </View>

        {event.syncError && (
          <View className={styles.errorBox}>
            <Text className={styles.errorText}>❌ {event.syncError}</Text>
            {event.syncRetryCount > 0 && (
              <View className={styles.retryInfo}>
                <Text>⏱️ 已自动重试 {event.syncRetryCount} 次</Text>
              </View>
            )}
          </View>
        )}
      </View>

      <View className={styles.bottomBar}>
        <View
          className={classnames(styles.btn, styles.btnDanger)}
          onClick={handleDelete}
        >
          <Text>删除</Text>
        </View>
        <View
          className={classnames(styles.btn, styles.btnPrimary, {
            [styles.btnDisabled]: !canSync || isSyncing
          })}
          onClick={handleSync}
        >
          <Text>{isSyncing ? '同步中...' : canSync ? '立即同步' : '已同步'}</Text>
        </View>
      </View>
    </View>
  );
};

export default DetailPage;
