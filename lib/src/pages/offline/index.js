"use strict";
var _a;
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const taro_1 = require("@tarojs/taro");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useSync_1 = require("@/hooks/useSync");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const useSyncStore_1 = require("@/store/useSyncStore");
const FILTER_TABS = [
    { key: 'all', label: '全部' },
    { key: 'pending', label: '待同步' },
    { key: 'synced', label: '已同步' },
    { key: 'failed', label: '同步失败' }
];
const OfflinePage = () => {
    const router = (0, taro_1.useRouter)();
    const { events, deleteEvent, formatDate, formatRelativeTime } = (0, useOfflineDB_1.useOfflineDB)();
    const { isSyncing, syncProgress, syncAll, syncSingle, cancelSync } = (0, useSync_1.useSync)();
    const { refreshAll } = (0, useOfflineStore_1.useOfflineStore)();
    const { setPendingCount, setFailedCount } = (0, useSyncStore_1.useSyncStore)();
    const [activeFilter, setActiveFilter] = (0, react_1.useState)('all');
    const [selectedIds, setSelectedIds] = (0, react_1.useState)(new Set());
    const [isBatchMode, setIsBatchMode] = (0, react_1.useState)(false);
    (0, taro_1.useDidShow)(() => {
        console.log('[OfflinePage] 页面显示');
        refreshAll();
        updateCounts();
        const highlightId = router.params.highlightId;
        if (highlightId) {
            console.log('[OfflinePage] 高亮事件:', highlightId);
        }
    });
    (0, taro_1.usePullDownRefresh)(async () => {
        console.log('[OfflinePage] 下拉刷新');
        await refreshAll();
        updateCounts();
        taro_1.default.stopPullDownRefresh();
        taro_1.default.showToast({
            title: '刷新成功',
            icon: 'success'
        });
    });
    const updateCounts = (0, react_1.useCallback)(() => {
        const pending = events.filter(e => e.syncStatus === 'pending' || e.syncStatus === 'failed').length;
        const failed = events.filter(e => e.syncStatus === 'failed').length;
        setPendingCount(pending);
        setFailedCount(failed);
    }, [events, setPendingCount, setFailedCount]);
    (0, react_1.useEffect)(() => {
        updateCounts();
    }, [events, updateCounts]);
    const filteredEvents = (0, react_1.useMemo)(() => {
        if (activeFilter === 'all')
            return events;
        return events.filter(e => e.syncStatus === activeFilter);
    }, [events, activeFilter]);
    const getFilterCount = (filter) => {
        if (filter === 'all')
            return events.length;
        return events.filter(e => e.syncStatus === filter).length;
    };
    const handleSelectAll = () => {
        if (selectedIds.size === filteredEvents.length) {
            setSelectedIds(new Set());
        }
        else {
            setSelectedIds(new Set(filteredEvents.map(e => e.clientId)));
        }
    };
    const handleSelectItem = (clientId) => {
        const newSelected = new Set(selectedIds);
        if (newSelected.has(clientId)) {
            newSelected.delete(clientId);
        }
        else {
            newSelected.add(clientId);
        }
        setSelectedIds(newSelected);
    };
    const handleSyncSelected = async () => {
        const selectedEvents = events.filter(e => selectedIds.has(e.clientId));
        if (selectedEvents.length === 0) {
            taro_1.default.showToast({
                title: '请选择要同步的事件',
                icon: 'none'
            });
            return;
        }
        console.log('[OfflinePage] 批量同步:', selectedEvents.length);
        await syncAll(selectedEvents);
        setSelectedIds(new Set());
        setIsBatchMode(false);
    };
    const handleDeleteSelected = async () => {
        if (selectedIds.size === 0) {
            taro_1.default.showToast({
                title: '请选择要删除的事件',
                icon: 'none'
            });
            return;
        }
        const res = await taro_1.default.showModal({
            title: '确认删除',
            content: `确定要删除选中的 ${selectedIds.size} 条记录吗？',
      confirmText: '删除',
      confirmColor: '#F53F3F'
    });

    if (res.confirm) {
      for (const id of selectedIds) {
        await deleteEvent(id);
      }
      setSelectedIds(new Set());
      setIsBatchMode(false);
      Taro.showToast({
        title: '删除成功',
        icon: 'success'
      });
    }
  };

  const handleSyncSingle = useCallback(async (event: OfflineEvent) => {
    console.log('[OfflinePage] 同步单条事件:', event.clientId);
    await syncSingle(event);
  }, [syncSingle]);

  const handleDeleteSingle = useCallback(async (clientId: string) => {
    deleteEvent(clientId);
  }, [deleteEvent]);

  const handleSyncAllPending = async () => {
    console.log('[OfflinePage] 一键同步所有待同步');
    syncAll();
  };

  const handleClearSynced = async () => {
    const syncedEvents = events.filter(e => e.syncStatus === 'synced');
    if (syncedEvents.length === 0) {
      Taro.showToast({
        title: '没有可清理',
        icon: 'none'
      });
      return;
    }

    const res = await Taro.showModal({
      title: '清理已同步记录',
      content: `, 确定要清理所有已同步的, $
        }, { syncedEvents, : .length }, 条记录吗, ',, confirmText, '清理', confirmColor, '#F53F3F');
    };
    if (res.confirm) {
        for (const event of syncedEvents) {
            await deleteEvent(event.clientId);
        }
        taro_1.default.showToast({
            title: '清理成功',
            icon: 'success'
        });
    }
};
return ((0, jsx_runtime_1.jsxs)(components_1.ScrollView, { className: index_module_scss_1.default.offlinePage, scrollY: true, enhanced: true, showScrollbar: false, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.filterBar, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.filterTabs, children: FILTER_TABS.map(tab => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.filterTab, {
                            [index_module_scss_1.default.active]: activeFilter === tab.key
                        }), onClick: () => setActiveFilter(tab.key), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.filterTabText, children: tab.label }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.filterTabCount, children: getFilterCount(tab.key) })] }, tab.key))) }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionBar, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.primaryAction), onClick: handleSyncAllPending, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionBtnText, children: "\u4E00\u952E\u540C\u6B65" }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.secondaryAction), onClick: () => setIsBatchMode(!isBatchMode), children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionBtnText, children: isBatchMode ? '取消' : '批量管理' }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.dangerAction), onClick: handleClearSynced, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionBtnText, children: "\u6E05\u7406\u5DF2\u540C\u6B65" }) })] })] }), isSyncing && syncProgress && ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.syncProgressSection, children: (0, jsx_runtime_1.jsx)(SyncProgress_1.default, { progress: syncProgress, onCancel: cancelSync }) })), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.eventList, children: filteredEvents.length > 0 ? (filteredEvents.map(event => ((0, jsx_runtime_1.jsxs)(components_1.View, { children: [isBatchMode && ((0, jsx_runtime_1.jsx)(components_1.View, { onClick: () => handleSelectItem(event.clientId), children: (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.checkbox, {
                                [index_module_scss_1.default.checked]: selectedIds.has(event.clientId)
                            }), children: selectedIds.has(event.clientId) && ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.checkmark, children: "\u2713" })) }) })), (0, jsx_runtime_1.jsx)(EventCard_1.default, { event: event, showSyncButton: !isBatchMode && (event.syncStatus === 'pending' || event.syncStatus === 'failed'), onSync: () => handleSyncSingle(event), onDelete: () => handleDeleteSingle(event.clientId) })] }, event.clientId)))) : ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.emptyState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyIcon, children: "\uD83D\uDCE6" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyTitle, children: "\u6682\u65E0\u6570\u636E" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyDesc, children: activeFilter === 'all'
                            ? '还没有本地事件记录'
                            : `没有${(_a = FILTER_TABS.find(t => t.key === activeFilter)) === null || _a === void 0 ? void 0 : _a.label}记录` })] })) }), isBatchMode && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.fixedBottom, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.batchSelectBar, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.selectAll, onClick: handleSelectAll, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.checkbox, {
                                        [index_module_scss_1.default.checked]: selectedIds.size === filteredEvents.length && filteredEvents.length > 0
                                    }), children: selectedIds.size === filteredEvents.length && filteredEvents.length > 0 && ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.checkmark, children: "\u2713" })) }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.selectAllText, children: "\u5168\u9009" })] }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.selectedCount, children: ["\u5DF2\u9009\u62E9 ", selectedIds.size, " \u9879"] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.batchActions, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.batchBtn, index_module_scss_1.default.batchSyncBtn, {
                                [index_module_scss_1.default.disabled]: selectedIds.size === 0
                            }), onClick: handleSyncSelected, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.batchBtnText, children: "\u6279\u91CF\u540C\u6B65" }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.batchBtn, index_module_scss_1.default.batchDeleteBtn, {
                                [index_module_scss_1.default.disabled]: selectedIds.size === 0
                            }), onClick: handleDeleteSelected, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.batchBtnText, children: "\u6279\u91CF\u5220\u9664" }) })] })] }))] }));
;
exports.default = OfflinePage;
//# sourceMappingURL=index.js.map