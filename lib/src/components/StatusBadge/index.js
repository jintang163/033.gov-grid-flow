"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const components_1 = require("@tarojs/components");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const event_1 = require("@/types/event");
const StatusBadge = ({ status, size = 'medium', showText = true }) => {
    const statusConfig = event_1.SYNC_STATUS_OPTIONS.find(opt => opt.code === status) || event_1.SYNC_STATUS_OPTIONS[0];
    const getStatusIcon = () => {
        switch (status) {
            case 'pending':
                return '⏳';
            case 'syncing':
                return '🔄';
            case 'synced':
                return '✓';
            case 'failed':
                return '✕';
            default:
                return '';
        }
    };
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.statusBadge, index_module_scss_1.default[status], index_module_scss_1.default[size]), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.icon, children: getStatusIcon() }), showText && ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.text, children: statusConfig.name }))] }));
};
exports.default = StatusBadge;
//# sourceMappingURL=index.js.map