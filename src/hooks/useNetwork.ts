import { useEffect, useState, useCallback } from 'react';
import { getNetworkType, onNetworkStatusChange } from '@tarojs/taro';
import { useSyncStore } from '@/store/useSyncStore';

export const useNetwork = () => {
  const { isOnline, networkType, setNetworkStatus } = useSyncStore();
  const [isChecking, setIsChecking] = useState(false);

  const checkNetwork = useCallback(async () => {
    setIsChecking(true);
    try {
      const res = await getNetworkType();
      const online = res.networkType !== 'none';
      setNetworkStatus(online, res.networkType);
      console.log('[useNetwork] 网络检查结果:', { online, type: res.networkType });
      return online;
    } catch (error) {
      console.error('[useNetwork] 检查网络失败:', error);
      setNetworkStatus(false, 'unknown');
      return false;
    } finally {
      setIsChecking(false);
    }
  }, [setNetworkStatus]);

  useEffect(() => {
    checkNetwork();
    const unsubscribe = onNetworkStatusChange((res) => {
      console.log('[useNetwork] 网络状态变化:', res);
      setNetworkStatus(res.isConnected, res.networkType);
    });

    return () => {
      unsubscribe();
    };
  }, [checkNetwork, setNetworkStatus]);

  return {
    isOnline,
    networkType,
    isChecking,
    checkNetwork,
    isWifi: networkType === 'wifi',
    isMobile: networkType === '2g' || networkType === '3g' || networkType === '4g' || networkType === '5g'
  };
};
