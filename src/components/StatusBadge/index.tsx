import React from 'react';
import { View, Text } from '@tarojs/components';
import classnames from 'classnames';
import styles from './index.module.scss';
import type { SyncStatus } from '@/types/event';
import { SYNC_STATUS_OPTIONS } from '@/types/event';

interface StatusBadgeProps {
  status: SyncStatus;
  size?: 'small' | 'medium' | 'large';
  showText?: boolean;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  size = 'medium',
  showText = true
}) => {
  const statusConfig = SYNC_STATUS_OPTIONS.find(opt => opt.code === status) || SYNC_STATUS_OPTIONS[0];

  const getStatusIcon = () => {
    switch (status) {
      case 'pending':
        return '⏳';
      case 'syncing':
        return '🔄';
      case 'synced':
        return '✓';
      case 'failed':
        return '✕';
      default:
        return '';
    }
  };

  return (
    <View
      className={classnames(
        styles.statusBadge,
        styles[status],
        styles[size]
      )}
    >
      <Text className={styles.icon}>{getStatusIcon()}</Text>
      {showText && (
        <Text className={styles.text}>{statusConfig.name}</Text>
      )}
    </View>
  );
};

export default StatusBadge;
