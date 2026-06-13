import React from 'react';
import { View, Text, Progress } from '@tarojs/components';
import classnames from 'classnames';
import styles from './index.module.scss';
import type { SyncProgress as SyncProgressType } from '@/types/sync';

interface SyncProgressProps {
  progress: SyncProgressType;
  showCancel?: boolean;
  onCancel?: () => void;
}

const SyncProgress: React.FC<SyncProgressProps> = ({
  progress,
  showCancel = true,
  onCancel
}) => {
  const percent = progress.total > 0
    ? Math.round(((progress.completed + progress.failed) / progress.total) * 100)
    : 0;

  return (
    <View className={styles.syncProgress}>
      <View className={styles.progressHeader}>
        <View className={styles.progressInfo}>
          <Text className={styles.progressTitle}>正在同步</Text>
          <Text className={styles.progressCount}>
            {progress.completed + progress.failed} / {progress.total}
          </Text>
        </View>
        {showCancel && (
          <View className={styles.cancelBtn} onClick={onCancel}>
            <Text className={styles.cancelText}>取消</Text>
          </View>
        )}
      </View>

      <View className={styles.progressBarContainer}>
        <Progress
          percent={percent}
          strokeWidth={8}
          activeColor="#165DFF"
          backgroundColor="#E5E6EB"
          active
        />
      </View>

      <View className={styles.progressDetails}>
        <View className={styles.detailItem}>
          <View className={classnames(styles.dot, styles.successDot)} />
          <Text className={styles.detailText}>成功 {progress.completed}</Text>
        </View>
        {progress.failed > 0 && (
          <View className={styles.detailItem}>
            <View className={classnames(styles.dot, styles.failedDot)} />
            <Text className={styles.detailText}>失败 {progress.failed}</Text>
          </View>
        )}
        <View className={styles.detailItem}>
          <View className={classnames(styles.dot, styles.pendingDot)} />
          <Text className={styles.detailText}>
            待处理 {progress.total - progress.completed - progress.failed}
          </Text>
        </View>
      </View>

      {progress.currentEvent && (
        <View className={styles.currentEvent}>
          <Text className={styles.currentEventText}>
            当前: {progress.currentEvent.title}
          </Text>
        </View>
      )}
    </View>
  );
};

export default SyncProgress;
