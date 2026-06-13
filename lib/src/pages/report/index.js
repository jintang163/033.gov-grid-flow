"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const ImageUploader_1 = require("@/components/ImageUploader");
const useNetwork_1 = require("@/hooks/useNetwork");
const useOfflineDB_1 = require("@/hooks/useOfflineDB");
const useSync_1 = require("@/hooks/useSync");
const event_1 = require("@/services/event");
const event_2 = require("@/types/event");
const ReportPage = () => {
    const { isOnline, networkType } = (0, useNetwork_1.useNetwork)();
    const { createEvent } = (0, useOfflineDB_1.useOfflineDB)();
    const { syncSingle } = (0, useSync_1.useSync)();
    const [eventType, setEventType] = (0, react_1.useState)('');
    const [title, setTitle] = (0, react_1.useState)('');
    const [description, setDescription] = (0, react_1.useState)('');
    const [priority, setPriority] = (0, react_1.useState)('medium');
    const [images, setImages] = (0, react_1.useState)([]);
    const [location, setLocation] = (0, react_1.useState)(null);
    const [isLocationLoading, setIsLocationLoading] = (0, react_1.useState)(false);
    const [anonymous, setAnonymous] = (0, react_1.useState)(false);
    const [isSubmitting, setIsSubmitting] = (0, react_1.useState)(false);
    (0, taro_1.useDidShow)(() => {
        if (!location) {
            getCurrentLocation();
        }
    });
    const getCurrentLocation = (0, react_1.useCallback)(async () => {
        setIsLocationLoading(true);
        try {
            const res = await (0, taro_1.getLocation)({
                type: 'gcj02',
                isHighAccuracy: true
            });
            console.log('[ReportPage] 获取位置成功:', res);
            setLocation({
                longitude: res.longitude,
                latitude: res.latitude,
                address: '正在解析地址...',
                gridCode: '110101001001',
                gridName: '和平里第一网格'
            });
            setTimeout(() => {
                setLocation(prev => prev ? Object.assign(Object.assign({}, prev), { address: '北京市东城区和平里街道和平里小区' }) : null);
            }, 1000);
        }
        catch (error) {
            console.error('[ReportPage] 获取位置失败:', error);
            taro_1.default.showToast({
                title: '获取位置失败，请手动选择',
                icon: 'none'
            });
            setLocation({
                longitude: 116.397,
                latitude: 39.908,
                address: '北京市东城区和平里街道',
                gridCode: '110101001001',
                gridName: '和平里第一网格'
            });
        }
        finally {
            setIsLocationLoading(false);
        }
    }, []);
    const isFormValid = eventType && title.trim() && description.trim() && location;
    const handleSubmit = (0, react_1.useCallback)(async () => {
        if (!isFormValid) {
            taro_1.default.showToast({
                title: '请填写完整信息',
                icon: 'none'
            });
            return;
        }
        if (isSubmitting)
            return;
        setIsSubmitting(true);
        console.log('[ReportPage] 提交事件:', { eventType, title, description, priority, isOnline });
        try {
            if (isOnline) {
                taro_1.default.showLoading({ title: '正在上报...', mask: true });
                const uploadedImages = await Promise.all(images.map(async (img) => {
                    try {
                        const remoteUrl = await (0, event_1.uploadImage)(img.path);
                        return remoteUrl;
                    }
                    catch (error) {
                        console.error('[ReportPage] 图片上传失败:', error);
                        throw new Error('图片上传失败');
                    }
                }));
                const result = await (0, event_1.reportEvent)({
                    type: eventType,
                    title: title.trim(),
                    description: description.trim(),
                    priority,
                    imageUrls: uploadedImages,
                    longitude: location.longitude,
                    latitude: location.latitude,
                    address: location.address,
                    anonymous: anonymous ? 1 : 0
                });
                taro_1.default.hideLoading();
                console.log('[ReportPage] 在线上报成功:', result);
                taro_1.default.showModal({
                    title: '上报成功',
                    content: '事件已成功上报，是否继续上报？',
                    confirmText: '继续上报',
                    cancelText: '返回首页',
                    success: (res) => {
                        if (res.confirm) {
                            resetForm();
                        }
                        else {
                            taro_1.default.switchTab({ url: '/pages/home/index' });
                        }
                    }
                });
            }
            else {
                const confirmRes = await taro_1.default.showModal({
                    title: '离线模式',
                    content: '当前无网络连接，事件将保存到本地，网络恢复后可同步上传。是否继续？',
                    confirmText: '保存到本地',
                    cancelText: '取消'
                });
                if (!confirmRes.confirm) {
                    setIsSubmitting(false);
                    return;
                }
                const event = await createEvent({
                    type: eventType,
                    title: title.trim(),
                    description: description.trim(),
                    priority,
                    images: images.map(img => ({ path: img.path, size: img.size })),
                    location: location,
                    reporterId: '1',
                    reporterName: '张三',
                    anonymous: anonymous ? 1 : 0
                });
                console.log('[ReportPage] 离线保存成功:', event.clientId);
                taro_1.default.showModal({
                    title: '已保存到本地',
                    content: '事件已保存到本地，网络恢复后可在"本地事件"中同步。',
                    confirmText: '查看本地事件',
                    cancelText: '继续上报',
                    success: (res) => {
                        if (res.confirm) {
                            taro_1.default.switchTab({ url: '/pages/offline/index' });
                        }
                        else {
                            resetForm();
                        }
                    }
                });
            }
        }
        catch (error) {
            console.error('[ReportPage] 提交失败:', error);
            taro_1.default.hideLoading();
            const errorMsg = error instanceof Error ? error.message : '提交失败';
            if (isOnline) {
                taro_1.default.showModal({
                    title: '上报失败',
                    content: `${errorMsg}，是否保存到本地待稍后同步？`,
                    confirmText: '保存到本地',
                    cancelText: '取消',
                    success: async (res) => {
                        if (res.confirm) {
                            await createEvent({
                                type: eventType,
                                title: title.trim(),
                                description: description.trim(),
                                priority,
                                images: images.map(img => ({ path: img.path, size: img.size })),
                                location: location,
                                reporterId: '1',
                                reporterName: '张三',
                                anonymous: anonymous ? 1 : 0
                            });
                            taro_1.default.showToast({
                                title: '已保存到本地',
                                icon: 'success'
                            });
                            resetForm();
                        }
                    }
                });
            }
            else {
                taro_1.default.showToast({
                    title: errorMsg,
                    icon: 'none'
                });
            }
        }
        finally {
            setIsSubmitting(false);
        }
    }, [isFormValid, isSubmitting, isOnline, eventType, title, description, priority, images, location, anonymous, createEvent]);
    const resetForm = () => {
        setEventType('');
        setTitle('');
        setDescription('');
        setPriority('medium');
        setImages([]);
        setAnonymous(false);
    };
    const formatCoords = (lng, lat) => {
        return `${lng.toFixed(4)}, ${lat.toFixed(4)}`;
    };
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.reportPage, children: [!isOnline && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.offlineNotice, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.offlineIcon, children: "\uD83D\uDCE1" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.offlineText, children: "\u5F53\u524D\u5904\u4E8E\u79BB\u7EBF\u6A21\u5F0F\uFF0C\u63D0\u4EA4\u540E\u5C06\u4FDD\u5B58\u5230\u672C\u5730" })] })), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.formContainer, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.formSection, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionLabel, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.required, children: "*" }), "\u4E8B\u4EF6\u7C7B\u578B"] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.typeGrid, children: event_2.EVENT_TYPE_OPTIONS.map(type => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.typeItem, {
                                        [index_module_scss_1.default.active]: eventType === type.code
                                    }), onClick: () => setEventType(type.code), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.typeIcon, children: type.icon }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.typeName, children: type.name })] }, type.code))) })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.formSection, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.inputWrapper, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.inputLabel, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.required, children: "*" }), "\u4E8B\u4EF6\u6807\u9898"] }), (0, jsx_runtime_1.jsx)(components_1.Input, { className: index_module_scss_1.default.textInput, placeholder: "\u8BF7\u7B80\u8981\u63CF\u8FF0\u4E8B\u4EF6", value: title, onInput: (e) => setTitle(e.detail.value), maxlength: 50 }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.wordCount, children: [title.length, "/50"] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.inputWrapper, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.inputLabel, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.required, children: "*" }), "\u8BE6\u7EC6\u63CF\u8FF0"] }), (0, jsx_runtime_1.jsx)(components_1.Textarea, { className: index_module_scss_1.default.textareaInput, placeholder: "\u8BF7\u8BE6\u7EC6\u63CF\u8FF0\u4E8B\u4EF6\u60C5\u51B5\uFF0C\u5305\u62EC\u65F6\u95F4\u3001\u5730\u70B9\u3001\u5177\u4F53\u95EE\u9898\u7B49", value: description, onInput: (e) => setDescription(e.detail.value), maxlength: 500 }), (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.wordCount, children: [description.length, "/500"] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.inputWrapper, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.inputLabel, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.required, children: "*" }), "\u4F18\u5148\u7EA7"] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.priorityRow, children: event_2.PRIORITY_OPTIONS.map(opt => ((0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.priorityItem, index_module_scss_1.default[opt.code], {
                                                [index_module_scss_1.default.active]: priority === opt.code
                                            }), onClick: () => setPriority(opt.code), children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.priorityText, children: opt.name }) }, opt.code))) })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.formSection, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.sectionLabel, children: "\u73B0\u573A\u7167\u7247" }), (0, jsx_runtime_1.jsx)(ImageUploader_1.default, { images: images, onChange: setImages, maxCount: 9 })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.formSection, children: [(0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.sectionLabel, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.required, children: "*" }), "\u4F4D\u7F6E\u4FE1\u606F"] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.locationRow, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.locationIcon, children: "\uD83D\uDCCD" }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.locationInfo, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.locationAddress, children: isLocationLoading ? '定位中...' : (location === null || location === void 0 ? void 0 : location.address) || '未知位置' }), location && ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.locationCoords, children: formatCoords(location.longitude, location.latitude) }))] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.locationBtn, onClick: getCurrentLocation, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.locationBtnText, children: isLocationLoading ? '定位中' : '重新定位' }) })] })] })] }), (0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.bottomBar, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.anonymousRow, onClick: () => setAnonymous(!anonymous), children: [(0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.anonymousCheckbox, {
                                    [index_module_scss_1.default.checked]: anonymous
                                }), children: anonymous && (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.checkmark, children: "\u2713" }) }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.anonymousText, children: "\u533F\u540D\u4E0A\u62A5" })] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.submitBtn, {
                            [index_module_scss_1.default.disabled]: !isFormValid || isSubmitting
                        }), onClick: handleSubmit, children: isSubmitting ? ((0, jsx_runtime_1.jsxs)(components_1.Text, { className: (0, classnames_1.default)(index_module_scss_1.default.submitBtnText, index_module_scss_1.default.loadingText), children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.loadingIcon, children: "\uD83D\uDD04" }), isOnline ? '上报中...' : '保存中...'] })) : ((0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.submitBtnText, children: isOnline ? '立即上报' : '保存到本地' })) })] })] }));
};
exports.default = ReportPage;
//# sourceMappingURL=index.js.map