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
const SyncProgress_1 = require("@/components/SyncProgress");
const EventCard_1 = require("@/components/EventCard");
const useNetwork_1 = require("@/hooks/useNetwork");
const useSync_1 = require("@/hooks/useSync");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useSyncStore_1 = require("@/store/useSyncStore");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const HomePage = () => {
    const { isOnline, networkType } = (0, useNetwork_1.useNetwork)();
    const { isSyncing, syncProgress, syncAll, cancelSync, pendingCount } = (0, useSync_1.useSync)();
    const { pendingEvents, formatRelativeTime, getPendingCount } = (0, useOfflineDB_1.useOfflineDB)();
    const { failedCount, showRedDot } = (0, useSyncStore_1.useSyncStore)();
    const { refreshAll, storageStats } = (0, useOfflineStore_1.useOfflineStore)();
    (0, taro_1.useDidShow)(() => {
        console.log('[HomePage] 页面显示，刷新数据');
        refreshAll();
        getPendingCount();
    });
    (0, taro_1.usePullDownRefresh)(async () => {
        console.log('[HomePage] 下拉刷新');
        await refreshAll();
        getPendingCount();
        taro_1.default.stopPullDownRefresh();
        taro_1.default.showToast({
            title: '刷新成功',
            icon: 'success'
        });
    });
    (0, react_1.useEffect)(() => {
        refreshAll();
        getPendingCount();
    }, [refreshAll, getPendingCount]);
    const handleSyncAll = (0, react_1.useCallback)(async () => {
        console.log('[HomePage] 点击一键同步');
        await syncAll();
    }, [syncAll]);
    const handleNavigateTo = (0, react_1.useCallback)((url) => {
        console.log('[HomePage] 跳转到:', url);
        taro_1.default.navigateTo({ url });
    }, []);
    const handleSwitchTab = (0, react_1.useCallback)((url) => {
        console.log('[HomePage] 切换到Tab:', url);
        taro_1.default.switchTab({ url });
    }, []);
    const handleSyncSingle = (0, react_1.useCallback)(async (event) => {
        console.log('[HomePage] 同步单条事件:', event.clientId);
        await taro_1.default.navigateTo({
            url: `/pages/offline/index?highlightId=${event.clientId}`
        });
    }, []);
    const handleDeleteEvent = (0, react_1.useCallback)(async (clientId) => {
        console.log('[HomePage] 删除事件:', clientId);
    }, []);
    const previewEvents = pendingEvents.slice(0, 3);
    return ((0, jsx_runtime_1.jsxs)(components_1.ScrollView, { className: index_module_scss_1.default.homePage, scrollY: true, enhanced: true, showScrollbar: false, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.header, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.headerTop, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.greeting, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.greetingTitle, children: "\u60A8\u597D\uFF0C\u7F51\u683C\u5458" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.greetingSubtitle, children: [formatRelativeTime(Date.now() - 3600000), " \u5F00\u59CB\u4ECA\u65E5\u5DE1\u67E5"] })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.statusBar, children: (0, jsx_runtime_1.jsx)(NetworkStatus_1.default, { isOnline: isOnline, networkType: networkType, size: "small" }) })] }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.syncStatusCard, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncStatusRow, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncStats, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.statNumber, index_module_scss_1.default.pending), children: pendingCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u5F85\u540C\u6B65" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.statNumber, index_module_scss_1.default.failed), children: failedCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u540C\u6B65\u5931\u8D25" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statNumber, children: storageStats.eventCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u672C\u5730\u603B\u6570" })] })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)('primaryBtn', {
                                [index_module_scss_1.default.syncingBtn]: isSyncing
                            }), onClick: handleSyncAll, children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: isSyncing ? '同步中...' : '一键同步' }) })] }) }), isSyncing && syncProgress && ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.syncProgressContainer, children: (0, jsx_runtime_1.jsx)(SyncProgress_1.default, { progress: syncProgress, onCancel: cancelSync }) })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.mapSection, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: "\u6211\u7684\u7F51\u683C" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.mapContainer, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.mapGridInfo, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.gridName, children: "\uD83D\uDCCD \u548C\u5E73\u91CC\u7B2C\u4E00\u7F51\u683C" }) }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.mapPlaceholder, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.mapIcon, children: "\uD83D\uDDFA\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.mapText, children: "\u7F51\u683C\u5730\u56FE" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.mapSubText, children: isOnline ? '加载在线地图中...' : '使用离线地图缓存' })] })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.quickActions, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: "\u5FEB\u6377\u64CD\u4F5C" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionGrid, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionItem, onClick: () => handleSwitchTab('/pages/report/index'), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionIcon, children: "\uD83D\uDCDD" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionText, children: "\u5FEB\u901F\u4E0A\u62A5" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionItem, onClick: () => handleSwitchTab('/pages/offline/index'), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionIcon, children: "\uD83D\uDCE6" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionText, children: "\u672C\u5730\u4E8B\u4EF6" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.redDotWrapper, children: (0, jsx_runtime_1.jsx)(RedDot_1.default, { show: showRedDot, size: "small" }) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionItem, onClick: () => handleNavigateTo('/pages/map-cache/index'), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionIcon, children: "\uD83D\uDDFA\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionText, children: "\u5730\u56FE\u7F13\u5B58" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionItem, onClick: () => handleNavigateTo('/pages/sync-log/index'), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionIcon, children: "\uD83D\uDCCB" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionText, children: "\u540C\u6B65\u8BB0\u5F55" })] })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.pendingPreview, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.previewHeader, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: "\u5F85\u540C\u6B65\u4E8B\u4EF6" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.viewAllBtn, onClick: () => handleSwitchTab('/pages/offline/index'), children: "\u67E5\u770B\u5168\u90E8 \u2192" })] }), previewEvents.length > 0 ? ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.previewList, children: previewEvents.map(event => ((0, jsx_runtime_1.jsx)(EventCard_1.default, { event: event, showSyncButton: true, onSync: () => handleSyncSingle(event), onDelete: () => handleDeleteEvent(event.clientId) }, event.clientId))) })) : ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.emptyState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyIcon, children: "\u2705" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyText, children: "\u6682\u65E0\u5F85\u540C\u6B65\u4E8B\u4EF6" })] }))] })] }));
};
exports.default = HomePage;
//# sourceMappingURL=index.js.map