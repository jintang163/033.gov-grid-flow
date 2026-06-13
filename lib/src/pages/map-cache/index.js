"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const useNetwork_1 = require("@/hooks/useNetwork");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const mockRegions = [
    {
        id: 'region-1',
        name: '和平里街道全域',
        minZoom: 12,
        maxZoom: 18,
        bounds: { west: 116.40, south: 39.92, east: 116.45, north: 39.96 },
        totalTiles: 15680,
        downloadedTiles: 15680,
        totalSize: 256 * 1024 * 1024,
        status: 'completed',
        createdAt: Date.now() - 86400000 * 7,
        completedAt: Date.now() - 86400000 * 6
    },
    {
        id: 'region-2',
        name: '第一网格重点区域',
        minZoom: 14,
        maxZoom: 19,
        bounds: { west: 116.41, south: 39.93, east: 116.43, north: 39.95 },
        totalTiles: 8420,
        downloadedTiles: 5200,
        totalSize: 128 * 1024 * 1024,
        status: 'downloading',
        createdAt: Date.now() - 3600000 * 2
    },
    {
        id: 'region-3',
        name: '偏远山区巡查路线',
        minZoom: 10,
        maxZoom: 16,
        bounds: { west: 116.30, south: 39.80, east: 116.35, north: 39.85 },
        totalTiles: 5400,
        downloadedTiles: 0,
        totalSize: 64 * 1024 * 1024,
        status: 'paused',
        createdAt: Date.now() - 86400000 * 3
    }
];
const MapCachePage = () => {
    const { isOnline, networkType } = (0, useNetwork_1.useNetwork)();
    const { formatFileSize, formatDate, formatRelativeTime } = (0, useOfflineDB_1.useOfflineDB)();
    const { storageStats, mapCacheRegions, settings, refreshAll, updateSettings } = (0, useOfflineStore_1.useOfflineStore)();
    const [regions, setRegions] = (0, react_1.useState)(mockRegions);
    const [minZoom, setMinZoom] = (0, react_1.useState)(12);
    const [maxZoom, setMaxZoom] = (0, react_1.useState)(18);
    const [loading, setLoading] = (0, react_1.useState)(false);
    const loadData = (0, react_1.useCallback)(async () => {
        setLoading(true);
        try {
            await refreshAll();
            setRegions(mockRegions);
        }
        catch (error) {
            console.error('[MapCachePage] 加载数据失败:', error);
        }
        finally {
            setLoading(false);
        }
    }, [refreshAll]);
    (0, taro_1.useDidShow)(() => {
        console.log('[MapCachePage] 页面显示');
        loadData();
    });
    (0, taro_1.usePullDownRefresh)(async () => {
        console.log('[MapCachePage] 下拉刷新');
        await loadData();
        taro_1.default.stopPullDownRefresh();
        taro_1.default.showToast({
            title: '刷新成功',
            icon: 'success'
        });
    });
    (0, react_1.useEffect)(() => {
        loadData();
    }, [loadData]);
    const totalDownloaded = regions.reduce((sum, r) => sum + (r.downloadedTiles / r.totalTiles) * r.totalSize, 0);
    const totalSize = regions.reduce((sum, r) => sum + r.totalSize, 0);
    const totalRegions = regions.length;
    const completedRegions = regions.filter(r => r.status === 'completed').length;
    const handleToggleMapCache = (0, react_1.useCallback)(() => {
        updateSettings({ enableMapCache: !settings.enableMapCache });
    }, [settings.enableMapCache, updateSettings]);
    const handleNewRegion = (0, react_1.useCallback)(() => {
        taro_1.default.showActionSheet({
            itemList: ['选择地图区域', '下载当前视野', '下载常用网格'],
            success: (res) => {
                console.log('[MapCachePage] 选择:', res.tapIndex);
                taro_1.default.showToast({
                    title: '功能开发中',
                    icon: 'none'
                });
            }
        });
    }, []);
    const handleDownload = (0, react_1.useCallback)((region) => {
        console.log('[MapCachePage] 下载区域:', region.id);
        if (!isOnline) {
            taro_1.default.showToast({
                title: '请先连接网络',
                icon: 'none'
            });
            return;
        }
        if (networkType !== 'wifi' && settings.syncOnlyOnWifi) {
            taro_1.default.showModal({
                title: '非WiFi网络',
                content: '当前为移动数据网络，确认继续下载吗？',
                success: (res) => {
                    if (res.confirm) {
                        startDownload(region.id);
                    }
                }
            });
            return;
        }
        startDownload(region.id);
    }, [isOnline, networkType, settings.syncOnlyOnWifi]);
    const startDownload = (0, react_1.useCallback)((regionId) => {
        setRegions(prev => prev.map(r => r.id === regionId ? Object.assign(Object.assign({}, r), { status: 'downloading' }) : r));
        taro_1.default.showToast({
            title: '开始下载',
            icon: 'success'
        });
    }, []);
    const handlePause = (0, react_1.useCallback)((regionId) => {
        console.log('[MapCachePage] 暂停下载:', regionId);
        setRegions(prev => prev.map(r => r.id === regionId ? Object.assign(Object.assign({}, r), { status: 'paused' }) : r));
        taro_1.default.showToast({
            title: '已暂停',
            icon: 'success'
        });
    }, []);
    const handleResume = (0, react_1.useCallback)((regionId) => {
        console.log('[MapCachePage] 继续下载:', regionId);
        handleDownload(regions.find(r => r.id === regionId));
    }, [regions, handleDownload]);
    const handleDelete = (0, react_1.useCallback)(async (region) => {
        const res = await taro_1.default.showModal({
            title: '删除缓存',
            content: `确定要删除"${region.name}"的地图缓存吗？这将释放 ${formatFileSize(region.totalSize)} 空间。`,
            confirmText: '删除',
            confirmColor: '#F53F3F'
        });
        if (res.confirm) {
            setRegions(prev => prev.filter(r => r.id !== region.id));
            taro_1.default.showToast({
                title: '已删除',
                icon: 'success'
            });
        }
    }, [formatFileSize]);
    const handleDownloadAll = (0, react_1.useCallback)(() => {
        const pendingRegions = regions.filter(r => r.status !== 'completed');
        if (pendingRegions.length === 0) {
            taro_1.default.showToast({
                title: '所有区域已完成',
                icon: 'success'
            });
            return;
        }
        pendingRegions.forEach(r => startDownload(r.id));
    }, [regions, startDownload]);
    const handleClearAll = (0, react_1.useCallback)(async () => {
        if (regions.length === 0) {
            taro_1.default.showToast({
                title: '暂无缓存',
                icon: 'none'
            });
            return;
        }
        const res = await taro_1.default.showModal({
            title: '清理所有缓存',
            content: `确定要清理所有地图缓存吗？这将释放 ${formatFileSize(storageStats.mapCacheSize)} 空间。`,
            confirmText: '清理',
            confirmColor: '#F53F3F'
        });
        if (res.confirm) {
            taro_1.default.showLoading({ title: '清理中...' });
            setTimeout(() => {
                setRegions([]);
                taro_1.default.hideLoading();
                taro_1.default.showToast({
                    title: '清理完成',
                    icon: 'success'
                });
            }, 1000);
        }
    }, [regions, storageStats.mapCacheSize, formatFileSize]);
    const getStatusConfig = (status) => {
        switch (status) {
            case 'downloading': return { text: '下载中', className: index_module_scss_1.default.downloading, icon: '⬇️' };
            case 'completed': return { text: '已完成', className: index_module_scss_1.default.completed, icon: '✅' };
            case 'paused': return { text: '已暂停', className: index_module_scss_1.default.paused, icon: '⏸️' };
            case 'failed': return { text: '失败', className: index_module_scss_1.default.failed, icon: '❌' };
            default: return { text: '未知', className: '', icon: '❓' };
        }
    };
    const handleZoomChange = (type, delta) => {
        if (type === 'min') {
            const newVal = Math.max(1, Math.min(maxZoom - 1, minZoom + delta));
            setMinZoom(newVal);
        }
        else {
            const newVal = Math.max(minZoom + 1, Math.min(20, maxZoom + delta));
            setMaxZoom(newVal);
        }
    };
    if (loading) {
        return ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.mapCachePage, children: (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.loadingState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingIcon, children: "\u23F3" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingText, children: "\u52A0\u8F7D\u4E2D..." })] }) }));
    }
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.mapCachePage, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statsHeader, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statsTitle, children: "\uD83D\uDDFA\uFE0F \u79BB\u7EBF\u5730\u56FE\u7F13\u5B58" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statsGrid, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83D\uDCE6" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statValue, children: formatFileSize(totalDownloaded) }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u5DF2\u7F13\u5B58" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\u2705" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.statValue, children: [completedRegions, "/", totalRegions] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u533A\u57DF\u5B8C\u6210" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.statCard, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statIcon, children: "\uD83C\uDFAF" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.statValue, children: [Math.round(totalDownloaded / totalSize * 100), "%"] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.statLabel, children: "\u603B\u8FDB\u5EA6" })] })] })] }), settings.enableMapCache && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.downloadAllBtn, {
                    [index_module_scss_1.default.disabled]: !isOnline
                }), onClick: handleDownloadAll, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.downloadAllIcon, children: "\u2B07\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.downloadAllText, children: isOnline ? '下载所有待缓存区域' : '请连接网络后下载' })] })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.section, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.sectionHeader, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\uD83D\uDCCD" }), "\u7F13\u5B58\u533A\u57DF"] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionBtn, onClick: handleNewRegion, children: "+ \u65B0\u589E" })] }), regions.length > 0 ? (regions.map(region => {
                        const statusConfig = getStatusConfig(region.status);
                        const progress = Math.round(region.downloadedTiles / region.totalTiles * 100);
                        return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.regionCard, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.regionHeader, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.regionInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.regionName, children: region.name }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.regionMeta, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { children: "\uD83D\uDD0D" }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u7F29\u653E ", region.minZoom, "-", region.maxZoom] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { children: "\uD83D\uDCBE" }), (0, jsx_runtime_1.jsx)(components_1.Text, { children: formatFileSize(region.totalSize) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.metaItem, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { children: "\uD83D\uDDD3\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { children: formatRelativeTime(region.createdAt) })] })] })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.statusBadge, statusConfig.className), children: (0, jsx_runtime_1.jsxs)(components_1.Text, { children: [statusConfig.icon, " ", statusConfig.text] }) })] }), region.status !== 'completed' && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.progressSection, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.progressBar, children: (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.progressFill, style: { width: `${progress}%` } }) }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.progressInfo, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u8FDB\u5EA6: ", progress, "%"] }), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: [region.downloadedTiles.toLocaleString(), " / ", region.totalTiles.toLocaleString(), " \u74E6\u7247"] })] })] })), region.completedAt && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.progressInfo, style: { marginBottom: 0 }, children: [(0, jsx_runtime_1.jsx)(components_1.Text, {}), (0, jsx_runtime_1.jsxs)(components_1.Text, { children: ["\u5B8C\u6210\u4E8E ", formatDate(region.completedAt)] })] })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.regionActions, children: [region.status === 'downloading' && ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.regionBtn, index_module_scss_1.default.secondary), onClick: () => handlePause(region.id), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\u23F8\uFE0F \u6682\u505C" }) })), region.status === 'paused' && ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.regionBtn, index_module_scss_1.default.primary), onClick: () => handleResume(region.id), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\u25B6\uFE0F \u7EE7\u7EED" }) })), region.status === 'failed' && ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.regionBtn, index_module_scss_1.default.primary), onClick: () => handleDownload(region), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\uD83D\uDD04 \u91CD\u8BD5" }) })), region.status === 'completed' && ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.regionBtn, index_module_scss_1.default.secondary, index_module_scss_1.default.disabled), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\u2705 \u5DF2\u5B8C\u6210" }) })), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.regionBtn, index_module_scss_1.default.danger), onClick: () => handleDelete(region), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "\uD83D\uDDD1\uFE0F \u5220\u9664" }) })] })] }, region.id));
                    })) : ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.emptyState, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyIcon, children: "\uD83D\uDDFA\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyTitle, children: "\u6682\u65E0\u7F13\u5B58\u533A\u57DF" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.emptyDesc, children: "\u70B9\u51FB\u4E0A\u65B9\"\u65B0\u589E\"\u6309\u94AE\u4E0B\u8F7D\u5730\u56FE\u7F13\u5B58\uFF0C\u4FBF\u4E8E\u5728\u65E0\u7F51\u7EDC\u65F6\u4F7F\u7528" })] })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.newRegionCard, onClick: handleNewRegion, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.newRegionIcon, children: "\u2795" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.newRegionText, children: "\u6DFB\u52A0\u65B0\u7684\u7F13\u5B58\u533A\u57DF" })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.section, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionTitle, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionIcon, children: "\u2699\uFE0F" }), "\u7F13\u5B58\u8BBE\u7F6E"] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingsCard, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingRow, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: "\u542F\u7528\u5730\u56FE\u7F13\u5B58" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingDesc, children: "\u5F00\u542F\u540E\u53EF\u4E0B\u8F7D\u79BB\u7EBF\u5730\u56FE" })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.switch, {
                                            [index_module_scss_1.default.active]: settings.enableMapCache
                                        }), onClick: handleToggleMapCache, children: (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.switchKnob }) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingRow, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: "\u9ED8\u8BA4\u7F29\u653E\u7EA7\u522B" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingDesc, children: "\u65B0\u7F13\u5B58\u533A\u57DF\u7684\u7F29\u653E\u8303\u56F4" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.zoomSelector, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.zoomBtn, {
                                                    [index_module_scss_1.default.disabled]: minZoom <= 1
                                                }), onClick: () => handleZoomChange('min', -1), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "-" }) }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.zoomValue, children: minZoom }), (0, jsx_runtime_1.jsx)(components_1.Text, { style: { fontSize: '24rpx', color: '#86909C' }, children: "-" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.zoomValue, children: maxZoom }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.zoomBtn, {
                                                    [index_module_scss_1.default.disabled]: maxZoom >= 20
                                                }), onClick: () => handleZoomChange('max', 1), children: (0, jsx_runtime_1.jsx)(components_1.Text, { children: "+" }) })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingRow, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.settingInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: "\u6700\u5927\u7F13\u5B58\u5927\u5C0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingDesc, children: "\u8D85\u8FC7\u540E\u81EA\u52A8\u6E05\u7406\u6700\u65E9\u7684\u7F13\u5B58" })] }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.settingLabel, children: formatFileSize(settings.maxCacheSize) })] })] })] }), regions.length > 0 && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.clearCacheBtn, onClick: handleClearAll, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.clearCacheIcon, children: "\uD83D\uDDD1\uFE0F" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.clearCacheText, children: "\u6E05\u7406\u6240\u6709\u5730\u56FE\u7F13\u5B58" })] }))] }));
};
exports.default = MapCachePage;
//# sourceMappingURL=index.js.map