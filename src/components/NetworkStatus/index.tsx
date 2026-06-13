import React from 'react';
import { View, Text } from '@tarojs/components';
import classnames from 'classnames';
import styles from './index.module.scss';

interface NetworkStatusProps {
  isOnline: boolean;
  networkType: string;
  size?: 'small' | 'medium' | 'large';
  showText?: boolean;
}

const NetworkStatus: React.FC<NetworkStatusProps> = ({
  isOnline,
  networkType,
  size = 'medium',
  showText = true
}) => {
  const getNetworkIcon = () => {
    if (!isOnline) return '📡';
    if (networkType === 'wifi') return '📶';
    if (networkType === '5g') return '5G';
    if (networkType === '4g') return '4G';
    if (networkType === '3g') return '3G';
    if (networkType === '2g') return '2G';
    return '🌐';
  };

  const getNetworkText = () => {
    if (!isOnline) return '离线';
    if (networkType === 'wifi') return 'Wi-Fi';
    if (networkType === '5g') return '5G网络';
    if (networkType === '4g') return '4G网络';
    if (networkType === '3g') return '3G网络';
    if (networkType === '2g') return '2G网络';
    return '在线';
  };

  return (
    <View
      className={classnames(
        styles.networkStatus,
        styles[size],
        isOnline ? styles.online : styles.offline
      )}
    >
      <Text className={styles.icon}>{getNetworkIcon()}</Text>
      {showText && (
        <Text className={styles.text}>{getNetworkText()}</Text>
      )}
    </View>
  );
};

export default NetworkStatus;
