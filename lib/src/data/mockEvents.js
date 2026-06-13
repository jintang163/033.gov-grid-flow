"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.mockMapCacheRegions = exports.mockSyncLogs = exports.mockOfflineEvents = void 0;
const uuid_1 = require("@/utils/uuid");
const now = Date.now();
exports.mockOfflineEvents = [
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'environment',
        title: '小区门口垃圾堆积',
        description: '小区东门门口有大量生活垃圾堆积，已有3天未清理，散发异味，影响居民出行。',
        priority: 'high',
        images: [
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/292/400/300',
                size: 102400,
                uploadStatus: 'pending',
                createdAt: now - 3600000
            },
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/312/400/300',
                size: 153600,
                uploadStatus: 'pending',
                createdAt: now - 3600000
            }
        ],
        location: {
            longitude: 116.397,
            latitude: 39.908,
            address: '北京市东城区和平里街道和平里小区东门',
            gridCode: '110101001001',
            gridName: '和平里第一网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 0,
        createdAt: now - 3600000,
        updatedAt: now - 3600000,
        syncStatus: 'pending',
        syncRetryCount: 0
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'public_facility',
        title: '路灯损坏',
        description: '主街中段有一盏路灯不亮，晚上出行不便，存在安全隐患。',
        priority: 'medium',
        images: [
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/1082/400/300',
                size: 204800,
                uploadStatus: 'pending',
                createdAt: now - 7200000
            }
        ],
        location: {
            longitude: 116.400,
            latitude: 39.910,
            address: '北京市东城区和平里街道和平里主街中段',
            gridCode: '110101001001',
            gridName: '和平里第一网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 0,
        createdAt: now - 7200000,
        updatedAt: now - 7200000,
        syncStatus: 'failed',
        syncRetryCount: 2,
        syncError: '网络连接超时，请重试'
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'safety_hazard',
        title: '井盖破损',
        description: '人行道上有一处井盖破损，存在安全隐患，需要立即处理。',
        priority: 'urgent',
        images: [
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/119/400/300',
                size: 179200,
                uploadStatus: 'synced',
                remoteUrl: 'https://example.com/images/img1.jpg',
                createdAt: now - 86400000
            }
        ],
        location: {
            longitude: 116.402,
            latitude: 39.912,
            address: '北京市东城区和平里街道和平里西街',
            gridCode: '110101001002',
            gridName: '和平里第二网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 0,
        createdAt: now - 86400000,
        updatedAt: now - 82800000,
        syncStatus: 'synced',
        syncRetryCount: 0,
        syncedAt: now - 82800000,
        serverId: 1001
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'dispute',
        title: '邻里噪音纠纷',
        description: '楼上住户经常在深夜播放音乐，影响楼下居民休息，多次沟通无果。',
        priority: 'medium',
        images: [],
        location: {
            longitude: 116.395,
            latitude: 39.905,
            address: '北京市东城区和平里街道和平里小区5号楼',
            gridCode: '110101001001',
            gridName: '和平里第一网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 1,
        createdAt: now - 172800000,
        updatedAt: now - 172800000,
        syncStatus: 'pending',
        syncRetryCount: 0
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'traffic',
        title: '道路施工无警示',
        description: '文化路正在施工，但未设置明显警示标志，容易发生交通事故。',
        priority: 'high',
        images: [
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/160/400/300',
                size: 230400,
                uploadStatus: 'pending',
                createdAt: now - 259200000
            },
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/201/400/300',
                size: 192000,
                uploadStatus: 'pending',
                createdAt: now - 259200000
            },
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/3/400/300',
                size: 215040,
                uploadStatus: 'pending',
                createdAt: now - 259200000
            }
        ],
        location: {
            longitude: 116.405,
            latitude: 39.915,
            address: '北京市东城区和平里街道文化路',
            gridCode: '110101001003',
            gridName: '和平里第三网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 0,
        createdAt: now - 259200000,
        updatedAt: now - 259200000,
        syncStatus: 'pending',
        syncRetryCount: 0
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'service',
        title: '独居老人需要帮助',
        description: '小区内有一位独居老人，行动不便，希望能安排志愿者定期上门探望。',
        priority: 'low',
        images: [],
        location: {
            longitude: 116.398,
            latitude: 39.907,
            address: '北京市东城区和平里街道和平里小区3号楼2单元',
            gridCode: '110101001001',
            gridName: '和平里第一网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 0,
        createdAt: now - 345600000,
        updatedAt: now - 345600000,
        syncStatus: 'synced',
        syncRetryCount: 0,
        syncedAt: now - 342000000,
        serverId: 998
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'security',
        title: '可疑人员出没',
        description: '近日发现有不明人员在小区内徘徊，形迹可疑，希望加强巡逻。',
        priority: 'high',
        images: [],
        location: {
            longitude: 116.401,
            latitude: 39.909,
            address: '北京市东城区和平里街道和平里小区',
            gridCode: '110101001001',
            gridName: '和平里第一网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 1,
        createdAt: now - 432000000,
        updatedAt: now - 432000000,
        syncStatus: 'failed',
        syncRetryCount: 3,
        syncError: '服务器返回错误: 500 Internal Server Error'
    },
    {
        clientId: (0, uuid_1.generateClientId)(),
        type: 'other',
        title: '其他问题反馈',
        description: '小区宣传栏内容更新不及时，希望能定期更新相关政策信息。',
        priority: 'low',
        images: [
            {
                id: (0, uuid_1.generateImageId)(),
                localPath: 'https://picsum.photos/id/225/400/300',
                size: 128000,
                uploadStatus: 'pending',
                createdAt: now - 518400000
            }
        ],
        location: {
            longitude: 116.396,
            latitude: 39.906,
            address: '北京市东城区和平里街道和平里小区居委会门口',
            gridCode: '110101001001',
            gridName: '和平里第一网格'
        },
        reporterId: '1',
        reporterName: '张三',
        anonymous: 0,
        createdAt: now - 518400000,
        updatedAt: now - 518400000,
        syncStatus: 'pending',
        syncRetryCount: 0
    }
];
exports.mockSyncLogs = [
    {
        id: 'log_001',
        startTime: now - 82800000,
        endTime: now - 82780000,
        totalCount: 3,
        successCount: 3,
        failedCount: 0,
        status: 'success'
    },
    {
        id: 'log_002',
        startTime: now - 342000000,
        endTime: now - 341970000,
        totalCount: 5,
        successCount: 4,
        failedCount: 1,
        status: 'partial',
        errorMessage: '1条同步失败'
    },
    {
        id: 'log_003',
        startTime: now - 604800000,
        endTime: now - 604780000,
        totalCount: 2,
        successCount: 0,
        failedCount: 2,
        status: 'failed',
        errorMessage: '网络连接失败'
    }
];
exports.mockMapCacheRegions = [
    {
        id: 'region_001',
        name: '和平里街道',
        minZoom: 12,
        maxZoom: 18,
        bounds: {
            west: 116.39,
            south: 39.90,
            east: 116.41,
            north: 39.92
        },
        totalTiles: 2500,
        downloadedTiles: 2500,
        totalSize: 150 * 1024 * 1024,
        status: 'completed',
        createdAt: now - 86400000 * 7,
        completedAt: now - 86400000 * 6
    },
    {
        id: 'region_002',
        name: '安定门街道',
        minZoom: 12,
        maxZoom: 18,
        bounds: {
            west: 116.40,
            south: 39.93,
            east: 116.42,
            north: 39.95
        },
        totalTiles: 3200,
        downloadedTiles: 1600,
        totalSize: 96 * 1024 * 1024,
        status: 'downloading',
        createdAt: now - 3600000
    }
];
//# sourceMappingURL=mockEvents.js.map