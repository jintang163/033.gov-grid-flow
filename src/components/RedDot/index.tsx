import React from 'react';
import { View, Text } from '@tarojs/components';
import classnames from 'classnames';
import styles from './index.module.scss';

interface RedDotProps {
  count?: number;
  show?: boolean;
  size?: 'small' | 'medium' | 'large';
  maxCount?: number;
}

const RedDot: React.FC<RedDotProps> = ({
  count,
  show = false,
  size = 'medium',
  maxCount = 99
}) => {
  if (!show && (count === undefined || count <= 0)) {
    return null;
  }

  const displayCount = count !== undefined && count > maxCount
    ? `${maxCount}+`
    : count;

  const hasNumber = count !== undefined && count > 0;

  return (
    <View
      className={classnames(
        styles.redDot,
        styles[size],
        hasNumber ? styles.withNumber : styles.withoutNumber
      )}
    >
      {hasNumber && (
        <Text className={styles.count}>{displayCount}</Text>
      )}
    </View>
  );
};

export default RedDot;
