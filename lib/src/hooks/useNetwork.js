"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useNetwork = void 0;
const react_1 = require("react");
const taro_1 = require("@tarojs/taro");
const useSyncStore_1 = require("@/store/useSyncStore");
const useNetwork = () => {
    const { isOnline, networkType, setNetworkStatus } = (0, useSyncStore_1.useSyncStore)();
    const [isChecking, setIsChecking] = (0, react_1.useState)(false);
    const checkNetwork = (0, react_1.useCallback)(async () => {
        setIsChecking(true);
        try {
            const res = await (0, taro_1.getNetworkType)();
            const online = res.networkType !== 'none';
            setNetworkStatus(online, res.networkType);
            console.log('[useNetwork] 网络检查结果:', { online, type: res.networkType });
            return online;
        }
        catch (error) {
            console.error('[useNetwork] 检查网络失败:', error);
            setNetworkStatus(false, 'unknown');
            return false;
        }
        finally {
            setIsChecking(false);
        }
    }, [setNetworkStatus]);
    (0, react_1.useEffect)(() => {
        checkNetwork();
        const unsubscribe = (0, taro_1.onNetworkStatusChange)((res) => {
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
exports.useNetwork = useNetwork;
//# sourceMappingURL=useNetwork.js.map