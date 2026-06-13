"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const useSync_1 = require("@/hooks/useSync");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const mockLogs = [
    {
        id: 'log-1',
        startTime: Date.now() - 3600000,
        endTime: Date.now() - 3580000,
        totalCount: 5,
        successCount: 5,
        failedCount: 0,
        status: 'success'
    },
    {
        id: 'log-2',
        startTime: Date.now() - 7200000,
        endTime: Date.now() - 7150000,
        totalCount: 8,
        successCount: 6,
        failedCount: 2,
        status: 'partial',
        errorMessage: '网络不稳定，部分数据同步失败'
    },
    {
        id: 'log-3',
        startTime: Date.now() - 86400000,
        endTime: Date.now() - 86350000,
        totalCount: 3,
        successCount: 0,
        failedCount: 3,
        status: 'failed',
        errorMessage: '服务器连接超时'
    },
    {
        id: 'log-4',
        startTime: Date.now() - 86400000 * 2,
        endTime: Date.now() - 86400000 * 2 + 120000,
        totalCount: 12,
        successCount: 12,
        failedCount: 0,
        status: 'success'
    },
    {
        id: 'log-5',
        startTime: Date.now() - 86400000 * 3,
        endTime: Date.now() - 86400000 * 3 + 45000,
        totalCount: 2,
        successCount: 2,
        failedCount: 0,
        status: 'success'
    }
];
const failedEvents = [
    { clientId: 'event-1', title: '路灯损坏报修', error: '图片上传失败：网络超时' },
    { clientId: 'event-2', title: '垃圾清运不及时', error: '服务器返回500错误' }
];
const FILTER_TABS = [
    { key: 'all', label: '全部', icon: '📋' },
    { key: 'success', label: '成功', icon: '✅' },
    { key: 'partial', label: '部分成功', icon: '⚠️' },
    { key: 'failed', label: '失败', icon: '❌' }
];
const SyncLogPage = () => {
    const { syncAll, isSyncing } = (0, useSync_1.useSync)();
    const { formatDate, formatRelativeTime, formatFileSize } = (0, useOfflineDB_1.useOfflineDB)();
    const { syncLogs, refreshAll } = (0, useOfflineStore_1.useOfflineStore)();
    const [logs, setLogs] = (0, react_1.useState)(mockLogs);
    const [activeFilter, setActiveFilter] = (0, react_1.useState)('all');
    const [loading, setLoading] = (0, react_1.useState)(false);
    const [expandedLogId, setExpandedLogId] = (0, react_1.useState)(null);
    const loadData = (0, react_1.useCallback)(async () => {
        setLoading(true);
        try {
            await refreshAll();
            setLogs(mockLogs);
        }
        catch (error) {
            console.error('[SyncLogPage] 加载数据失败:', error);
        }
        finally {
            setLoading(false);
        }
    }, [refreshAll]);
    (0, taro_1.useDidShow)(() => {
        console.log('[SyncLogPage] 页面显示');
        loadData();
    });
    (0, taro_1.usePullDownRefresh)(async () => {
        console.log('[SyncLogPage] 下拉刷新');
        await loadData();
        taro_1.default.stopPullDownRefresh();
        taro_1.default.showToast({
            title: '刷新成功',
            icon: 'success'
        });
    });
    const totalSyncs = logs.length;
    const totalSuccess = logs.filter(l => l.status === 'success').length;
    const totalPartial = logs.filter(l => l.status === 'partial').length;
    const totalFailed = logs.filter(l => l.status === 'failed').length;
    const totalEventsSynced = logs.reduce((sum, l) => sum + l.successCount, 0);
    const filteredLogs = (0, react_1.useMemo)(() => {
        if (activeFilter === 'all')
            return logs;
        return logs.filter(l => l.status === activeFilter);
    }, [logs, activeFilter]);
    const groupedLogs = (0, react_1.useMemo)(() => {
        const groups = {};
        filteredLogs.forEach(log => {
            const date = new Date(log.startTime);
            const dateKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
            if (!groups[dateKey]) {
                groups[dateKey] = [];
            }
            groups[dateKey].push(log);
        });
        return groups;
    }, [filteredLogs]);
    const formatDuration = (start, end) => {
        const diff = end - start;
        if (diff < 60000)
            return `${Math.floor(diff / 1000)}秒`;
        if (diff < 3600000)
            return `${Math.floor(diff / 60000)}分${Math.floor((diff % 60000) / 1000)}秒`;
        return `${Math.floor(diff / 3600000)}小时${Math.floor((diff % 3600000) / 60000)}分`;
    };
    const getStatusConfig = (status) => {
        switch (status) {
            case 'success': return { text: '全部成功', className: index_module_scss_1.default.success, icon: '✅' };
            case 'partial': return { text: '部分成功', className: index_module_scss_1.default.partial, icon: '⚠️' };
            case 'failed': return { text: '全部失败', className: index_module_scss_1.default.failed, icon: '❌' };
            default: return { text: '未知', className: '', icon: '❓' };
        }
    };
    const handleRetryFailed = (0, react_1.useCallback)(async (log) => {
        console.log('[SyncLogPage] 重试失败事件:', log.id);
        if (!isSyncing) {
            await syncAll();
            loadData();
        }
    }, [syncAll, isSyncing, loadData]);
    const handleViewDetails = (0, react_1.useCallback)((log) => {
        console.log('[SyncLogPage] 查看详情:', log.id);
        setExpandedLogId(expandedLogId === log.id ? null : log.id);
    }, [expandedLogId]);
    const handleClearLogs = (0, react_1.useCallback)(async () => {
        if (logs.length === 0) {
            taro_1.default.showToast({
                title: '暂无记录',
                icon: 'none'
            });
            return;
        }
        const res = await taro_1.default.showModal({
            title: '清空记录',
            content: '确定要清空所有同步记录吗？此操作不会影响已同步的数据。',
            confirmText: '清空',
            confirmColor: '#F53F3F'
        });
        if (res.confirm) {
            setLogs([]);
            taro_1.default.showToast({
                title: '已清空',
                icon: 'success'
            });
        }
    }, [logs.length]);
    const handleSyncNow = (0, react_1.useCallback)(async () => {
        console.log('[SyncLogPage] 立即同步');
        if (!isSyncing) {
            await syncAll();
            loadData();
        }
    }, [syncAll, isSyncing, loadData]);
    const formatDateGroup = (dateKey) => {
        const date = new Date(dateKey);
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);
        if (dateKey === `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`) {
            return '今天';
        }
        if (dateKey === `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`) {
            return '昨天';
        }
        return `${date.getMonth() + 1}月${date.getDate()}日`;
    };
    if (loading) {
        return ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.syncLogPage, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.loadingState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingIcon, children: "\u23F3" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingText, children: "\u52A0\u8F7D\u4E2D..." })] }) }));
    }
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncLogPage, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statsHeader, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statsTitle, children: "\uD83D\uDCCB \u540C\u6B65\u8BB0\u5F55\u7EDF\u8BA1" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statsGrid, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDD04" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: totalSyncs }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u603B\u6B21\u6570" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\u2705" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: totalSuccess }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u6210\u529F" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\u26A0\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: totalPartial }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u90E8\u5206" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDCCA" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: totalEventsSynced }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u540C\u6B65\u603B\u6570" })] })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.clearBtn, onClick: handleSyncNow, style: {
                    marginTop: '24rpx',
                    background: 'linear-gradient(135deg, #165dff 0%, #4080ff 100%)',
                    border: 'none'
                }, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.clearIcon, children: "\uD83D\uDD04" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.clearText, style: { color: '#fff' }, children: isSyncing ? '同步中...' : '立即同步所有待同步数据' })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.filterBar, children: FILTER_TABS.map(tab => ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.filterTab, {
                        [index_module_scss_1.default.active]: activeFilter === tab.key
                    }), onClick: () => setActiveFilter(tab.key), children: (0, jsx_runtime_1.jsxs)(components_1.Text, { children: [tab.icon, " ", tab.label] }) }, tab.key))) }), Object.keys(groupedLogs).length > 0 ? ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.logList, children: Object.entries(groupedLogs).map(([dateKey, dateLogs]) => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.dateGroup, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.dateHeader, children: [formatDateGroup(dateKey), " \u00B7 ", dateLogs.length, "\u6761\u8BB0\u5F55"] }), dateLogs.map(log => {
                            const statusConfig = getStatusConfig(log.status);
                            const isExpanded = expandedLogId === log.id;
                            return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.logCard, index_module_scss_1.default[log.status]), children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logHeader, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logStatus, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statusIcon, children: statusConfig.icon }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.statusText, statusConfig.className), children: statusConfig.text })] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.logTime, children: formatRelativeTime(log.startTime) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logStats, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logStatItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.logStatValue, children: log.totalCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.logStatLabel, children: "\u603B\u6570" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logStatItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.logStatValue, index_module_scss_1.default.success), children: log.successCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.logStatLabel, children: "\u6210\u529F" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logStatItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.logStatValue, index_module_scss_1.default.failed), children: log.failedCount }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.logStatLabel, children: "\u5931\u8D25" })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logDuration, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u23F1\uFE0F \u8017\u65F6: ", formatDuration(log.startTime, log.endTime)] }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\uD83D\uDCC5 ", formatDate(log.startTime)] })] }), log.errorMessage && log.status !== 'success' && ((0, jsx_runtime_1.jsx)(components_1.View, { style: {
                                            marginBottom: '24rpx',
                                            padding: '16rpx',
                                            backgroundColor: '#fff7e8',
                                            borderRadius: '12rpx',
                                            fontSize: '24rpx',
                                            color: '#FF7D00'
                                        }, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u26A0\uFE0F ", log.errorMessage] }) })), isExpanded && log.failedCount > 0 && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.failedItems, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.failedTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { children: "\u274C" }), "\u5931\u8D25\u8BE6\u60C5 (", log.failedCount, "\u9879)"] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.failedList, children: failedEvents.map((item, index) => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.failedItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.failedItemIcon, children: "\u26A0\uFE0F" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.failedItemContent, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.failedItemTitle, children: item.title }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.failedItemError, children: item.error })] })] }, index))) })] })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.logActions, children: [log.status !== 'success' && ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.primary, {
                                                    [index_module_scss_1.default.disabled]: isSyncing
                                                }), onClick: () => handleRetryFailed(log), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\uD83D\uDD04 \u91CD\u8BD5\u5931\u8D25" }) })), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.secondary), onClick: () => handleViewDetails(log), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: isExpanded ? '收起详情' : '查看详情' }) })] })] }, log.id));
                        })] }, dateKey))) })) : ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.emptyState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyIcon, children: "\uD83D\uDCED" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyTitle, children: "\u6682\u65E0\u540C\u6B65\u8BB0\u5F55" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyDesc, children: "\u540C\u6B65\u8BB0\u5F55\u5C06\u5728\u8FD9\u91CC\u663E\u793A" })] })), logs.length > 0 && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.clearBtn, onClick: handleClearLogs, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.clearIcon, children: "\uD83D\uDDD1\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.clearText, children: "\u6E05\u7A7A\u6240\u6709\u540C\u6B65\u8BB0\u5F55" })] }))] }));
};
exports.default = SyncLogPage;
//# sourceMappingURL=index.js.map