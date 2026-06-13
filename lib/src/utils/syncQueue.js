"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.syncQueue = void 0;
const offlineDB_1 = require("./offlineDB");
const event_1 = require("@/services/event");
const uuid_1 = require("./uuid");
class SyncQueue {
    constructor() {
        this.isProcessing = false;
        this.queue = [];
        this.options = {};
    }
    setOptions(options) {
        this.options = options;
    }
    addEvents(events) {
        const newEvents = events.filter(e => !this.queue.find(q => q.clientId === e.clientId));
        this.queue.push(...newEvents);
        console.log('[SyncQueue] 添加事件到队列，当前队列长度:', this.queue.length);
    }
    clear() {
        this.queue = [];
        this.isProcessing = false;
    }
    async process() {
        var _a, _b, _c, _d, _e, _f;
        if (this.isProcessing) {
            console.warn('[SyncQueue] 已有同步任务在进行中');
            throw new Error('同步任务进行中');
        }
        this.isProcessing = true;
        const startTime = Date.now();
        const settings = await (0, offlineDB_1.getSettings)();
        const total = this.queue.length;
        let completed = 0;
        let failed = 0;
        const failedItems = [];
        console.log('[SyncQueue] 开始处理同步队列，共', total, '条事件');
        try {
            const sortedEvents = [...this.queue].sort((a, b) => a.createdAt - b.createdAt);
            for (const event of sortedEvents) {
                if (!this.isProcessing) {
                    console.log('[SyncQueue] 同步任务已取消');
                    break;
                }
                (_b = (_a = this.options).onProgress) === null || _b === void 0 ? void 0 : _b.call(_a, {
                    total,
                    completed,
                    failed,
                    currentEvent: event
                });
                try {
                    await (0, offlineDB_1.updateEventSyncStatus)(event.clientId, 'syncing');
                    console.log('[SyncQueue] 正在同步事件:', event.clientId);
                    const uploadedImages = await Promise.all(event.images.map(async (img) => {
                        if (img.uploadStatus === 'synced' && img.remoteUrl) {
                            return img.remoteUrl;
                        }
                        try {
                            const remoteUrl = await (0, event_1.uploadImage)(img.localPath);
                            return remoteUrl;
                        }
                        catch (imgError) {
                            console.error('[SyncQueue] 图片上传失败:', img.id, imgError);
                            throw new Error(`图片上传失败: ${imgError instanceof Error ? imgError.message : '未知错误'}`);
                        }
                    }));
                    const result = await (0, event_1.reportEvent)({
                        type: event.type,
                        title: event.title,
                        description: event.description,
                        priority: event.priority,
                        imageUrls: uploadedImages,
                        longitude: event.location.longitude,
                        latitude: event.location.latitude,
                        address: event.location.address,
                        anonymous: event.anonymous,
                        clientId: event.clientId,
                        timestamp: event.createdAt
                    });
                    await (0, offlineDB_1.updateEventSyncStatus)(event.clientId, 'synced', undefined, result.id);
                    completed++;
                    console.log('[SyncQueue] 事件同步成功:', event.clientId, '服务器ID:', result.id);
                }
                catch (error) {
                    failed++;
                    const errorMsg = error instanceof Error ? error.message : '同步失败';
                    failedItems.push({ clientId: event.clientId, error: errorMsg });
                    const shouldRetry = settings.autoRetry && event.syncRetryCount < settings.maxRetryCount;
                    await (0, offlineDB_1.updateEventSyncStatus)(event.clientId, shouldRetry ? 'pending' : 'failed', errorMsg);
                    console.error('[SyncQueue] 事件同步失败:', event.clientId, errorMsg, shouldRetry ? '将自动重试' : '已达最大重试次数');
                }
            }
            const endTime = Date.now();
            const result = {
                success: failed === 0,
                total,
                successCount: completed,
                failedCount: failed,
                failedItems
            };
            const log = {
                id: (0, uuid_1.generateUUID)(),
                startTime,
                endTime,
                totalCount: total,
                successCount: completed,
                failedCount: failed,
                status: failed === 0 ? 'success' : completed > 0 ? 'partial' : 'failed',
                errorMessage: failed > 0 ? `${failed} 条同步失败` : undefined
            };
            await (0, offlineDB_1.saveSyncLog)(log);
            console.log('[SyncQueue] 同步完成:', result);
            (_d = (_c = this.options).onComplete) === null || _d === void 0 ? void 0 : _d.call(_c, result);
            return result;
        }
        catch (error) {
            console.error('[SyncQueue] 同步过程发生严重错误:', error);
            (_f = (_e = this.options).onError) === null || _f === void 0 ? void 0 : _f.call(_e, error instanceof Error ? error : new Error('同步失败'));
            throw error;
        }
        finally {
            this.isProcessing = false;
            this.queue = [];
        }
    }
    async processBatch() {
        var _a, _b, _c, _d, _e, _f, _g, _h;
        if (this.isProcessing) {
            console.warn('[SyncQueue] 已有同步任务在进行中');
            throw new Error('同步任务进行中');
        }
        this.isProcessing = true;
        const startTime = Date.now();
        const total = this.queue.length;
        console.log('[SyncQueue] 开始批量同步，共', total, '条事件');
        try {
            const sortedEvents = [...this.queue].sort((a, b) => a.createdAt - b.createdAt);
            (_b = (_a = this.options).onProgress) === null || _b === void 0 ? void 0 : _b.call(_a, {
                total,
                completed: 0,
                failed: 0,
                currentEvent: sortedEvents[0]
            });
            for (const event of sortedEvents) {
                await (0, offlineDB_1.updateEventSyncStatus)(event.clientId, 'syncing');
            }
            const batchResult = await (0, event_1.batchSyncEvents)(sortedEvents);
            let completed = 0;
            let failed = 0;
            const failedItems = [];
            for (const result of batchResult.results) {
                if (result.success) {
                    await (0, offlineDB_1.updateEventSyncStatus)(result.clientId, 'synced', undefined, result.serverId);
                    completed++;
                }
                else {
                    failed++;
                    failedItems.push({ clientId: result.clientId, error: result.error || '同步失败' });
                    await (0, offlineDB_1.updateEventSyncStatus)(result.clientId, 'failed', result.error);
                }
            }
            const endTime = Date.now();
            const syncResult = {
                success: failed === 0,
                total,
                successCount: completed,
                failedCount: failed,
                failedItems
            };
            const log = {
                id: (0, uuid_1.generateUUID)(),
                startTime,
                endTime,
                totalCount: total,
                successCount: completed,
                failedCount: failed,
                status: failed === 0 ? 'success' : completed > 0 ? 'partial' : 'failed',
                errorMessage: failed > 0 ? `${failed} 条同步失败` : undefined
            };
            await (0, offlineDB_1.saveSyncLog)(log);
            console.log('[SyncQueue] 批量同步完成:', syncResult);
            (_d = (_c = this.options).onProgress) === null || _d === void 0 ? void 0 : _d.call(_c, {
                total,
                completed,
                failed
            });
            (_f = (_e = this.options).onComplete) === null || _f === void 0 ? void 0 : _f.call(_e, syncResult);
            return syncResult;
        }
        catch (error) {
            console.error('[SyncQueue] 批量同步失败:', error);
            for (const event of this.queue) {
                await (0, offlineDB_1.updateEventSyncStatus)(event.clientId, 'failed', error instanceof Error ? error.message : '批量同步失败');
            }
            const result = {
                success: false,
                total,
                successCount: 0,
                failedCount: total,
                failedItems: this.queue.map(e => ({
                    clientId: e.clientId,
                    error: error instanceof Error ? error.message : '同步失败'
                }))
            };
            (_h = (_g = this.options).onError) === null || _h === void 0 ? void 0 : _h.call(_g, error instanceof Error ? error : new Error('批量同步失败'));
            return result;
        }
        finally {
            this.isProcessing = false;
            this.queue = [];
        }
    }
    cancel() {
        this.isProcessing = false;
        console.log('[SyncQueue] 同步任务已取消');
    }
    get isProcessingSync() {
        return this.isProcessing;
    }
    get queueSize() {
        return this.queue.length;
    }
}
exports.syncQueue = new SyncQueue();
//# sourceMappingURL=syncQueue.js.map