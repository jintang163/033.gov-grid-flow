"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getEventDetail = exports.getEventTypeList = exports.getEventList = exports.batchSyncEvents = exports.uploadImage = exports.reportEvent = void 0;
const taro_1 = require("@tarojs/taro");
const BASE_URL = 'http://localhost:8080/api';
const reportEvent = async (data) => {
    var _a, _b;
    console.log('[EventService] 上报事件:', data);
    try {
        const response = await taro_1.default.request({
            url: `${BASE_URL}/event/report`,
            method: 'POST',
            data: Object.assign(Object.assign({}, data), { clientId: data.clientId, eventTimestamp: data.timestamp }),
            header: {
                'Content-Type': 'application/json',
                'X-User-Id': '1'
            },
            timeout: 30000
        });
        if (response.statusCode === 200 && response.data.success) {
            console.log('[EventService] 事件上报成功:', response.data);
            return {
                id: ((_a = response.data.data) === null || _a === void 0 ? void 0 : _a.id) || 0,
                clientId: data.clientId,
                status: 'success',
                message: response.data.message || '上报成功'
            };
        }
        throw new Error(((_b = response.data) === null || _b === void 0 ? void 0 : _b.message) || '上报失败');
    }
    catch (error) {
        console.error('[EventService] 事件上报失败:', error);
        throw error;
    }
};
exports.reportEvent = reportEvent;
const uploadImage = async (filePath) => {
    console.log('[EventService] 上传图片:', filePath);
    try {
        const response = await taro_1.default.uploadFile({
            url: `${BASE_URL}/file/upload`,
            filePath,
            name: 'file',
            header: {
                'X-User-Id': '1'
            },
            timeout: 60000
        });
        if (response.statusCode === 200) {
            const data = JSON.parse(response.data);
            if (data.success) {
                console.log('[EventService] 图片上传成功:', data.data);
                return data.data || '';
            }
            throw new Error(data.message || '上传失败');
        }
        throw new Error(`上传失败，状态码: ${response.statusCode}`);
    }
    catch (error) {
        console.error('[EventService] 图片上传失败:', error);
        throw error;
    }
};
exports.uploadImage = uploadImage;
const batchSyncEvents = async (events) => {
    var _a;
    console.log('[EventService] 批量同步事件，数量:', events.length);
    const request = {
        events: events.map(e => (Object.assign(Object.assign({}, e), { images: e.images.map(img => (Object.assign(Object.assign({}, img), { localPath: img.localPath, remoteUrl: img.remoteUrl || '' }))) }))),
        timestamp: Date.now(),
        deviceId: 'mobile-device-001'
    };
    try {
        const response = await taro_1.default.request({
            url: `${BASE_URL}/event/batch-sync`,
            method: 'POST',
            data: request,
            header: {
                'Content-Type': 'application/json',
                'X-User-Id': '1'
            },
            timeout: 120000
        });
        if (response.statusCode === 200 && response.data.success) {
            console.log('[EventService] 批量同步成功:', response.data);
            return response.data;
        }
        throw new Error(((_a = response.data) === null || _a === void 0 ? void 0 : _a.message) || '批量同步失败');
    }
    catch (error) {
        console.error('[EventService] 批量同步失败:', error);
        throw error;
    }
};
exports.batchSyncEvents = batchSyncEvents;
const getEventList = async (params) => {
    var _a, _b, _c;
    console.log('[EventService] 获取事件列表:', params);
    try {
        const response = await taro_1.default.request({
            url: `${BASE_URL}/event/list`,
            method: 'GET',
            data: params,
            header: {
                'X-User-Id': '1'
            }
        });
        if (response.statusCode === 200 && response.data.success) {
            return {
                list: ((_a = response.data.data) === null || _a === void 0 ? void 0 : _a.records) || [],
                total: ((_b = response.data.data) === null || _b === void 0 ? void 0 : _b.total) || 0
            };
        }
        throw new Error(((_c = response.data) === null || _c === void 0 ? void 0 : _c.message) || '获取列表失败');
    }
    catch (error) {
        console.error('[EventService] 获取事件列表失败:', error);
        return { list: [], total: 0 };
    }
};
exports.getEventList = getEventList;
const getEventTypeList = async () => {
    var _a;
    console.log('[EventService] 获取事件类型列表');
    try {
        const response = await taro_1.default.request({
            url: `${BASE_URL}/event/type/list`,
            method: 'GET',
            header: {
                'X-User-Id': '1'
            }
        });
        if (response.statusCode === 200 && response.data.success) {
            return response.data.data || [];
        }
        throw new Error(((_a = response.data) === null || _a === void 0 ? void 0 : _a.message) || '获取类型列表失败');
    }
    catch (error) {
        console.error('[EventService] 获取事件类型列表失败:', error);
        return [
            { code: 'environment', name: '环境卫生' },
            { code: 'public_facility', name: '公共设施' },
            { code: 'dispute', name: '矛盾纠纷' },
            { code: 'safety_hazard', name: '安全隐患' },
            { code: 'traffic', name: '交通出行' },
            { code: 'service', name: '民生服务' },
            { code: 'security', name: '治安问题' },
            { code: 'other', name: '其他问题' }
        ];
    }
};
exports.getEventTypeList = getEventTypeList;
const getEventDetail = async (id) => {
    var _a;
    console.log('[EventService] 获取事件详情:', id);
    try {
        const response = await taro_1.default.request({
            url: `${BASE_URL}/event/${id}`,
            method: 'GET',
            header: {
                'X-User-Id': '1'
            }
        });
        if (response.statusCode === 200 && response.data.success) {
            return response.data.data;
        }
        throw new Error(((_a = response.data) === null || _a === void 0 ? void 0 : _a.message) || '获取详情失败');
    }
    catch (error) {
        console.error('[EventService] 获取事件详情失败:', error);
        return null;
    }
};
exports.getEventDetail = getEventDetail;
//# sourceMappingURL=event.js.map