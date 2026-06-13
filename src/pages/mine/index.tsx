import React, { useState, useCallback } from 'react';
import { View, Text, ScrollView } from '@tarojs/components';
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import NetworkStatus from '@/components/NetworkStatus';
import RedDot from '@/components/RedDot';
import { useNetwork } from '@/hooks/useNetwork';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useOfflineStore } from '@/store/useOfflineStore';
import { useSyncStore } from '@/store/useSyncStore';
import type { AppSettings } from '@/types/offline';

const MinePage: React.FC = () => {
  const { isOnline, networkType } = useNetwork();
  const { events, formatFileSize } = useOfflineDB();
  const { storageStats, settings, refreshAll, updateSettings } = useOfflineStore();
  const { pendingCount, failedCount, showRedDot } = useSyncStore();

  const [showSettings, setShowSettings] = useState(false);

  useDidShow(() => {
    console.log('[MinePage] 页面显示');
    refreshAll();
  });

  usePullDownRefresh(async () => {
    console.log('[MinePage] 下拉刷新');
    await refreshAll();
    Taro.stopPullDownRefresh();
    Taro.showToast({
      title: '刷新成功',
      icon: 'success'
    });
  });

  const handleNavigateTo = useCallback((url: string) => {
    console.log('[MinePage] 跳转到:', url);
    Taro.navigateTo({ url });
  }, []);

  const handleToggleSetting = useCallback(async (key: keyof AppSettings) => {
    const newValue = !settings[key];
    console.log('[MinePage] 切换设置:', key, '->', newValue);
    await updateSettings({ [key]: newValue });
  }, [settings, updateSettings]);

  const handleClearCache = useCallback(async () => {
    const res = await Taro.showModal({
      title: '清理缓存',
      content: '确定要清理所有本地缓存吗？这将删除所有本地事件、图片和地图缓存。',
      confirmText: '清理',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      Taro.showLoading({ title: '清理中...' });
      try {
        await Taro.clearStorage();
        await refreshAll();
        Taro.hideLoading();
        Taro.showToast({
          title: '清理成功',
          icon: 'success'
        });
      } catch (error) {
        Taro.hideLoading();
        Taro.showToast({
          title: '清理失败',
          icon: 'none'
        });
      }
    }
  }, [refreshAll]);

  const handleLogout = useCallback(() => {
    Taro.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          Taro.showToast({
            title: '已退出登录',
            icon: 'success'
          });
        }
      }
    });
  }, []);

  const menuItems = [
    {
      icon: '📋',
      text: '同步记录',
      desc: '查看历史同步记录',
      url: '/pages/sync-log/index',
      showRedDot: false
    },
    {
      icon: '🗺️',
      text: '地图缓存',
      desc: `已缓存 ${formatFileSize(storageStats.mapCacheSize)}`,
      url: '/pages/map-cache/index',
      showRedDot: false
    },
    {
      icon: '📊',
      text: '我的上报',
      desc: `共 ${events.length} 条记录`,
      url: '/pages/offline/index',
      showRedDot: showRedDot
    }
  ];

  const settingItems: Array<{ key: keyof AppSettings; label: string; desc: string }> = [
    { key: 'autoSync', label: '自动同步', desc: '网络恢复后自动同步' },
    { key: 'syncOnlyOnWifi', label: '仅WiFi同步', desc: '节省移动数据流量' },
    { key: 'autoRetry', label: '自动重试', desc: '同步失败后自动重试' },
    { key: 'enableMapCache', label: '地图缓存', desc: '启用离线地图缓存' }
  ];

  return (
    <ScrollView
      className={styles.minePage}
      scrollY
      enhanced
      showScrollbar={false}
    >
      <View className={styles.userHeader}>
        <View className={styles.userInfo}>
          <View className={styles.avatar}>👤</View>
          <View className={styles.userDetails}>
            <Text className={styles.userName}>张建国</Text>
            <Text className={styles.userGrid}>📍 和平里街道第一网格</Text>
            <Text className={styles.userRole}>网格员</Text>
          </View>
          <View>
            <NetworkStatus
              isOnline={isOnline}
              networkType={networkType}
              size="small"
            />
          </View>
        </View>
      </View>

      <View className={styles.statsCard}>
        <Text className={styles.statsTitle}>本地存储统计</Text>
        <View className={styles.statsGrid}>
          <View className={styles.statItem}>
            <Text className={styles.statIcon}>📝</Text>
            <Text className={styles.statValue}>{storageStats.eventCount}</Text>
            <Text className={styles.statLabel}>事件数</Text>
          </View>
          <View className={styles.statItem}>
            <Text className={styles.statIcon}>🖼️</Text>
            <Text className={styles.statValue}>{storageStats.imageCount}</Text>
            <Text className={styles.statLabel}>图片数</Text>
          </View>
          <View className={styles.statItem}>
            <Text className={styles.statIcon}>🗺️</Text>
            <Text className={styles.statValue}>{formatFileSize(storageStats.mapCacheSize)}</Text>
            <Text className={styles.statLabel}>地图缓存</Text>
          </View>
          <View className={styles.statItem}>
            <Text className={styles.statIcon}>💾</Text>
            <Text className={styles.statValue}>{formatFileSize(storageStats.totalSize)}</Text>
            <Text className={styles.statLabel}>总计</Text>
          </View>
        </View>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>功能菜单</Text>
        <View className={styles.menuCard}>
          {menuItems.map((item, index) => (
            <View
              key={index}
              className={styles.menuItem}
              onClick={() => handleNavigateTo(item.url)}
            >
              <View className={styles.menuIcon}>{item.icon}</View>
              <View className={styles.menuContent}>
                <View style={{ display: 'flex', alignItems: 'center', gap: '16rpx' }}>
                  <Text className={styles.menuText}>{item.text}</Text>
                  {item.showRedDot && <RedDot show={true} size="small" />}
                </View>
                <Text className={styles.menuDesc}>{item.desc}</Text>
              </View>
              <Text className={styles.menuArrow}>›</Text>
            </View>
          ))}
        </View>
      </View>

      <View className={styles.section}>
        <View
          className={styles.menuCard}
          onClick={() => setShowSettings(!showSettings)}
        >
          <View className={styles.menuItem}>
            <View className={styles.menuIcon}>⚙️</View>
            <View className={styles.menuContent}>
              <Text className={styles.menuText}>同步设置</Text>
              <Text className={styles.menuDesc}>
                自动重试 {settings.maxRetryCount} 次 · 最大缓存 {formatFileSize(settings.maxCacheSize)}
              </Text>
            </View>
            <Text className={styles.menuArrow}>{showSettings ? '∧' : '∨'}</Text>
          </View>
        </View>

        {showSettings && (
          <View className={styles.menuCard} style={{ marginTop: '16rpx' }}>
            {settingItems.map((item, index) => (
              <View key={item.key} className={styles.settingRow}>
                <View className={styles.switchLabel}>
                  <View style={{ marginRight: '24rpx' }}>
                    <Text className={styles.settingLabel}>{item.label}</Text>
                    <Text className={styles.menuDesc}>{item.desc}</Text>
                  </View>
                </View>
                <View
                  className={classnames(styles.switch, {
                    [styles.active]: settings[item.key] as boolean
                  })}
                  onClick={() => handleToggleSetting(item.key)}
                >
                  <View className={styles.switchKnob} />
                </View>
              </View>
            ))}

            <View className={styles.settingRow}>
              <Text className={styles.settingLabel}>最大重试次数</Text>
              <Text className={styles.settingValue}>{settings.maxRetryCount} 次</Text>
            </View>

            <View className={styles.settingRow}>
              <Text className={styles.settingLabel}>最大缓存大小</Text>
              <Text className={styles.settingValue}>{formatFileSize(settings.maxCacheSize)}</Text>
            </View>
          </View>
        )}
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>数据管理</Text>
        <View className={styles.menuCard}>
          <View
            className={styles.menuItem}
            onClick={handleClearCache}
          >
            <View className={styles.menuIcon} style={{ background: 'linear-gradient(135deg, #fff1f0 0%, #ffe3e0 100%)' }}>🗑️</View>
            <View className={styles.menuContent}>
              <Text className={styles.menuText} style={{ color: '#F53F3F' }}>清理本地缓存</Text>
              <Text className={styles.menuDesc}>删除所有本地数据</Text>
            </View>
            <Text className={styles.menuArrow}>›</Text>
          </View>
        </View>
      </View>

      <View className={styles.logoutBtn} onClick={handleLogout}>
        <Text className={styles.logoutText}>退出登录</Text>
      </View>

      <View className={styles.versionInfo}>
        <Text>政务网格 v1.0.0</Text>
      </View>
    </ScrollView>
  );
};

export default MinePage;
