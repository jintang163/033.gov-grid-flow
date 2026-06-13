"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const NetworkStatus_1 = require("@/components/NetworkStatus");
const RedDot_1 = require("@/components/RedDot");
const useNetwork_1 = require("@/hooks/useNetwork");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const useSyncStore_1 = require("@/store/useSyncStore");
const MinePage = () => {
    const { isOnline, networkType } = (0, useNetwork_1.useNetwork)();
    const { events, formatFileSize } = (0, useOfflineDB_1.useOfflineDB)();
    const { storageStats, settings, refreshAll, updateSettings } = (0, useOfflineStore_1.useOfflineStore)();
    const { pendingCount, failedCount, showRedDot } = (0, useSyncStore_1.useSyncStore)();
    const [showSettings, setShowSettings] = (0, react_1.useState)(false);
    (0, taro_1.useDidShow)(() => {
        console.log('[MinePage] 页面显示');
        refreshAll();
    });
    (0, taro_1.usePullDownRefresh)(async () => {
        console.log('[MinePage] 下拉刷新');
        await refreshAll();
        taro_1.default.stopPullDownRefresh();
        taro_1.default.showToast({
            title: '刷新成功',
            icon: 'success'
        });
    });
    const handleNavigateTo = (0, react_1.useCallback)((url) => {
        console.log('[MinePage] 跳转到:', url);
        taro_1.default.navigateTo({ url });
    }, []);
    const handleToggleSetting = (0, react_1.useCallback)(async (key) => {
        const newValue = !settings[key];
        console.log('[MinePage] 切换设置:', key, '->', newValue);
        await updateSettings({ [key]: newValue });
    }, [settings, updateSettings]);
    const handleClearCache = (0, react_1.useCallback)(async () => {
        const res = await taro_1.default.showModal({
            title: '清理缓存',
            content: '确定要清理所有本地缓存吗？这将删除所有本地事件、图片和地图缓存。',
            confirmText: '清理',
            confirmColor: '#F53F3F'
        });
        if (res.confirm) {
            taro_1.default.showLoading({ title: '清理中...' });
            try {
                await taro_1.default.clearStorage();
                await refreshAll();
                taro_1.default.hideLoading();
                taro_1.default.showToast({
                    title: '清理成功',
                    icon: 'success'
                });
            }
            catch (error) {
                taro_1.default.hideLoading();
                taro_1.default.showToast({
                    title: '清理失败',
                    icon: 'none'
                });
            }
        }
    }, [refreshAll]);
    const handleLogout = (0, react_1.useCallback)(() => {
        taro_1.default.showModal({
            title: '退出登录',
            content: '确定要退出登录吗？',
            success: (res) => {
                if (res.confirm) {
                    taro_1.default.showToast({
                        title: '已退出登录',
                        icon: 'success'
                    });
                }
            }
        });
    }, []);
    const menuItems = [
        {
            icon: '📋',
            text: '同步记录',
            desc: '查看历史同步记录',
            url: '/pages/sync-log/index',
            showRedDot: false
        },
        {
            icon: '🗺️',
            text: '地图缓存',
            desc: `已缓存 ${formatFileSize(storageStats.mapCacheSize)}`,
            url: '/pages/map-cache/index',
            showRedDot: false
        },
        {
            icon: '📊',
            text: '我的上报',
            desc: `共 ${events.length} 条记录`,
            url: '/pages/offline/index',
            showRedDot: showRedDot
        }
    ];
    const settingItems = [
        { key: 'autoSync', label: '自动同步', desc: '网络恢复后自动同步' },
        { key: 'syncOnlyOnWifi', label: '仅WiFi同步', desc: '节省移动数据流量' },
        { key: 'autoRetry', label: '自动重试', desc: '同步失败后自动重试' },
        { key: 'enableMapCache', label: '地图缓存', desc: '启用离线地图缓存' }
    ];
    return ((0, jsx_runtime_1.jsxs)(components_1.ScrollView, { className: index_module_scss_1.default.minePage, scrollY: true, enhanced: true, showScrollbar: false, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.userHeader, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.userInfo, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.avatar, children: "\uD83D\uDC64" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.userDetails, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.userName, children: "\u5F20\u5EFA\u56FD" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.userGrid, children: "\uD83D\uDCCD \u548C\u5E73\u91CC\u8857\u9053\u7B2C\u4E00\u7F51\u683C" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.userRole, children: "\u7F51\u683C\u5458" })] }), (0, jsx_runtime_1.jsx)(components_1.View, { children: (0, jsx_runtime_1.jsx)(NetworkStatus_1.default, { isOnline: isOnline, networkType: networkType, size: "small" }) })] }) }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statsCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statsTitle, children: "\u672C\u5730\u5B58\u50A8\u7EDF\u8BA1" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statsGrid, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDCDD" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: storageStats.eventCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u4E8B\u4EF6\u6570" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDDBC\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: storageStats.imageCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u56FE\u7247\u6570" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDDFA\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: formatFileSize(storageStats.mapCacheSize) }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u5730\u56FE\u7F13\u5B58" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDCBE" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: formatFileSize(storageStats.totalSize) }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u603B\u8BA1" })] })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.section, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: "\u529F\u80FD\u83DC\u5355" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.menuCard, children: menuItems.map((item, index) => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuItem, onClick: () => handleNavigateTo(item.url), children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.menuIcon, children: item.icon }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuContent, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { style: { display: 'flex', alignItems: 'center', gap: '16rpx' }, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuText, children: item.text }), item.showRedDot && (0, jsx_runtime_1.jsx)(RedDot_1.default, { show: true, size: "small" })] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuDesc, children: item.desc })] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuArrow, children: "\u203A" })] }, index))) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.section, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.menuCard, onClick: () => setShowSettings(!showSettings), children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuItem, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.menuIcon, children: "\u2699\uFE0F" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuContent, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuText, children: "\u540C\u6B65\u8BBE\u7F6E" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.menuDesc, children: ["\u81EA\u52A8\u91CD\u8BD5 ", settings.maxRetryCount, " \u6B21 \u00B7 \u6700\u5927\u7F13\u5B58 ", formatFileSize(settings.maxCacheSize)] })] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuArrow, children: showSettings ? '∧' : '∨' })] }) }), showSettings && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuCard, style: { marginTop: '16rpx' }, children: [settingItems.map((item, index) => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingRow, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.switchLabel, children: (0, jsx_runtime_1.jsxs)(components_1.View, { style: { marginRight: '24rpx' }, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: item.label }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuDesc, children: item.desc })] }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.switch, {
                                            [index_module_scss_1.default.active]: settings[item.key]
                                        }), onClick: () => handleToggleSetting(item.key), children: (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.switchKnob }) })] }, item.key))), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: "\u6700\u5927\u91CD\u8BD5\u6B21\u6570" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.settingValue, children: [settings.maxRetryCount, " \u6B21"] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: "\u6700\u5927\u7F13\u5B58\u5927\u5C0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingValue, children: formatFileSize(settings.maxCacheSize) })] })] }))] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.section, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: "\u6570\u636E\u7BA1\u7406" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.menuCard, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuItem, onClick: handleClearCache, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.menuIcon, style: { background: 'linear-gradient(135deg, #fff1f0 0%, #ffe3e0 100%)' }, children: "\uD83D\uDDD1\uFE0F" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.menuContent, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuText, style: { color: '#F53F3F' }, children: "\u6E05\u7406\u672C\u5730\u7F13\u5B58" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuDesc, children: "\u5220\u9664\u6240\u6709\u672C\u5730\u6570\u636E" })] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.menuArrow, children: "\u203A" })] }) })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.logoutBtn, onClick: handleLogout, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.logoutText, children: "\u9000\u51FA\u767B\u5F55" }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.versionInfo, children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\u653F\u52A1\u7F51\u683C v1.0.0" }) })] }));
};
exports.default = MinePage;
//# sourceMappingURL=index.js.map