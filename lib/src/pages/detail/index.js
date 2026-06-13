"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const StatusBadge_1 = require("@/components/StatusBadge");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useSync_1 = require("@/hooks/useSync");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const event_1 = require("@/types/event");
const DetailPage = () => {
    const router = (0, taro_1.useRouter)();
    const clientId = router.params.clientId;
    const { getEvent, deleteEvent, formatDate, formatRelativeTime, formatFileSize } = (0, useOfflineDB_1.useOfflineDB)();
    const { syncSingle, isSyncing } = (0, useSync_1.useSync)();
    const { refreshAll } = (0, useOfflineStore_1.useOfflineStore)();
    const [event, setEvent] = (0, react_1.useState)(null);
    const [loading, setLoading] = (0, react_1.useState)(true);
    const loadEvent = (0, react_1.useCallback)(async () => {
        if (!clientId)
            return;
        setLoading(true);
        try {
            const data = await getEvent(clientId);
            setEvent(data);
        }
        catch (error) {
            console.error('[DetailPage] 加载事件失败:', error);
        }
        finally {
            setLoading(false);
        }
    }, [clientId, getEvent]);
    (0, taro_1.useDidShow)(() => {
        console.log('[DetailPage] 页面显示');
        loadEvent();
    });
    (0, taro_1.usePullDownRefresh)(async () => {
        console.log('[DetailPage] 下拉刷新');
        await loadEvent();
        taro_1.default.stopPullDownRefresh();
    });
    (0, react_1.useEffect)(() => {
        loadEvent();
    }, [loadEvent]);
    const handleSync = (0, react_1.useCallback)(async () => {
        if (!event)
            return;
        console.log('[DetailPage] 同步事件:', event.clientId);
        await syncSingle(event);
        await loadEvent();
        await refreshAll();
    }, [event, syncSingle, loadEvent, refreshAll]);
    const handleDelete = (0, react_1.useCallback)(async () => {
        if (!event)
            return;
        await deleteEvent(event.clientId);
        taro_1.default.navigateBack();
    }, [event, deleteEvent]);
    const handlePreviewImage = (0, react_1.useCallback)((current) => {
        if (!event)
            return;
        const urls = event.images.map(img => img.localPath);
        taro_1.default.previewImage({
            current,
            urls
        });
    }, [event]);
    if (loading) {
        return ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.detailPage, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.loadingState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingIcon, children: "\u23F3" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingText, children: "\u52A0\u8F7D\u4E2D..." })] }) }));
    }
    if (!event) {
        return ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.detailPage, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.emptyState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyIcon, children: "\uD83D\uDCED" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyTitle, children: "\u4E8B\u4EF6\u4E0D\u5B58\u5728" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyDesc, children: "\u8BE5\u4E8B\u4EF6\u53EF\u80FD\u5DF2\u88AB\u5220\u9664" })] }) }));
    }
    const eventType = event_1.EVENT_TYPE_OPTIONS.find(opt => opt.code === event.type);
    const priority = event_1.PRIORITY_OPTIONS.find(opt => opt.code === event.priority);
    const syncStatus = event_1.SYNC_STATUS_OPTIONS.find(opt => opt.code === event.syncStatus);
    const getStatusIcon = () => {
        switch (event.syncStatus) {
            case 'pending': return '📦';
            case 'syncing': return '🔄';
            case 'synced': return '✅';
            case 'failed': return '❌';
            default: return '📝';
        }
    };
    const canSync = event.syncStatus === 'pending' || event.syncStatus === 'failed';
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.detailPage, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statusHeader, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statusLeft, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statusIcon, children: getStatusIcon() }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statusInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statusText, children: (syncStatus === null || syncStatus === void 0 ? void 0 : syncStatus.name) || '未知状态' }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statusTime, children: event.syncedAt
                                            ? `同步于 ${formatRelativeTime(event.syncedAt)}`
                                            : `创建于 ${formatRelativeTime(event.createdAt)}` })] })] }), (0, jsx_runtime_1.jsx)(StatusBadge_1.default, { status: event.syncStatus, size: "medium" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.contentSection, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\uD83D\uDCDD" }), "\u57FA\u672C\u4FE1\u606F"] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.basicInfo, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.infoRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.infoLabel, children: "\u4E8B\u4EF6\u7C7B\u578B" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.infoValue, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.typeTag, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { children: (eventType === null || eventType === void 0 ? void 0 : eventType.icon) || '📝' }), (0, jsx_runtime_1.jsx)(components_1.Text, { children: (eventType === null || eventType === void 0 ? void 0 : eventType.name) || '其他' })] }) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.infoRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.infoLabel, children: "\u4F18\u5148\u7EA7" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.infoValue, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.priorityTag, index_module_scss_1.default[event.priority]), children: (priority === null || priority === void 0 ? void 0 : priority.name) || '中' }) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.infoRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.infoLabel, children: "\u4E8B\u4EF6\u6807\u9898" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.infoValue, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.titleText, children: event.title }) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.infoRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.infoLabel, children: "\u4E8B\u4EF6\u63CF\u8FF0" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.infoValue, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.descText, children: event.description }) })] })] })] }), event.images.length > 0 && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.contentSection, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\uD83D\uDDBC\uFE0F" }), "\u73B0\u573A\u7167\u7247 (", event.images.length, "\u5F20)"] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.imageGrid, children: event.images.map((img) => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.imageItem, onClick: () => handlePreviewImage(img.localPath), children: [(0, jsx_runtime_1.jsx)(components_1.Image, { className: index_module_scss_1.default.image, src: img.localPath, mode: "aspectFill" }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.imageUploadStatus, children: img.uploadStatus === 'synced' ? '已上传' :
                                        img.uploadStatus === 'syncing' ? '上传中' :
                                            img.uploadStatus === 'failed' ? '上传失败' : '待上传' })] }, img.id))) })] })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.contentSection, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\uD83D\uDCCD" }), "\u4F4D\u7F6E\u4FE1\u606F"] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.locationCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.locationIcon, children: "\uD83D\uDCCD" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.locationInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.locationAddress, children: event.location.address }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.locationCoords, children: [event.location.gridName && `网格: ${event.location.gridName} · `, "\u5750\u6807: ", event.location.longitude.toFixed(6), ", ", event.location.latitude.toFixed(6)] })] })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.contentSection, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\uD83D\uDC64" }), "\u4E0A\u62A5\u4FE1\u606F"] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaInfo, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.metaIcon, children: "\uD83C\uDD94" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u4E0A\u62A5\u4EBA: ", event.anonymous ? '匿名' : event.reporterName] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.metaIcon, children: "\uD83D\uDCC5" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u521B\u5EFA\u65F6\u95F4: ", formatDate(event.createdAt)] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.metaIcon, children: "\u270F\uFE0F" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u66F4\u65B0\u65F6\u95F4: ", formatDate(event.updatedAt)] })] }), event.serverId && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.metaIcon, children: "\uD83D\uDD17" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u670D\u52A1\u7AEFID: ", event.serverId] })] }))] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncInfo, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\uD83D\uDD04" }), "\u540C\u6B65\u4FE1\u606F"] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncStatusRow, children: [(0, jsx_runtime_1.jsx)(components_1.View, { children: (0, jsx_runtime_1.jsx)(components_1.Text, { style: { fontSize: '28rpx', color: '#86909C' }, children: "\u540C\u6B65\u72B6\u6001" }) }), (0, jsx_runtime_1.jsx)(StatusBadge_1.default, { status: event.syncStatus, size: "medium" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncDetails, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncDetailItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.syncDetailLabel, children: "\u91CD\u8BD5\u6B21\u6570" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.syncDetailValue, children: [event.syncRetryCount, " \u6B21"] })] }), event.syncedAt && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncDetailItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.syncDetailLabel, children: "\u540C\u6B65\u65F6\u95F4" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.syncDetailValue, children: formatDate(event.syncedAt) })] })), event.images.length > 0 && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.syncDetailItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.syncDetailLabel, children: "\u56FE\u7247\u5927\u5C0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.syncDetailValue, children: formatFileSize(event.images.reduce((sum, img) => sum + img.size, 0)) })] }))] }), event.syncError && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.errorBox, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.errorText, children: ["\u274C ", event.syncError] }), event.syncRetryCount > 0 && ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.retryInfo, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u23F1\uFE0F \u5DF2\u81EA\u52A8\u91CD\u8BD5 ", event.syncRetryCount, " \u6B21"] }) }))] }))] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.bottomBar, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.btn, index_module_scss_1.default.btnDanger), onClick: handleDelete, children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\u5220\u9664" }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.btn, index_module_scss_1.default.btnPrimary, {
                            [index_module_scss_1.default.btnDisabled]: !canSync || isSyncing
                        }), onClick: handleSync, children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: isSyncing ? '同步中...' : canSync ? '立即同步' : '已同步' }) })] })] }));
};
exports.default = DetailPage;
//# sourceMappingURL=index.js.map