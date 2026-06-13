"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const components_1 = require("@tarojs/components");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const NetworkStatus = ({ isOnline, networkType, size = 'medium', showText = true }) => {
    const getNetworkIcon = () => {
        if (!isOnline)
            return '📡';
        if (networkType === 'wifi')
            return '📶';
        if (networkType === '5g')
            return '5G';
        if (networkType === '4g')
            return '4G';
        if (networkType === '3g')
            return '3G';
        if (networkType === '2g')
            return '2G';
        return '🌐';
    };
    const getNetworkText = () => {
        if (!isOnline)
            return '离线';
        if (networkType === 'wifi')
            return 'Wi-Fi';
        if (networkType === '5g')
            return '5G网络';
        if (networkType === '4g')
            return '4G网络';
        if (networkType === '3g')
            return '3G网络';
        if (networkType === '2g')
            return '2G网络';
        return '在线';
    };
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.networkStatus, index_module_scss_1.default[size], isOnline ? index_module_scss_1.default.online : index_module_scss_1.default.offline), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.icon, children: getNetworkIcon() }), showText && ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.text, children: getNetworkText() }))] }));
};
exports.default = NetworkStatus;
//# sourceMappingURL=index.js.map