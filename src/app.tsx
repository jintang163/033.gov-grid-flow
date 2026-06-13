import React, { useEffect } from 'react';
import { useDidShow, useDidHide, getNetworkType, onNetworkStatusChange } from '@tarojs/taro';
import './app.scss';
import { useSyncStore } from '@/store/useSyncStore';
import { useOfflineStore } from '@/store/useOfflineStore';
import { initOfflineDB } from '@/utils/offlineDB';

function App(props) {
  const { setNetworkStatus } = useSyncStore();
  const { initStore } = useOfflineStore();

  useEffect(() => {
    console.log('[App] 初始化应用');
    initOfflineDB();
    initStore();
    checkNetworkStatus();
    const unsubscribe = onNetworkStatusChange((res) => {
      console.log('[App] 网络状态变化:', res);
      setNetworkStatus(res.isConnected, res.networkType);
    });
    return () => {
      console.log('[App] 清理网络监听');
      unsubscribe();
    };
  }, []);

  useDidShow(() => {
    console.log('[App] 应用显示');
    checkNetworkStatus();
  });

  useDidHide(() => {
    console.log('[App] 应用隐藏');
  });

  const checkNetworkStatus = async () => {
    try {
      const res = await getNetworkType();
      console.log('[App] 当前网络状态:', res);
      setNetworkStatus(res.networkType !== 'none', res.networkType);
    } catch (error) {
      console.error('[App] 获取网络状态失败:', error);
      setNetworkStatus(false, 'unknown');
    }
  };

  return props.children;
}

export default App;
