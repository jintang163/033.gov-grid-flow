"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const react_1 = require("react");
const taro_1 = require("@tarojs/taro");
require("./app.scss");
const useSyncStore_1 = require("@/store/useSyncStore");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const offlineDB_1 = require("@/utils/offlineDB");
function App(props) {
    const { setNetworkStatus } = (0, useSyncStore_1.useSyncStore)();
    const { initStore } = (0, useOfflineStore_1.useOfflineStore)();
    (0, react_1.useEffect)(() => {
        console.log('[App] 初始化应用');
        (0, offlineDB_1.initOfflineDB)();
        initStore();
        checkNetworkStatus();
        const unsubscribe = (0, taro_1.onNetworkStatusChange)((res) => {
            console.log('[App] 网络状态变化:', res);
            setNetworkStatus(res.isConnected, res.networkType);
        });
        return () => {
            console.log('[App] 清理网络监听');
            unsubscribe();
        };
    }, []);
    (0, taro_1.useDidShow)(() => {
        console.log('[App] 应用显示');
        checkNetworkStatus();
    });
    (0, taro_1.useDidHide)(() => {
        console.log('[App] 应用隐藏');
    });
    const checkNetworkStatus = async () => {
        try {
            const res = await (0, taro_1.getNetworkType)();
            console.log('[App] 当前网络状态:', res);
            setNetworkStatus(res.networkType !== 'none', res.networkType);
        }
        catch (error) {
            console.error('[App] 获取网络状态失败:', error);
            setNetworkStatus(false, 'unknown');
        }
    };
    return props.children;
}
exports.default = App;
//# sourceMappingURL=app.js.map