import React, { useState, useEffect, useCallback } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { useNetwork } from '@/hooks/useNetwork';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useOfflineStore } from '@/store/useOfflineStore';
import type { MapCacheRegion } from '@/types/offline';

const mockRegions: MapCacheRegion[] = [
  {
    id: 'region-1',
    name: '和平里街道全域',
    minZoom: 12,
    maxZoom: 18,
    bounds: { west: 116.40, south: 39.92, east: 116.45, north: 39.96 },
    totalTiles: 15680,
    downloadedTiles: 15680,
    totalSize: 256 * 1024 * 1024,
    status: 'completed',
    createdAt: Date.now() - 86400000 * 7,
    completedAt: Date.now() - 86400000 * 6
  },
  {
    id: 'region-2',
    name: '第一网格重点区域',
    minZoom: 14,
    maxZoom: 19,
    bounds: { west: 116.41, south: 39.93, east: 116.43, north: 39.95 },
    totalTiles: 8420,
    downloadedTiles: 5200,
    totalSize: 128 * 1024 * 1024,
    status: 'downloading',
    createdAt: Date.now() - 3600000 * 2
  },
  {
    id: 'region-3',
    name: '偏远山区巡查路线',
    minZoom: 10,
    maxZoom: 16,
    bounds: { west: 116.30, south: 39.80, east: 116.35, north: 39.85 },
    totalTiles: 5400,
    downloadedTiles: 0,
    totalSize: 64 * 1024 * 1024,
    status: 'paused',
    createdAt: Date.now() - 86400000 * 3
  }
];

