"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.useOfflineDB = void 0;
const react_1 = require("react");
const taro_1 = require("@tarojs/taro");
const useOfflineStore_1 = require("@/store/useOfflineStore");
const useSyncStore_1 = require("@/store/useSyncStore");
const offlineDB_1 = require("@/utils/offlineDB");
const uuid_1 = require("@/utils/uuid");
const useOfflineDB = () => {
    const { events, pendingEvents, addEvent, updateEvent, removeEvent, refreshAll } = (0, useOfflineStore_1.useOfflineStore)();
    const { setPendingCount } = (0, useSyncStore_1.useSyncStore)();
    const createEvent = (0, react_1.useCallback)(async (params) => {
        const now = Date.now();
        const eventImages = params.images.map(img => ({
            id: (0, uuid_1.generateImageId)(),
            localPath: img.path,
            size: img.size,
            uploadStatus: 'pending',
            createdAt: now
        }));
        const event = {
            clientId: (0, uuid_1.generateClientId)(),
            type: params.type,
            title: params.title,
            description: params.description,
            priority: params.priority,
            images: eventImages,
            location: params.location,
            reporterId: params.reporterId,
            reporterName: params.reporterName,
            anonymous: params.anonymous,
            createdAt: now,
            updatedAt: now,
            syncStatus: 'pending',
            syncRetryCount: 0
        };
        await (0, offlineDB_1.saveEvent)(event);
        await refreshAll();
        console.log('[useOfflineDB] 事件已创建:', event.clientId);
        taro_1.default.showToast({
            title: '已保存到本地',
            icon: 'success'
        });
        return event;
    }, [addEvent, refreshAll]);
    const updateExistingEvent = (0, react_1.useCallback)(async (event) => {
        const updatedEvent = Object.assign(Object.assign({}, event), { updatedAt: Date.now() });
        await (0, offlineDB_1.saveEvent)(updatedEvent);
        await refreshAll();
        console.log('[useOfflineDB] 事件已更新:', event.clientId);
    }, [updateEvent, refreshAll]);
    const deleteEventById = (0, react_1.useCallback)(async (clientId) => {
        await taro_1.default.showModal({
            title: '确认删除',
            content: '确定要删除这条本地记录吗？',
            success: async (res) => {
                if (res.confirm) {
                    await (0, offlineDB_1.deleteEvent)(clientId);
                    await refreshAll();
                    taro_1.default.showToast({
                        title: '删除成功',
                        icon: 'success'
                    });
                }
            }
        });
    }, [removeEvent, refreshAll]);
    const getEvent = (0, react_1.useCallback)(async (clientId) => {
        return (0, offlineDB_1.getEventByClientId)(clientId);
    }, []);
    const getEventsByStatus = (0, react_1.useCallback)((status) => {
        return events.filter(e => e.syncStatus === status);
    }, [events]);
    const getPendingCount = (0, react_1.useCallback)(() => {
        const count = pendingEvents.length;
        setPendingCount(count);
        return count;
    }, [pendingEvents, setPendingCount]);
    const formatFileSize = (0, react_1.useCallback)((bytes) => {
        if (bytes < 1024)
            return bytes + ' B';
        if (bytes < 1024 * 1024)
            return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }, []);
    const formatDate = (0, react_1.useCallback)((timestamp) => {
        const date = new Date(timestamp);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}`;
    }, []);
    const formatRelativeTime = (0, react_1.useCallback)((timestamp) => {
        const now = Date.now();
        const diff = now - timestamp;
        if (diff < 60000)
            return '刚刚';
        if (diff < 3600000)
            return Math.floor(diff / 60000) + '分钟前';
        if (diff < 86400000)
            return Math.floor(diff / 3600000) + '小时前';
        if (diff < 7 * 86400000)
            return Math.floor(diff / 86400000) + '天前';
        return formatDate(timestamp);
    }, [formatDate]);
    return {
        events,
        pendingEvents,
        createEvent,
        updateEvent: updateExistingEvent,
        deleteEvent: deleteEventById,
        getEvent,
        getEventsByStatus,
        getPendingCount,
        formatFileSize,
        formatDate,
        formatRelativeTime
    };
};
exports.useOfflineDB = useOfflineDB;
//# sourceMappingURL=useOfflineDB.js.map