"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.generateImageId = exports.generateClientId = exports.generateUUID = void 0;
const generateUUID = () => {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
};
exports.generateUUID = generateUUID;
const generateClientId = () => {
    const timestamp = Date.now().toString(36);
    const random = Math.random().toString(36).substring(2, 10);
    return `evt_${timestamp}_${random}`;
};
exports.generateClientId = generateClientId;
const generateImageId = () => {
    const timestamp = Date.now().toString(36);
    const random = Math.random().toString(36).substring(2, 8);
    return `img_${timestamp}_${random}`;
};
exports.generateImageId = generateImageId;
//# sourceMappingURL=uuid.js.map