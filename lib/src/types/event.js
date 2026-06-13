"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SYNC_STATUS_OPTIONS = exports.PRIORITY_OPTIONS = exports.EVENT_TYPE_OPTIONS = void 0;
exports.EVENT_TYPE_OPTIONS = [
    { code: 'environment', name: '环境卫生', icon: '🧹' },
    { code: 'public_facility', name: '公共设施', icon: '🏢' },
    { code: 'dispute', name: '矛盾纠纷', icon: '⚖️' },
    { code: 'safety_hazard', name: '安全隐患', icon: '⚠️' },
    { code: 'traffic', name: '交通出行', icon: '🚗' },
    { code: 'service', name: '民生服务', icon: '🤝' },
    { code: 'security', name: '治安问题', icon: '👮' },
    { code: 'other', name: '其他问题', icon: '📝' }
];
exports.PRIORITY_OPTIONS = [
    { code: 'low', name: '低', color: '#86909C' },
    { code: 'medium', name: '中', color: '#165DFF' },
    { code: 'high', name: '高', color: '#FF7D00' },
    { code: 'urgent', name: '紧急', color: '#F53F3F' }
];
exports.SYNC_STATUS_OPTIONS = [
    { code: 'pending', name: '待同步', color: '#FF7D00' },
    { code: 'syncing', name: '同步中', color: '#165DFF' },
    { code: 'synced', name: '已同步', color: '#00B42A' },
    { code: 'failed', name: '同步失败', color: '#F53F3F' }
];
//# sourceMappingURL=event.js.map