const MapCachePage: React.FC = () => {
  const { isOnline, networkType } = useNetwork();
  const { formatFileSize, formatDate, formatRelativeTime } = useOfflineDB();
  const { storageStats, mapCacheRegions, settings, refreshAll, updateSettings } = useOfflineStore();

  const [regions, setRegions] = useState<MapCacheRegion[]>(mockRegions);
  const [minZoom, setMinZoom] = useState(12);
  const [maxZoom, setMaxZoom] = useState(18);
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      await refreshAll();
      setRegions(mockRegions);
    } catch (error) {
      console.error('[MapCachePage] 加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  }, [refreshAll]);

  useDidShow(() => {
    console.log('[MapCachePage] 页面显示');
    loadData();
  });

  usePullDownRefresh(async () => {
    console.log('[MapCachePage] 下拉刷新');
    await loadData();
    Taro.stopPullDownRefresh();
    Taro.showToast({
      title: '刷新成功',
      icon: 'success'
    });
  });

  useEffect(() => {
    loadData();
  }, [loadData]);

  const totalDownloaded = regions.reduce((sum, r) => sum + (r.downloadedTiles / r.totalTiles) * r.totalSize, 0);
  const totalSize = regions.reduce((sum, r) => sum + r.totalSize, 0);
  const totalRegions = regions.length;
  const completedRegions = regions.filter(r => r.status === 'completed').length;

  const handleToggleMapCache = useCallback(() => {
    updateSettings({ enableMapCache: !settings.enableMapCache });
  }, [settings.enableMapCache, updateSettings]);

  const handleNewRegion = useCallback(() => {
    Taro.showActionSheet({
      itemList: ['选择地图区域', '下载当前视野', '下载常用网格'],
      success: (res) => {
        console.log('[MapCachePage] 选择:', res.tapIndex);
        Taro.showToast({
          title: '功能开发中',
          icon: 'none'
        });
      }
    });
  }, []);

  const handleDownload = useCallback((region: MapCacheRegion) => {
    console.log('[MapCachePage] 下载区域:', region.id);
    if (!isOnline) {
      Taro.showToast({
        title: '请先连接网络',
        icon: 'none'
      });
      return;
    }
    if (networkType !== 'wifi' && settings.syncOnlyOnWifi) {
      Taro.showModal({
        title: '非WiFi网络',
        content: '当前为移动数据网络，确认继续下载吗？',
        success: (res) => {
          if (res.confirm) {
            startDownload(region.id);
          }
        }
      });
      return;
    }
    startDownload(region.id);
  }, [isOnline, networkType, settings.syncOnlyOnWifi]);

  const startDownload = useCallback((regionId: string) => {
    setRegions(prev => prev.map(r =>
      r.id === regionId ? { ...r, status: 'downloading' as const } : r
    ));
    Taro.showToast({
      title: '开始下载',
      icon: 'success'
    });
  }, []);

  const handlePause = useCallback((regionId: string) => {
    console.log('[MapCachePage] 暂停下载:', regionId);
    setRegions(prev => prev.map(r =>
      r.id === regionId ? { ...r, status: 'paused' as const } : r
    ));
    Taro.showToast({
      title: '已暂停',
      icon: 'success'
    });
  }, []);

  const handleResume = useCallback((regionId: string) => {
    console.log('[MapCachePage] 继续下载:', regionId);
    handleDownload(regions.find(r => r.id === regionId)!);
  }, [regions, handleDownload]);

  const handleDelete = useCallback(async (region: MapCacheRegion) => {
    const res = await Taro.showModal({
      title: '删除缓存',
      content: `确定要删除"${region.name}"的地图缓存吗？这将释放 ${formatFileSize(region.totalSize)} 空间。`,
      confirmText: '删除',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      setRegions(prev => prev.filter(r => r.id !== region.id));
      Taro.showToast({
        title: '已删除',
        icon: 'success'
      });
    }
  }, [formatFileSize]);

  const handleDownloadAll = useCallback(() => {
    const pendingRegions = regions.filter(r => r.status !== 'completed');
    if (pendingRegions.length === 0) {
      Taro.showToast({
        title: '所有区域已完成',
        icon: 'success'
      });
      return;
    }
    pendingRegions.forEach(r => startDownload(r.id));
  }, [regions, startDownload]);

  const handleClearAll = useCallback(async () => {
    if (regions.length === 0) {
      Taro.showToast({
        title: '暂无缓存',
        icon: 'none'
      });
      return;
    }

    const res = await Taro.showModal({
      title: '清理所有缓存',
      content: `确定要清理所有地图缓存吗？这将释放 ${formatFileSize(storageStats.mapCacheSize)} 空间。`,
      confirmText: '清理',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      Taro.showLoading({ title: '清理中...' });
      setTimeout(() => {
        setRegions([]);
        Taro.hideLoading();
        Taro.showToast({
          title: '清理完成',
          icon: 'success'
        });
      }, 1000);
    }
  }, [regions, storageStats.mapCacheSize, formatFileSize]);

  const getStatusConfig = (status: MapCacheRegion['status']) => {
    switch (status) {
      case 'downloading': return { text: '下载中', className: styles.downloading, icon: '⬇️' };
      case 'completed': return { text: '已完成', className: styles.completed, icon: '✅' };
      case 'paused': return { text: '已暂停', className: styles.paused, icon: '⏸️' };
      case 'failed': return { text: '失败', className: styles.failed, icon: '❌' };
      default: return { text: '未知', className: '', icon: '❓' };
    }
  };

  const handleZoomChange = (type: 'min' | 'max', delta: number) => {
    if (type === 'min') {
      const newVal = Math.max(1, Math.min(maxZoom - 1, minZoom + delta));
      setMinZoom(newVal);
    } else {
      const newVal = Math.max(minZoom + 1, Math.min(20, maxZoom + delta));
      setMaxZoom(newVal);
    }
  };

  if (loading) {
    return (
      <View className={styles.mapCachePage}>
        <View className={styles.loadingState}>
          <Text className={styles.loadingIcon}>⏳</Text>
          <Text className={styles.loadingText}>加载中...</Text>
        </View>
      </View>
    );
  }

  return (
    <View className={styles.mapCachePage}>
      <View className={styles.statsHeader}>
        <Text className={styles.statsTitle}>🗺️ 离线地图缓存</Text>
        <View className={styles.statsGrid}>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>📦</Text>
            <Text className={styles.statValue}>{formatFileSize(totalDownloaded)}</Text>
            <Text className={styles.statLabel}>已缓存</Text>
          </View>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>✅</Text>
            <Text className={styles.statValue}>{completedRegions}/{totalRegions}</Text>
            <Text className={styles.statLabel}>区域完成</Text>
          </View>
          <View className={styles.statCard}>
            <Text className={styles.statIcon}>🎯</Text>
            <Text className={styles.statValue}>{Math.round(totalDownloaded / totalSize * 100)}%</Text>
            <Text className={styles.statLabel}>总进度</Text>
          </View>
        </View>
      </View>

      {settings.enableMapCache && (
        <View
          className={classnames(styles.downloadAllBtn, {
            [styles.disabled]: !isOnline
          })}
          onClick={handleDownloadAll}
        >
          <Text className={styles.downloadAllIcon}>⬇️</Text>
          <Text className={styles.downloadAllText}>
            {isOnline ? '下载所有待缓存区域' : '请连接网络后下载'}
          </Text>
        </View>
      )}

      <View className={styles.section}>
        <View className={styles.sectionHeader}>
          <Text className={styles.sectionTitle}>
            <Text className={styles.sectionIcon}>📍</Text>
            缓存区域
          </Text>
          <Text className={styles.actionBtn} onClick={handleNewRegion}>+ 新增</Text>
        </View>

        {regions.length > 0 ? (
          regions.map(region => {
            const statusConfig = getStatusConfig(region.status);
            const progress = Math.round(region.downloadedTiles / region.totalTiles * 100);

            return (
              <View key={region.id} className={styles.regionCard}>
                <View className={styles.regionHeader}>
                  <View className={styles.regionInfo}>
                    <Text className={styles.regionName}>{region.name}</Text>
                    <View className={styles.regionMeta}>
                      <View className={styles.metaItem}>
                        <Text>🔍</Text>
                        <Text>缩放 {region.minZoom}-{region.maxZoom}</Text>
                      </View>
                      <View className={styles.metaItem}>
                        <Text>💾</Text>
                        <Text>{formatFileSize(region.totalSize)}</Text>
                      </View>
                      <View className={styles.metaItem}>
                        <Text>🗓️</Text>
                        <Text>{formatRelativeTime(region.createdAt)}</Text>
                      </View>
                    </View>
                  </View>
                  <View className={classnames(styles.statusBadge, statusConfig.className)}>
                    <Text>{statusConfig.icon} {statusConfig.text}</Text>
                  </View>
                </View>

                {region.status !== 'completed' && (
                  <View className={styles.progressSection}>
                    <View className={styles.progressBar}>
                      <View className={styles.progressFill} style={{ width: `${progress}%` }} />
                    </View>
                    <View className={styles.progressInfo}>
                      <Text>进度: {progress}%</Text>
                      <Text>{region.downloadedTiles.toLocaleString()} / {region.totalTiles.toLocaleString()} 瓦片</Text>
                    </View>
                  </View>
                )}

                {region.completedAt && (
                  <View className={styles.progressInfo} style={{ marginBottom: 0 }}>
                    <Text></Text>
                    <Text>完成于 {formatDate(region.completedAt)}</Text>
                  </View>
                )}

                <View className={styles.regionActions}>
                  {region.status === 'downloading' && (
                    <View
                      className={classnames(styles.regionBtn, styles.secondary)}
                      onClick={() => handlePause(region.id)}
                    >
                      <Text>⏸️ 暂停</Text>
                    </View>
                  )}
                  {region.status === 'paused' && (
                    <View
                      className={classnames(styles.regionBtn, styles.primary)}
                      onClick={() => handleResume(region.id)}
                    >
                      <Text>▶️ 继续</Text>
                    </View>
                  )}
                  {region.status === 'failed' && (
                    <View
                      className={classnames(styles.regionBtn, styles.primary)}
                      onClick={() => handleDownload(region)}
                    >
                      <Text>🔄 重试</Text>
                    </View>
                  )}
                  {region.status === 'completed' && (
                    <View
                      className={classnames(styles.regionBtn, styles.secondary, styles.disabled)}
                    >
                      <Text>✅ 已完成</Text>
                    </View>
                  )}
                  <View
                    className={classnames(styles.regionBtn, styles.danger)}
                    onClick={() => handleDelete(region)}
                  >
                    <Text>🗑️ 删除</Text>
                  </View>
                </View>
              </View>
            );
          })
        ) : (
          <View className={styles.emptyState}>
            <Text className={styles.emptyIcon}>🗺️</Text>
            <Text className={styles.emptyTitle}>暂无缓存区域</Text>
            <Text className={styles.emptyDesc}>
              点击上方"新增"按钮下载地图缓存，便于在无网络时使用
            </Text>
          </View>
        )}

        <View className={styles.newRegionCard} onClick={handleNewRegion}>
          <Text className={styles.newRegionIcon}>➕</Text>
          <Text className={styles.newRegionText}>添加新的缓存区域</Text>
        </View>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>
          <Text className={styles.sectionIcon}>⚙️</Text>
          缓存设置
        </Text>
        <View className={styles.settingsCard}>
          <View className={styles.settingRow}>
            <View className={styles.settingInfo}>
              <Text className={styles.settingLabel}>启用地图缓存</Text>
              <Text className={styles.settingDesc}>开启后可下载离线地图</Text>
            </View>
            <View
              className={classnames(styles.switch, {
                [styles.active]: settings.enableMapCache
              })}
              onClick={handleToggleMapCache}
            >
              <View className={styles.switchKnob} />
            </View>
          </View>

          <View className={styles.settingRow}>
            <View className={styles.settingInfo}>
              <Text className={styles.settingLabel}>默认缩放级别</Text>
              <Text className={styles.settingDesc}>新缓存区域的缩放范围</Text>
            </View>
            <View className={styles.zoomSelector}>
              <View
                className={classnames(styles.zoomBtn, {
                  [styles.disabled]: minZoom <= 1
                })}
                onClick={() => handleZoomChange('min', -1)}
              >
                <Text>-</Text>
              </View>
              <Text className={styles.zoomValue}>{minZoom}</Text>
              <Text style={{ fontSize: '24rpx', color: '#86909C' }}>-</Text>
              <Text className={styles.zoomValue}>{maxZoom}</Text>
              <View
                className={classnames(styles.zoomBtn, {
                  [styles.disabled]: maxZoom >= 20
                })}
                onClick={() => handleZoomChange('max', 1)}
              >
                <Text>+</Text>
              </View>
            </View>
          </View>

          <View className={styles.settingRow}>
            <View className={styles.settingInfo}>
              <Text className={styles.settingLabel}>最大缓存大小</Text>
              <Text className={styles.settingDesc}>超过后自动清理最早的缓存</Text>
            </View>
            <Text className={styles.settingLabel}>{formatFileSize(settings.maxCacheSize)}</Text>
          </View>
        </View>
      </View>

      {regions.length > 0 && (
        <View className={styles.clearCacheBtn} onClick={handleClearAll}>
          <Text className={styles.clearCacheIcon}>🗑️</Text>
          <Text className={styles.clearCacheText}>清理所有地图缓存</Text>
        </View>
      )}
    </View>
  );
};

export default MapCachePage;
