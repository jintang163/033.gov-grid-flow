import React from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import StatusBadge from '@/components/StatusBadge';
import { EVENT_TYPE_OPTIONS, PRIORITY_OPTIONS } from '@/types/event';
import type { OfflineEvent } from '@/types/event';

interface EventCardProps {
  event: OfflineEvent;
  onClick?: () => void;
  showSyncButton?: boolean;
  onSync?: () => void;
  onDelete?: () => void;
}

const EventCard: React.FC<EventCardProps> = ({
  event,
  onClick,
  showSyncButton = false,
  onSync,
  onDelete
}) => {
  const eventType = EVENT_TYPE_OPTIONS.find(opt => opt.code === event.type);
  const priority = PRIORITY_OPTIONS.find(opt => opt.code === event.priority);

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - timestamp;

    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';

    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
  };

  const handleCardClick = () => {
    if (onClick) {
      onClick();
    } else {
      Taro.navigateTo({
        url: `/pages/detail/index?clientId=${event.clientId}`
      });
    }
  };

  return (
    <View className={styles.eventCard} onClick={handleCardClick}>
      <View className={styles.cardHeader}>
        <View className={styles.typeInfo}>
          <Text className={styles.typeIcon}>{eventType?.icon || '📝'}</Text>
          <Text className={styles.typeName}>{eventType?.name || '其他'}</Text>
        </View>
        <StatusBadge status={event.syncStatus} size="small" />
      </View>

      <View className={styles.cardBody}>
        <Text className={styles.eventTitle}>{event.title}</Text>
        <Text className={styles.eventDesc}>{event.description}</Text>

        {event.images.length > 0 && (
          <View className={styles.imageList}>
            {event.images.slice(0, 3).map((img, index) => (
              <Image
                key={img.id}
                className={styles.thumbnail}
                src={img.localPath}
                mode="aspectFill"
              />
            ))}
            {event.images.length > 3 && (
              <View className={styles.moreImages}>
                <Text className={styles.moreText}>+{event.images.length - 3}</Text>
              </View>
            )}
          </View>
        )}
      </View>

      <View className={styles.cardFooter}>
        <View className={styles.footerLeft}>
          <Text className={styles.location}>📍 {event.location.address}</Text>
        </View>
        <View className={styles.footerRight}>
          {priority && (
            <View
              className={classnames(styles.priorityBadge, styles[event.priority])}
            >
              <Text className={styles.priorityText}>{priority.name}</Text>
            </View>
          )}
          <Text className={styles.time}>{formatTime(event.createdAt)}</Text>
        </View>
      </View>

      {showSyncButton && (event.syncStatus === 'pending' || event.syncStatus === 'failed') && (
        <View className={styles.actionBar}>
          <View
            className={classnames(styles.actionBtn, styles.syncBtn)}
            onClick={(e) => {
              e.stopPropagation();
              onSync?.();
            }}
          >
            <Text className={styles.actionText}>立即同步</Text>
          </View>
          <View
            className={classnames(styles.actionBtn, styles.deleteBtn)}
            onClick={(e) => {
              e.stopPropagation();
              onDelete?.();
            }}
          >
            <Text className={styles.actionText}>删除</Text>
          </View>
        </View>
      )}

      {event.syncError && event.syncStatus === 'failed' && (
        <View className={styles.errorInfo}>
          <Text className={styles.errorText}>❌ {event.syncError}</Text>
        </View>
      )}
    </View>
  );
};

export default EventCard;
