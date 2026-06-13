"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const StatusBadge_1 = require("@/components/StatusBadge");
const event_1 = require("@/types/event");
const EventCard = ({ event, onClick, showSyncButton = false, onSync, onDelete }) => {
    const eventType = event_1.EVENT_TYPE_OPTIONS.find(opt => opt.code === event.type);
    const priority = event_1.PRIORITY_OPTIONS.find(opt => opt.code === event.priority);
    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now.getTime() - timestamp;
        if (diff < 60000)
            return '刚刚';
        if (diff < 3600000)
            return Math.floor(diff / 60000) + '分钟前';
        if (diff < 86400000)
            return Math.floor(diff / 3600000) + '小时前';
        return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
    };
    const handleCardClick = () => {
        if (onClick) {
            onClick();
        }
        else {
            taro_1.default.navigateTo({
                url: `/pages/detail/index?clientId=${event.clientId}`
            });
        }
    };
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.eventCard, onClick: handleCardClick, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.cardHeader, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.typeInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.typeIcon, children: (eventType === null || eventType === void 0 ? void 0 : eventType.icon) || '📝' }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.typeName, children: (eventType === null || eventType === void 0 ? void 0 : eventType.name) || '其他' })] }), (0, jsx_runtime_1.jsx)(StatusBadge_1.default, { status: event.syncStatus, size: "small" })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.cardBody, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.eventTitle, children: event.title }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.eventDesc, children: event.description }), event.images.length > 0 && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.imageList, children: [event.images.slice(0, 3).map((img, index) => ((0, jsx_runtime_1.jsx)(components_1.Image, { className: index_module_scss_1.default.thumbnail, src: img.localPath, mode: "aspectFill" }, img.id))), event.images.length > 3 && ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.moreImages, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.moreText, children: ["+", event.images.length - 3] }) }))] }))] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.cardFooter, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.footerLeft, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.location, children: ["\uD83D\uDCCD ", event.location.address] }) }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.footerRight, children: [priority && ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.priorityBadge, index_module_scss_1.default[event.priority]), children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.priorityText, children: priority.name }) })), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.time, children: formatTime(event.createdAt) })] })] }), showSyncButton && (event.syncStatus === 'pending' || event.syncStatus === 'failed') && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.actionBar, children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.syncBtn), onClick: (e) => {
                            e.stopPropagation();
                            onSync === null || onSync === void 0 ? void 0 : onSync();
                        }, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionText, children: "\u7ACB\u5373\u540C\u6B65" }) }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.actionBtn, index_module_scss_1.default.deleteBtn), onClick: (e) => {
                            e.stopPropagation();
                            onDelete === null || onDelete === void 0 ? void 0 : onDelete();
                        }, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.actionText, children: "\u5220\u9664" }) })] })), event.syncError && event.syncStatus === 'failed' && ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.errorInfo, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.errorText, children: ["\u274C ", event.syncError] }) }))] }));
};
exports.default = EventCard;
//# sourceMappingURL=index.js.map