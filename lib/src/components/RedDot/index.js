"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const components_1 = require("@tarojs/components");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const RedDot = ({ count, show = false, size = 'medium', maxCount = 99 }) => {
    if (!show && (count === undefined || count <= 0)) {
        return null;
    }
    const displayCount = count !== undefined && count > maxCount
        ? `${maxCount}+`
        : count;
    const hasNumber = count !== undefined && count > 0;
    return ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.redDot, index_module_scss_1.default[size], hasNumber ? index_module_scss_1.default.withNumber : index_module_scss_1.default.withoutNumber), children: hasNumber && ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.count, children: displayCount })) }));
};
exports.default = RedDot;
//# sourceMappingURL=index.js.map