import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, Input, Textarea, Checkbox } from '@tarojs/components';
import Taro, { getLocation, useDidShow } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import NetworkStatus from '@/components/NetworkStatus';
import ImageUploader, { ImageItem } from '@/components/ImageUploader';
import { useNetwork } from '@/hooks/useNetwork';
import { useOfflineDB } from '@/hooks/useOfflineDB';
import { useSync } from '@/hooks/useSync';
import { reportEvent, uploadImage } from '@/services/event';
import { EVENT_TYPE_OPTIONS, PRIORITY_OPTIONS } from '@/types/event';
import type { EventType, EventPriority, LocationInfo } from '@/types/event';

const ReportPage: React.FC = () => {
  const { isOnline, networkType } = useNetwork();
  const { createEvent } = useOfflineDB();
  const { syncSingle } = useSync();

  const [eventType, setEventType] = useState<EventType | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<EventPriority>('medium');
  const [images, setImages] = useState<ImageItem[]>([]);
  const [location, setLocation] = useState<LocationInfo | null>(null);
  const [isLocationLoading, setIsLocationLoading] = useState(false);
  const [anonymous, setAnonymous] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useDidShow(() => {
    if (!location) {
      getCurrentLocation();
    }
  });

  const getCurrentLocation = useCallback(async () => {
    setIsLocationLoading(true);
    try {
      const res = await getLocation({
        type: 'gcj02',
        isHighAccuracy: true
      });
      console.log('[ReportPage] 获取位置成功:', res);
      setLocation({
        longitude: res.longitude,
        latitude: res.latitude,
        address: '正在解析地址...',
        gridCode: '110101001001',
        gridName: '和平里第一网格'
      });

      setTimeout(() => {
        setLocation(prev => prev ? {
          ...prev,
          address: '北京市东城区和平里街道和平里小区'
        } : null);
      }, 1000);
    } catch (error) {
      console.error('[ReportPage] 获取位置失败:', error);
      Taro.showToast({
        title: '获取位置失败，请手动选择',
        icon: 'none'
      });
      setLocation({
        longitude: 116.397,
        latitude: 39.908,
        address: '北京市东城区和平里街道',
        gridCode: '110101001001',
        gridName: '和平里第一网格'
      });
    } finally {
      setIsLocationLoading(false);
    }
  }, []);

  const isFormValid = eventType && title.trim() && description.trim() && location;

  const handleSubmit = useCallback(async () => {
    if (!isFormValid) {
      Taro.showToast({
        title: '请填写完整信息',
        icon: 'none'
      });
      return;
    }

    if (isSubmitting) return;

    setIsSubmitting(true);
    console.log('[ReportPage] 提交事件:', { eventType, title, description, priority, isOnline });

    try {
      if (isOnline) {
        Taro.showLoading({ title: '正在上报...', mask: true });

        const uploadedImages = await Promise.all(
          images.map(async (img) => {
            try {
              const remoteUrl = await uploadImage(img.path);
              return remoteUrl;
            } catch (error) {
              console.error('[ReportPage] 图片上传失败:', error);
              throw new Error('图片上传失败');
            }
          })
        );

        const result = await reportEvent({
          type: eventType as EventType,
          title: title.trim(),
          description: description.trim(),
          priority,
          imageUrls: uploadedImages,
          longitude: location!.longitude,
          latitude: location!.latitude,
          address: location!.address,
          anonymous: anonymous ? 1 : 0
        });

        Taro.hideLoading();
        console.log('[ReportPage] 在线上报成功:', result);

        Taro.showModal({
          title: '上报成功',
          content: '事件已成功上报，是否继续上报？',
          confirmText: '继续上报',
          cancelText: '返回首页',
          success: (res) => {
            if (res.confirm) {
              resetForm();
            } else {
              Taro.switchTab({ url: '/pages/home/index' });
            }
          }
        });
      } else {
        const confirmRes = await Taro.showModal({
          title: '离线模式',
          content: '当前无网络连接，事件将保存到本地，网络恢复后可同步上传。是否继续？',
          confirmText: '保存到本地',
          cancelText: '取消'
        });

        if (!confirmRes.confirm) {
          setIsSubmitting(false);
          return;
        }

        const event = await createEvent({
          type: eventType as EventType,
          title: title.trim(),
          description: description.trim(),
          priority,
          images: images.map(img => ({ path: img.path, size: img.size })),
          location: location!,
          reporterId: '1',
          reporterName: '张三',
          anonymous: anonymous ? 1 : 0
        });

        console.log('[ReportPage] 离线保存成功:', event.clientId);

        Taro.showModal({
          title: '已保存到本地',
          content: '事件已保存到本地，网络恢复后可在"本地事件"中同步。',
          confirmText: '查看本地事件',
          cancelText: '继续上报',
          success: (res) => {
            if (res.confirm) {
              Taro.switchTab({ url: '/pages/offline/index' });
            } else {
              resetForm();
            }
          }
        });
      }
    } catch (error) {
      console.error('[ReportPage] 提交失败:', error);
      Taro.hideLoading();

      const errorMsg = error instanceof Error ? error.message : '提交失败';

      if (isOnline) {
        Taro.showModal({
          title: '上报失败',
          content: `${errorMsg}，是否保存到本地待稍后同步？`,
          confirmText: '保存到本地',
          cancelText: '取消',
          success: async (res) => {
            if (res.confirm) {
              await createEvent({
                type: eventType as EventType,
                title: title.trim(),
                description: description.trim(),
                priority,
                images: images.map(img => ({ path: img.path, size: img.size })),
                location: location!,
                reporterId: '1',
                reporterName: '张三',
                anonymous: anonymous ? 1 : 0
              });
              Taro.showToast({
                title: '已保存到本地',
                icon: 'success'
              });
              resetForm();
            }
          }
        });
      } else {
        Taro.showToast({
          title: errorMsg,
          icon: 'none'
        });
      }
    } finally {
      setIsSubmitting(false);
    }
  }, [isFormValid, isSubmitting, isOnline, eventType, title, description, priority, images, location, anonymous, createEvent]);

  const resetForm = () => {
    setEventType('');
    setTitle('');
    setDescription('');
    setPriority('medium');
    setImages([]);
    setAnonymous(false);
  };

  const formatCoords = (lng: number, lat: number) => {
    return `${lng.toFixed(4)}, ${lat.toFixed(4)}`;
  };

  return (
    <View className={styles.reportPage}>
      {!isOnline && (
        <View className={styles.offlineNotice}>
          <Text className={styles.offlineIcon}>📡</Text>
          <Text className={styles.offlineText}>
            当前处于离线模式，提交后将保存到本地
          </Text>
        </View>
      )}

      <View className={styles.formContainer}>
        <View className={styles.formSection}>
          <Text className={styles.sectionLabel}>
            <Text className={styles.required}>*</Text>
            事件类型
          </Text>
          <View className={styles.typeGrid}>
            {EVENT_TYPE_OPTIONS.map(type => (
              <View
                key={type.code}
                className={classnames(styles.typeItem, {
                  [styles.active]: eventType === type.code
                })}
                onClick={() => setEventType(type.code)}
              >
                <Text className={styles.typeIcon}>{type.icon}</Text>
                <Text className={styles.typeName}>{type.name}</Text>
              </View>
            ))}
          </View>
        </View>

        <View className={styles.formSection}>
          <View className={styles.inputWrapper}>
            <Text className={styles.inputLabel}>
              <Text className={styles.required}>*</Text>
              事件标题
            </Text>
            <Input
              className={styles.textInput}
              placeholder="请简要描述事件"
              value={title}
              onInput={(e) => setTitle(e.detail.value)}
              maxlength={50}
            />
            <Text className={styles.wordCount}>{title.length}/50</Text>
          </View>

          <View className={styles.inputWrapper}>
            <Text className={styles.inputLabel}>
              <Text className={styles.required}>*</Text>
              详细描述
            </Text>
            <Textarea
              className={styles.textareaInput}
              placeholder="请详细描述事件情况，包括时间、地点、具体问题等"
              value={description}
              onInput={(e) => setDescription(e.detail.value)}
              maxlength={500}
            />
            <Text className={styles.wordCount}>{description.length}/500</Text>
          </View>

          <View className={styles.inputWrapper}>
            <Text className={styles.inputLabel}>
              <Text className={styles.required}>*</Text>
              优先级
            </Text>
            <View className={styles.priorityRow}>
              {PRIORITY_OPTIONS.map(opt => (
                <View
                  key={opt.code}
                  className={classnames(styles.priorityItem, styles[opt.code], {
                    [styles.active]: priority === opt.code
                  })}
                  onClick={() => setPriority(opt.code as EventPriority)}
                >
                  <Text className={styles.priorityText}>{opt.name}</Text>
                </View>
              ))}
            </View>
          </View>
        </View>

        <View className={styles.formSection}>
          <Text className={styles.sectionLabel}>现场照片</Text>
          <ImageUploader
            images={images}
            onChange={setImages}
            maxCount={9}
          />
        </View>

        <View className={styles.formSection}>
          <Text className={styles.sectionLabel}>
            <Text className={styles.required}>*</Text>
            位置信息
          </Text>
          <View className={styles.locationRow}>
            <Text className={styles.locationIcon}>📍</Text>
            <View className={styles.locationInfo}>
              <Text className={styles.locationAddress}>
                {isLocationLoading ? '定位中...' : location?.address || '未知位置'}
              </Text>
              {location && (
                <Text className={styles.locationCoords}>
                  {formatCoords(location.longitude, location.latitude)}
                </Text>
              )}
            </View>
            <View
              className={styles.locationBtn}
              onClick={getCurrentLocation}
            >
              <Text className={styles.locationBtnText}>
                {isLocationLoading ? '定位中' : '重新定位'}
              </Text>
            </View>
          </View>
        </View>
      </View>

      <View className={styles.bottomBar}>
        <View className={styles.anonymousRow} onClick={() => setAnonymous(!anonymous)}>
          <View
            className={classnames(styles.anonymousCheckbox, {
              [styles.checked]: anonymous
            })}
          >
            {anonymous && <Text className={styles.checkmark}>✓</Text>}
          </View>
          <Text className={styles.anonymousText}>匿名上报</Text>
        </View>
        <View
          className={classnames(styles.submitBtn, {
            [styles.disabled]: !isFormValid || isSubmitting
          })}
          onClick={handleSubmit}
        >
          {isSubmitting ? (
            <Text className={classnames(styles.submitBtnText, styles.loadingText)}>
              <Text className={styles.loadingIcon}>🔄</Text>
              {isOnline ? '上报中...' : '保存中...'}
            </Text>
          ) : (
            <Text className={styles.submitBtnText}>
              {isOnline ? '立即上报' : '保存到本地'}
            </Text>
          )}
        </View>
      </View>
    </View>
  );
};

export default ReportPage;
