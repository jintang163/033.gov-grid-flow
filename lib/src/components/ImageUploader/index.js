"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const components_1 = require("@tarojs/components");
const taro_1 = require("@tarojs/taro");
const classnames_1 = require("classnames");
const index_module_scss_1 = require("./index.module.scss");
const ImageUploader = ({ images, onChange, maxCount = 9, disabled = false }) => {
    const handleChooseImage = (0, react_1.useCallback)(async () => {
        if (disabled)
            return;
        if (images.length >= maxCount) {
            taro_1.default.showToast({
                title: `最多只能上传${maxCount}张图片`,
                icon: 'none'
            });
            return;
        }
        try {
            const res = await (0, taro_1.chooseImage)({
                count: maxCount - images.length,
                sizeType: ['compressed'],
                sourceType: ['album', 'camera']
            });
            const newImages = [];
            for (const tempFilePath of res.tempFilePaths) {
                try {
                    const fileInfo = await taro_1.default.getFileInfo({ filePath: tempFilePath });
                    const imgInfo = await (0, taro_1.getImageInfo)({ src: tempFilePath });
                    newImages.push({
                        id: `img_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
                        path: tempFilePath,
                        size: fileInfo.size || 0,
                        width: imgInfo.width,
                        height: imgInfo.height
                    });
                }
                catch (error) {
                    console.error('[ImageUploader] 获取图片信息失败:', error);
                    newImages.push({
                        id: `img_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
                        path: tempFilePath,
                        size: 0
                    });
                }
            }
            onChange([...images, ...newImages]);
            console.log('[ImageUploader] 已选择图片:', newImages.length);
        }
        catch (error) {
            console.error('[ImageUploader] 选择图片失败:', error);
            if (error.errMsg !== 'chooseImage:fail cancel') {
                taro_1.default.showToast({
                    title: '选择图片失败',
                    icon: 'none'
                });
            }
        }
    }, [images, maxCount, disabled, onChange]);
    const handleRemoveImage = (0, react_1.useCallback)((index) => {
        if (disabled)
            return;
        taro_1.default.showModal({
            title: '确认删除',
            content: '确定要删除这张图片吗？',
            success: (res) => {
                if (res.confirm) {
                    const newImages = [...images];
                    newImages.splice(index, 1);
                    onChange(newImages);
                }
            }
        });
    }, [images, disabled, onChange]);
    const handlePreviewImage = (0, react_1.useCallback)((index) => {
        const urls = images.map(img => img.path);
        taro_1.default.previewImage({
            current: urls[index],
            urls
        });
    }, [images]);
    const formatFileSize = (bytes) => {
        if (bytes < 1024)
            return bytes + ' B';
        if (bytes < 1024 * 1024)
            return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    };
    return ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.imageUploader, children: [(0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.imageList, children: [images.map((image, index) => ((0, jsx_runtime_1.jsxs)(components_1.View, { className: index_module_scss_1.default.imageItem, children: [(0, jsx_runtime_1.jsx)(components_1.Image, { className: index_module_scss_1.default.image, src: image.path, mode: "aspectFill", onClick: () => handlePreviewImage(index) }), !disabled && ((0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.removeBtn, onClick: () => handleRemoveImage(index), children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.removeIcon, children: "\u00D7" }) })), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.imageInfo, children: (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.imageSize, children: formatFileSize(image.size) }) })] }, image.id))), images.length < maxCount && !disabled && ((0, jsx_runtime_1.jsxs)(components_1.View, { className: (0, classnames_1.default)(index_module_scss_1.default.imageItem, index_module_scss_1.default.addBtn), onClick: handleChooseImage, children: [(0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.addIcon, children: "+" }), (0, jsx_runtime_1.jsx)(components_1.Text, { className: index_module_scss_1.default.addText, children: "\u6DFB\u52A0\u56FE\u7247" })] }))] }), (0, jsx_runtime_1.jsx)(components_1.View, { className: index_module_scss_1.default.uploaderTip, children: (0, jsx_runtime_1.jsxs)(components_1.Text, { className: index_module_scss_1.default.tipText, children: ["\u5DF2\u9009 ", images.length, "/", maxCount, " \u5F20\u56FE\u7247\uFF08\u652F\u6301\u62CD\u7167\u548C\u76F8\u518C\u9009\u62E9\uFF09"] }) })] }));
};
exports.default = ImageUploader;
//# sourceMappingURL=index.js.map